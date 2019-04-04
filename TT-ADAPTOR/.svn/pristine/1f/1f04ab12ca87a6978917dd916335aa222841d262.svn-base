package com.tts.fixapi.type;

import quickfix.Message;
import quickfix.SessionID;


public interface IFIXAcceptorRoutingAgent {
	
	public void registerSession(SessionID sessionId) throws Exception;
	
	public void unregisterSession(SessionID sessionId);
	
	public SessionID sendMessge(Message message, String target);
	
	public boolean isSessionConnected(String target);
}
