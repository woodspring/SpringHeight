package com.tts.fixapi.type;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tts.fixapi.core.FIXAcceptorSession;

import quickfix.Application;
import quickfix.SessionID;


public interface IFIXAcceptorMessageDispatcher extends Application {
	final Map<String, FIXAcceptorSession> fixAppSessions = new ConcurrentHashMap<>();
	
	public SessionID sendExecutionReport(Hashtable<String, Object> msgParam, String targetSession);
	
	public SessionID sendOrderCancelReject(Hashtable<String, Object> msgParam, String targetSession);
}
