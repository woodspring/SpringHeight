package com.tts.mlp.app.price.subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;

import quickfix.Session;
import quickfix.SessionID;

public class PriceSubscriptionRegistry {
	private final static Logger logger = LoggerFactory.getLogger(PriceSubscriptionRegistry.class);
	final HashMap<String, AbstractSubscriptionHandler> repoIndexedByReqId 
		= new HashMap<String, AbstractSubscriptionHandler>();
	final HashMap<String, AbstractSubscriptionHandler> repoIndexedBySubscriptionId 
		= new HashMap<String, AbstractSubscriptionHandler>();
	
	final HashMap<SessionID, List<AbstractSubscriptionHandler>> repoIndexedBySessionId
			= new HashMap<SessionID, List<AbstractSubscriptionHandler>>();
	
	final HashMap<String,Boolean> indicativeMap = new HashMap<String,Boolean>();
	
	public synchronized List<AbstractSubscriptionHandler> getAndRemoveSessionSubscriptions(SessionID sessionID) {
		List<AbstractSubscriptionHandler> list = repoIndexedBySessionId.remove(sessionID);
		if ( list != null) {
			for ( AbstractSubscriptionHandler h: list) {
				repoIndexedByReqId.remove(h.getRequest().getClientReqId());
				repoIndexedBySubscriptionId.remove(h.getIdentity());
			}
		}
		return list;
	}
	
	public synchronized void addSubscription(AbstractSubscriptionHandler handler, SessionID sessionID) {
		List<AbstractSubscriptionHandler>  sessionList = repoIndexedBySessionId.get(sessionID);
		if ( sessionList == null ) {
			sessionList = new ArrayList<AbstractSubscriptionHandler>();
			repoIndexedBySessionId.put(sessionID, sessionList);
		}
		repoIndexedByReqId.put(sessionID.toString() + handler.getRequest().getClientReqId(), handler);
		repoIndexedBySubscriptionId.put(handler.getIdentity(), handler);
		sessionList.add(handler);
		logger.info(sessionID+" "+handler.getRequest().getSymbol()+"."+handler.getRequest().getTenor());
	}
	
	public synchronized AbstractSubscriptionHandler findByRequestID(String requestID, SessionID sessionID ) {
		return repoIndexedByReqId.get(sessionID.toString() + requestID);
	}
	
	public synchronized AbstractSubscriptionHandler findBySubscriptionId(String subscriptionId) {
		return repoIndexedBySubscriptionId.get(subscriptionId);
	}
	
	public String cancelAndRemoveSubscripton(String requestId, String sessionIdStr) {
		SessionID foundSessionID = null;
		HashSet<SessionID> s = new HashSet<SessionID>(repoIndexedBySessionId.keySet() );
		for ( SessionID sessionID: s) {
			if ( sessionIdStr.equals(sessionID.toString()) ) {
				foundSessionID = sessionID;
			}
		}
		if ( foundSessionID != null) {
			cancelAndRemoveSubscripton(requestId, foundSessionID);
		} else {
			StringBuilder sb = new StringBuilder("Active Sessions:\n");
			for ( SessionID sessionID: s ) {
				sb.append(sessionID.toString()).append('\n');
			}
			return sb.toString();
		}
		return "DONE";
	}

	public synchronized void cancelAndRemoveSubscripton(String requestId, SessionID sessionID) {
		AbstractSubscriptionHandler h = repoIndexedByReqId.get(sessionID.toString() + requestId);
		h.getRequest().getSymbol();
	}

	
	public synchronized void removeSubscription(String requestId, SessionID sessionID) {
		AbstractSubscriptionHandler h = repoIndexedByReqId.remove(sessionID.toString() + requestId);
		if ( h != null ) {
			Session session = h.getSession();
			
			//TODO: why session can be null
			if ( session != null ) {
				List<AbstractSubscriptionHandler> list = repoIndexedBySessionId.get(sessionID);
				list.remove(h);
			}
			repoIndexedBySubscriptionId.remove(h.getIdentity());
			
		}
	}
	
	public List<SessionID> listSessions() {
		return new ArrayList<SessionID>(repoIndexedBySessionId.keySet());
	}
	
	public List<SubscriptionRequestVo> getSubscriptionRequests() {
		ArrayList<SubscriptionRequestVo> result = new ArrayList<SubscriptionRequestVo>();
		
		for(Map.Entry<SessionID, List<AbstractSubscriptionHandler>> entry : repoIndexedBySessionId.entrySet()) {
			for(AbstractSubscriptionHandler h : entry.getValue()) {
				result.add(h.getRequest());
			}
			
		}
		return result;
	}
	
	public String setIndicativeStatus(String id, boolean status) {
		
		if(indicativeMap.get(id) == null) {
			indicativeMap.put(id, status);
		} else {
			indicativeMap.put(id, status);
		}
		logger.info(id +" indicative status set to " +status);
		return id +" indicative status set to " +status;
	}
	
	public boolean getIndicativeStatus(String id) {
		if(indicativeMap.get(id) == null) {
			return false;
		} else {
			return indicativeMap.get(id);
		}
	}
}
