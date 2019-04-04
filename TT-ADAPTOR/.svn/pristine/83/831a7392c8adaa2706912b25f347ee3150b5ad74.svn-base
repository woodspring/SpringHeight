package com.tts.mas.qfx.impl.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.vo.LogControlVo;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;

import quickfix.Log;
import quickfix.SessionID;

public class TtsFileLogV2 implements Log {
	private static final String SEPARATOR        = ".";
	private static final String EVENT_FORMAT_STR = "%s:%s";

	private final Logger eventLogger;
	private final Logger dataLogger;
	private final LogControlVo logControlUnit;
	private static final Logger logger = LoggerFactory.getLogger(TtsFileLogV2.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	private final SessionID sessionID;
	private final boolean isMarketDataSession;
	private final boolean isMDDynamicLogControlEnabled;
	
	public TtsFileLogV2(
			String string, 
			SessionID sessionID,
			LogControlVo logControlUnit,
			boolean includeMillis,
			boolean includeTimestampInMessages,
			boolean logHeartbeats) {
		
		boolean _isMarketDataSession = false;
		boolean _hourlyRollOver      = false;
		IFixSetting fixSettings      = AppContext.getContext().getBean(IFixSetting.class);
		String priceSessionId        = fixSettings.getMarketPriceSessionId();
		
		System.out.println("<<<<<>>>>> priceSessionId: " + priceSessionId + ", sessionId: " + sessionID.toString());
		if((priceSessionId != null) && (sessionID.toString().equals(priceSessionId)))	{
			_isMarketDataSession = true;
			_hourlyRollOver = true;
		}
		
		
		String loggerName  = sessionID.getSenderCompID()
				   		   + SEPARATOR
				   		   + sessionID.getTargetCompID();
		String logFileName = loggerName
						   + SEPARATOR
						   + AppUtils.getAppName()
						   + ".msg.log";
		
		if((priceSessionId != null) && (sessionID.toString().equals(priceSessionId)))	{
			this.dataLogger = LoggerFactory.getLogger("FixMarketLogger");
		}
		else	{
			AppUtils.createCustomRollingFileAppender(logFileName, loggerName, AppUtils.LOGLEVEL__INFO, "%d - %msg%n%ex", _hourlyRollOver);
			this.dataLogger = LoggerFactory.getLogger(loggerName);
		}
		
		
		this.logControlUnit = logControlUnit;
		this.isMarketDataSession = _isMarketDataSession;
		this.eventLogger = LoggerFactory.getLogger("FixEventLogger");
		this.sessionID   = sessionID;
		
		String isMdDynamicLogControlEnabledStr = System.getenv("MARKET_DATA_LOGGING_DYNAMIC_CONTROL");
		boolean isMdDynamicLogControlEnabled = false;
		if (isMdDynamicLogControlEnabledStr != null ) {
			isMdDynamicLogControlEnabled = Boolean.parseBoolean(isMdDynamicLogControlEnabledStr);
		}
		this.isMDDynamicLogControlEnabled = isMdDynamicLogControlEnabled;
		if ( isMarketDataSession && isMDDynamicLogControlEnabled) {
			logger.info("Enabling MarketData logging dynamic control.");
		}
	}

	@Override
	public void clear() {

	}

	@Override
	public void onErrorEvent(String arg0) {
		eventLogger.error(String.format(EVENT_FORMAT_STR, sessionID.toString(), arg0));
		logger.error("FIX engine ERROR EVENT: " + sessionID.toString() + ": " + arg0);		
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());
		String msg = String.format("FIX engine ERROR: SessionID<%s> %s", sessionID.toString() ,arg0);
    	monitorAgent.logErrorNotification("FIX engine:ERROR", topic,  MonitorConstant.FXADT.ERROR_ON_FIX_ERROR_EVENT, AppUtils.getAppName(), msg);
	}

	@Override
	public void onEvent(String arg0) {
		eventLogger.info(String.format(EVENT_FORMAT_STR, sessionID.toString(), arg0));
		logger.info("FIX engine EVENT: " + sessionID.toString() + ": " + arg0);
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
		String msg = String.format("FIX engine event: SessionID<%s> %s", sessionID.toString() ,arg0);
    	monitorAgent.logInfoNotification("FIX engine:Event", topic,  MonitorConstant.FXADT.INFO_ON_FIX_EVENT, AppUtils.getAppName(), msg);
	}

	@Override
	public void onIncoming(String arg0) {
		if(isMarketDataSession && isMDDynamicLogControlEnabled) {
			if(logControlUnit.isLogMarketData()) {
				dataLogger.info(" [IN ]  >>>   " + arg0);
			}
		} else {
			dataLogger.info(" [IN ]  >>>   " + arg0);
		}
	}

	@Override
	public void onOutgoing(String arg0) {
		if(isMarketDataSession && isMDDynamicLogControlEnabled) {
			if(logControlUnit.isLogMarketData()) {
				dataLogger.info(" [OUT]  <<<   " + arg0);
			}
		} else {
			dataLogger.info(" [OUT]  <<<   " + arg0);
		}
	}
}