package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.MarketDatasetVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.ForwardCurveBuilderFullScanStructureDifferentiator;
import com.tts.service.biz.price.structure.diff.full.IFullScanStructureDifferentiator;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.CollectionUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.RateChangeIndFlag;
import com.tts.util.flag.RateChangeIndFlag.Change;
import com.tts.vo.TenorVo;

public class DefaultEspOnlyFwdAdapterAppImpl extends AbstractPublishingApp implements IEspPriceRepoDependent {
	public final static String NAME_TTS_FWD_ADAPTER = "FIX_ADAPTER_FWD";
	
//	private final FbPricePublishHandler pricePublishHandler;
//	private final MasterPriceStore<ForwardCurve.Builder> priceStores;
	
	private final ISymbolIdMapper symbolIdMapper;
	private final IFxCalendarBizService fxCalendarBizService;
		
	private volatile ForwardCurve.Builder[] fwdCurves = null;
	private final MasterPriceStore<ForwardCurve.Builder> priceStores;
	private final PriceStructureRepo<ForwardCurve.Builder> structureRepo;
	private final IFullScanStructureDifferentiator<ForwardCurve.Builder> differentiator;
	private volatile RefreshRequesterFwd requester;
	private IEspRepo<?> espRepo;
	
	public DefaultEspOnlyFwdAdapterAppImpl(
			IMkQfixApp qfixApp,
			ISchedulingWorker worker, 
			SessionInfo sessionInfo, 
			IPublishingEndpoint iPublishingEndpoint, 
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint, 
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) throws Exception {
		super(
				qfixApp, 
				worker, 
				sessionInfo, 
				iPublishingEndpoint, 
				iCertifiedPublishingEndpoint,
				IFixIntegrationPluginSpi
		);
		
	
    	this.symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
    	this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
		
		String[] symbols = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.FCADAPTER.getPublishingFormatType().toString());
		int numberOfInstruments = Math.max(symbols.length, symbolIdMapper.getSymbols().size());
		//this.marketDataset = sessionInfo.getMarketDataset();
		ForwardCurve.Builder[] fwdCurves = new ForwardCurve.Builder[numberOfInstruments];
		for ( String symbol: symbols) {
			int pos = symbolIdMapper.map(symbol);
			ForwardCurve.Builder fc = ForwardCurve.newBuilder();
			fc.setSymbol(symbol);
			fwdCurves[pos] = fc;
		}
		this.fwdCurves = fwdCurves;
			
		this.priceStores = new MasterPriceStore<ForwardCurve.Builder>(symbols, AppType.FCADAPTER);
		this.structureRepo = new PriceStructureRepo<ForwardCurve.Builder>(symbolIdMapper);
		this.differentiator = new ForwardCurveBuilderFullScanStructureDifferentiator();
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}
	
	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxForwards;
	}
	
	@Override
	public String getName() {
		return NAME_TTS_FWD_ADAPTER;
	}

	@Override
	public void atPublish(long masGlobalSeq) {
		
		for ( int i = 0; i < fwdCurves.length; i++ ) {
			ForwardCurve.Builder forwardCurveBuilder = fwdCurves[i];
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
				final Set<String> expectedTenors = CollectionUtil.transformArrayToSet(individualInfo.getTenors());
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
				int tickCount = forwardCurveBuilder.getTenorsCount();
				for ( int i1 = 0; tradeDate != null && i1 < tickCount; i1++ ) {
					Tenor.Builder tenorBuilder = forwardCurveBuilder.getTenorsBuilder(i1);

					if ( tradeDateChanged || !tenorBuilder.hasActualDate() ) {
						String[] nmSplit1 = tenorBuilder.getName().split("[^\\d]");
						String[] nmSplit2 = tenorBuilder.getName().split("[\\d]");
						Long periodValue = Long.valueOf(nmSplit1.length > 0 ? nmSplit1[0] : "-1");
						String periodCode = nmSplit2.length > 0 ? nmSplit2[nmSplit2.length - 1] : "";
						LocalDate valueDate = fxCalendarBizService.getForwardValueDate(symbol, tradeDate, periodCode, periodValue, PricingConventionConstants.INTERBANK);
						tenorBuilder.setActualDate(ChronologyUtil.getDateString(valueDate));
					} else if ( tenorBuilder.hasActualDate() && !tenorBuilder.hasName()) {
						TenorVo[] tenors = findTenors(symbol, tradeDateStr, tenorBuilder.getActualDate());

						tenorBuilder.setName(tenors[0].equals(tenors[1]) ? tenors[0].getTenorNm() : tenorBuilder.getActualDate());
					}
					expectedTenors.remove(tenorBuilder.getName());
				}
//				if ( expectedTenors.size() != 0) {
//					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
//							IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
//					indicativeSubStatus = IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus,
//							IndicativeSubStatus.CONFIGURATION_MktStructNotMatched);
//				}
				forwardCurveBuilder.setTradingSession(getSessionInfo().getTradingSessionId());
				
				long lastRefresh = individualInfo.getLastRefresh();
				long receiveTimestamp = forwardCurveBuilder.getLatencyBuilder().getFaReceiveTimestamp();
				boolean isReceiveRefresh = (receiveTimestamp - lastRefresh) > -1;
				if (!isReceiveRefresh && System.currentTimeMillis() > lastRefresh + getSessionInfo().getTimeoutInterval()) {
//					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
//							IndicativeFlag.IndicativeReason.MA_NoData);
				}
				//TODO CHECK THIS!!!
				forwardCurveBuilder.clearLatency();
				
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
				ForwardCurve curve = forwardCurveBuilder.build();
//				if(symbol.equals("GBPUSD"))
//					logger.info("GBPUSD\n"+curve.getTenorsList());
				getPublishingEndpoint().publish(topic, curve);
				//logger.info(topic+"\n"+curve.toString());
			}
		}
	}
	
	@Override
	public void init() {

		
	}

	@Override
	public void start() {
		String[] symbols = getMarketDataSet().getAvailableSymbolsToArrayByType(AppType.FCADAPTER.getPublishingFormatType().toString());

		RefreshRequesterFwd requester = new RefreshRequesterFwd(
				getSessionInfo(), symbols, fwdCurves, priceStores, getWorker(), getQfixApp(), getIntegrationPlugin(), espRepo);
		requester.init();
		
		this.requester = requester;
	}

	@Override
	public void stop() {
		if ( this.requester != null ) {
			this.requester.destroy();		
		}
	}
	private TenorVo[] findTenors(final String symbol, final String tradeDateStr, final String selectedBrokenDateStr ) {
		final SessionInfo sessionInfo = getSessionInfo();
		final MarketDatasetVo marketDataSet = sessionInfo.getMarketDataset();
		final IndividualInfoVo individualInfo = marketDataSet.getMarketStructuresByType(AppType.FCADAPTER.getPublishingFormatType().toString()).get(symbol);
		final String[] tenorStrArray = individualInfo.getTenors();
		final LocalDate tradeDate =  ChronologyUtil.getLocalDateFromString(tradeDateStr);
		final LocalDate selectedBrokenDate = ChronologyUtil.getLocalDateFromString(selectedBrokenDateStr);
		
		TenorVo lowerTenor = null;
		TenorVo upperTenor = null;
		List<TenorActualDateVo> possibleTenorActualDateList = new LinkedList<TenorActualDateVo>();
		
		for (String tenorStr : tenorStrArray) {
			TenorVo tenor = TenorVo.fromString(tenorStr);
			TenorActualDateVo tenorActualDate = new TenorActualDateVo();
			tenorActualDate.setTenor(tenor);
			LocalDate tenorValueDate = fxCalendarBizService.getForwardValueDate(symbol, tradeDate, tenor.getPeriodCd(), tenor.getValue(), PricingConventionConstants.INTERBANK);
			tenorActualDate.setActualDate(tenorValueDate);
			possibleTenorActualDateList.add(tenorActualDate);
		}
		Collections.sort(possibleTenorActualDateList);
		for (TenorActualDateVo tenorActualDate : possibleTenorActualDateList) {
			LocalDate tenorValueDate = tenorActualDate.getActualDate();

			if (tenorValueDate.isEqual(selectedBrokenDate)
					|| selectedBrokenDate.isAfter(tenorValueDate)) {
				lowerTenor = tenorActualDate.getTenor();
			}
			if (tenorValueDate.isEqual(selectedBrokenDate)
					|| selectedBrokenDate.isBefore(tenorValueDate)) {
				upperTenor = tenorActualDate.getTenor();
				break;
			}
		}
		return new TenorVo[] { lowerTenor, upperTenor };

	}
	private static class TenorActualDateVo implements Comparable<TenorActualDateVo> {
		private TenorVo tenor;
		private LocalDate actualDate;
		
		public TenorVo getTenor() {
			return tenor;
		}
		
		public void setTenor(TenorVo tenor) {
			this.tenor = tenor;
		}
		
		public LocalDate getActualDate() {
			return actualDate;
		}
		
		public void setActualDate(LocalDate actualDate) {
			this.actualDate = actualDate;
		}
		
		@Override
		public int compareTo(TenorActualDateVo o) {
			return this.actualDate.compareTo(o.actualDate);
		}
	}
	
	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo = espRepo;
		
	}
	
}
