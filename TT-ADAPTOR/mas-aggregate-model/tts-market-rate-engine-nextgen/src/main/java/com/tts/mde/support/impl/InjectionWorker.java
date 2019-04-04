package com.tts.mde.support.impl;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.support.IMarketDataHandler;
import com.tts.mde.support.IPublishingEndpoint;
import com.tts.service.db.RuntimeDataService;
import com.tts.util.AppContext;
import com.tts.util.constant.SysProperty;

public class InjectionWorker implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(InjectionWorker.class);

	private static final String KEY_INJECTION_INTERVAL_OVERRIDE = "INJECTION.INTERVAL.OVERRIDE";
	
	private final boolean injectionDebug;

	private final ScheduledExecutorService executor;
	private final MasGlobalSequenceProvider masGlobalSequenceProvider;
	private final IPublishingEndpoint publishingEndpoint;
	private final long priceInjectionInterval;
	private volatile List<IMarketDataHandler> marketDataHandlers;
	
	public InjectionWorker() {
		super();
		
		boolean  injectionDebug = false;
		String injectionDebugStr = System.getProperty("injection.debug");
		if (injectionDebugStr != null && !injectionDebugStr.isEmpty()  ) {
			injectionDebug = Boolean.parseBoolean(injectionDebugStr);
		}
		
		Long _priceInjectionInterval = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.CONSTANTS, SysProperty.Key1.INJECT_INTERVAL, null);
		if (_priceInjectionInterval == null) {
			_priceInjectionInterval = SysProperty.DefaultValue.INJECT_INTERVAL;
		} 
		try {
			_priceInjectionInterval = Long.parseLong(System.getProperty(KEY_INJECTION_INTERVAL_OVERRIDE));
		} catch (Exception e) { 
			
		}
		
		if (injectionDebug) {
			logger.info("Enabled injection debugging");
		}
		
		this.publishingEndpoint = AppContext.getContext().getBean(IPublishingEndpoint.class);
		this.priceInjectionInterval = _priceInjectionInterval;
		this.masGlobalSequenceProvider = AppContext.getContext().getBean(MasGlobalSequenceProvider.class);
		this.executor = Executors.newSingleThreadScheduledExecutor();
		this.injectionDebug = injectionDebug;
		logger.debug("Created new instance of InjectionWorker");
	}



	@Override
	public void run() {
		long masGlobalSeq = masGlobalSequenceProvider.getNewSequence();
		long overallStartTime = System.currentTimeMillis();

		if ( injectionDebug || (masGlobalSeq % 1000 ) == 0 ) {
			logger.debug("Injection worker running on sequence, " + masGlobalSeq );
		}
		
		if ( marketDataHandlers != null) {
			
			for ( IMarketDataHandler handler: marketDataHandlers) {
				try {
					long startTime = System.currentTimeMillis();
					handler.atPublish(masGlobalSeq, publishingEndpoint);
					long endTime = System.currentTimeMillis();
					long escaped = endTime - startTime;
					if ( escaped > priceInjectionInterval ) {
						logger.warn("IMarketDataHandler " + handler.getClass().getName() + " took abnormally long");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		long overallEndTime = System.currentTimeMillis();

		if ( injectionDebug ) {
			logger.debug("Injection worker completed on sequence, " + masGlobalSeq + " used:" + (overallEndTime - overallStartTime));
		}
	}

	
	public void start() {
		executor.scheduleAtFixedRate(this, priceInjectionInterval, priceInjectionInterval, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
		executor.shutdownNow();
	}



	public void setMarketDataHandlers(List<IMarketDataHandler> marketDataHandlers) {
		this.marketDataHandlers = marketDataHandlers;
	}







}
