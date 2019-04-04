package com.tts.fixapi.type;

import java.util.Hashtable;

import com.tts.vo.CustomerAccountVo;

import quickfix.SessionID;

public interface IFIXAcceptorMessageProcessor {

	public void setFIXMessageDispatcher(IFIXAcceptorMessageDispatcher msgSender);
	
	public void notifySessionConnectionForCustomer(CustomerAccountVo customer, SessionID sessionId);
	
	public void notifySessionDisconnectionForCustomer(Long accountId, SessionID sessionId);
	
	public void processNewOrderSingle(String orderSender, String orderTarget, Hashtable<String, Object> orderParam, Long accountId);
	
	public void processOrderCancelRequest(String orderSender, String orderTarget, Hashtable<String, Object> orderParam, Long accountId);
	
	public void processOrderStatusRequest(String orderSender, String orderTarget, Hashtable<String, Object> orderParam, Long accountId);
}
