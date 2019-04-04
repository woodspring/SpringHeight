package com.tts.mlp.app.price.subscription;

import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.StreamType;
import com.tts.util.AppContext;

import quickfix.SessionID;

public class PriceSubscriptionHandlerFactory {
	
	private final IRandomMarketPriceProvider p;
	
	public PriceSubscriptionHandlerFactory() {
		p = AppContext.getContext().getBean(IRandomMarketPriceProvider.class);
	}
	
	public AbstractSubscriptionHandler getSubscriptionHandler(
				SubscriptionRequestVo request, 
				SessionID sessionID,
				quickfix.Message originalMessage,
				PriceSubscriptionRegistry registry
				) {
		if ( request.getStreamType() == StreamType.RFS ) {
			return new RfsHandler(request, p, sessionID, originalMessage);
		} else if ( request.getStreamType() == StreamType.ESP){
			return new EspHandler(request, p, sessionID, originalMessage,registry);
		}
		return null;
		
	}
	
}
