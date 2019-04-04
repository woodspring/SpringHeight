package com.tts.mas.support;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.vo.InjectionConfig;
import com.tts.plugin.adapter.api.app.IPublishingApp;
import com.tts.service.db.RuntimeDataService;
import com.tts.util.AppContext;
import com.tts.util.constant.SysProperty;

public class InjectionWorker implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(InjectionWorker.class);

	private static final String KEY_INJECTION_INTERVAL_OVERRIDE = "INJECTION.INTERVAL.OVERRIDE";
	
	private final ScheduledExecutorService executor;
	private final MasGlobalSequenceProvider masGlobalSequenceProvider;
	private final InjectionConfig injectionConfig;
	private final long priceInjectionInterval;

	public InjectionWorker(InjectionConfig injectionConfig) {
		super();
		
		Long _priceInjectionInterval = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.CONSTANTS, SysProperty.Key1.INJECT_INTERVAL, null);
		if (_priceInjectionInterval == null) {
			_priceInjectionInterval = SysProperty.DefaultValue.INJECT_INTERVAL;
		} 
		try {
			_priceInjectionInterval = Long.parseLong(System.getProperty(KEY_INJECTION_INTERVAL_OVERRIDE));
		} catch (Exception e) { 
			
		}
		
		this.priceInjectionInterval = _priceInjectionInterval;
		this.masGlobalSequenceProvider = AppContext.getContext().getBean(MasGlobalSequenceProvider.class);
		this.injectionConfig = injectionConfig;
		this.executor = Executors.newSingleThreadScheduledExecutor();
		
		logger.debug("Created new instance of InjectionWorker");
	}



	@Override
	public void run() {
		final List<IPublishingApp> apps = injectionConfig.getPublishingApps();
		final MasGlobalSequenceProvider masGlobalSequenceProvider = this.masGlobalSequenceProvider;
				
		long masGlobalSequence = masGlobalSequenceProvider.getNewSequence();
		for ( IPublishingApp app: apps) {
			try {
				app.atPublish(masGlobalSequence);
				Thread.sleep(1L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.warn("Exception in Injection Worker while publishing " + app.getName(), e);
			}
		}
	}

	
	public void start() {
		executor.scheduleAtFixedRate(this, priceInjectionInterval, priceInjectionInterval, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
		executor.shutdownNow();
	}




}
