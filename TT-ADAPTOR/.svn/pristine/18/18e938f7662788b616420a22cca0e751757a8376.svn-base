package com.tts.mde.fwdc.ykb;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.service.IMDEmbeddedAdapter;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IMarketDataHandler;
import com.tts.mde.support.IPublishingEndpoint;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.vo.ISessionInfo;
import com.tts.mde.vo.SubscriptionWithSourceVo;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve.Builder;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.ForwardCurveBuilderFullScanStructureDifferentiator;
import com.tts.service.db.RuntimeDataService;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.SysProperty;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.util.flag.RateChangeIndFlag;
import com.tts.util.flag.RateChangeIndFlag.Change;
import com.tts.vo.TenorVo;

public class YkbWebServiceBasedForwardCurveMarketDataHandler implements IMarketDataHandler, IMDEmbeddedAdapter {
	private static final long REFRESH_RATE = 30000L;

	private final static Logger logger = LoggerFactory.getLogger(YkbWebServiceBasedForwardCurveMarketDataHandler.class);

	private final IFxCalendarBizServiceApi fxCalendarBizService;
	private final ISymbolIdMapper symbolIdMapper;
	private final ForwardCurve.Builder[] fwdCurves;
	private final PriceStructureRepo<Builder> structureRepo;
	private final ForwardCurveBuilderFullScanStructureDifferentiator differentiator;
	private final List<String> interestedSymbol;
	private final YkbForwardCurveRunnableV2 runnable;
	private final AtomicBoolean publishing = new AtomicBoolean(false);
	private final long tradingSessionId;
	private final ISchedulingWorker schedulingWorker;

	private volatile ScheduledFuture<?> futureTask;
	private int publishCounter = 0;

	public YkbWebServiceBasedForwardCurveMarketDataHandler(
			ISessionInfo sessionInfo, ISchedulingWorker schedulingWorker) {
    	this.symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
    	this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizServiceApi.class);

		String[] symbols = symbolIdMapper.getSymbols().toArray(new String[0]);
		int numberOfInstruments = Math.max(symbols.length, symbolIdMapper.getSymbols().size());
		
		fwdCurves = new ForwardCurve.Builder[numberOfInstruments];
		for ( String symbol: symbols) {
			int pos = symbolIdMapper.map(symbol);
			ForwardCurve.Builder fc = ForwardCurve.newBuilder();
			fc.setSymbol(symbol);
			fc.setIndicativeFlag(IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE, IndicativeReason.MA_NoData));
			fc.setLatency(Latency.newBuilder());
			fwdCurves[pos] = fc;
		}
		
		Long _priceInjectionInterval = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.CONSTANTS, SysProperty.Key1.INJECT_INTERVAL, null);
		long validIntervalInTicks = REFRESH_RATE / _priceInjectionInterval;
		
		this.schedulingWorker = schedulingWorker;
		this.interestedSymbol = Collections.unmodifiableList(Arrays.asList(symbols));
		this.structureRepo = new PriceStructureRepo<ForwardCurve.Builder>(symbolIdMapper);
		this.differentiator = new ForwardCurveBuilderFullScanStructureDifferentiator();
		
		this.runnable= new YkbForwardCurveRunnableV2(interestedSymbol, symbolIdMapper, fwdCurves, validIntervalInTicks);
		this.tradingSessionId = sessionInfo.getTradingSessionId();
	}

	public String getName() {
		return "YKBWebServiceFcAdap";
	}

	/* (non-Javadoc)
	 * @see com.tts.mde.fwdc.ykb.IMarketDataHandler#atPublish(long, com.tts.mde.support.IPublishingEndpoint)
	 */
	@Override
	public void atPublish(long masGlobalSeq, IPublishingEndpoint publishingEndpoint) {
		ForwardCurve.Builder[] publishingCopy = Arrays.copyOf(fwdCurves, fwdCurves.length, ForwardCurve.Builder[].class);
		if ( publishing.compareAndSet(false, true)) {
			try {	
				int totalTenorsProcessed = 0;
				long startTime = System.currentTimeMillis();
				for ( int i = 0; i < publishingCopy.length; i++ ) {
					ForwardCurve.Builder forwardCurveBuilder = publishingCopy[i];
					if ( forwardCurveBuilder != null ) {
						final String symbol = forwardCurveBuilder.getSymbol();
		
						final long sentTime = System.currentTimeMillis();

						String topic = "TTS.MD.FX.MR.FWDC." + symbol;
		
						String tradeDateStr = fxCalendarBizService.getCurrentBusinessDay(symbol);
						forwardCurveBuilder.setTradeDate(tradeDateStr);
						
					
						LocalDate tradeDate =  ChronologyUtil.getLocalDateFromString(tradeDateStr);
						int tenorCount = forwardCurveBuilder.getTenorsCount();
		
						for ( int i1 = 0; tradeDate != null && i1 < tenorCount; i1++ ) {
							Tenor.Builder tenorBuilder = forwardCurveBuilder.getTenorsBuilder(i1);
							tenorBuilder.setActualDate(fxCalendarBizService.getForwardValueDate(symbol, tenorBuilder.getName()));
						}
						totalTenorsProcessed += tenorCount;
						
						forwardCurveBuilder.setSpotDate(fxCalendarBizService.getForwardValueDate(symbol, TenorVo.NOTATION_SPOT));
						forwardCurveBuilder.setTradingSession(tradingSessionId);
						
						boolean isStructureChanged = structureRepo.hasStructureChanged(symbol, differentiator, forwardCurveBuilder);
						int rateChangeIndFlag = RateChangeIndFlag.NO_CHANGE;
						if ( isStructureChanged ) {
							rateChangeIndFlag = RateChangeIndFlag.setChanged(rateChangeIndFlag, Change.Structure);
						}
						forwardCurveBuilder.setRateChangeInd(rateChangeIndFlag);
						forwardCurveBuilder.setIndicativeFlag(0L);
						forwardCurveBuilder.setIndicativeSubFlag(0L);
						forwardCurveBuilder.getLatencyBuilder().setFaSendTimestamp(sentTime);
						forwardCurveBuilder.setSequence(masGlobalSeq);
						
						publishingEndpoint.publish(topic, forwardCurveBuilder.build());
					}
				}
				publishCounter++;
				long endTime = System.currentTimeMillis();

				if ( (publishCounter % 128) == 0) {
					logger.debug("Completed publishing forward curve, took:" + (endTime - startTime) + ", totalTenorProcessed:" + totalTenorsProcessed);
				}
			} finally {
				publishing.set(false);
			}
		} else {
			logger.debug("Previous publishing forward curve still running, skipping...");
		}
	}


	public void init() {
		start();
	}

	public void start() {
		ScheduledFuture<?>  f = this.schedulingWorker.scheduleAtFixedRate(runnable, REFRESH_RATE);
		this.futureTask = f;
		
	}

	public void stop() {
		if ( this.futureTask != null ) {
			this.futureTask.cancel(true);	
		}
		
		this.futureTask = null;
	}

	@Override
	public void destroy() {
		stop();
	}

	@Override
	public IMarketDataHandler addSubscriptions(List<SubscriptionWithSourceVo> list) {
		return this;
	}

}
