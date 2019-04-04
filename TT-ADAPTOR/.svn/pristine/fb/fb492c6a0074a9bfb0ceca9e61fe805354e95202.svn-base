package com.tts.plugin.adapter.api.app;

/**
 * IExternalInterfacingApp is an "app" we internally use in the platform.
 * 
 * It will send/get data from an ISupportiveApp
 */
public interface IExternalInterfacingApp extends IApp {

	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior();
	
	/**
	 * Steps to be run before actual start servicing
	 * 
	 * 
	 */
	public void init();
	
	public enum ChangeTradingSessionBehavior {
		REFRESH,
		NO_CHANGE;
	}
	
	
}
