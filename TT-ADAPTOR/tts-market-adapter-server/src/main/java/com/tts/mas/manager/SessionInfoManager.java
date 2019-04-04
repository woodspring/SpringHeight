package com.tts.mas.manager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.entity.session.TradingSession;
import com.tts.entity.system.SystemProperty;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.mas.vo.RolloverDateMap;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.plugin.adapter.api.app.IApp;
import com.tts.plugin.adapter.api.app.IApp.AdapterTypes;
import com.tts.plugin.adapter.api.app.IApp.PublishingFormatType;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.db.MarketForwardInstrInfoDataService;
import com.tts.service.db.MarketInterestRateCurrencyInfoDataService;
import com.tts.service.db.MarketSpotInstrInfoDataService;
import com.tts.service.db.RuntimeDataService;
import com.tts.service.db.TradingSessionManager;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.util.constant.SysProperty;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.util.flag.IndicativeFlag.IndicativeSubStatus;
import com.tts.vo.ForwardMarketInstrDetailInfoVo;
import com.tts.vo.InterestRateMarketCurrencyInfoVo;
import com.tts.vo.SpotMarketInstrDetailInfoVo;
import com.tts.vo.TenorVo;


public class SessionInfoManager {
	public final static long ACCEPTABLE_ROLLOVER_MAP_INTERVAL = ChronologyUtil.MILLIS_IN_MINUTE * 3; // 3 minutes
	public static final List<String> STD_TENORS;

	
	private static final Logger log = LoggerFactory.getLogger(SessionInfoManager.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(log);
	

	static {
		List<SystemProperty> stdTenorProperties = RuntimeDataService.getRunTimeDataList("CONSTANTS", "STD_TENOR", null);

		List<String> _stdTenors = new ArrayList<String>(stdTenorProperties.size());
		
		boolean foundSpot = false;
		for ( SystemProperty stdTenorProperty: stdTenorProperties) {
			String tenor = stdTenorProperty.getValue();
			if ( TenorVo.NOTATION_SPOT.equals(tenor)) {
				foundSpot = true;
			}
			_stdTenors.add(tenor);
		}
		if ( !foundSpot) {
			_stdTenors.add(TenorVo.NOTATION_SPOT);
		}
		
		STD_TENORS = Collections.unmodifiableList(_stdTenors);
		
	}
	@Inject
	IFxCalendarBizService fxCalendarBizService;

	@Inject
	TradingSessionManager tradingSessionManager;

	@Inject
	MarketSpotInstrInfoDataService marketSpotInstrInfoDataService;
	
	@Inject
	MarketForwardInstrInfoDataService marketForwardInstrInfoDataService;
	
	@Inject
	MarketInterestRateCurrencyInfoDataService marketInterestRateCurrencyInfoDataService;
	
	@Inject
	SessionInfo sessionInfo;

	/**
	 * 
	 */
	public void init() {
		setup();
	}
	
	/**
	 * 
	 */
	public void setup() {
		sessionInfo.setTimeoutInterval(getTimeoutInterval());
		TradingSession tradingSession = tradingSessionManager.getActiveSession();
		String currentTradingSession = tradingSession.getName();
		switchTradingSession(currentTradingSession, null, true);
		
	}
	
	/**
	 * 
	 * @param toTradingSession
	 * @param rolloverDateMap
	 * @param firstTime
	 */
	public void switchTradingSession(String toTradingSession, RolloverDateMap rolloverDateMap, boolean firstTime) {
		sessionInfo.setActive(false);
		
		List<String> requiredPlugins = new ArrayList<String>();
		
		TradingSession tradingSession = tradingSessionManager.getSessionByName(toTradingSession);
		sessionInfo.setTradingSessionId(tradingSession.getPk());
		sessionInfo.setTradingSessionName(tradingSession.getName());
		
		populateSpotMarketDataSet(requiredPlugins, rolloverDateMap);
		populateForwardMarketDataSet(requiredPlugins, rolloverDateMap);
		populateInterestRateMarketDateSet(requiredPlugins, rolloverDateMap);
		//populateManualSpotMarketDataSet(requiredPlugins, rolloverDateMap);
		populateFxVanillaOptionsMarketDataSet(requiredPlugins, rolloverDateMap);
		
		sessionInfo.setActivePlugins(requiredPlugins.toArray(new String[0]));
		sessionInfo.setActive(true);
		
		preCalculateValueDate(rolloverDateMap);
		
		for (PublishingFormatType type : PublishingFormatType.values()) {
			monitorAgent.logMessage("switchTradingSession",
					String.format("Trading Session: %s, Product: %s, No. of Instruments: %d", 
							toTradingSession, type.name(), sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString()).size()));
		}
		
	}
	
	private void populateInterestRateMarketDateSet(
			List<String> requiredPlugins, RolloverDateMap rolloverDateMap) {
		final long currentSystemTime = System.currentTimeMillis();
		final String destinationType = PublishingFormatType.FxInterestRate.toString();
		final boolean useRolloverDateMap = rolloverDateMap != null && (rolloverDateMap.getReceivedTimestamp() - currentSystemTime) < ACCEPTABLE_ROLLOVER_MAP_INTERVAL;
		final List<InterestRateMarketCurrencyInfoVo> vos = marketInterestRateCurrencyInfoDataService.findSymbols(sessionInfo.getTradingSessionName()) ;
		final String[] newSymbols = new String[vos.size()];
		if (vos.size() > 0) {
			int i =0 ;
			for (InterestRateMarketCurrencyInfoVo instr : vos) {
				newSymbols[i] = instr.getSymbol();
				i++;
			}
		}
		sessionInfo.reconfigure(destinationType, newSymbols);

		
		for (InterestRateMarketCurrencyInfoVo instr: vos) {
			String symbol = instr.getSymbol();
			
			String tradeDateFormatted = null;
			if ( useRolloverDateMap) {
				tradeDateFormatted = rolloverDateMap.getTradeDate(symbol);
			} 
			if ( tradeDateFormatted == null ) {
				LocalDate tradeDate = fxCalendarBizService.getCurrentBusinessDay(symbol);
				tradeDateFormatted = ChronologyUtil.getDateString(tradeDate);
			}
			
			IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(destinationType, symbol);
			if (individualInfo == null) {
				log.debug(String.format("configuration is missing symbol: %s", symbol));
				individualInfo = new IndividualInfoVo(symbol);
				sessionInfo.getMarketDataset().getMarketStructuresByType(destinationType).put(symbol, individualInfo);
				log.debug(String.format("configuration for symbol: %s is added", symbol));
			}
			
			individualInfo.setTradeDateString(tradeDateFormatted);
			individualInfo.setRefreshInterval(instr.getRefreshInterval());
			individualInfo.setTopic(String.format(IEventMessageTypeConstant.Market.TOPIC_MMDC_TEMPLATE, symbol));

			String[] tenors = null;
			if (instr.getTenors() != null && !instr.getTenors().isEmpty()) {
				tenors = new String[instr.getTenors().size()];
				int j = 0;
				for (TenorVo tenor : instr.getTenors()) {
					tenors[j] = tenor.toString();
					j ++;
				}
			} else {
				tenors = new String[0];
				individualInfo.addIndicativeSubStatus(IndicativeSubStatus.CONFIGURATION_MktStructNotDefined);
			}
			individualInfo.setTenors(tenors);
			
			if (instr.getAdapterName() != null) {
				if (!requiredPlugins.contains(instr.getAdapterName())) {
					requiredPlugins.add(instr.getAdapterName());					
				}
			} else {
				individualInfo.addIndicativeSubStatus(IndicativeFlag.IndicativeSubStatus.CONFIGURATION_PluginNotDefined);
			}
			
			if (individualInfo.getIndicativeSubStatus() != IndicativeFlag.TRADABLE ) {
				individualInfo.addIndicativeReason(IndicativeReason.MA_MarketRateConfigurationError);
			}
		}
	}

	/**
	 * @return
	 */
	private long getTimeoutInterval() {
		Long injectInterval = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.CONSTANTS,
				SysProperty.Key1.INJECT_INTERVAL, null);
		if (injectInterval == null) {
			injectInterval = SysProperty.DefaultValue.INJECT_INTERVAL;
		}
		Long duration = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.CONSTANTS,
				SysProperty.Key1.NUM_LAST_LOOK_PRICES, null);
		if (duration == null) {
			duration = SysProperty.DefaultValue.NUM_LAST_LOOK_PRICES;
		}
		return injectInterval * Math.round(duration.doubleValue() / 2);
	}
	
	/**
	 * @param marketDataset
	 * @param requiredPlugins
	 * @param rolloverDateMap
	 */
	private void populateSpotMarketDataSet(List<String> requiredPlugins, RolloverDateMap rolloverDateMap) {
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		String debugSymbolSetting = p.getProperty("spotadapter.debugSymbols", "");
		
		long currentSystemTime = System.currentTimeMillis();
		boolean useRolloverDateMap = rolloverDateMap != null && (rolloverDateMap.getReceivedTimestamp() - currentSystemTime) < ACCEPTABLE_ROLLOVER_MAP_INTERVAL;
		List<SpotMarketInstrDetailInfoVo> spotMktInstrDetails = marketSpotInstrInfoDataService.findInstrument(sessionInfo.getTradingSessionName());
		List<String> newSpotSymbols       = new ArrayList<>();
		List<String> newSpotManualSymbols = new ArrayList<>();
		
		String destinationType1 = PublishingFormatType.FxSpot.toString();
		String destinationType2 = PublishingFormatType.FxSpotManual.toString();
		
		
		if(spotMktInstrDetails.size() > 0) {
			for(SpotMarketInstrDetailInfoVo instr : spotMktInstrDetails) {
				if(instr.getAdapterName().equals(IApp.AdapterTypes.FIXADAPTER.toString()))	{
					newSpotSymbols.add(instr.getSymbol());
				}
				else	{
					newSpotManualSymbols.add(instr.getSymbol());
				}
			}	
		}
		
		sessionInfo.reconfigure(destinationType1, newSpotSymbols.toArray(new String[0]));
		sessionInfo.reconfigure(destinationType2, newSpotManualSymbols.toArray(new String[0]));
		
		for (SpotMarketInstrDetailInfoVo instr : spotMktInstrDetails) {
			String symbol = instr.getSymbol();			
			String tradeDateFormatted = null;
			
			if(useRolloverDateMap) {
				tradeDateFormatted = rolloverDateMap.getTradeDate(symbol);
			} 
			if(tradeDateFormatted == null) {
				LocalDate tradeDate = fxCalendarBizService.getCurrentBusinessDay(symbol);
				tradeDateFormatted = ChronologyUtil.getDateString(tradeDate);
			}
			
			IndividualInfoVo individualInfo = null;
			if(instr.getAdapterName().equals(AdapterTypes.MANUALRATEADAPTER.toString()))
				individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(destinationType2, symbol);
			else
				individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(destinationType1, symbol);
			
			if (individualInfo == null) {
				log.debug(String.format("configuration is missing symbol: %s", symbol));
				individualInfo = new IndividualInfoVo(symbol);
				
				if(instr.getAdapterName().equals(AdapterTypes.MANUALRATEADAPTER.toString()))
					sessionInfo.getMarketDataset().getMarketStructuresByType(destinationType2).put(symbol, individualInfo);
				else
					sessionInfo.getMarketDataset().getMarketStructuresByType(destinationType1).put(symbol, individualInfo);
				log.debug(String.format("configuration for symbol: %s is added", symbol));
			}
			individualInfo.setTradeDateString(tradeDateFormatted);
			individualInfo.setRefreshInterval(instr.getRefreshInterval());
			individualInfo.setTopic(String.format(IEventMessageTypeConstant.Market.TOPIC_SPOT_TEMPLATE, symbol));
			
			long[] liquidities = null;
			if (instr.getLiquidities() != null && !instr.getLiquidities().isEmpty()) {
				liquidities = new long[instr.getLiquidities().size()];
				int j = 0;
				for (long liquidityLevel: instr.getLiquidities() ) {
					liquidities[j] = liquidityLevel;
					j++;
				}
			} else {
				liquidities = new long[0];
				individualInfo.addIndicativeSubStatus(IndicativeSubStatus.CONFIGURATION_MktStructNotDefined);
			}
			individualInfo.setLiquidities(liquidities);
			individualInfo.setDebug(debugSymbolSetting.contains(symbol));
			if (instr.getAdapterName() != null) {
				if (!requiredPlugins.contains(instr.getAdapterName())) {
					requiredPlugins.add(instr.getAdapterName());					
				}
			} else {
				individualInfo.addIndicativeSubStatus(IndicativeFlag.IndicativeSubStatus.CONFIGURATION_PluginNotDefined);
			}
			
			if (individualInfo.getIndicativeSubStatus() != IndicativeFlag.TRADABLE ) {
				individualInfo.addIndicativeReason(IndicativeReason.MA_MarketRateConfigurationError);
			}
			
			if ( rolloverDateMap == null )  {
				individualInfo.setLastRequest(-1L);
			}
		}
	}
	
	/*private void populateManualSpotMarketDataSet(List<String> requiredPlugins, RolloverDateMap rolloverDateMap)	{
		
		List<SpotMarketInstrDetailInfoVo> spotManualInstrDetails = marketSpotInstrInfoDataService.findInstruments(sessionInfo.getTradingSessionName(), null, AdapterTypes.MANUALRATEADAPTER.getAdapterName());
		
		long currentSystemTime     = System.currentTimeMillis();
		boolean useRolloverDateMap = rolloverDateMap != null && (rolloverDateMap.getReceivedTimestamp() - currentSystemTime) < ACCEPTABLE_ROLLOVER_MAP_INTERVAL;
		
		String[] newSymbols    = new String[spotManualInstrDetails.size()];
		String destinationType = PublishingFormatType.FxSpotManual.toString();
		
		if(spotManualInstrDetails.size() > 0) {
			int i =0 ;
			for(SpotMarketInstrDetailInfoVo instr : spotManualInstrDetails) {
				newSymbols[i] = instr.getSymbol();
				i++;
			}	
		}	
		sessionInfo.reconfigure(destinationType, newSymbols);
		
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		String debugSymbolSetting  = p.getProperty("spotadapter.debugSymbols", "");
		
		for(SpotMarketInstrDetailInfoVo instr : spotManualInstrDetails)	{
			String symbol = instr.getSymbol();
			String tradeDateFormatted = null;
			
			if(useRolloverDateMap) {
				tradeDateFormatted = rolloverDateMap.getTradeDate(symbol);
			} 
			if(tradeDateFormatted == null) {
				LocalDate tradeDate = fxCalendarBizService.getCurrentBusinessDay(symbol);
				tradeDateFormatted = ChronologyUtil.getDateString(tradeDate);
			}
			
			IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(destinationType, symbol);
			if (individualInfo == null) {
				log.debug(String.format("configuration is missing symbol: %s", symbol));
				individualInfo = new IndividualInfoVo(symbol);
				sessionInfo.getMarketDataset().getMarketStructuresByType(destinationType).put(symbol, individualInfo);
				log.debug(String.format("configuration for symbol: %s is added", symbol));
			}
			
			long[] liquidities = null;
			if (instr.getLiquidities() != null && !instr.getLiquidities().isEmpty()) {
				liquidities = new long[instr.getLiquidities().size()];
				int j = 0;
				for (long liquidityLevel: instr.getLiquidities() ) {
					liquidities[j] = liquidityLevel;
					j++;
				}
			} else {
				liquidities = new long[0];
				individualInfo.addIndicativeSubStatus(IndicativeSubStatus.CONFIGURATION_MktStructNotDefined);
			}
			
			individualInfo.setLiquidities(liquidities);
			individualInfo.setDebug(debugSymbolSetting.contains(symbol));
			individualInfo.setTradeDateString(tradeDateFormatted);
			individualInfo.setRefreshInterval(instr.getRefreshInterval());
			individualInfo.setTopic(String.format(IEventMessageTypeConstant.Market.TOPIC_SPOT_TEMPLATE, symbol));
			
			if(rolloverDateMap == null)  {
				individualInfo.setLastRequest(-1L);
			}
			
			if(instr.getAdapterName() != null) {
				if(!requiredPlugins.contains(instr.getAdapterName())) {
					requiredPlugins.add(instr.getAdapterName());					
				}
			} 
			else {
				individualInfo.addIndicativeSubStatus(IndicativeFlag.IndicativeSubStatus.CONFIGURATION_PluginNotDefined);
			}
			
			if(individualInfo.getIndicativeSubStatus() != IndicativeFlag.TRADABLE) {
				individualInfo.addIndicativeReason(IndicativeReason.MA_MarketRateConfigurationError);
			}
		}
	}*/
	
	private void populateFxVanillaOptionsMarketDataSet(List<String> requiredPlugins, RolloverDateMap rolloverDateMap) {
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		String debugSymbolSetting  = p.getProperty("spotadapter.debugSymbols", "");
		
		long currentSystemTime = System.currentTimeMillis();
		boolean useRolloverDateMap = rolloverDateMap != null && (rolloverDateMap.getReceivedTimestamp() - currentSystemTime) < ACCEPTABLE_ROLLOVER_MAP_INTERVAL;
		//List<SpotMarketInstrDetailInfoVo> spotMktInstrDetails = marketSpotInstrInfoDataService.findInstrument(sessionInfo.getTradingSessionName());
		List<String> fxVanillaSymbols = new ArrayList<>();
				
		String destinationType = PublishingFormatType.FxVanillaOptions.toString();
		
//		fxVanillaSymbols.add("USDCAD");
//		fxVanillaSymbols.add("EURUSD");
		//fxVanillaSymbols.add("AUDCAD");
		
		sessionInfo.reconfigure(destinationType, fxVanillaSymbols.toArray(new String[0]));
		for(String symbol: fxVanillaSymbols)	{
			String tradeDateFormatted = null;
			
			if(useRolloverDateMap) {
				tradeDateFormatted = rolloverDateMap.getTradeDate(symbol);
			} 
			if(tradeDateFormatted == null) {
				LocalDate tradeDate = fxCalendarBizService.getCurrentBusinessDay(symbol);
				tradeDateFormatted = ChronologyUtil.getDateString(tradeDate);
			}
			
			IndividualInfoVo individualInfo = null;
			individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(destinationType, symbol);
			
			if (individualInfo == null) {
				log.debug(String.format("configuration is missing symbol: %s", symbol));
				individualInfo = new IndividualInfoVo(symbol);
				
				sessionInfo.getMarketDataset().getMarketStructuresByType(destinationType).put(symbol, individualInfo);
				log.debug(String.format("configuration for symbol: %s is added", symbol));
			}
			
			individualInfo.setTradeDateString(tradeDateFormatted);
			//individualInfo.setRefreshInterval(instr.getRefreshInterval());
			individualInfo.setTopic(IEventMessageTypeConstant.Market.TOPIC_VOL_CURVE);
			individualInfo.setDebug(debugSymbolSetting.contains(symbol));
			
			if(rolloverDateMap == null)  {
				individualInfo.setLastRequest(-1L);
			}
		}
	}
	
	
	/**
	 * @param marketDataset
	 * @param requiredPlugins
	 * @param rolloverDateMap
	 */
	private void populateForwardMarketDataSet(List<String> requiredPlugins, RolloverDateMap rolloverDateMap) {
		long currentSystemTime = System.currentTimeMillis();
		boolean useRolloverDateMap = rolloverDateMap != null && (rolloverDateMap.getReceivedTimestamp() - currentSystemTime) < ACCEPTABLE_ROLLOVER_MAP_INTERVAL;
		List<ForwardMarketInstrDetailInfoVo> forwardMktInstrDetails = marketForwardInstrInfoDataService.findInstrument(sessionInfo.getTradingSessionName());
		String[] newSymbols = new String[forwardMktInstrDetails.size()];
		String destinationType = PublishingFormatType.FxForwards.toString();
		
		if (forwardMktInstrDetails.size() > 0) {
			int i =0 ;
			for (ForwardMarketInstrDetailInfoVo instr : forwardMktInstrDetails) {
				newSymbols[i] = instr.getSymbol();
				i++;
			}
		}
		sessionInfo.reconfigure(destinationType, newSymbols);

		for (ForwardMarketInstrDetailInfoVo instr: forwardMktInstrDetails) {
			String symbol = instr.getSymbol();
			
			String tradeDateFormatted = null;
			if ( useRolloverDateMap) {
				tradeDateFormatted = rolloverDateMap.getTradeDate(symbol);
			} 
			if ( tradeDateFormatted == null ) {
				LocalDate tradeDate = fxCalendarBizService.getCurrentBusinessDay(symbol);
				tradeDateFormatted = ChronologyUtil.getDateString(tradeDate);
			}
			
			IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(destinationType, symbol);
			if (individualInfo == null) {
				log.debug(String.format("configuration is missing symbol: %s", symbol));
				individualInfo = new IndividualInfoVo(symbol);
				sessionInfo.getMarketDataset().getMarketStructuresByType(destinationType).put(symbol, individualInfo);
				log.debug(String.format("configuration for symbol: %s is added", symbol));
			}
			
			individualInfo.setTradeDateString(tradeDateFormatted);
			individualInfo.setRefreshInterval(instr.getRefreshInterval());
			individualInfo.setTopic(String.format(IEventMessageTypeConstant.Market.TOPIC_FWDC_TEMPLATE, symbol));

			String[] tenors = null;
			if (instr.getTenors() != null && !instr.getTenors().isEmpty()) {
				tenors = new String[instr.getTenors().size()];
				int j = 0;
				for (TenorVo tenor : instr.getTenors()) {
					tenors[j] = tenor.toString();
					j ++;
				}
			} else {
				tenors = new String[0];
				individualInfo.addIndicativeSubStatus(IndicativeSubStatus.CONFIGURATION_MktStructNotDefined);
			}
			individualInfo.setTenors(tenors);
			
			if (instr.getAdapterName() != null) {
				if (!requiredPlugins.contains(instr.getAdapterName())) {
					requiredPlugins.add(instr.getAdapterName());					
				}
			} else {
				individualInfo.addIndicativeSubStatus(IndicativeFlag.IndicativeSubStatus.CONFIGURATION_PluginNotDefined);
			}
			
			if (individualInfo.getIndicativeSubStatus() != IndicativeFlag.TRADABLE ) {
				individualInfo.addIndicativeReason(IndicativeReason.MA_MarketRateConfigurationError);
			}
		}
	}
	
	private void preCalculateValueDate(RolloverDateMap rolloverDateMap) {
		final List<String> stdTenors = STD_TENORS;
		for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
			StringBuilder sb = new StringBuilder();
			Set<String> symbols = sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString());
			for (String symbol : symbols) {
				sb.append(symbol).append(' ');
				LocalDate tradeDate = LocalDate.now();
				if ( rolloverDateMap != null && rolloverDateMap.getTradeDate(symbol) != null) { 
					tradeDate = ChronologyUtil.getLocalDateFromString(rolloverDateMap.getTradeDate(symbol));
				}
				IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
				if (individualInfo != null) {
					Map<String, String> valueDates = new HashMap<String, String>();
					for ( String stdTenor: stdTenors) {
						LocalDate d = null;
						if ( TenorVo.NOTATION_SPOT.equals(stdTenor)) {
							d = fxCalendarBizService.getSpotValueDate(symbol, tradeDate, PricingConventionConstants.INTERBANK);
						} else {
							TenorVo tenor = TenorVo.fromString(stdTenor);
							d = fxCalendarBizService.getForwardValueDate(symbol, tradeDate, tenor.getPeriodCd(), tenor.getValue(), PricingConventionConstants.INTERBANK);
						}
						String date = ChronologyUtil.getDateString(d);
						sb.append(stdTenor).append(date);
						valueDates.put(stdTenor, date);
					}
					individualInfo.setValueDateMap(valueDates);	
				}
				sb.append('\n');
			}
			if ( IApp.PublishingFormatType.FxSpot == type ) {
				log.debug(sb.toString());
			}
		}
	}
}
