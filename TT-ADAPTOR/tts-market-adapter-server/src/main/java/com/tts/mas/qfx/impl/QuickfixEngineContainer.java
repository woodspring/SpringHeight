package com.tts.mas.qfx.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.management.JMException;
import javax.management.ObjectName;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.qfx.impl.log.TtsFileLogFactoryV2;
import com.tts.mas.vo.LogControlVo;
import com.tts.util.chronology.ChronologyUtil;

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
import quickfix.SocketInitiator;

public class QuickfixEngineContainer {
    private final static Logger log = LoggerFactory.getLogger(QuickfixEngineContainer.class);
    private final SocketInitiator initiator;
    private final JmxExporter jmxExporter;
    private final ObjectName connectorObjectName;
    private boolean initiatorStarted = false;
    
	public QuickfixEngineContainer(
			SessionSettings settings, 
			Application quickfixApplication, 
			LogControlVo logControl) throws ConfigError, FieldConvertError, JMException {
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new TtsFileLogFactoryV2(settings, logControl);
        MessageFactory messageFactory = new DefaultMessageFactory();

        initiator = new SocketInitiator(
        		quickfixApplication, 
        		messageStoreFactory, 
        		settings, 
        		logFactory,
                messageFactory);

        jmxExporter = new JmxExporter();
        connectorObjectName = jmxExporter.register(initiator);
        log.info("Acceptor registered with JMX, name=" + connectorObjectName);
        
	}
	

    public synchronized void logon() {
        if (!initiatorStarted) {
            try {
                initiator.start();
                initiatorStarted = true;
                
                List<SessionID> sessions = initiator.getSessions();
                sessions = sortSessionLoginSequence(sessions);
                for (SessionID sessionId : sessions) {
                    Session session = Session.lookupSession(sessionId);
                    session.logon();
                    Thread.sleep(30 * ChronologyUtil.MILLIS_IN_SECOND);
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

    private List<SessionID>  sortSessionLoginSequence(List<SessionID> sessions) {
    	LinkedList<SessionID> sl= new LinkedList<SessionID>( sessions);
		Collections.sort(sl, new FixSessionOrderingComparator());
		log.debug("sorted:" + sl.toString());
		return sl;
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
	
	private static class FixSessionOrderingComparator implements Comparator<SessionID>{

		@Override
		public int compare(SessionID arg0, SessionID arg1) {
			String session1 = arg0.toString();
			String session2 = arg1.toString();
			if  ( session1.contains("TRADING") &&  !session2.contains("TRADING") ) {
				return Integer.MAX_VALUE;
			} else if  ( session1.contains("STREAMING") &&  !session2.contains("STREAMING") ) {
				return Integer.MIN_VALUE;
			}
			return 0;
		}
		
		
	}
}
