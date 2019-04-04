package com.tts.mlp.qfix.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		if ( sessionID.toString().contains("ESP") ) {
			this.dataLogger = LoggerFactory.getLogger("FixMarketLogger");
		} else {
			this.dataLogger = LoggerFactory.getLogger("FixExecutionLogger");
		}
		this.eventLogger = LoggerFactory.getLogger("FixEventLogger");
		this.sessionID = sessionID;

	}

	@Override
	public void clear() {

	}

	@Override
	public void onErrorEvent(String arg0) {
		eventLogger.error(String.format(EVENT_FORMAT_STR, sessionID.toString(), arg0));

	}

	@Override
	public void onEvent(String arg0) {
		eventLogger.info(String.format(EVENT_FORMAT_STR, sessionID.toString(), arg0));
	}

	@Override
	public void onIncoming(String arg0) {
		dataLogger.debug(arg0);

	}

	@Override
	public void onOutgoing(String arg0) {
		dataLogger.debug(arg0);
	}

}
