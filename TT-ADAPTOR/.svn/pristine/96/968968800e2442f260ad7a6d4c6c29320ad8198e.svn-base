package com.tts.fixapi.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fixapi.type.IFIXAcceptorRoutingAgent;

import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;


public class FIXAcceptorDefaultRoutingAgent implements IFIXAcceptorRoutingAgent {
	private static final String SEPARATOR = ".";
	private static final Logger logger    = LoggerFactory.getLogger("FixAPILogger");
	
	private final SessionSet availableFixSessions = new SessionSet();
	
	@Override
	public void registerSession(SessionID sessionId) throws Exception {
		String senderCompID = sessionId.getSenderCompID();
		String targetCompID = sessionId.getTargetCompID();
		String sessionKey   = (senderCompID + SEPARATOR + targetCompID);
		
		SessionID existingSessionId = availableFixSessions.getFixSession(sessionKey);
		if(existingSessionId != null)	{
			logger.error("Duplicate Session Found for " + senderCompID + "->" + targetCompID);
			throw new Exception("Duplicate Session Found for " + senderCompID + "->" + targetCompID);
		}
		
		availableFixSessions.setFixSession(sessionKey, sessionId);
		logger.info("Session Created & Registered. Key: " + sessionKey + " SessionId: " + sessionId.toString());
	}

	@Override
	public void unregisterSession(SessionID sessionId) {
		String senderCompID = sessionId.getSenderCompID();
		String targetCompID = sessionId.getTargetCompID();
		String sessionKey   = (senderCompID + SEPARATOR + targetCompID);
		
		availableFixSessions.removeFixSession(sessionKey);
		logger.warn("Session Removed & UnRegistered. Key: " + sessionKey + " SessionId: " + sessionId.toString());
	}
	
	@Override
	public SessionID sendMessge(Message message, String target) {
		final SessionSet activeSessions = this.availableFixSessions;
		Session fixSession  = null;
		SessionID sessionId = null;
		
		if((target == null) || (target.trim().length() <= 0))
			return null;
		
		try	{
			sessionId = activeSessions.getFixSession(target);
			
			if(sessionId != null)	{
				fixSession = Session.lookupSession(sessionId);
				if((fixSession != null) && (fixSession.isLoggedOn()))	{
					fixSession.send(message);
				}	
				else
					sessionId = null;
			}
		}
		catch (Exception exp) {
			logger.error("FAILED TO SEND MESSAGE. MESSAGE: " + message.toString() + " ERROR: " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
			sessionId = null;
		}
		
		return sessionId;
	}

	@Override
	public boolean isSessionConnected(String target) {
		final SessionSet activeSessions = this.availableFixSessions;
		Session fixSession  = null;
		SessionID sessionId = null;
				
		if((target == null) || (target.trim().length() <= 0))
			return(false);
		
		sessionId = activeSessions.getFixSession(target);
		if(sessionId != null)	{
			fixSession = Session.lookupSession(sessionId);
			return((fixSession != null) && (fixSession.isLoggedOn()));
		}
				
		return(false);
	}
	
	
	
	private static class SessionSet {
		private final Map<String, SessionID> fixSessions = new ConcurrentHashMap<>();
		
		public void setFixSession(String target, SessionID sessionId)	{
			fixSessions.put(target, sessionId);
		}
		public SessionID getFixSession(String target)	{
			return(fixSessions.get(target));
		}
		public SessionID removeFixSession(String target)	{
			return(fixSessions.remove(target));
		}
	}
}
