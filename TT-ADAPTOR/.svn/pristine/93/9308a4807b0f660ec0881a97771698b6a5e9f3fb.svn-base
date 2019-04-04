package com.tts.plugin.adapter.impl.base.app.interest;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.InterestRateCurveStruct.InterestRateCurve;
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
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.util.flag.IndicativeFlag;

public class DefaultFileBasedInterestRateAppImpl  extends AbstractPublishingApp {
	public final static String MY_NAME = "IRADAPTER";
	private static final String CSV_COMMENT_PREFIX = "#";
	private static final String CSV_COLUMN_SEPERATOR = ",";
	private static final String CSV_FILE = "app-resources/ir.csv";
	
	private static final Logger logger = LoggerFactory
			.getLogger(DefaultFileBasedInterestRateAppImpl.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory
			.getMonitorAgent(logger);

	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private final FileIrFormatConversionHandler fileIrFormatConversionHandler = new FileIrFormatConversionHandler();
	private final ISymbolIdMapper symbolIdMapper;
	private final String csvFile;
	private final String[] symbols;
	
	private volatile Map<String, ContextVo> symbolData = null;
	private volatile InterestRateCurve.Builder[] irCurves = null;
	private volatile Thread curveFileMonitoringThread = null;

	public DefaultFileBasedInterestRateAppImpl(IMkQfixApp qfixApp,
			ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint,
				iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
    	

		String[] symbols = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.IRADAPTER.getPublishingFormatType().toString());
		this.symbolIdMapper = new CurrencySymbolIdMapper(symbols);
		int numberOfInstruments = Math.max(symbols.length, symbolIdMapper.getSymbols().size());
		this.csvFile = CSV_FILE;
		InterestRateCurve.Builder[] irCurves = new InterestRateCurve.Builder[numberOfInstruments];
		for ( String symbol: symbols) {
			int pos = symbolIdMapper.map(symbol);
			InterestRateCurve.Builder fc = InterestRateCurve.newBuilder();
			fc.setSymbol(symbol);
			irCurves[pos] = fc;
		}
		this.irCurves = irCurves;
		this.symbols = symbols;
	}
	
	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxInterestRate;
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}
	
	public String getCSVFile() {
		String func = "CSVIrAdapter.getCSVFile".intern();
		if (csvFile == null || csvFile.isEmpty()) {
			monitorAgent.logError(func, MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT, "Interest Rate Curve Data file undefined");
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
		String func = "CSVIrAdapter.requestMarketData".intern();

		URL orgFileUrl = Thread.currentThread().getContextClassLoader()
				.getResource(getCSVFile());
		if (orgFileUrl == null) {
			monitorAgent.logMessage(func,
					"Interest Rate Data file does not exist");
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
		InterestRateCurve.Builder[] irCurves = this.irCurves;
		for ( int i = 0; i < irCurves.length; i++ ) {
			InterestRateCurve.Builder interestRateCurveBuilder = irCurves[i];
			if ( interestRateCurveBuilder != null ) {
				final String symbol = interestRateCurveBuilder.getSymbol();

				final long sentTime = System.currentTimeMillis();
				final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(
						AppType.IRADAPTER.getPublishingFormatType().toString(), symbol);
				if ( individualInfo == null  ) {
					continue;
				}
				String topic = individualInfo.getTopic();

				if ( topic == null ) {
					// Market Structure is NOT defined
					continue;
				}

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
					interestRateCurveBuilder.setTradeDate(tradeDateStr );
				}
				
				interestRateCurveBuilder.setTradingSession(getSessionInfo().getTradingSessionId());
				interestRateCurveBuilder.setTradeDate(tradeDateStr);
				long lastRefresh = individualInfo.getLastRefresh();
				long receiveTimestamp = interestRateCurveBuilder.getLatencyBuilder().getFaReceiveTimestamp();
				boolean isReceiveRefresh = (receiveTimestamp - lastRefresh) > -1;
				if (!isReceiveRefresh && System.currentTimeMillis() > lastRefresh + getSessionInfo().getTimeoutInterval()) {
					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
							IndicativeFlag.IndicativeReason.MA_NoData);
				}
				
				interestRateCurveBuilder.setIndicativeFlag(indicativeFlag);
				interestRateCurveBuilder.setIndicativeSubFlag(indicativeSubStatus);
				interestRateCurveBuilder.getLatencyBuilder().setFaSendTimestamp(sentTime);
				interestRateCurveBuilder.setSequence(masGlobalSeq);
				
				getPublishingEndpoint().publish(topic, interestRateCurveBuilder.build());
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
	
	private void reloadFromFile(String newFile) {
		monitorAgent.debug("reloadFromFile: " + newFile + " started");		

		String func = "CSVIrAdapter.reloadFromFile".intern();

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
					String[] elements = line.split(CSV_COLUMN_SEPERATOR);
					symbol = elements[1];
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
					fileIrFormatConversionHandler.doHandle(irCurves[symbolId], contextData);
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
		
		monitorAgent.debug("reloadFromFile: " + newFile + " completed for " + this.symbolData.keySet().size() + " symbols");		

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




}
