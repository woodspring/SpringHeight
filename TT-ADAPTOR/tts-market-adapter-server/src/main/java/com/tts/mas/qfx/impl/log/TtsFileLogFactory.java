package com.tts.mas.qfx.impl.log;

import quickfix.ConfigError;
import quickfix.FieldConvertError;
import quickfix.Log;
import quickfix.LogFactory;
import quickfix.RuntimeError;
import quickfix.SessionID;
import quickfix.SessionSettings;

public class TtsFileLogFactory implements LogFactory {

    private SessionSettings settings;
    private boolean incoming;
    private boolean outgoing;
    private boolean events;
    private boolean heartBeats;
    private boolean includeMillis;

    /**
     * Enables incoming message logging.
     * 
     * Valid values: "Y" or "N"<br/>
     * Default Value: "N"
     */
    public static final String SETTING_LOG_INCOMING = "ScreenLogShowIncoming";

    /**
     * Enables outgoing message logging.
     * 
     * Valid values: "Y" or "N"<br/>
     * Default Value: "N"
     */
    public static final String SETTING_LOG_OUTGOING = "ScreenLogShowOutgoing";

    /**
     * Enables session event logging.
     * 
     * Valid values: "Y" or "N"<br/>
     * Default Value: "N"
     */
    public static final String SETTING_LOG_EVENTS = "ScreenLogShowEvents";

    /**
     * Flag for controlling output of heartbeat messages.
     * 
     * Valid values: "Y" or "N"<br/>
     * Default Value: "Y"
     */
    public static final String SETTING_LOG_HEARTBEATS = "ScreenLogShowHeartBeats";

    /**
     * Specify whether to include milliseconds in log output time stamps. Off, by
     * default.
     */
    public static final String SETTING_INCLUDE_MILLIS_IN_TIMESTAMP = "ScreenIncludeMilliseconds";

    /**
     * Create factory using configuration in session settings.
     * 
     * @param settings
     *            the session settings
     */
    public TtsFileLogFactory(SessionSettings settings) {
        this(true, true, true);
        this.settings = settings;
    }

    /**
     * 
     * Create factory with explicit control of message categories.
     * 
     * @param incoming
     *            if true, log incoming messages
     * @param outgoing
     *            if true, log outgoing messages
     * @param events
     *            if true, log events
     */
    public TtsFileLogFactory(boolean incoming, boolean outgoing, boolean events) {
        this(incoming, outgoing, events, true);
    }

    /**
     * Default constructor that logs incoming, outgoing, and events without heartbeats.
     *
     */
    public TtsFileLogFactory() {
        this(true, true, true, false);
    }

    /**
     * 
     * Create factory with explicit control of message categories.
     * 
     * @param incoming
     *            if true, log incoming messages
     * @param outgoing
     *            if true, log outgoing messages
     * @param events
     *            if true, log events
     * @param logHeartBeats
     *            if true, log heart beat messages (the default)
     */
    public TtsFileLogFactory(boolean incoming, boolean outgoing, boolean events,
            boolean logHeartBeats) {
        this.incoming = incoming;
        this.outgoing = outgoing;
        this.events = events;
        this.heartBeats = logHeartBeats;
    }

    @Override
    public Log create(SessionID sessionID) {
        try {
            incoming = getBooleanSetting(sessionID, TtsFileLogFactory.SETTING_LOG_INCOMING, incoming);
            outgoing = getBooleanSetting(sessionID, TtsFileLogFactory.SETTING_LOG_OUTGOING, outgoing);
            events = getBooleanSetting(sessionID, TtsFileLogFactory.SETTING_LOG_EVENTS, events);
            heartBeats = getBooleanSetting(sessionID, TtsFileLogFactory.SETTING_LOG_HEARTBEATS, heartBeats);
            includeMillis = getBooleanSetting(sessionID, TtsFileLogFactory.SETTING_INCLUDE_MILLIS_IN_TIMESTAMP, false);
            return new TtsFileLog(incoming, outgoing, events, heartBeats, includeMillis, sessionID);
        } catch (FieldConvertError e) {
            throw new RuntimeError(e);
        } catch (ConfigError e) {
            throw new RuntimeError(e);
        }
    }

    private boolean getBooleanSetting(SessionID sessionID, String key, boolean incoming)
            throws ConfigError, FieldConvertError {
        if (settings != null && settings.isSetting(sessionID, key)) {
            incoming = settings.getBool(sessionID, key);
        }
        return incoming;
    }

	@Override
	public Log create() {
		return null;
	}

}
