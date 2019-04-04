package com.tts.mde.support.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.support.ISchedulingWorker;


public class MdeSchedulingWorkerImpl implements ISchedulingWorker {
	
	private static final Logger logger = LoggerFactory.getLogger(ISchedulingWorker.class);
	private final ScheduledExecutorService service;

	public MdeSchedulingWorkerImpl(ScheduledExecutorService executorService ) {
		this.service = executorService;
	}
	
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long period) {
		logger.debug(String.format("Scheduling runnable, %s, at rate %s ms", runnable.getClass().getName(), period));
		return service.scheduleAtFixedRate(new ExceptionWrappedRunnable(runnable), 0, period, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public ScheduledFuture<?> scheduleAtFixedDelay(Runnable runnable, long period)	{
		logger.debug(String.format("Scheduling runnable, %s, at rate %s ms", runnable.getClass().getName(), period));
		return service.schedule(new ExceptionWrappedRunnable(runnable), period, TimeUnit.MILLISECONDS);
	}

	@Override
	public void execute(Runnable runnable) {
		logger.debug(String.format("Executing runnable, %s", runnable.getClass().getName()));
		service.execute(new ExceptionWrappedRunnable(runnable));
	}

	@Override
	public void init() {

	}

	@Override
	public void destroy() {

	}
	
	public static class MasThreadFactory implements ThreadFactory {

		private volatile int count = 0;
		
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			count++;
			logger.debug(String.format("Added new Thread in MasSchedulingWorker. First runnable is %s. Total thread = %d", 
					r.getClass().getName(), 
					count));
			return t;
		}
		
	}
	
	private static class ExceptionWrappedRunnable implements Runnable {
		final Runnable r;
		
		private ExceptionWrappedRunnable(Runnable r) {
			this.r = r;
		}

		@Override
		public void run() {
			try {
				r.run();
				Thread.sleep(1L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.debug(String.format("Exception in scheduling task, %s", r.getClass().getName() ));
			}
			
		}
		
	}

}
