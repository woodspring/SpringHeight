package com.tts.mde.support;

import java.util.concurrent.ScheduledFuture;

/**
 * Interface for Scheduling Worker Implementation. Allowing plugin/"apps" to scheduling
 * task (Runnables) using the core's threads.
 * 
 * Note: NOT for plugin to implement
 * 
 */
public interface ISchedulingWorker {
	
	/**
	 * Allow plugin to schedule a task periodically 
	 * 
	 * @param arg0  the runnable task to be run periodically 
	 * @param arg1  the period in milliseconds with the task will be invoke
	 * 
	 * @return Future object representing the task
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable arg0, long arg1);
	
	/**
	 * Allow plugin to schedule a task after a fixed time delay
	 * @param runnable	the runnable task to be executed after a fixed time 
	 * @param period	the period in milliseconds after which the task gets executed.
	 * @return			Future object representing the task
	 */	
	public ScheduledFuture<?> scheduleAtFixedDelay(Runnable runnable, long period);
	
	
	/**
	 * Allow plugin to execute a task  
	 * 
	 * @param arg0  the runnable task to be run periodically 
	 * 
	 */
	public void execute(Runnable r);


	public void init();

	public void destroy();

}
