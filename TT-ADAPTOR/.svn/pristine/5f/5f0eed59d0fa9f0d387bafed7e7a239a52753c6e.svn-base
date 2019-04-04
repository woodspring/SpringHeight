package com.tts.mas.support;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.vo.InjectionConfig;
import com.tts.plugin.adapter.api.app.IPublishingApp;
import com.tts.service.db.RuntimeDataService;
import com.tts.util.AppContext;
import com.tts.util.constant.SysProperty;

public class AdvancedInjectionWorker implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(IPublishingApp.class);

	private static final String KEY_INJECTION_INTERVAL_OVERRIDE = "INJECTION.INTERVAL.OVERRIDE";
	
	private final InjectionHelperThread injectionHelperThread1;
	private final InjectionHelperThread injectionHelperThread2;
	private final MasGlobalSequenceProvider masGlobalSequenceProvider;
	private final long priceInjectionInterval;
	private final ArrayBlockingQueue<Long> sequenceQ;

	private volatile Thread executor;
	
	public AdvancedInjectionWorker(InjectionConfig injectionConfig) {
		super();
		
		Long _priceInjectionInterval = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.CONSTANTS, SysProperty.Key1.INJECT_INTERVAL, null);
		if (_priceInjectionInterval == null) {
			_priceInjectionInterval = SysProperty.DefaultValue.INJECT_INTERVAL;
		} 
		try {
			_priceInjectionInterval = Long.parseLong(System.getProperty(KEY_INJECTION_INTERVAL_OVERRIDE));
		} catch (Exception e) { 
			
		}
		
		ArrayBlockingQueue<Long> sequenceQ = new ArrayBlockingQueue<Long>(2); 
		MasGlobalSequenceProvider masGlobalSequenceProvider = AppContext.getContext().getBean(MasGlobalSequenceProvider.class);
		this.masGlobalSequenceProvider = masGlobalSequenceProvider;
		this.injectionHelperThread1 = new InjectionHelperThread(injectionConfig, sequenceQ, 0, "InjectionHelperThread#1");
		this.injectionHelperThread2 = new InjectionHelperThread(injectionConfig, sequenceQ, 100, "InjectionHelperThread#2");
		this.sequenceQ = sequenceQ;
		this.priceInjectionInterval = _priceInjectionInterval;
		this.executor = new Thread(this);
		this.executor.setName("InjectionWorker");
		this.executor.setDaemon(true);
		logger.debug("Created new instance of InjectionWorker");
	}

	public void run() {
		final long priceInjectionInterval = this.priceInjectionInterval;
		final MasGlobalSequenceProvider masGlobalSequenceProvider = this.masGlobalSequenceProvider;
		final ArrayBlockingQueue<Long> sequenceQ = this.sequenceQ;
		while ( Thread.currentThread() == executor) {
			try {
				long startTime = System.currentTimeMillis();
				long seq = masGlobalSequenceProvider.getNewSequence();
				sequenceQ.put(seq);
				logger.debug("added " + seq);
				long endTime = System.currentTimeMillis();
				long expectedSleepTime = priceInjectionInterval - (endTime - startTime);
				if (expectedSleepTime < 0 ) expectedSleepTime = 0;
				if (expectedSleepTime > priceInjectionInterval ) expectedSleepTime = priceInjectionInterval; 
				long sleepTime =0;
				while ( sleepTime < expectedSleepTime ) {
					long startSleepTime = System.currentTimeMillis();
					Thread.sleep(10L);
					long endSleepTime = System.currentTimeMillis();
					sleepTime = sleepTime + (endSleepTime - startSleepTime);
				}
			} catch (InterruptedException e) {
				break;
			} catch (Exception e) {
				logger.warn("Exception in Injection Worker while publishing ", e);
			}
		}
	}
	


	
	public void start() {
		//executor.scheduleAtFixedRate(this, priceInjectionInterval, priceInjectionInterval, TimeUnit.MILLISECONDS);
		this.injectionHelperThread1.start();
		this.injectionHelperThread2.start();
		this.executor.start();

		logger.debug("started InjectionWorker");

	}
	
	public void stop() {
		//executor.shutdownNow();
		this.injectionHelperThread1.stop();
		this.injectionHelperThread2.stop();
		this.executor.interrupt();
		this.executor = null;

	}


	private final static class InjectionHelperThread implements Runnable {
		private final InjectionConfig injectionConfig;
		private final BlockingQueue<Long> sequenceQ;
		private final long delayStart;
		
		private volatile Thread executor;

		public InjectionHelperThread(InjectionConfig injectionConfig, BlockingQueue<Long> sequenceQ,
				long delayStart, String name) {
			super();
			this.injectionConfig = injectionConfig;
			this.delayStart = delayStart;
			this.sequenceQ = sequenceQ;
			this.executor = new Thread(this);
			this.executor.setName(name);
			this.executor.setDaemon(true);
		}
		
		public void start() {
			this.executor.start();
		}
		
		public void stop() {
			this.executor.interrupt();
			this.executor = null;
		}
		
		@Override
		public void run() {
			logger.debug("started " + Thread.currentThread().getName());
						
			try {
				Thread.sleep(delayStart);
			} catch (InterruptedException e1) {

			}
			while ( Thread.currentThread() == executor) {
				try {
					Long seq = sequenceQ.take();
					if ( seq != null) {
						run0(seq.longValue());
					}

				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					logger.warn("Exception in Injection Worker while publishing ", e);
				}
			}
			
			logger.debug("exiting " + Thread.currentThread().getName());

		}
		public void run0(long masGlobalSequence) {
			long startTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder("injection begins::::");
			sb.append("StartTime: ").append(startTime).append(' ');
			
			final List<IPublishingApp> apps = injectionConfig.getPublishingApps();

			for ( IPublishingApp app: apps) {
				long beginTime = System.currentTimeMillis();
				try {
					app.atPublish(masGlobalSequence);
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					logger.warn("Exception in Injection Worker while publishing " + app.getName(), e);
				}
				sb.append("\t").append(app.getName()).append(" spent ").append( (System.currentTimeMillis() - beginTime)).append(' ');
			}
			long endTime = System.currentTimeMillis();
			sb.append("End Time: ").append(endTime).append(" escaped: ").append( (endTime - startTime)).append(' ');
			logger.debug(sb.toString());
		}
		
	}

}
