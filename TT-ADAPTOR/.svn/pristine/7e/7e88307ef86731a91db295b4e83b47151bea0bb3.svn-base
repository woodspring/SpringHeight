package com.tts.ske.app.price.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.ske.vo.SubscriptionRequestVo;
import com.tts.ske.vo.SubscriptionRequestVo.StreamType;

import quickfix.SessionID;

public class PriceSubscriptionHandlerFactory {
	private final static Logger logger = LoggerFactory.getLogger(PriceSubscriptionHandlerFactory.class);

	
	public AbstractSubscriptionHandler getSubscriptionHandler(
				SubscriptionRequestVo request, 
				SessionID sessionID,
				quickfix.Message originalMessage,
				PriceSubscriptionRegistry registry
				) {
		if ( request.getStreamType() == StreamType.ESP){
			logger.info("created EspHandler for " + request.getSymbol());

			return new EspHandler(request,  sessionID, originalMessage,registry);
		}
		return null;
		
	}
	
}
