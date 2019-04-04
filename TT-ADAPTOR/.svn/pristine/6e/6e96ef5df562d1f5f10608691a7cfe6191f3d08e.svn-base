package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.algo.IMDPriceAndExceAlgo;
import com.tts.mde.algo.IMDPriceAndExceAlgo.BuySellActionCd;
import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.ILiquidityPool.QuoteUpdateStatVo;
import com.tts.mde.spot.IMrEndpoint;
import com.tts.mde.spot.IMrEndpoint.OutboundType;
import com.tts.mde.spot.IMrSubscriptionHandler;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IInstrumentDetailProp;
import com.tts.mde.vo.ISessionInfo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.ProviderStat;
import com.tts.message.market.FullBookStruct.ProvidersStat;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.util.flag.IndicativeFlag;
import com.tts.vo.TenorVo;

public class AutoMrSubscriptionHandler extends AbstractMrSubscriptionHandler implements IMrSubscriptionHandler {
	private final static Logger logger = LoggerFactory.getLogger(AutoMrSubscriptionHandler.class);
	private final IFxCalendarBizServiceApi fxCalendarBizService;
	private final ISessionInfo sessionInfo;
	private final int instrumentPrecision;
	private final IMDPriceAndExceAlgo aggAlgo;
	private final String qualifierNm;
	private final boolean debug;

	public AutoMrSubscriptionHandler(String qualifierNm, String handlerId, IMrEndpoint mrEndpoint, MrSubscriptionProperties properties, ILiquidityPool lp,
			IFxCalendarBizServiceApi fxCalendarBizService, ISessionInfo sessionInfo, IInstrumentDetailProp iInstrumentDetailProp, IMDPriceAndExceAlgo aggAlgo) {
		super(handlerId, mrEndpoint, properties, lp);

		boolean injectionDebug = false;
		String injectionDebugStr = System.getProperty("injection.debug");
		if (injectionDebugStr != null && !injectionDebugStr.isEmpty()) {
			injectionDebug = Boolean.parseBoolean(injectionDebugStr);
		}

		this.fxCalendarBizService = fxCalendarBizService;
		this.sessionInfo = sessionInfo;
		this.instrumentPrecision = iInstrumentDetailProp.getPrecision();
		this.aggAlgo = aggAlgo;
		this.qualifierNm = qualifierNm;
		this.debug = injectionDebug && "EURUSD".equals(properties.getSymbol());
	}

	@Override
	public void onPublish(long masGlobalSeq) {
		if (debug) {
			logger.debug("AutoMrSubscriptionHandler publishing start " + masGlobalSeq);
		}
		RawLiquidityVo[] _bid = getLiquidityPool().getBidLqy(false);
		RawLiquidityVo[] _ask = getLiquidityPool().getAskLqy(false);
		List<QuoteUpdateStatVo> stats = getLiquidityPool().getQuoteUpdateCount();
		List<RawLiquidityVo> bid = _bid == null ? Collections.emptyList() : new ArrayList<>(_bid.length);
		List<RawLiquidityVo> ask = _ask == null ? Collections.emptyList() : new ArrayList<>(_ask.length);
		long receiveTime = -1;

		if (_bid != null) {
			for (int i = 0; i < _bid.length; i++) {
				if (_bid[i] != null && _bid[i].isValid()) {
					bid.add(_bid[i]);
					if (_bid[i].getReceivedTime() > receiveTime) {
						receiveTime = _bid[i].getReceivedTime();
					}
				}
			}
		}

		if (_ask != null) {
			for (int i = 0; i < _ask.length; i++) {
				if (_ask[i] != null && _ask[i].isValid()) {
					ask.add(_ask[i]);
					if (_ask[i].getReceivedTime() > receiveTime) {
						receiveTime = _ask[i].getReceivedTime();
					}
				}
			}
		}

		VwapByPriceAggressive.sortTickByPrice(bid, true);
		VwapByPriceAggressive.sortTickByPrice(ask, false);

		long updateTimestamp = System.currentTimeMillis();
		FullBook.Builder rawBook = createRawBookFromRawLqy(bid, ask, instrumentPrecision);
		rawBook.setSymbol(getProperties().getSymbol());
		rawBook.setSequence(masGlobalSeq);
		rawBook.setSymbol(getProperties().getSymbol());
		rawBook.setSequence(masGlobalSeq);
		rawBook.getLatencyBuilder().setFaReceiveTimestamp(receiveTime);
		rawBook.getLatencyBuilder().setFaSendTimestamp(updateTimestamp);
		rawBook.setUpdateTimestamp(updateTimestamp);
		rawBook.setTradingSession(this.sessionInfo.getTradingSessionId());
		rawBook.setRateChangeInd(0);
		ProvidersStat.Builder pss = ProvidersStat.newBuilder(); 
		for ( QuoteUpdateStatVo stat : stats ) {
			pss.addProviderStat(ProviderStat.newBuilder().setProvider(stat.getProviderNm()).setTotalUpdates(stat.getCount()));
		}
		rawBook.setProvidersStat(pss);
		getMrEndpoint().publish(OutboundType.RAW, getProperties(), rawBook);

		// ~~~~~~ CONSOLIDATE RAW BOOK ~~~~~~
		try {
			FullBook.Builder consolidateBook = consolidateLiquidityVo(bid, ask);
			consolidateBook.setSymbol(getProperties().getSymbol());
			consolidateBook.setSequence(masGlobalSeq);
			consolidateBook.getLatencyBuilder().setFaReceiveTimestamp(receiveTime);
			consolidateBook.getLatencyBuilder().setFaSendTimestamp(updateTimestamp);
			consolidateBook.setUpdateTimestamp(updateTimestamp);
			consolidateBook.setTradingSession(this.sessionInfo.getTradingSessionId());
			consolidateBook.setRateChangeInd(0);
			getMrEndpoint().publish(OutboundType.CONSOLIDATED, getProperties(), consolidateBook);
		} catch (Exception e) {
			logger.error("[onPublish] Failed to consolidate bid and ask ticks", e);
		}
		// ~~~~~~ CONSOLIDATE RAW BOOK ~~~~~~

		long[] outLqyRungs = getProperties().getSize();

		FullBook.Builder vwapBook = FullBook.newBuilder();
		int level = 1;
		for (long outLqysize : outLqyRungs) {
			int thisLevel = level++;

			com.tts.mde.vo.AggPxVo bidP = null;
			com.tts.mde.vo.AggPxVo askP = null;
			try {
				bidP = aggAlgo.getVwapPrice(bid, BuySellActionCd.SELL, outLqysize, instrumentPrecision, sessionInfo.getMarketMode());
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				askP = aggAlgo.getVwapPrice(ask, BuySellActionCd.BUY, outLqysize,  instrumentPrecision,  sessionInfo.getMarketMode());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (bidP != null && askP != null) {
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
		vwapBook.getLatencyBuilder().setFaSendTimestamp(updateTimestamp);
		vwapBook.setUpdateTimestamp(updateTimestamp);
		vwapBook.setIndicativeFlag(IndicativeFlag.TRADABLE);
		vwapBook.setIndicativeSubFlag(IndicativeFlag.TRADABLE);
		vwapBook.setQuoteRefId(Long.toString(masGlobalSeq));
		vwapBook.setTopOfBookIdx(0);
		vwapBook.setTradingSession(this.sessionInfo.getTradingSessionId());
		vwapBook.setTradeDate(fxCalendarBizService.getCurrentBusinessDay(getProperties().getSymbol()));
		vwapBook.setSpotValueDate(fxCalendarBizService.getForwardValueDate(getProperties().getSymbol(), TenorVo.NOTATION_SPOT));
		vwapBook.setRateChangeInd(0);
		vwapBook.setQualifier(qualifierNm);
		getMrEndpoint().publish(OutboundType.VWAP, getProperties(), vwapBook);

		if (debug) {
			logger.debug("AutoMrSubscriptionHandler publishing completed " + masGlobalSeq);
		}
	}

	static FullBook.Builder consolidateLiquidityVo(List<RawLiquidityVo> bidList, List<RawLiquidityVo> askList) {
		FullBook.Builder builder = FullBook.newBuilder();

		assert (bidList != null);
		if (!bidList.isEmpty()) {
			double prevBidRate = -1;
			Tick.Builder bidTickBuilder = null;
			Set<String> bidLpNames = new HashSet<>();
			for (RawLiquidityVo vo : bidList) {
				assert (vo.getRate() > 0);
				if (vo.getRate() != prevBidRate) {
					if (bidTickBuilder != null) {
						builder.addBidTicks(bidTickBuilder.setSourceNm(bidLpNames.toString()).build());
						bidLpNames.clear();
					}
					bidTickBuilder = Tick.newBuilder();
					bidTickBuilder.setRate(String.valueOf(vo.getRate()));
					bidLpNames.add(vo.getLiquidityProviderSrc());
					bidTickBuilder.setSize(vo.getSize());
				} else {
					assert bidTickBuilder != null;
					bidLpNames.add(vo.getLiquidityProviderSrc());
					bidTickBuilder.setSize(bidTickBuilder.getSize() + vo.getSize());
				}
				prevBidRate = vo.getRate();
			}
			assert bidTickBuilder != null;
			builder.addBidTicks(bidTickBuilder.setSourceNm(bidLpNames.toString()).build());
			bidLpNames.clear();
		}

		assert (askList != null);
		if (!askList.isEmpty()) {
			double prevAskRate = -1;
			Tick.Builder askTickBuilder = null;
			Set<String> askLpNames = new HashSet<>();
			for (RawLiquidityVo vo : askList) {
				if (vo.getRate() != prevAskRate) {
					assert (vo.getRate() > 0);
					if (askTickBuilder != null) {
						builder.addAskTicks(askTickBuilder.setSourceNm(askLpNames.toString()).build());
						askLpNames.clear();
					}
					askTickBuilder = Tick.newBuilder();
					askTickBuilder.setRate(String.valueOf(vo.getRate()));
					askLpNames.add(vo.getLiquidityProviderSrc());
					askTickBuilder.setSize(vo.getSize());
				} else {
					assert askTickBuilder != null;
					askLpNames.add(vo.getLiquidityProviderSrc());
					askTickBuilder.setSize(askTickBuilder.getSize() + vo.getSize());
				}
				prevAskRate = vo.getRate();
			}
			assert askTickBuilder != null;
			builder.addAskTicks(askTickBuilder.setSourceNm(askLpNames.toString()).build());
			askLpNames.clear();
		}

		return builder;
	}

	@Override
	public void onOrderOrHedgingRequest() {
		// TODO Auto-generated method stub

	}

	@Override
	public IMDPriceAndExceAlgo getMdeAggAlgo() {
		return this.aggAlgo;
	}

}
