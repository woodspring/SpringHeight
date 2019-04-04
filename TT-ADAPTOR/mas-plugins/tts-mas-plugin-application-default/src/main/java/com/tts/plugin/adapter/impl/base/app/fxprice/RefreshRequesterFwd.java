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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.support.IFxCalendarBizServiceApi;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;

public class RefreshRequesterFwd implements Runnable {
	private static final String KEY__SEPERATOR = ".";
	private final static Logger logger = LoggerFactory.getLogger(RefreshRequesterFwd.class);
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
	private static final String REQUEST_SENT = "SENT";

	private final String[] symbols;
	private final MasterPriceStore<ForwardCurve.Builder> priceStores;
	private ForwardCurve.Builder[] fwdcurves;
	private final ISchedulingWorker worker;
	private final IMkQfixApp qfixApp;
	private final ConcurrentHashMap<String, String> requestIdMap;
	private final IFixIntegrationPluginSpi integrationPlugin;
	private final Map<String, IndividualInfoVo> sessionMarketSetup;
	private final IEspRepo<?> espRepo;
	private final IFxCalendarBizServiceApi fxCalendarBizServiceApi;

	private volatile ScheduledFuture<?> scheduled = null;
	//private volatile boolean sent = false;
	
	public RefreshRequesterFwd(SessionInfo sessionInfo, 
			String[] symbols, 
			ForwardCurve.Builder[] fwdcurves,
			MasterPriceStore<ForwardCurve.Builder> priceStores,
			ISchedulingWorker worker, IMkQfixApp qfixApp, 
			IFixIntegrationPluginSpi integrationPlugin, 
			IEspRepo<?> espRepo ) {
		this.sessionMarketSetup = sessionInfo.getMarketDataset().getMarketStructuresByType(AppType.FCADAPTER.getPublishingFormatType().toString());
		this.symbols = symbols;
		this.fwdcurves = fwdcurves;
		this.priceStores = priceStores;
		this.worker = worker;
		this.qfixApp = qfixApp;
		this.requestIdMap = new ConcurrentHashMap<String, String>(symbols.length);
		this.integrationPlugin = integrationPlugin;
		this.espRepo = espRepo;
		this.fxCalendarBizServiceApi = AppContext.getContext().getBean(IFxCalendarBizServiceApi.class);
	}
	
	public void init() {
		if ( symbols.length > 0 ) {
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
		HashSet<String> toStopList = new HashSet<String>(requestIdEntrySet.size());
		logger.debug("requestIdMap.size=" + requestIdMap.size());

		Iterator<Entry<String, String>> requestIdEntrySetIterator = requestIdEntrySet.iterator();
		while ( requestIdEntrySetIterator.hasNext() ) {
			Entry<String, String> e = requestIdEntrySetIterator.next();
			if ( !REQUEST_ID_NOT_SET.equals(e.getValue())) {
				String symbolTenor = e.getKey();
				int symbolLastPos = symbolTenor.indexOf(KEY__SEPERATOR);
				if ( symbolLastPos < 0) {
					toStopList.add(symbolTenor);
				}
			}
		}
		cancelRefreshRequest(toStopList.toArray(new String[0]));
	}
	
	@Override
	public void run() {
		//if ( sent ) return;
		@SuppressWarnings("unused")
		final ForwardCurve.Builder[] fc = this.fwdcurves;
		final String[] symbols = this.symbols;
		logger.debug("RefreshRequesterFwd running");
		
		ArrayList<String> outdatedSymbols = new ArrayList<String>(symbols.length);
		ArrayList<String> requireCancelationSymbols = new ArrayList<String>(symbols.length);
		for ( String symbol: symbols) {
			if ( isSymbolFilteringOn && !instrumentSet.contains(symbol)) {
				continue;
			}
			IndividualInfoVo individualInfo = sessionMarketSetup.get( symbol);

			IndividualPriceStore<ForwardCurve.Builder> individualPriceStore = priceStores.getPriceStore(symbol);
			if ( individualPriceStore != null ) {
				ForwardCurve.Builder fb = individualPriceStore.getLatest();
				if ( "EURUSD".equals(symbol) && !fb.hasLatency()) logger.debug("EURUSD:fb.hasLatency(): false");
				if ( "EURUSD".equals(symbol) && fb.hasLatency()) 
					logger.debug("EURUSD:fb.getFaReceiveTimestamp()=" + fb.getLatencyBuilder().getFaReceiveTimestamp());
				if ( "EURUSD".equals(symbol) ) {
					logger.debug("EURUSD:fb.getFaReceiveTimestamp()=" + fb.getLatencyBuilder().getFaReceiveTimestamp());
				}
				
				if ( "EURUSD".equals(symbol)) logger.debug("EURUSD:fb.getRefreshInterval()=" + individualInfo.getRefreshInterval());
				if ( !fb.hasLatency() 
						|| !fb.getLatencyBuilder().hasFaReceiveTimestamp()
						) {
						//|| (fb.getLatencyBuilder().getFaReceiveTimestamp() - System.currentTimeMillis()) > individualInfo.getRefreshInterval()) {
					logger.debug(symbol + " fwds do not have latency record. probably does not have market rate. Flag as no market rate");
					individualInfo.addIndicativeReason(IndicativeReason.MA_NoData);
					//priceStores.getPriceStore(symbol).updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), null);
					if ( individualInfo.getLastRefresh() == 0L 
							|| (System.currentTimeMillis() - individualInfo.getLastRefresh() ) > ChronologyUtil.MILLIS_IN_MINUTE * 10) {
						outdatedSymbols.add(symbol);
						if ( requestIdMap.containsKey(symbol) && !requestIdMap.get(symbol).equals(REQUEST_ID_NOT_SET)) {
							requireCancelationSymbols.add(symbol);
						}
					}
				}
			}
		}
		logger.debug("requireCancelationSymbols.size=" + requireCancelationSymbols.size() + ";outdatedSymbols=" + outdatedSymbols.size());

		if ( getQfixApp().isLoggedOn(AppType.FCADAPTER)) {
			if ( requireCancelationSymbols.size() > 0) {
				cancelRefreshRequest(requireCancelationSymbols.toArray(new String[0]));
			}
			if(outdatedSymbols.size() > 0) {
				submitRefreshRequest(outdatedSymbols.toArray(new String[0]));
			}

			//sent =true;
		}
		logger.debug("RefreshRequester exiting");
		logger.debug("requestIdMap.size=" + requestIdMap.size());



	}
	
	private void cancelRefreshRequest(String[] symbols) {
		WeakHashMap<String,String> requestIdMap = new WeakHashMap<String,String>(this.requestIdMap);

		HashMap<String, String> newRequestIdMap = new HashMap<String, String>(symbols.length);

		for ( String symbol: symbols) {
			IndividualInfoVo individualInfo = sessionMarketSetup.get( symbol);
			for(String tenor : individualInfo.getTenors()) {
				String requestId = requestIdMap.get(symbol+KEY__SEPERATOR+tenor);
				if  (getQfixApp().cancelEspRequest(symbol, tenor, null, requestId,  AppType.FCADAPTER) ) {
					newRequestIdMap.put(symbol+KEY__SEPERATOR+tenor, REQUEST_ID_NOT_SET);
					logger.debug(String.format("Cancelling FwdEsp request for %s, request Id %s", symbol, requestId));
				} else {
					logger.warn(String.format("Unable to send cancelling Esp request for %s, request Id %s", symbol, requestId));
				}

			}
			newRequestIdMap.put(symbol, REQUEST_ID_NOT_SET);

		}
		if ( newRequestIdMap.size() > 0 ) {
			this.requestIdMap.putAll(newRequestIdMap);
		}
	}
	
	private void submitRefreshRequest(String[] symbols) {
		HashMap<String, String> newRequestIdMap = new HashMap<String, String>(symbols.length);
		
		for (String symbol: symbols) {
			IndividualPriceStore<ForwardCurve.Builder> priceStore = priceStores.getPriceStore(symbol);
			
			IndividualInfoVo individualInfo = sessionMarketSetup.get( symbol);
			FixPriceUpdaterFwd fixUpdater = new FixPriceUpdaterFwd(getIntegrationPlugin(), priceStore, fwdcurves, sessionMarketSetup, espRepo);
			for(String tenor : individualInfo.getTenors()) {
				String settleDate = fxCalendarBizServiceApi.getForwardValueDate(symbol, tenor);
				String requestId = getQfixApp().sendEspRequest(symbol, tenor, settleDate, fixUpdater, AppType.FCADAPTER);
				if ( requestId != null ) {
					newRequestIdMap.put(symbol+KEY__SEPERATOR+tenor, requestId);
					logger.debug(String.format("Submit FwdEsp request for %s, request Id %s", symbol+KEY__SEPERATOR+tenor, requestId));
				} else {
					logger.warn(String.format("Unable to send FwdEsp request for %s", symbol+KEY__SEPERATOR+tenor));

				}

			}
			individualInfo.setLastRefresh(System.currentTimeMillis());
			individualInfo.setLastRequest(System.currentTimeMillis());

			newRequestIdMap.put(symbol, REQUEST_SENT);
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
	
	
}
