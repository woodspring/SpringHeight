package com.tts.mlp.app.price.push;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.subscription.ISubscriptionHandler;
import com.tts.mlp.qfix.QuickfixEngineContainer;
import com.tts.util.AppConfig;
import com.tts.util.Constants;

public class PricePushWorker implements Runnable {
	public final static Logger logger = LoggerFactory.getLogger(QuickfixEngineContainer.class);

	public static final int DEFAULT_INJECT_INTERVAL = 5000;

	private final CopyOnWriteArrayList<ISubscriptionHandler> list 
					= new CopyOnWriteArrayList<ISubscriptionHandler>();
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	private final long injectInterval;
	
	public PricePushWorker() {
		injectInterval = AppConfig.getIntegerValue(
				Constants.APP_SECTION, 
				Constants.MARKET_INJECT_INTERVAL,
				DEFAULT_INJECT_INTERVAL);
		
	}
	
	@Override
	public void run() {
		long cycleStartTime = -1L;

		
		boolean ratePausedFlag = GlobalAppConfig.isRatePaused();
		
			cycleStartTime = System.currentTimeMillis();
			final long seq = cycleStartTime;

			ArrayList<ISubscriptionHandler> currentList = new ArrayList<ISubscriptionHandler>(list);
			for ( int i = 0; i < currentList.size(); i++ ) {
				ISubscriptionHandler h = null;
				try {

					h = currentList.get(i);
					if ( ratePausedFlag  ) {
						continue;
					}
					quickfix.Message m = h.push(seq);
					if ( m != null && m instanceof quickfix.fix50.QuoteCancel) {
						list.remove(h);
					} 
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
					
	}
	
	public void removeSubscription(ISubscriptionHandler subscription) {
		list.remove(subscription);
		logger.debug("Removed Subscription, " + subscription.getId());
	}
	
	public void addSubscription(ISubscriptionHandler subscription) {
		if ( subscription != null) {
			list.add(subscription);
		}
		logger.debug("Added Subscription, " + subscription.getId());
	}
	
	public void start() {
		executor.scheduleAtFixedRate(this, injectInterval, injectInterval, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
		executor.shutdownNow();
	}

}
