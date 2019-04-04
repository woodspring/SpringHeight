package com.tts.mlp.app.price.subscription;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.data.IMarketPriceProvider;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.MDReqID;

public class PlaybackEspHandler extends AbstractSubscriptionHandler {

	private final IMarketPriceProvider<?> marketPriceProvider;

	public PlaybackEspHandler(SubscriptionRequestVo request, IRandomMarketPriceProvider p, SessionID sessionID,
			Message originalMessage, PriceSubscriptionRegistry registry, IMarketPriceProvider<?> marketPriceProvider) {
		super(request, sessionID, originalMessage);
		this.marketPriceProvider = marketPriceProvider;
	}

	@Override
	public Message push(long seq) {
		final SubscriptionRequestVo request = getRequest();
//		if ( ( seq % 7) == 0 ) {
		Object instrumentPrice = GlobalAppConfig.isRateFreezed() ? marketPriceProvider.getCurrentPrice(request.getSymbol()) : marketPriceProvider.getNextMarketPrice(request.getSymbol());
												//priceProvider.getNextMarketPrice(request.getSymbol());
		
		if ( instrumentPrice instanceof quickfix.fix44.MarketDataSnapshotFullRefresh) {
			quickfix.fix44.MarketDataSnapshotFullRefresh fRefresh = (quickfix.fix44.MarketDataSnapshotFullRefresh) instrumentPrice;
			fRefresh.set(new MDReqID(request.getClientReqId()));
			getSession().send(fRefresh);
		}

		return null;	
	}

}
