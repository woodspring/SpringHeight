package com.tts.plugin.adapter.api;

import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;


/**
 * 
 * This is the top-level service provider interface (SPI) for the "apps" 
 * running in the market adapter server (MAS) 
 * 
 * Example of "apps" are SPOT adapter, FORWARD CURVE adapter
 */
public interface IMasApplicationPluginSpi {
	
	/**
	 * Provide the name of plugin implementation
	 * 
	 * @return name
	 */
	public String getName();
	
	/** 
	 * Provide the factory class for create PublishingApp
	 * 
	 * NOTE: See IInterfacingApp for InterfacingApp definition
	 * 
	 * @return factory class implementation
	 */
	public IInterfacingAppFactory getInterfacingAppFactory();
	
}
