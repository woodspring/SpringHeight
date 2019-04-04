package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class BestPriceRfsSubscriptionHandler extends OndemandMrSubscriptionHandler implements IMrSubscriptionHandler {

	public BestPriceRfsSubscriptionHandler(String handlerId, IMrEndpoint mrEndpoint,
			MrSubscriptionProperties properties, ILiquidityPool lp, IFxCalendarBizServiceApi fxCalendarBizService,
			ISessionInfo sessionInfo, IInstrumentDetailProp iInstrumentDetailProp) {
		super(handlerId, mrEndpoint, properties, lp, fxCalendarBizService, sessionInfo, iInstrumentDetailProp, null, false);
	}

	@Override
	public void onPublish(long masGlobalSeq) {
		RawLiquidityVo[] _bid = getLiquidityPool().getBidLqy(true);
		RawLiquidityVo[] _ask = getLiquidityPool().getAskLqy(true);
		

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


			
		FullBook.Builder vwapBook = FullBook.newBuilder(); 
		String bidFwdPts = null, askFwdPts = null;
		if ( bid != null ) {
			int level = 1;
			int thisLevel = level++;
		
			for ( int i = 0; i < bid.size(); i++ ){
				RawLiquidityVo rawQ = bid.get(i); 
			
				Tick.Builder bidTick = Tick.newBuilder();
				bidTick.setLevel(thisLevel);
				bidTick.setSize(rawQ.getSize());
				bidTick.setSourceNm(rawQ.getLiquidityProviderSrc());
				String rate = DoubleFormatter.convertToString(rawQ.getRate(), instrumentPrecision, RoundingMode.FLOOR);
				bidTick.setRate(rate);
				String spotRate = DoubleFormatter.convertToString(rawQ.getSpotRate(), instrumentPrecision, RoundingMode.FLOOR);
				bidTick.setSpotRate(spotRate);
				bidFwdPts = DoubleFormatter.convertToString(rawQ.getForwardPts(), instrumentPrecision, RoundingMode.FLOOR);
				vwapBook.addBidTicks(bidTick);
			}
		}
		if ( ask != null ) {
			int level = 1;
			int thisLevel = level++;
		
			for ( int i = 0; i < ask.size(); i++ ){
				RawLiquidityVo rawQ = ask.get(i); 
			
				Tick.Builder askTick = Tick.newBuilder();
				askTick.setLevel(thisLevel);
				askTick.setSize(rawQ.getSize());
				askTick.setSourceNm(rawQ.getLiquidityProviderSrc());

				String bidRate = DoubleFormatter.convertToString(rawQ.getRate(), instrumentPrecision, RoundingMode.CEILING);
				askTick.setRate(bidRate);
				String bidSpotRate = DoubleFormatter.convertToString(rawQ.getSpotRate(), instrumentPrecision, RoundingMode.CEILING);
				askTick.setSpotRate(bidSpotRate);
				askFwdPts = DoubleFormatter.convertToString(rawQ.getForwardPts(), instrumentPrecision, RoundingMode.CEILING);
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
		vwapBook.setQuoteRefId(Long.toString(masGlobalSeq));
		vwapBook.setTopOfBookIdx(0);
		vwapBook.setTradingSession(getSessionInfo().getTradingSessionId());
		vwapBook.setTradeDate(getFxCalendarBizService().getCurrentBusinessDay(getProperties().getSymbol()));
		vwapBook.setSpotValueDate(getFxCalendarBizService().getForwardValueDate(getProperties().getSymbol(), TenorVo.NOTATION_SPOT));
		vwapBook.setRateChangeInd(0);
		getMrEndpoint().publish(OutboundType.RAW, getProperties(), vwapBook);
		
	}

	@Override
	public void onOrderOrHedgingRequest() {
		// TODO Auto-generated method stub
		
	}

}
