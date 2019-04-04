package com.tts.mas.app.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.IQfixApp;
import com.tts.mas.app.MarketQFixApp;
import com.tts.mas.manager.SessionInfoManager;
import com.tts.mas.vo.RolloverDateMap;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.service.ITradingSessionAware;
import com.tts.message.system.RolloverStruct.RolloverNotification;
import com.tts.message.system.RolloverStruct.TradeDate;
import com.tts.message.system.SystemStatusStruct.ChangeTradingSession;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.app.IApp;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;
import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.protocol.platform.event.IEventReceiver;
import com.tts.protocol.platform.event.IEventSubscriber;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.db.TradingSessionManager;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.util.exception.InvalidServiceException;
import com.tts.util.flag.IndicativeFlag;
import com.tts.vo.TenorVo;

public class DataFlowController implements IEventSubscriber, ITradingSessionAware {


	private static final Logger logger = LoggerFactory.getLogger(DataFlowController.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	private static final String[] TOPICS_INTERESTED = new String[] {
		IEventMessageTypeConstant.REM.CE_TRADING_SESSION,
		IEventMessageTypeConstant.System.FX_ROLLOVER_EVENT
	};

	private final FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
	private final boolean spotLogoutWhileChangeTs;
	private final long spotThrottleBetweenCancelAndSubscription;
	private final IQfixApp qfixApp;
	private final TradingSessionManager tradingSessionManager;
	private final SessionInfo sessionInfo;
	private final SessionInfoManager sessionInfoManager;
	private final IInterfacingAppFactory interfacingAppFactory;
	private final IFxCalendarBizService fxCalendarBizSerivce;
	private final IReutersApp reutersAdapter;
	private final IEventReceiver[] eventReceivers = new IEventReceiver[TOPICS_INTERESTED.length];
	private volatile RolloverDateMap rolloverDateMap = null;	
	private volatile InterfacingAppController interfacingAppController = null;
	

	public DataFlowController(
			IInterfacingAppFactory publishingAppFactory, IQfixApp qfixApp) {
		super();
		IReutersApp _reutersAdapter = null;
		this.tradingSessionManager = AppContext.getContext().getBean(TradingSessionManager.class);
		this.sessionInfo = AppContext.getContext().getBean(SessionInfo.class);
		this.sessionInfoManager = AppContext.getContext().getBean(SessionInfoManager.class);;
		this.fxCalendarBizSerivce = AppContext.getContext().getBean(IFxCalendarBizService.class);
		try { 
			_reutersAdapter = AppContext.getContext().getBean(IReutersApp.class);
		} catch ( org.springframework.beans.factory.NoSuchBeanDefinitionException  e) {

		}
		this.reutersAdapter = _reutersAdapter;
		this.interfacingAppFactory = publishingAppFactory;
		this.qfixApp = qfixApp;
		this.spotThrottleBetweenCancelAndSubscription = p.getProperty("spotadapter.throttle_cancel_subscribe", -1L);
		this.spotLogoutWhileChangeTs = p.getProperty("spotadapter.logout_while_change_ts", false);		

	}

	public void init() throws Exception {
		String currentActiveTradingSession = tradingSessionManager.getActiveSessionNm();

		switchTradingSession(currentActiveTradingSession);
		
		int i = 0;
		for (String topic : TOPICS_INTERESTED) {
			IEventReceiver eventReceiver = AppContext.getContext().getBean("defaultEventReceiver", IEventReceiver.class);
			eventReceiver.setEventSubscriber(this);
			eventReceiver.setTopicInterested(topic);
			eventReceivers[i] = eventReceiver;
			i++;
		}
	}

	public void destroy() throws Exception {
		if ( interfacingAppController != null ) {
			interfacingAppController.destroy();
		}

		for ( IEventReceiver receiver: eventReceivers) {
			if (receiver != null ) receiver.destroy();
		}
	}

	@Override
	public void onEvent(TtMsg message, IMsgProperties msgProperties) {
		String func = "EventController.onEvent".intern();
		String eventType = msgProperties.getSendTopic();
		
		try {
			monitorAgent.debug(String.format("OnEvent(SYSTEM): %s", eventType));
			
			/**
			 * process event one-by-one
			 * 
			 * - potential race condition if Rollover event and CRM arrived near the same time 
			 */
			synchronized (sessionInfo) {
				if (IEventMessageTypeConstant.REM.CE_TRADING_SESSION.equals(eventType)) {
					ChangeTradingSession tradingSession = ChangeTradingSession.parseFrom(message.getParameters());
					if (tradingSession.getChangeTo() != null) {
						String toTradingSession = tradingSession.getChangeTo().getTradingSessionNm();
						switchTradingSession(toTradingSession);
					}

				} else if (IEventMessageTypeConstant.System.FX_ROLLOVER_EVENT.equals(eventType)) {
					for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
						Set<String> symbols = sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString());
						for (String symbol : symbols) {
							IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
							if (individualInfo != null) {
								individualInfo.addIndicativeReason(IndicativeFlag.IndicativeReason.MA_RolloverConfiguration);
							}
						}
					}
					
					RolloverDateMap newRolloverDateMap = new RolloverDateMap();
					newRolloverDateMap.setReceivedTimestamp(System.currentTimeMillis());
					RolloverNotification rolloverNotification = RolloverNotification.parseFrom(message.getParameters());
					for (TradeDate tradeDate: rolloverNotification.getNewTradeDateList() )  {
						String symbol = tradeDate.getCurrencyOrInstrument();
						String localTradeDate = tradeDate.getLocalDate();
						LocalDate tradeDateDate = ChronologyUtil.getLocalDateFromString(localTradeDate);
						newRolloverDateMap.setTradeDate(symbol, localTradeDate);
						for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
							IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
							if (individualInfo != null) {
								individualInfo.setTradeDateString(localTradeDate);
								Map<String, String> valueDates = new HashMap<String, String>(individualInfo.getValueDateMap());
								for ( String stdTenor: SessionInfoManager.STD_TENORS) {
									LocalDate d = null;
									if ( TenorVo.NOTATION_SPOT.equals(stdTenor)) {
										d = fxCalendarBizSerivce.getSpotValueDate(symbol, tradeDateDate, PricingConventionConstants.INTERBANK);
									} else {
										TenorVo tenor = TenorVo.fromString(stdTenor);
										d = fxCalendarBizSerivce.getForwardValueDate(symbol, tradeDateDate, tenor.getPeriodCd(), tenor.getValue(), PricingConventionConstants.INTERBANK);
									}
									valueDates.put(stdTenor, ChronologyUtil.getDateString(d));
								}
								individualInfo.setValueDateMap(valueDates);	
							}
						}
					}
					rolloverDateMap = newRolloverDateMap;
					
					for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
						Set<String> symbols = sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString());
						for (String symbol : symbols) {
							IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
							if (individualInfo != null) {
								individualInfo.removeIndicativeReason(IndicativeFlag.IndicativeReason.MA_RolloverConfiguration);
							}
						}
					}
					
				} else {
					monitorAgent.warn(String.format("Unknow Event Type: %s", eventType));
				}
			}

		} catch (Exception ex) {
			monitorAgent.logError(func, eventType, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, ex.getMessage(), ex);
		}
	}

	@Override
	public void switchTradingSession(String toTradingSession) throws InvalidServiceException {
		for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
			Set<String> symbols = sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString());
			for (String symbol : symbols) {
				IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
				if (individualInfo != null) {
					individualInfo.addIndicativeReason(IndicativeFlag.IndicativeReason.MA_MarketRateTradingSessionChange);
					individualInfo.setLastRefresh(0L);
				}
			}
		}

		
		try {
			logger.debug("switching trading session: cleaning up");
			if ( interfacingAppController != null ) {
				interfacingAppController.destroy(true);
			}

			for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
				Set<String> symbols = sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString());
				for (String symbol : symbols) {
					IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
					if (individualInfo != null) {
						individualInfo.setLastRefresh(-1L);
						individualInfo.setLastRequest(-1L);
					}
				}
			}

			if ( !spotLogoutWhileChangeTs && spotThrottleBetweenCancelAndSubscription > 0 ) {
				Thread.sleep(spotThrottleBetweenCancelAndSubscription);
			}
			
			p.reload();
			sessionInfoManager.switchTradingSession(toTradingSession, rolloverDateMap, false);
			qfixApp.switchTradingSession(toTradingSession);
			if ( reutersAdapter != null ) {
				reutersAdapter.switchTradingSession(toTradingSession);
			}
			
			MarketQFixApp mQfixApp = null;
			if ( qfixApp instanceof MarketQFixApp) {
				mQfixApp = (MarketQFixApp) qfixApp; 
				logger.debug("switching trading session: checking quickfix connectivity");
				int count = 0;
				while ( count < 30  && !mQfixApp.isAllSessionsLoggedOn() ) {
					Thread.sleep(500L);
					count++;
				}
				if ( !mQfixApp.isAllSessionsLoggedOn()) {
					logger.warn("please check FIX connectivity");
				} else {
					logger.debug("switching trading session: checking quickfix connectivity... DONE");
				}			
			}
			
			logger.debug("switching trading session: starting up");
			
			if ( interfacingAppController == null ) {
				interfacingAppController = new InterfacingAppController(interfacingAppFactory);
			}
			interfacingAppController.init(sessionInfo);
			
			logger.debug("switching trading session: completing");

		} catch (Exception e) {
			e.printStackTrace();
			throw new InvalidServiceException("Fail to change trading session. ", e);
		}
		
		
		for (IApp.PublishingFormatType type : IApp.PublishingFormatType.values()) {
			Set<String> symbols = sessionInfo.getMarketDataset().getAvailableSymbolsByType(type.toString());
			for (String symbol : symbols) {
				IndividualInfoVo individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(type.toString(), symbol);
				if (individualInfo != null) {
					individualInfo.removeIndicativeReason(IndicativeFlag.IndicativeReason.MA_MarketRateTradingSessionChange);
				}
			}
		}
	}
}
