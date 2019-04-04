package com.tts.fixapi.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.util.AppUtils;

import quickfix.Log;
import quickfix.SessionID;

public class TtsFileLogV2 implements Log {
	
	private static final String EVENT_FORMAT_STR = "%s:%s";

	private final Logger eventLogger;
	private final Logger dataLogger;

	private final SessionID sessionID;
	
	public TtsFileLogV2(
			String string, 
			SessionID sessionID, 
			boolean includeMillis,
			boolean includeTimestampInMessages,
			boolean logHeartbeats) {
		
		String fileName        = sessionID.getSenderCompID() + "_" + sessionID.getTargetCompID();
		String msgLoggerName   = fileName + ".FixMsgLogger";
		String evntsLoggerName = fileName + ".FixEventLogger";
		
		AppUtils.createCustomRollingFileAppender((fileName.toUpperCase().trim() + "_" + "MSGS.log"), msgLoggerName, AppUtils.LOGLEVEL__INFO);
		AppUtils.createCustomRollingFileAppender((fileName.toUpperCase().trim() + "_" + "EVNTS.log"), evntsLoggerName, AppUtils.LOGLEVEL__INFO);
		
		this.dataLogger  = LoggerFactory.getLogger(msgLoggerName);
		this.eventLogger = LoggerFactory.getLogger(evntsLoggerName);
		
		this.sessionID   = sessionID;
	}

	@Override
	public void clear() {

	}

	@Override
	public void onErrorEvent(String text) {
		eventLogger.error(String.format(EVENT_FORMAT_STR, sessionID.toString(), text));

	}

	@Override
	public void onEvent(String text) {
		eventLogger.info(String.format(EVENT_FORMAT_STR, sessionID.toString(), text));
	}

	@Override
	public void onIncoming(String message) {
		dataLogger.info(" [IN ]  >>>   " + message);
	}

	@Override
	public void onOutgoing(String message) {
		dataLogger.info(" [OUT]  <<<   " + message);
	}
}
