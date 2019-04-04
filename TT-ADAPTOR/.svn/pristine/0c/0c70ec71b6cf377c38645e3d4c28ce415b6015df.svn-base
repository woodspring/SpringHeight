package com.tts.ske.qfx.impl.log;

import com.tts.ske.vo.LogControlVo;

import quickfix.Log;
import quickfix.LogFactory;
import quickfix.RuntimeError;
import quickfix.SessionID;
import quickfix.SessionSettings;

public class TtsFileLogFactoryV2 implements LogFactory {
    /**
     * File path for writing the session log.
     */
    public static final String SETTING_FILE_LOG_PATH = "FileLogPath";

    /**
     * Specify whether to include milliseconds in log output time stamps. Off, by
     * default.
     */
    public static final String SETTING_INCLUDE_MILLIS_IN_TIMESTAMP = "FileIncludeMilliseconds";
    
    /**
     * Specify whether to include time stamps for message input and output. Off, by
     * default.
     */
    public static final String SETTING_INCLUDE_TIMESTAMP_FOR_MESSAGES = "FileIncludeTimeStampForMessages";

    /**
     * Specify whether to include time stamps for message input and output. Off, by
     * default.
     */
    public static final String SETTING_LOG_HEARTBEATS = "FileLogHeartbeats";

    private final SessionSettings settings;
    
	private final LogControlVo logControl;

    /**
     * Create the factory with configuration in session settings.
     * 
     * @param settings
     */
    public TtsFileLogFactoryV2(SessionSettings settings, LogControlVo logControl) {
        this.settings = settings;
        this.logControl = logControl;
    }

    /**
     * Creates a file-based logger.
     * 
     * @param sessionID
     *            session ID for the logger
     */
    @Override
	public Log create(SessionID sessionID) {
        try {
            boolean includeMillis = false;
            if (settings.isSetting(sessionID, SETTING_INCLUDE_MILLIS_IN_TIMESTAMP)) {
                includeMillis = settings.getBool(sessionID, SETTING_INCLUDE_MILLIS_IN_TIMESTAMP);
            }
            
            boolean includeTimestampInMessages = false;
            if (settings.isSetting(sessionID, SETTING_INCLUDE_TIMESTAMP_FOR_MESSAGES)) {
                includeTimestampInMessages = settings.getBool(sessionID, SETTING_INCLUDE_TIMESTAMP_FOR_MESSAGES);
            }
       
            boolean logHeartbeats = true;
            if (settings.isSetting(sessionID, SETTING_LOG_HEARTBEATS)) {
                logHeartbeats = settings.getBool(sessionID, SETTING_LOG_HEARTBEATS);
            }

            //boolean isTTSSim = sessionIDstr.contains("TTS") && sessionIDstr.contains("BANK");
            //if ( isTTSSim ) {
            	return new TtsFileLogV2(settings.getString(sessionID, TtsFileLogFactoryV2.SETTING_FILE_LOG_PATH),
                    sessionID, logControl, includeMillis, includeTimestampInMessages, logHeartbeats);
            //}
            //return fileLogFactory.create(sessionID);
        } catch (Exception e) {
            throw new RuntimeError(e);
        }
    }

    @Override
	public Log create() {
        throw new UnsupportedOperationException();
    }
}
