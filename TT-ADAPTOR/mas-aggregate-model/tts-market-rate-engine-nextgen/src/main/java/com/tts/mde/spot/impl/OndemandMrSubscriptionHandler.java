package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.algo.IMDPriceAndExceAlgo;
import com.tts.mde.algo.IMDPriceAndExceAlgo.BuySellActionCd;
import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.IMrEndpoint;
import com.tts.mde.spot.IMrEndpoint.OutboundType;
import com.tts.mde.spot.IMrSubscriptionHandler;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IInstrumentDetailProp;
import com.tts.mde.vo.ISessionInfo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.util.flag.IndicativeFlag;
import com.tts.vo.TenorVo;

public class OndemandMrSubscriptionHandler extends AbstractMrSubscriptionHandler implements IMrSubscriptionHandler {
	private final static Logger logger = LoggerFactory.getLogger(OndemandMrSubscriptionHandler.class);
	private final IFxCalendarBizServiceApi fxCalendarBizService;
	private final ISessionInfo sessionInfo;
	private final MrSubscriptionProperties mrSubscriptionProperties;
	private final IMDPriceAndExceAlgo mdAggAlgo;
	protected final int instrumentPrecision;
	private volatile boolean isFirstPriceSent = false;
	private boolean isSpecificLpSubscription;
	
	public OndemandMrSubscriptionHandler(
			String handlerId,
			IMrEndpoint mrEndpoint, 
			MrSubscriptionProperties properties,  
			ILiquidityPool lp,
			IFxCalendarBizServiceApi fxCalendarBizService,
			ISessionInfo sessionInfo, 
			IInstrumentDetailProp iInstrumentDetailProp,
			IMDPriceAndExceAlgo mdAggAlgo,
			boolean isSpecificLpSubscription) {
		super(handlerId, mrEndpoint, properties, lp);
		this.fxCalendarBizService = fxCalendarBizService;
		this.sessionInfo = sessionInfo;
		this.instrumentPrecision = iInstrumentDetailProp.getPrecision();
		this.mrSubscriptionProperties = properties;
		this.mdAggAlgo = mdAggAlgo;
		this.isSpecificLpSubscription = isSpecificLpSubscription;
	}

	@Override
	public void onPublish(long masGlobalSeq) {
		if ( !isFirstPriceSent ) {
			logger.debug("about to send the first price for " + mrSubscriptionProperties.getHandlerId());
		}
		RawLiquidityVo[] _bid = getLiquidityPool().getBidLqy(isSpecificLpSubscription);
		RawLiquidityVo[] _ask = getLiquidityPool().getAskLqy(isSpecificLpSubscription);

		List<RawLiquidityVo> bid = _bid == null ? Collections.emptyList() : new ArrayList<>(_bid.length);
		List<RawLiquidityVo> ask = _ask == null ? Collections.emptyList() : new ArrayList<>(_ask.length);
		long receiveTime = -1;

		if ( _bid != null ) {
			for ( int i = 0; i < _bid.length; i++ ){
				if ( _bid[i] != null && _bid[i].isValid()) {
					bid.add(_bid[i]);
					if ( _bid[i].getReceivedTime() > receiveTime ) {
						receiveTime = _bid[i].getReceivedTime() ;
					}
				}
			}
		}
		if ( _ask != null ) {
			for ( int i = 0; i < _ask.length; i++ ){
				if ( _ask[i] != null && _ask[i].isValid()) {
					ask.add(_ask[i]);
					if ( _ask[i].getReceivedTime() > receiveTime ) {
						receiveTime = _ask[i].getReceivedTime() ;
					}
				}
			}
		}
		VwapByPriceAggressive.sortTickByPrice(bid, true);
		VwapByPriceAggressive.sortTickByPrice(ask, false);

		if ( this.mrSubscriptionProperties.getInterestedOutboundTypes().contains(IMrEndpoint.OutboundType.RAW)) {
			FullBook.Builder rawBook = createRawBookFromRawLqy(bid, ask, instrumentPrecision);
			rawBook.setSymbol(getProperties().getSymbol());
			rawBook.setSequence(masGlobalSeq);
			rawBook.getLatencyBuilder().setFaReceiveTimestamp(receiveTime);
			rawBook.getLatencyBuilder().setFaSendTimestamp(System.currentTimeMillis());
			rawBook.setUpdateTimestamp(System.currentTimeMillis());
			rawBook.setIndicativeFlag(IndicativeFlag.TRADABLE);
			rawBook.setIndicativeSubFlag(IndicativeFlag.TRADABLE);
			getMrEndpoint().publish(OutboundType.RAW, getProperties(), rawBook);
			if ( !isFirstPriceSent ) {
				logger.debug("sent first RAW price for " + mrSubscriptionProperties.getHandlerId());
				isFirstPriceSent = true;
			}
		}
		
		// ~~~~~~ CONSOLIDATE RAW BOOK ~~~~~~
		if ( this.mrSubscriptionProperties.getInterestedOutboundTypes().contains(IMrEndpoint.OutboundType.CONSOLIDATED)) {
			try {
				FullBook.Builder consolidateBook = AutoMrSubscriptionHandler.consolidateLiquidityVo(bid, ask);
				consolidateBook.setSymbol(getProperties().getSymbol());
				consolidateBook.setSequence(masGlobalSeq);
				consolidateBook.getLatencyBuilder().setFaReceiveTimestamp(receiveTime);
				consolidateBook.getLatencyBuilder().setFaSendTimestamp(System.currentTimeMillis());
				consolidateBook.setUpdateTimestamp(System.currentTimeMillis());
				consolidateBook.setIndicativeFlag(IndicativeFlag.TRADABLE);
				consolidateBook.setIndicativeSubFlag(IndicativeFlag.TRADABLE);
				getMrEndpoint().publish(OutboundType.CONSOLIDATED, getProperties(), consolidateBook);
				if ( !isFirstPriceSent ) {
					logger.debug("sent first CONSOLIDATED price for " + mrSubscriptionProperties.getHandlerId());
					isFirstPriceSent = true;
				}
			} catch (Exception e) {
				logger.error("[onPublish] Failed to consolidate bid and ask ticks", e);
			}
		}
		// ~~~~~~ CONSOLIDATE RAW BOOK ~~~~~~
		
		if ( this.mrSubscriptionProperties.getInterestedOutboundTypes().contains(IMrEndpoint.OutboundType.VWAP)) {
			long[] outLqyRungs = getProperties().getSize();
			
			FullBook.Builder vwapBook = FullBook.newBuilder(); 
			int level = 1;
			com.tts.mde.vo.AggPxVo bidP = null;
			com.tts.mde.vo.AggPxVo askP = null;
			
			for ( long outLqysize : outLqyRungs) {
				int thisLevel = level++;
				bidP = mdAggAlgo.getVwapPrice(bid, BuySellActionCd.SELL, outLqysize, instrumentPrecision, sessionInfo.getMarketMode());
				askP = mdAggAlgo.getVwapPrice(ask, BuySellActionCd.BUY, outLqysize,  instrumentPrecision,  sessionInfo.getMarketMode());

				if ( bidP != null && askP != null ) {
					Tick.Builder bidTick = Tick.newBuilder();
					bidTick.setLevel(thisLevel);
					bidTick.setSize(outLqysize);
					String bidRate = DoubleFormatter.convertToString(bidP.getVwapPrice(), instrumentPrecision, RoundingMode.FLOOR);
					bidTick.setRate(bidRate);
					bidTick.setSpotRate(bidRate);
					vwapBook.addBidTicks(bidTick);
					
					Tick.Builder askTick = Tick.newBuilder();
					askTick.setLevel(thisLevel);
					askTick.setSize(outLqysize);
					String askRate = DoubleFormatter.convertToString(askP.getVwapPrice(), instrumentPrecision, RoundingMode.CEILING);
					askTick.setRate(askRate);
					askTick.setSpotRate(askRate);
					vwapBook.addAskTicks(askTick);
				}
			}
			vwapBook.setSymbol(getProperties().getSymbol());
			vwapBook.setSequence(masGlobalSeq);
			vwapBook.getLatencyBuilder().setFaReceiveTimestamp(receiveTime);
			vwapBook.getLatencyBuilder().setFaSendTimestamp(System.currentTimeMillis());
			vwapBook.setUpdateTimestamp(System.currentTimeMillis());
			vwapBook.setIndicativeFlag(IndicativeFlag.TRADABLE);
			vwapBook.setIndicativeSubFlag(IndicativeFlag.TRADABLE);
			vwapBook.setTopOfBookIdx(0);
			vwapBook.setTradingSession(this.getSessionInfo().getTradingSessionId());
			vwapBook.setTradeDate(getFxCalendarBizService().getCurrentBusinessDay(getProperties().getSymbol()));
			vwapBook.setSpotValueDate(getFxCalendarBizService().getForwardValueDate(getProperties().getSymbol(), TenorVo.NOTATION_SPOT));
			vwapBook.setRateChangeInd(0);
			getMrEndpoint().publish(OutboundType.VWAP, getProperties(), vwapBook);
			if ( !isFirstPriceSent ) {
				logger.debug("sent first VWAP price for " + mrSubscriptionProperties.getHandlerId());
				isFirstPriceSent = true;
			}
		}
	}

	@Override
	public void onOrderOrHedgingRequest() {
		// TODO Auto-generated method stub
		
	}

	public ISessionInfo getSessionInfo() {
		return sessionInfo;
	}

	public IFxCalendarBizServiceApi getFxCalendarBizService() {
		return fxCalendarBizService;
	}

	@Override
	public IMDPriceAndExceAlgo getMdeAggAlgo() {
		return mdAggAlgo;
	}

}
