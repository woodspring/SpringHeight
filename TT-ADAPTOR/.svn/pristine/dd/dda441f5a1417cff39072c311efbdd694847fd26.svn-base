package com.tts.plugin.adapter.impl.cibc.routing;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.plugin.adapter.api.setting.IFixSetting.SessionType;

import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;

public class CibcFixRoutingAgentImpl implements IQfixRoutingAgent {
	private static final Logger logger = LoggerFactory.getLogger(IQfixRoutingAgent.class);
	private static final String SEPARATOR = ".";

	private final IFixSetting additionalSetting;
	private final SessionSet cibcSessionSet       = new SessionSet();
	private final SessionSet simulationSessionSet = new SessionSet();
	private SessionSet activeSessionSet           = cibcSessionSet;
	
	private final boolean enableSimulatorSet;
	private final boolean forceSimulatorSet;

	@SuppressWarnings("unused")
	private volatile boolean online = false;
	
	public CibcFixRoutingAgentImpl(IFixSetting additionalSetting) {
		this.additionalSetting = additionalSetting;
		
		String sim = System.getenv("MARKET_SIMULATION");
		this.forceSimulatorSet = "FORCED".equals(sim);
		this.enableSimulatorSet = "ENABLED".equals(sim);
	}

	@Override
	public synchronized void registerSession(SessionID sessionID) {		
		String senderCompID = sessionID.getSenderCompID();
		String targetCompID = sessionID.getTargetCompID();
		
		if(targetCompID.contains("CIBC")) {
			if(senderCompID.indexOf("_ORD") > 0) { 
				cibcSessionSet.setEspOrdSession( Session.lookupSession(sessionID));
			} else if(senderCompID.indexOf("_ESP") > 0 
					|| senderCompID.indexOf("_MKD") > 0
					|| senderCompID.indexOf("_MD") > 0) {
				cibcSessionSet.setEspPriceSession( Session.lookupSession(sessionID));
			} else if(senderCompID.indexOf("_RFS") > 0) {
				cibcSessionSet.setRfsSession( Session.lookupSession(sessionID));
			}
		} 
		else {
			if(senderCompID.indexOf("_ORD") > 0) { 
				simulationSessionSet.setEspOrdSession( Session.lookupSession(sessionID));
			} else if(senderCompID.indexOf("_ESP") > 0) {
				simulationSessionSet.setEspPriceSession( Session.lookupSession(sessionID));
			} else if(senderCompID.indexOf("_RFS") > 0) {
				simulationSessionSet.setRfsSession( Session.lookupSession(sessionID));
			}
		}
		
		cibcSessionSet.setFixSession((senderCompID + SEPARATOR + targetCompID), sessionID);
		simulationSessionSet.setFixSession((senderCompID + SEPARATOR + targetCompID), sessionID);
		online = evaluateOnline();
	}

	@Override
	public synchronized SessionID send(AppType requestSource, Message message) {
		final SessionSet activeSessionSet = this.activeSessionSet;
		if(requestSource == AppType.SPOTADAPTER 
				|| requestSource == AppType.FCADAPTER
				|| requestSource == AppType.QUOTEADAPTER ) {
			Session session = activeSessionSet.getEspPriceSession();
			if(session != null) {
				session.send(message);
				return session.getSessionID();
			}
			else {
				return null;
			}
		} 
		else if(requestSource == AppType.FIXTRADEADAPTER) {
			Session session = activeSessionSet.getEspOrdSession();
			if(session != null ) {
				session.send(message);
				return session.getSessionID();
			} 
			else {
				return null;
			}
		}
		else if(requestSource == AppType.ROETRADEADAPTER)	{
			return(null);
		}
		else {
			Session session = activeSessionSet.getRfsSession();
			if(session != null ) {
				session.send(message);
				return session.getSessionID();
			} 
			else {
				return null;
			}
		}
	}
	
	@Override
	public SessionID send(Message message, String target) {
		final SessionSet activeSessionSet = this.activeSessionSet;
		Session fixSession  = null;
		SessionID sessionId = null;
		
		if((target == null) || (target.trim().length() <= 0))
			return null;
		
		try	{
			sessionId = activeSessionSet.getFixSession(target);
			if(sessionId != null)	{
				fixSession = Session.lookupSession(sessionId);
				if((fixSession != null) && (fixSession.isLoggedOn()))	{
					//Session.sendToTarget(message, sessionId.getSenderCompID(), sessionId.getTargetCompID());
					fixSession.send(message);
				}
			}
		}
		catch(Exception exp) {
			exp.printStackTrace();
			sessionId = null;
		}
		
		return sessionId;
	}

	@Override
	public synchronized void unregisterSession(SessionID sessionID) {
		String senderCompID = sessionID.getSenderCompID();
		String targetCompID = sessionID.getTargetCompID();
		
		if(targetCompID.contains("CIBC")) {
			if(senderCompID.indexOf("_ORD") > 0) {
				cibcSessionSet.setEspOrdSession(null);
			} else if(senderCompID.indexOf("_ESP") > 0 
					|| senderCompID.indexOf("_MKD") > 0
					|| senderCompID.indexOf("_MD") > 0) {
				cibcSessionSet.setEspPriceSession(null);
			} else if(senderCompID.indexOf("_RFS") > 0) {
				cibcSessionSet.setRfsSession(null);
			}
		} 
		else {
			if(senderCompID.indexOf("_ORD") > 0) {
				simulationSessionSet.setEspOrdSession( null);
			} else if(senderCompID.indexOf("_ESP") > 0) {
				simulationSessionSet.setEspPriceSession(null);
			} else if(senderCompID.indexOf("_RFS") > 0 ) {
				simulationSessionSet.setRfsSession(null);
			}
		}

		cibcSessionSet.removeFixSession((senderCompID + SEPARATOR + targetCompID));
		simulationSessionSet.removeFixSession((senderCompID + SEPARATOR + targetCompID));
		online = evaluateOnline();
	}

	@Override
	public synchronized void switchTradingSession(String tradingSessionName) {

		if(cibcSessionSet == null 
				|| forceSimulatorSet
				|| (tradingSessionName.equals("Simulator") && enableSimulatorSet )) {
			activeSessionSet = simulationSessionSet;
			logger.debug("FIX active session set is now simulator");
		} else {
			activeSessionSet = cibcSessionSet;
			logger.debug("FIX active session set is now cibc");
		}

		online = evaluateOnline();
	}

	@Override
	public boolean isRequiredSessionConnected(AppType appType) {
		if ( appType == AppType.FCADAPTER
				|| appType == AppType.QUOTEADAPTER
				|| appType == AppType.SPOTADAPTER
				|| appType == AppType.IRADAPTER ) {
			return activeSessionSet.getEspPriceSession() != null && activeSessionSet.getEspPriceSession().isLoggedOn();
		} else if ( appType == AppType.BANKFIXTRADEADAPTER
				|| appType == AppType.FIXTRADEADAPTER 
				|| appType == AppType.TRADEREPORTADAPTER ) {
			return activeSessionSet.getEspOrdSession() != null && activeSessionSet.getEspOrdSession().isLoggedOn();
		} else {
			return true;
		}
	}
	
	@Override
	public boolean isRequiredSessionConnected(String target) {
		SessionID sessionId = activeSessionSet.getFixSession(target);
		
		if(sessionId == null)
			return false;
		
		Session session = Session.lookupSession(sessionId);
		return((session != null) && (session.isLoggedOn()));
	}
	
	@Override
	public boolean isSessionDisconnectExcepted(SessionID arg0) {
		LocalDateTime dt = LocalDateTime.now(ZoneId.of("America/New_York"));
		DayOfWeek d = dt.getDayOfWeek();
		LocalTime t = dt.toLocalTime();
		if ( t.getHour() == 17 
				&& ( t.getMinute() >=0 || t.getMinute() <=15)
				&&  d != DayOfWeek.SATURDAY  ) {
			return true;
		}
		return false;
	}
	
	@Override
	public List<String> getMessageFromStore(int startSeqNo, int endSeqNo, String target) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getMessageFromStore(int startSeqNo, int endSeqNo, AppType appType) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean evaluateOnline() {
		boolean online = activeSessionSet != null; 
		List<SessionType> expectedSessions = additionalSetting.getExpectedSessions();
		if (expectedSessions.contains(SessionType.MARKET_PRICE) ) {
			online = online && activeSessionSet.getEspPriceSession() != null && activeSessionSet.getEspPriceSession().isLoggedOn();
		}
		if (expectedSessions.contains(SessionType.MARKET_ORDER) ) {
			online = online && activeSessionSet.getEspOrdSession() != null && activeSessionSet.getEspOrdSession().isLoggedOn();
		}
		if (expectedSessions.contains(SessionType.BANK_RFS) ) {
			online = online && activeSessionSet.getRfsSession() != null && activeSessionSet.getRfsSession().isLoggedOn();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("activeSessionSet=").append(activeSessionSet != null).append('\n');
		sb.append("\t.getEspPriceSession=").append(activeSessionSet.getEspPriceSession() != null);
		if ( activeSessionSet.getEspPriceSession() != null) {
			sb.append(".getEspPriceSession.sessionID=").append(activeSessionSet.getEspPriceSession().toString());
			sb.append(".getEspPriceSession.loggedOn=").append(activeSessionSet.getEspPriceSession().isLoggedOn());
		}
		sb.append('\n');
		sb.append("\t.getEspOrdSession=").append(activeSessionSet.getEspOrdSession() != null);
		if ( activeSessionSet.getEspOrdSession() != null) {
			sb.append(".getEspOrdSession.sessionID=").append(activeSessionSet.getEspOrdSession().toString());
			sb.append(".getEspOrdSession.loggedOn=").append(activeSessionSet.getEspOrdSession().isLoggedOn());
		}
		sb.append('\n');
		sb.append("\t.getRfsSession=").append(activeSessionSet.getRfsSession() != null);
		if ( activeSessionSet.getRfsSession() != null) {
			sb.append(".getRfsSession.sessionID=").append(activeSessionSet.getRfsSession().toString());
			sb.append(".getRfsSession.loggedOn=").append(activeSessionSet.getRfsSession().isLoggedOn());
		}
		sb.append('\n');
		sb.append("online=").append(online).append('\n');
		
		logger.debug(sb.toString());

		return online;
	}
	
	private static class SessionSet {
		private volatile Session espPriceSession;
		private volatile Session espOrdSession;
		private volatile Session rfsSession;
		private final Map<String, SessionID> fixAppSessions = new ConcurrentHashMap<>();
		
		private Session getEspPriceSession() {
			return espPriceSession;
		}
		private void setEspPriceSession(Session espSession) {
			this.espPriceSession = espSession;
		}
		private Session getEspOrdSession() {
			return espOrdSession;
		}
		private void setEspOrdSession(Session espSession) {
			this.espOrdSession = espSession;
		}		
		private Session getRfsSession() {
			return rfsSession;
		}
		private void setRfsSession(Session rfsSession) {
			this.rfsSession = rfsSession;
		}
		
		public void setFixSession(String target, SessionID sessionId)	{
			fixAppSessions.put(target, sessionId);
		}
		public SessionID getFixSession(String target)	{
			return(fixAppSessions.get(target));
		}
		public SessionID removeFixSession(String target)	{
			return(fixAppSessions.remove(target));
		}
	}
}