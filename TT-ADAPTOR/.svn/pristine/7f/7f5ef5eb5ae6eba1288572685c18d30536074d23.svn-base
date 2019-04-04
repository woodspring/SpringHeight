package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.support.IFxCalendarBizServiceApi;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.TenorVo;

public class RefreshRequesterSpot implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(RefreshRequesterSpot.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

	private final static String[] instruments = new String[] {
		"EURGBP",
		"EURUSD",
		"GBPUSD",
		"USDCAD"
	};
	private final static Set<String> instrumentSet = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(instruments)));
	private final static boolean isSymbolFilteringOn;
	
	static {
		boolean f = false;
		try {
			f = Boolean.parseBoolean(System.getProperty("SYMBOL.FILTER", Boolean.FALSE.toString()));
		} catch (Exception e) {
			f = false;
		}
		isSymbolFilteringOn = f;
	}
	
	

	private static final String REQUEST_ID_NOT_SET = "NOT_SET";
	private final FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
	private final String[] symbols;
	private final MasterPriceStore<FullBook.Builder> priceStores;
	private final ISchedulingWorker worker;
	private final IMkQfixApp qfixApp;
	private final ConcurrentHashMap<String, String> requestIdMap;
	private final IFixIntegrationPluginSpi integrationPlugin;
	private final Map<String, IndividualInfoVo> sessionMarketSetup;
	private final IEspRepo<?> espRepo;
	private final IFxCalendarBizServiceApi fxCalendarBizServiceApi;
	private volatile ScheduledFuture<?> scheduled = null;
	private volatile long scheduledTime = -1;
	private final long throttleBetweenCancelAndSubscription;
	private final long throttleRequest;
	private final long defaultMaxPriceAgeAllowance;
	private final long minIntervalBetweenSubscribes;
	private final boolean disableRetrySubscription;
	
	//private volatile boolean sent = false;
	
	public RefreshRequesterSpot(SessionInfo sessionInfo, 
			String[] symbols, 
			MasterPriceStore<FullBook.Builder> priceStores, 
			ISchedulingWorker worker, IMkQfixApp qfixApp, 
			IFixIntegrationPluginSpi integrationPlugin, IEspRepo<?> espRepo ) {
		this.sessionMarketSetup = sessionInfo.getMarketDataset().getMarketStructuresByType(AppType.SPOTADAPTER.getPublishingFormatType().toString());
		this.symbols = symbols;
		this.priceStores = priceStores;
		this.worker = worker;
		this.qfixApp = qfixApp;
		this.requestIdMap = new ConcurrentHashMap<String, String>(symbols.length);
		this.integrationPlugin = integrationPlugin;
		this.fxCalendarBizServiceApi = AppContext.getContext().getBean(IFxCalendarBizServiceApi.class);
		this.espRepo = espRepo;
		this.defaultMaxPriceAgeAllowance = p.getProperty("spotadapter.defaultMaxPriceAgeAllowance", ChronologyUtil.MILLIS_IN_MINUTE);
		this.throttleBetweenCancelAndSubscription = p.getProperty("spotadapter.throttle_cancel_subscribe", -1L);
		this.minIntervalBetweenSubscribes = p.getProperty("spotadapter.min_interval_between_subscribes", 10 * ChronologyUtil.MILLIS_IN_MINUTE);
		this.throttleRequest = p.getProperty("spotadapter.throttle_request", -1L);
		this.disableRetrySubscription = !p.getProperty("spotadapter.retry_by_unsub_and_sub", false);
	}
	
	public void init() {
		
		if ( symbols.length > 0 ) {
			scheduledTime = System.currentTimeMillis();
			scheduled = getWorker().scheduleAtFixedRate(this, 30L);
		}

	}
	
	public void destroy() {
		if ( scheduled != null ) {
			scheduled.cancel(false);
		
			int attempt = 0;
			while ( attempt < 3 
					&& 	(!scheduled.isDone() || !scheduled.isCancelled())) {
				try {
					Thread.sleep(500L);
				} catch (InterruptedException e) {
	
				}
				attempt++;
			}
		}
		WeakHashMap<String,String> requestIdMap = new WeakHashMap<String,String>(this.requestIdMap);
		Set<Entry<String, String>> requestIdEntrySet = requestIdMap.entrySet();
		ArrayList<String> toStopList = new ArrayList<String>(requestIdEntrySet.size());
		logger.debug("requestIdMap.size=" + requestIdMap.size());

		Iterator<Entry<String, String>> requestIdEntrySetIterator = requestIdEntrySet.iterator();
		while ( requestIdEntrySetIterator.hasNext() ) {
			Entry<String, String> e = requestIdEntrySetIterator.next();
			if ( !REQUEST_ID_NOT_SET.equals(e.getValue())) {
				toStopList.add(e.getKey());
			}
		}
		cancelRefreshRequest(toStopList.toArray(new String[0]));
	}
	
	@Override
	public void run() {
		long lastMarketTimestamp = -1;
		long checkTime = System.currentTimeMillis();
		
		//if ( sent ) return;
		final MasterPriceStore<FullBook.Builder> priceStores = this.priceStores;
		final String[] symbols = this.symbols;
		logger.debug("RefreshRequester running");
		
		ArrayList<String> outdatedSymbols = new ArrayList<String>(symbols.length);
		ArrayList<String> requireCancelationSymbols = new ArrayList<String>(symbols.length);
		for ( String symbol: symbols) {
			if ( isSymbolFilteringOn && !instrumentSet.contains(symbol)) {
				continue;
			}
			IndividualInfoVo individualInfo = sessionMarketSetup.get( symbol);

			IndividualPriceStore<FullBook.Builder> individualPriceStore = priceStores.getPriceStore(symbol);
			if ( individualPriceStore != null ) {
				FullBook.Builder fb = individualPriceStore.getLatest();
				
				long marketTimestamp = -1;
				long currentTime = checkTime;
				if ( fb.hasLatency() && fb.getLatency().hasFaReceiveTimestamp()) {
					marketTimestamp = fb.getLatency().getFaReceiveTimestamp();
					if ( marketTimestamp > lastMarketTimestamp) {
						lastMarketTimestamp = marketTimestamp;
					}
				}				
				String dMsg = String.format("Symbol<%s> marketTimestamp=%s lastRequest=%s lastRefresh=%s currentTimestamp=%s defaultMaxPriceAgeAllowance=%s disableRetrySubscription=%s", 
						fb.getSymbol(),
						marketTimestamp,
						individualInfo.getLastRequest(),
						individualInfo.getLastRefresh(),
						currentTime,
						defaultMaxPriceAgeAllowance,
						disableRetrySubscription);
				logger.debug(dMsg);

				
				if ( marketTimestamp <= 0 
						|| individualInfo.getLastRefresh() <= 0L
						|| (currentTime - marketTimestamp) > defaultMaxPriceAgeAllowance 	) {
					fb.setIndicativeFlag(IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE, IndicativeReason.MA_NoData));
					if ( !disableRetrySubscription) {
						if ( individualInfo.getLastRequest() < 0L 
								|| (currentTime - individualInfo.getLastRequest() ) > minIntervalBetweenSubscribes) {
							outdatedSymbols.add(symbol);
							if ( requestIdMap.containsKey(symbol) 
									&& !requestIdMap.get(symbol).equals(REQUEST_ID_NOT_SET)
									&& individualInfo.getLastRefresh() != -1) {
								requireCancelationSymbols.add(symbol);
							}
						}
					} else {
						if (disableRetrySubscription && individualInfo.getLastRequest() <= 0L) {
							outdatedSymbols.add(symbol);	
						} else if ( individualInfo.getLastRequest() < 0L ) {
							outdatedSymbols.add(symbol);
						}
					}
					
				}
			}
		}
		logger.debug("requireCancelationSymbols.size=" + requireCancelationSymbols.size() + ";outdatedSymbols=" + outdatedSymbols.size());

		if ( getQfixApp().isLoggedOn(AppType.SPOTADAPTER)) {
			if ( requireCancelationSymbols.size() > 0) {
				cancelRefreshRequest(requireCancelationSymbols.toArray(new String[0]));
				if ( throttleBetweenCancelAndSubscription > 0 ){
					try {
						Thread.sleep(throttleBetweenCancelAndSubscription);
					} catch (InterruptedException e) {
						
					}
				}
			}

			submitRefreshRequest(outdatedSymbols.toArray(new String[0]));
			//sent =true;
		} else {
			logger.debug("Reported not online");
		}
		logger.debug("RefreshRequester exiting");
		logger.debug("requestIdMap.size=" + requestIdMap.size());

		if ( (checkTime - scheduledTime ) > ChronologyUtil.MILLIS_IN_MINUTE 
				&& (checkTime - lastMarketTimestamp ) > ChronologyUtil.MILLIS_IN_MINUTE ) {
			for ( String symbol: symbols) {
				if ( isSymbolFilteringOn && !instrumentSet.contains(symbol)) {
					continue;
				}

				IndividualPriceStore<FullBook.Builder> individualPriceStore = priceStores.getPriceStore(symbol);
				if ( individualPriceStore != null ) {
					FullBook.Builder fb = individualPriceStore.getLatest();
					long indicativeFlag = fb.getIndicativeFlag();
					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_NoData);
					fb.setIndicativeFlag(indicativeFlag);
				}
			}
			String msg1 = "No market rate was received in the past 1 minute.";
			String msg2 = msg1 + " lastMarketTimestamp= " + lastMarketTimestamp + "currentTime:" + checkTime;
			monitorAgent.logInfoMessage("MarketRateChecker", msg1);
			logger.debug("MarketRateChecker", msg2);

		}

	}
	
	private void cancelRefreshRequest(String[] symbols) {
		WeakHashMap<String,String> requestIdMap = new WeakHashMap<String,String>(this.requestIdMap);

		HashMap<String, String> newRequestIdMap = new HashMap<String, String>(symbols.length);

		for ( String symbol: symbols) {
			String requestId = requestIdMap.get(symbol);
			if  (getQfixApp().cancelEspRequest(symbol, TenorVo.NOTATION_SPOT, null, requestId,  AppType.SPOTADAPTER) ) {
				newRequestIdMap.put(symbol, REQUEST_ID_NOT_SET);
				logger.debug(String.format("Cancelling Esp request for %s, request Id %s", symbol, requestId));
			} else {
				logger.warn(String.format("Unable to send cancelling Esp request for %s, request Id %s", symbol, requestId));
			}
			if ( this.throttleRequest > 0 ) {
				try {
					Thread.sleep(throttleRequest);
				} catch (InterruptedException e) {

				}
			}
		}
		if ( newRequestIdMap.size() > 0 ) {
			this.requestIdMap.putAll(newRequestIdMap);
		}
	}
	
	private void submitRefreshRequest(String[] symbols) {
		HashMap<String, String> newRequestIdMap = new HashMap<String, String>(symbols.length);
		
		for (String symbol: symbols) {
			IndividualPriceStore<FullBook.Builder> priceStore = priceStores.getPriceStore(symbol);
			FixPriceUpdater fixUpdater = new FixPriceUpdater(getIntegrationPlugin(), priceStore, sessionMarketSetup, espRepo, new OnMDCancelCallback(symbol, this.requestIdMap));
			String settleDate = fxCalendarBizServiceApi.getForwardValueDate(symbol, TenorVo.NOTATION_SPOT);

			String requestId = getQfixApp().sendEspRequest(symbol, TenorVo.NOTATION_SPOT, settleDate, fixUpdater, AppType.SPOTADAPTER);
			if ( requestId != null ) {
				newRequestIdMap.put(symbol, requestId);
				logger.debug(String.format("Submit Esp request for %s, request Id %s", symbol, requestId));
			} else {
				logger.warn(String.format("Unable to send Esp request for %s", symbol));
			}
			IndividualInfoVo individualInfo = sessionMarketSetup.get( symbol);
			individualInfo.setLastRefresh(System.currentTimeMillis());
			individualInfo.setLastRequest(System.currentTimeMillis());
			if ( this.throttleRequest > 0 ) {
				try {
					Thread.sleep(throttleRequest);
				} catch (InterruptedException e) {

				}
			}
		}
		
		if ( newRequestIdMap.size() > 0 ) {
			this.requestIdMap.putAll(newRequestIdMap); 
		}
	}
	



	private ISchedulingWorker getWorker() {
		return worker;
	}

	private IMkQfixApp getQfixApp() {
		return qfixApp;
	}

	private IFixIntegrationPluginSpi getIntegrationPlugin() {
		return integrationPlugin;
	}
	
    static class OnMDCancelCallback implements Function<Object, Object>  {
		
		private final String symbol;
		private final Map<String, String> requestIdMap;
		
		
		public OnMDCancelCallback(String symbol, Map<String, String> requestIdMap) {
			super();
			this.symbol = symbol;
			this.requestIdMap = requestIdMap;
		}


		@Override
		public Object apply(Object t) {
			requestIdMap.remove(symbol);
			return null;
		}
		
	}
	
}
