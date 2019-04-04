package com.tts.mas.qfx.impl;

import javax.management.JMException;
import javax.management.ObjectName;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.qfx.impl.log.TtsFileLogFactoryV2;
import com.tts.mas.vo.LogControlVo;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketInitiator;

public class QuickfixEngineInitiatorContainer {
    private final static Logger log = LoggerFactory.getLogger(QuickfixEngineInitiatorContainer.class);
    private final ThreadedSocketInitiator initiator;
    private final static JmxExporter jmxExporter;
    private final ObjectName connectorObjectName;
    private boolean initiatorStarted = false;
    
    static {
    	JmxExporter _jmxExporter;
        try {
        	_jmxExporter = new JmxExporter();
		} catch (JMException e) {
			e.printStackTrace();
			_jmxExporter = null;
		}
        jmxExporter = _jmxExporter;
    }
    
	public QuickfixEngineInitiatorContainer(
			SessionSettings settings, 
			Application quickfixApplication,
			LogControlVo logControl	) throws ConfigError, FieldConvertError, JMException {
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new TtsFileLogFactoryV2(settings, logControl);
        MessageFactory messageFactory = new DefaultMessageFactory();

        initiator = new ThreadedSocketInitiator (
        		quickfixApplication, 
        		messageStoreFactory, 
        		settings, 
        		logFactory,
                messageFactory);

        connectorObjectName = jmxExporter.register(initiator);
        log.info("Initiator registered with JMX, name=" + connectorObjectName);
        
	}
	

    public synchronized void logon() {
        if (!initiatorStarted) {
            try {
                initiator.start();
                initiatorStarted = true;
                
                for (SessionID sessionId : initiator.getSessions()) {
                    Session session = Session.lookupSession(sessionId);
                    log.debug("Logging on FixSession, " + sessionId);
                    session.logon();
                }
            } catch (Exception e) {
                log.error("Logon failed", e);
            }
        } else {
            for (SessionID sessionId : initiator.getSessions()) {
                Session session = Session.lookupSession(sessionId);
                if ( ! session.isLoggedOn()) {
                	session.logon();
                }
    
            }
        }
    }

    public synchronized void logout() {
        for (SessionID sessionId : initiator.getSessions()) {
            Session.lookupSession(sessionId).logout("user requested");
        }
        initiatorStarted = false;

    }


	public boolean isLoggedOn() {
		return initiator.isLoggedOn();
	}
	
	public int getStackedQueueSize() {
		return initiator.getQueueSize();
	}
}
