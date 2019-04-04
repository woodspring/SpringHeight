package com.tts.ske.app.price.subscription;

import com.tts.ske.vo.SubscriptionRequestVo;

import quickfix.Session;
import quickfix.SessionID;

public abstract class AbstractSubscriptionHandler implements ISubscriptionHandler {
	
	private final SubscriptionRequestVo request;
	private final Session session;
	private final String identity;
	private final quickfix.Message originalMessage;

	public AbstractSubscriptionHandler(SubscriptionRequestVo request, SessionID sessionId, quickfix.Message originalMessage) {
		this.request = request;
		this.session = Session.lookupSession(sessionId);
		this.identity = "S" + System.nanoTime();
		this.originalMessage = originalMessage;
	}
	
	public SubscriptionRequestVo getRequest() {
		return this.request;
	}
	
	public Session getSession() {
		return this.session;
		
	}
		
	public quickfix.Message getOriginalMessage() {
		return originalMessage;
	}

	public String getIdentity() {
		return identity;
	}

	/* (non-Javadoc)
	 * @see com.tts.mlp.price.impl.ISubscription#push(long)
	 */
	@Override
	public abstract quickfix.Message push(long seq);

	@Override
	public String getId() {
		return getRequest().getSymbol()+"."+getRequest().getTenor();
	}

}
