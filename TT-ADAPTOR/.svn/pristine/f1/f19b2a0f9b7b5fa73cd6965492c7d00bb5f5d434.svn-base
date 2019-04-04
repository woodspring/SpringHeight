package com.tts.mas.qfx.impl.log;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Log;
import quickfix.MessageUtils;
import quickfix.SessionID;
import quickfix.SystemTime;
import quickfix.field.converter.UtcTimestampConverter;

public class TtsFileLog implements Log, Closeable {
	
	@SuppressWarnings("unused")
	private static final String ERROR_EVENT_CATEGORY = "error";
	
	private static final String MSG_TEMPLATE = "<%s, %s, %s> (%s)";

	private static final String EVENT_CATEGORY = "event";

	private static final String OUTGOING_CATEGORY = "outgoing";

	private static final String INCOMING_CATEGORY = "incoming";

	private final Logger logger;

	private final SessionID sessionID;

	private final boolean incoming;

	private final boolean outgoing;

	private final boolean events;

	private final boolean includeMillis;

	private final boolean logHeartbeats;

	TtsFileLog(boolean incoming, boolean outgoing, boolean events, boolean logHeartbeats, boolean includeMillis, SessionID sessionID) {
		this.incoming = incoming;
		this.outgoing = outgoing;
		this.events = events;
		this.sessionID = sessionID;
		this.includeMillis = includeMillis;
		this.logHeartbeats = logHeartbeats;
		logger = LoggerFactory.getLogger("FixLogger");
	}

	private void logMessage(String message, String type) {
		log(message, type);
	}

	@Override
	public void onEvent(String message) {
		if (events) {
			log(message, EVENT_CATEGORY);
		}
	}

	@Override
	public void onErrorEvent(String message) {
		logger.warn(message);
	}

	private void log(String message, String type) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format(MSG_TEMPLATE, 
					UtcTimestampConverter.convert(SystemTime.getDate(), includeMillis), sessionID, type, message));
		}
	}

	@Override
	public void clear() {
		onEvent("Log clear operation is not supported: " + getClass().getName());
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public void onIncoming(String message) {
		if (!logHeartbeats && MessageUtils.isHeartbeat(message)) {
            return;
        }
		if (incoming) {
			logMessage(message, INCOMING_CATEGORY);
		}
	}

	@Override
	public void onOutgoing(String message) {
		if (!logHeartbeats && MessageUtils.isHeartbeat(message)) {
            return;
        }
		if (outgoing) {
			logMessage(message, OUTGOING_CATEGORY);
		}
	}

}
