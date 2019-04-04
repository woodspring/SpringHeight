package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.ContextVo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.MarketDatasetVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.ForwardCurveBuilderFullScanStructureDifferentiator;
import com.tts.service.biz.price.structure.diff.full.IFullScanStructureDifferentiator;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.CollectionUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeSubStatus;
import com.tts.util.flag.RateChangeIndFlag;
import com.tts.util.flag.RateChangeIndFlag.Change;
import com.tts.vo.TenorVo;

public class DefaultFileBasedForwardAppImpl  extends AbstractPublishingApp {
	public final static String MY_NAME = "FCADAPTER";


	private static final String CSV_COMMENT_PREFIX = "#";
	@SuppressWarnings("unused")
	private static final String CSV_COLUMN_SEPERATOR = ",";
	private static final String DEFAULT_CSV_FILE = "app-resources/fc.csv";
	private static final String CSV_FILE_TEMPLETE = "app-resources/fc_%s.csv";

	private static final Logger logger = LoggerFactory
			.getLogger(DefaultFileBasedForwardAppImpl.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory
			.getMonitorAgent(logger);

	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private final FileFcFormatConversionHandler fileFcFormatConversionHandler = new FileFcFormatConversionHandler();
	private final ISymbolIdMapper symbolIdMapper;
	private final IFxCalendarBizService fxCalendarBizService;
	@SuppressWarnings("unused")
	private final MarketDatasetVo marketDataset;
	private final String csvFile;
	private final String[] symbols;
	private final PriceStructureRepo<ForwardCurve.Builder> structureRepo;
	private final IFullScanStructureDifferentiator<ForwardCurve.Builder> differentiator;
	
	private volatile Map<String, ContextVo> symbolData = null;
	private volatile ForwardCurve.Builder[] fwdCurves = null;
	private volatile Thread curveFileMonitoringThread = null;

	public DefaultFileBasedForwardAppImpl(IMkQfixApp qfixApp,
			ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint,
				iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
    	this.symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
    	this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);

		String[] symbols = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.FCADAPTER.getPublishingFormatType().toString());
		int numberOfInstruments = Math.max(symbols.length, symbolIdMapper.getSymbols().size());
		
		
		String appEnvFcName = String.format(CSV_FILE_TEMPLETE, AppUtils.getActiveEnvironment());
		URL u = Thread.currentThread().getContextClassLoader().getResource(appEnvFcName);
		if ( u == null ) {
			appEnvFcName = DEFAULT_CSV_FILE;
		}
		
		this.marketDataset = sessionInfo.getMarketDataset();
		this.csvFile = appEnvFcName;
		ForwardCurve.Builder[] fwdCurves = new ForwardCurve.Builder[numberOfInstruments];
		for ( String symbol: symbols) {
			int pos = symbolIdMapper.map(symbol);
			ForwardCurve.Builder fc = ForwardCurve.newBuilder();
			fc.setSymbol(symbol);
			fwdCurves[pos] = fc;
		}
		this.fwdCurves = fwdCurves;
		this.symbols = symbols;
		
		this.structureRepo = new PriceStructureRepo<ForwardCurve.Builder>(symbolIdMapper);
		this.differentiator = new ForwardCurveBuilderFullScanStructureDifferentiator();
	}
	
	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxForwards;
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}
	
	public String getCSVFile() {
		String func = "CSVFcAdapter.getCSVFile".intern();
		if (csvFile == null || csvFile.isEmpty()) {
			monitorAgent.logError(func, MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT, "Forward Curve Data file undefined");
			return null;
		}
		return csvFile;
	}

	public void init() {
		initLoad(symbols);
	}
	
	public void destroy() {
		if ( curveFileMonitoringThread != null ) {
			Thread m = curveFileMonitoringThread;
			curveFileMonitoringThread = null;
			m.interrupt();
		}
	}

	public void initLoad(String[] symbols) {
		String func = "CSVFcAdapter.requestMarketData".intern();

		URL orgFileUrl = Thread.currentThread().getContextClassLoader()
				.getResource(getCSVFile());
		if (orgFileUrl == null) {
			monitorAgent.logMessage(func,
					"ForwardCurve Data file does not exist");
			return;
		}

		try {


			if ( symbols.length > 0) {
				File newFile = new File(orgFileUrl.toURI());
				reloadFromFile(newFile.getAbsolutePath());
				scheduleWatcher(getWorker(), newFile.getParent(), newFile.getName());
			}			

		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	@Override
	public void atPublish(long masGlobalSeq) {
		ForwardCurve.Builder[] fwdCurves = this.fwdCurves;
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
						LocalDate valueDate = getFxCalendarBizService().getForwardValueDate(symbol, tradeDate, periodCode, periodValue, PricingConventionConstants.INTERBANK);
						tenorBuilder.setActualDate(ChronologyUtil.getDateString(valueDate));
					} else if ( tenorBuilder.hasActualDate() && !tenorBuilder.hasName()) {
						TenorVo[] tenors = findTenors(symbol, tradeDateStr, tenorBuilder.getActualDate());

						tenorBuilder.setName(tenors[0].equals(tenors[1]) ? tenors[0].getTenorNm() : tenorBuilder.getActualDate());
					}
					expectedTenors.remove(tenorBuilder.getName());
				}
				if ( expectedTenors.size() != 0) {
					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
							IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
					indicativeSubStatus = IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus,
							IndicativeSubStatus.CONFIGURATION_MktStructNotMatched);
				}
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
		
	}


	@Override
	public String getName() {
		return MY_NAME;
	}

	@Override
	public void start() {
		init();		
	}

	@Override
	public void stop() {
		destroy();		
	}

	private IFxCalendarBizService getFxCalendarBizService() {
		return fxCalendarBizService;
	}
	
	private void reloadFromFile(String newFile) {
		logger.debug("reloadFromFile: " + newFile + " started");		

		String func = "CSVFcAdapter.reloadFromFile".intern();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(newFile)))) {
			monitorAgent.logMessage(func, String.format("Open: %s", newFile));

			String line = null;
			String symbol = null, previousSymbol = null;
			ContextVo context = null;
			LinkedList<String> dataForThisSymbol = null;
			HashMap<String, ContextVo> manySymbolData = new HashMap<String, ContextVo>();
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0 || line.startsWith(CSV_COMMENT_PREFIX)) {
					continue;
				} else {
					if ( line.startsWith("<")) {
						symbol = line.substring(1, 7);
					} else {
						symbol = line.substring(0, 6);
					}
				}
				if (previousSymbol == null || !previousSymbol.equals(symbol)) {
					if (dataForThisSymbol != null) {
						context.setContext(dataForThisSymbol);
						manySymbolData.put(context.getSymbol(), context);
					}

					context = new ContextVo();
					context.setContextType(String.class);
					dataForThisSymbol = new LinkedList<String>();
				}
				context.setSymbol(symbol);
				dataForThisSymbol.add(line);
				previousSymbol = symbol;
			}
			context.setContext(dataForThisSymbol);
			manySymbolData.put(context.getSymbol(), context);
			this.symbolData = Collections.unmodifiableMap(manySymbolData);
			Map<String, ContextVo> symbolData = this.symbolData;
			for ( String _symbol: this.symbols) {
				int symbolId = symbolIdMapper.map(_symbol);
				ContextVo contextData = symbolData.get(_symbol);
				if (contextData != null ){
					fileFcFormatConversionHandler.doHandle(fwdCurves[symbolId], contextData);
				}
			}
		} catch (IOException e) {
			monitorAgent.logError(func,
					MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT,
					"IO ERROR with CSV file", e);
		} catch (Exception e) {
			monitorAgent.logError(func,
					MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR,
					"Unknown Exception", e);
		}
		
		logger.debug("reloadFromFile: " + newFile + " completed for " + this.symbolData.keySet().size() + " symbols");		

	}

	
	private void scheduleWatcher(final ISchedulingWorker worker2,
			final String parentFolder, String fileName) {

		if (scheduled.compareAndSet(false, true)) {
			worker2.execute(new Runnable() {

				@Override
				public void run() {
					curveFileMonitoringThread = Thread.currentThread();
					final Path path = Paths.get(parentFolder);
					try (WatchService watchService = FileSystems.getDefault()
							.newWatchService()) {

						path.register(watchService,
								StandardWatchEventKinds.ENTRY_CREATE,
								StandardWatchEventKinds.ENTRY_MODIFY); //,
								// StandardWatchEventKinds.ENTRY_DELETE);

						while (curveFileMonitoringThread == Thread.currentThread()) {
							final WatchKey key = watchService.take();

							for (WatchEvent<?> watchEvent : key.pollEvents()) {
								final Kind<?> kind = watchEvent.kind();

								if (kind == StandardWatchEventKinds.OVERFLOW) {
									continue;
								}

								@SuppressWarnings("unchecked")
								final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
								final Path entry = watchEventPath.context();

								if (entry.endsWith(fileName)) {
									reloadFromFile(parentFolder + File.separatorChar + fileName);
								}
							}

							key.reset();

							if (!key.isValid()) {
								break;
							}
						}
					} catch (IOException e) {
						monitorAgent.logError("scheduleWatcher",
								MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR,
								"Unknown Exception", e);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						scheduled.set(false);
					}
					curveFileMonitoringThread = null;
				}
				
			});
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
			LocalDate tenorValueDate = getFxCalendarBizService().getForwardValueDate(symbol, tradeDate, tenor.getPeriodCd(), tenor.getValue(), PricingConventionConstants.INTERBANK);
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



}
