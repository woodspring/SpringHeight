package com.tts.mlp.app.price.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.PlaybackPriceLoader;
import com.tts.mlp.app.price.data.IMarketPriceProvider;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.StreamType;
import com.tts.util.AppContext;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.SessionID;

public class PriceSubscriptionHandlerFactory {
	private final static Logger logger = LoggerFactory.getLogger(PriceSubscriptionHandlerFactory.class);
	private final IRandomMarketPriceProvider p;
	private final boolean playbackMode;
	private final IMarketPriceProvider<?> marketPriceProvider;
	
	public PriceSubscriptionHandlerFactory() {
		p = AppContext.getContext().getBean(IRandomMarketPriceProvider.class);
		String s = System.getProperty("PLAYBACK_FILE");
		

		IMarketPriceProvider<?> marketPriceProvider = null;
		if ( s != null ) {
			logger.info("PLAYBACK_FILE setting is not null");
	        DataDictionary dd = null;
			try {
				dd = new DataDictionary( "app-resources/TradAirFIX44.xml" );
		        dd.setCheckUnorderedGroupFields( true );

		        PlaybackPriceLoader l = new PlaybackPriceLoader();
		        marketPriceProvider = l.buildFixPriceProvider(s, dd);
				logger.info("PLAYBACK_FILE loaded successfully");

			} catch (ConfigError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			this.playbackMode = true;
			this.marketPriceProvider = marketPriceProvider;
		} else {
			this.playbackMode = false;
			this.marketPriceProvider = null;

		}
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
			if ( playbackMode ) {
				logger.info("created PlaybackEspHandler for " + request.getSymbol());

				return new PlaybackEspHandler(request, p, sessionID, originalMessage,registry, marketPriceProvider);
			}
			logger.info("created EspHandler for " + request.getSymbol());

			return new EspHandler(request, p, sessionID, originalMessage,registry);
		}
		return null;
		
	}
	
}
