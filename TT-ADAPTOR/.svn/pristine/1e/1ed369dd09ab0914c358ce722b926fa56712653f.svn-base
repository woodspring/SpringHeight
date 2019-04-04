package com.tts.plugin.adapter.impl.ykb.routing;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.plugin.adapter.api.setting.IFixSetting.SessionType;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;

import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;

public class YkbFixRoutingAgentImpl implements IQfixRoutingAgent {
	private static final int DEFAULT_RECONNECT_INTERVAL = 30 * ChronologyUtil.MILLIS_IN_SECOND;

	private static final Logger logger = LoggerFactory.getLogger(IQfixRoutingAgent.class);
	private static final String SEPARATOR = ".";

	private final IFixSetting fixSetting;
	private final SessionSet sessionSet = new SessionSet();
	private final long logonGracePeriod;
	private final long logoutTimeout;

	private SessionSet activeSessionSet = sessionSet;
	private volatile long lastSeenOnline = 0;
	@SuppressWarnings("unused")
	private volatile long lastSeenOffline = -1;
	private volatile boolean online = false;
	private final boolean spotLogoutWhileChangeTs;


	public YkbFixRoutingAgentImpl(IFixSetting fixSetting) {
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		
		this.fixSetting = fixSetting;		
		this.logonGracePeriod = p.getProperty("spotadapter.logon_grace_period", -1);
		this.spotLogoutWhileChangeTs = p.getProperty("spotadapter.logout_while_change_ts", false);	
		
		long _logoutTimeout = -1L;
		if ( this.spotLogoutWhileChangeTs ) {
			quickfix.SessionSettings qfixSetting = fixSetting.getQuickfixSessionSetting();
			if ( qfixSetting == null ) {
				qfixSetting = fixSetting.getQuickfixSessionSettings().get(0);
			}
			if ( qfixSetting != null ) {
				String s = (String) qfixSetting.getDefaultProperties().get("ReconnectInterval");
				if ( s != null ) {
					int l = Integer.parseInt(s);
					if ( l < 30) {
						_logoutTimeout = l * ChronologyUtil.MILLIS_IN_SECOND;
					} else {
						_logoutTimeout = DEFAULT_RECONNECT_INTERVAL;
					}	
				} else {
					_logoutTimeout = DEFAULT_RECONNECT_INTERVAL;
				}
			} else {
				_logoutTimeout = DEFAULT_RECONNECT_INTERVAL;
			}
		} 
		this.logoutTimeout = _logoutTimeout;
	}

	@Override
	public synchronized void registerSession(SessionID sessionID) {
		
		String senderCompID = sessionID.getSenderCompID();
		String targetCompID = sessionID.getTargetCompID();
		
		if(senderCompID.contains("-STREAMING")) {
			sessionSet.setEspPriceSession(Session.lookupSession(sessionID));
		} 
		else if(senderCompID.contains("-TRADING")) {
			sessionSet.setEspOrdSession(Session.lookupSession(sessionID));
		}
		
		sessionSet.setFixSession((senderCompID + SEPARATOR + targetCompID), sessionID);
		lastSeenOnline = System.currentTimeMillis();
		online = evaluateOnline();
	}

	@Override
	public synchronized SessionID send(AppType requestSource, Message message) {
		final SessionSet activeSessionSet = this.activeSessionSet;

		if(requestSource == AppType.SPOTADAPTER) {
			Session session = activeSessionSet.getEspPriceSession();
			if(session != null) {
				session.send(message);
				return session.getSessionID();
			} else {
				return null;
			}
		} 
		else if (requestSource == AppType.FIXTRADEADAPTER) {
			Session session = activeSessionSet.getEspOrdSession();
			if(session != null ) {
				session.send(message);
				return session.getSessionID();
			} else {
				return null;
			}
		}
		return null; 
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
		lastSeenOffline = System.currentTimeMillis();
		String senderCompID = sessionID.getSenderCompID();
		String targetCompID = sessionID.getTargetCompID();
		
		if(senderCompID.contains("-STREAMING")) {
			sessionSet.setEspPriceSession(null);
		} 
		else if(senderCompID.contains("-TRADING")) {
			sessionSet.setEspOrdSession(null);
		}
		
		sessionSet.removeFixSession((senderCompID + SEPARATOR + targetCompID));
		online = evaluateOnline();
	}

	@Override
	public synchronized void switchTradingSession(String tradingSessionName) {
		long startTime = System.currentTimeMillis();
		long onlineTime = startTime - lastSeenOnline;
		logger.debug("spotLogoutWhileChangeTs="+spotLogoutWhileChangeTs+",logoutTimeout="+logoutTimeout+",lastSeenOnline="+lastSeenOnline+",onlineTime="+onlineTime);
		if ( spotLogoutWhileChangeTs 
				&&  logoutTimeout > 0 
				&& onlineTime > DEFAULT_RECONNECT_INTERVAL) {
			logger.debug("sessionSet="+sessionSet+",EspPriceSession="+sessionSet.getEspPriceSession()+",EspOrdSession="+sessionSet.getEspOrdSession());

			if (sessionSet != null && sessionSet.getEspPriceSession() != null && sessionSet.getEspOrdSession() != null) {
				SessionID priceSessionID = sessionSet.getEspPriceSession().getSessionID();
				SessionID orderSessionID = sessionSet.getEspOrdSession().getSessionID();
				
				Session.lookupSession(priceSessionID).logout();
				Session.lookupSession(orderSessionID).logout();
				
				logger.debug("logging OFF FIX session as MARKET_SPOTADAPTER_LOGOUT_WHILE_CHANGE_TS = true, ReconnectInterval="+logoutTimeout);

				try {
					Thread.sleep(logoutTimeout);
				} catch (InterruptedException e) {
					
				}				
				
				logger.debug("logging ON FIX session as MARKET_SPOTADAPTER_LOGOUT_WHILE_CHANGE_TS = true");

				Session priceSession = Session.lookupSession(priceSessionID);
				priceSession.logon();
				Session orderSession = Session.lookupSession(orderSessionID);
				orderSession.logon();		
			}
		}
		online = evaluateOnline();
	}

	@Override
	public boolean isRequiredSessionConnected(AppType appType) {
		if ( !online ) {
			logger.debug("FIX Sessions are not being evaluated as online");
			return online;
		}
		final long currentTime = System.currentTimeMillis();
		final long loggedOnTime = currentTime - lastSeenOnline;
		
		if ( logonGracePeriod > 0 && loggedOnTime < logonGracePeriod ) {
			logger.debug("Grace period after logon, " + loggedOnTime + " " + logonGracePeriod);
			return false;
		} else if ( AppType.FIXTRADEADAPTER == appType) {
			if ( activeSessionSet.getEspOrdSession() == null || !activeSessionSet.getEspOrdSession().isLoggedOn() ) {
				return false;
			}
		}
		
		return online;
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
		LocalTime t = dt.toLocalTime();
		if ( t.getHour() == 0 &&  t.getMinute() == 0 ) {
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
		List<SessionType> expectedSessions = fixSetting.getExpectedSessions();
		if (expectedSessions.contains(SessionType.MARKET_PRICE) ) {
			online = activeSessionSet.getEspPriceSession() != null && activeSessionSet.getEspPriceSession().isLoggedOn();
		}
		if (expectedSessions.contains(SessionType.MARKET_ORDER) ) {
			online = activeSessionSet.getEspOrdSession() != null && activeSessionSet.getEspOrdSession().isLoggedOn();
		}
		if (expectedSessions.contains(SessionType.BANK_RFS) ) {
			online = activeSessionSet.getRfsSession() != null && activeSessionSet.getRfsSession().isLoggedOn();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("activeSessionSet=").append(activeSessionSet != null).append('\n');
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
		@SuppressWarnings("unused")
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
