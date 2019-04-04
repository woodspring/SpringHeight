package com.tts.plugin.adapter.impl.base;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve.Builder;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.ForwardCurveBuilderFullScanStructureDifferentiator;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.util.flag.RateChangeIndFlag;
import com.tts.util.flag.RateChangeIndFlag.Change;
import com.tts.vo.TenorVo;

public class YkbWebServiceBasedForwardCurveAdapterImpl extends AbstractPublishingApp {
	private final static Logger logger = LoggerFactory.getLogger(YkbWebServiceBasedForwardCurveAdapterImpl.class);

	private final FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
	private final IFxCalendarBizService fxCalendarBizService;
	private final ISymbolIdMapper symbolIdMapper;
	private final ForwardCurve.Builder[] fwdCurves;
	private final PriceStructureRepo<Builder> structureRepo;
	private final ForwardCurveBuilderFullScanStructureDifferentiator differentiator;
	private final List<String> interestedSymbol;
	private final YkbForwardCurveRunnable runnable;
	private final AtomicBoolean publishing = new AtomicBoolean(false);
	private volatile ScheduledFuture<?> futureTask;
	private int publishCounter = 0;

	public YkbWebServiceBasedForwardCurveAdapterImpl(IMkQfixApp qfixApp, ISchedulingWorker worker,
			SessionInfo sessionInfo, IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint, iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
    	this.symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
    	this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);

		String[] symbols = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.FCADAPTER.getPublishingFormatType().toString());
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
		
		this.interestedSymbol = Collections.unmodifiableList(Arrays.asList(symbols));
		this.structureRepo = new PriceStructureRepo<ForwardCurve.Builder>(symbolIdMapper);
		this.differentiator = new ForwardCurveBuilderFullScanStructureDifferentiator();
		this.runnable= new YkbForwardCurveRunnable(interestedSymbol, symbolIdMapper, fwdCurves);
	}

	@Override
	public String getName() {
		return "YKBWebServiceFcAdap";
	}

	@Override
	public void atPublish(long masGlobalSeq) {
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
						final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(
								AppType.FCADAPTER.getPublishingFormatType().toString(), symbol);
						if ( individualInfo == null  ) {
							continue;
						}
						String topic = individualInfo.getTopic();
		
						if ( topic == null ) {
							// Market Structure is NOT defined
							continue;
						}
						final boolean tradeDateChanged = individualInfo.isTradeDateChanged();
						final String tradeDateStr =  individualInfo.getTradeDateString();
		
						long indicativeFlag = individualInfo.getIndicativeFlag();
						long indicativeSubStatus = individualInfo.getIndicativeSubStatus();
						if ( tradeDateStr == null ) {
							indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
									IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
							indicativeSubStatus =IndicativeFlag.setIndicativeSubStatus(
									indicativeSubStatus, 
									IndicativeFlag.IndicativeSubStatus.CONFIGURATION_TradeDateNotDefined);
						} else {
							forwardCurveBuilder.setTradeDate(tradeDateStr );
						}
						
						LocalDate tradeDate =  individualInfo.getTradeDate();
						int tenorCount = forwardCurveBuilder.getTenorsCount();
		
						for ( int i1 = 0; tradeDate != null && i1 < tenorCount; i1++ ) {
							Tenor.Builder tenorBuilder = forwardCurveBuilder.getTenorsBuilder(i1);
		
							if ( tradeDateChanged || !tenorBuilder.hasActualDate() ) {
								TenorVo t = TenorVo.fromString(tenorBuilder.getName());
								LocalDate valueDate = getFxCalendarBizService().getForwardValueDate(symbol, tradeDate, t.getPeriodCd(), t.getValue(), PricingConventionConstants.INTERBANK);
								tenorBuilder.setActualDate(ChronologyUtil.getDateString(valueDate));
							} 
						}
						totalTenorsProcessed += tenorCount;
						
						forwardCurveBuilder.setTradingSession(getSessionInfo().getTradingSessionId());
						
						long lastRefresh = individualInfo.getLastRefresh();
						long receiveTimestamp = forwardCurveBuilder.getLatencyBuilder().getFaReceiveTimestamp();
						boolean isReceiveRefresh = (receiveTimestamp - lastRefresh) > -1;
						if (!isReceiveRefresh && System.currentTimeMillis() > lastRefresh + getSessionInfo().getTimeoutInterval()) {
							indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
									IndicativeFlag.IndicativeReason.MA_NoData);
						}
						boolean isStructureChanged = structureRepo.hasStructureChanged(symbol, differentiator, forwardCurveBuilder);
						int rateChangeIndFlag = RateChangeIndFlag.NO_CHANGE;
						if ( isStructureChanged ) {
							rateChangeIndFlag = RateChangeIndFlag.setChanged(rateChangeIndFlag, Change.Structure);
						}
						forwardCurveBuilder.setRateChangeInd(rateChangeIndFlag);
						forwardCurveBuilder.setIndicativeFlag(indicativeFlag);
						forwardCurveBuilder.setIndicativeSubFlag(indicativeSubStatus);
						forwardCurveBuilder.getLatencyBuilder().setFaSendTimestamp(sentTime);
						forwardCurveBuilder.setSequence(masGlobalSeq);
						
						getPublishingEndpoint().publish(topic, forwardCurveBuilder.build());
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

	private IFxCalendarBizService getFxCalendarBizService() {
		return this.fxCalendarBizService;
	}

	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxForwards;
	}

	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}

	@Override
	public void init() {
		start();
	}

	@Override
	public void start() {
		ScheduledFuture<?> f = getWorker().scheduleAtFixedRate(runnable, p.getProperty("fwdadapter.refresh_interval", 30L));
		this.futureTask = f;
	}

	@Override
	public void stop() {
		this.futureTask.cancel(true);
	}

}
