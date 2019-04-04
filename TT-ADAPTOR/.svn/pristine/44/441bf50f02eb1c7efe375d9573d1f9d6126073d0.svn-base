package com.tts.mas.manager.plugin;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.plugin.adapter.api.IMasApplicationPluginSpi;
import com.tts.plugin.adapter.impl.base.DefaultMasApplicationPlugin;


public class MasApplicationPluginManager extends  com.tts.util.plugin.AbstractPlugInManager<IMasApplicationPluginSpi> {
	
	private final static Logger logger = LoggerFactory.getLogger(MasApplicationPluginManager.class);
	
	private final IMasApplicationPluginSpi activePlugin;

	public MasApplicationPluginManager() {
		super(IMasApplicationPluginSpi.class);
		
		List<IMasApplicationPluginSpi> availablePlugins = new ArrayList<IMasApplicationPluginSpi>();
		for ( IMasApplicationPluginSpi plugin: loader) {
			availablePlugins.add(plugin);
		}
		loader = null;
		
		if ( availablePlugins.size() > 2) {
			logger.warn("More than 2 MAS Application plugins available..." );
		}
		
		IMasApplicationPluginSpi defaultPlugin = null;

		IMasApplicationPluginSpi activePlugin = null;
		for ( IMasApplicationPluginSpi plugin: availablePlugins) {
			if ( DefaultMasApplicationPlugin.NAME__TTS_DEFAULT.equals(plugin.getName())) {
				defaultPlugin = plugin;
			} else {
				activePlugin = plugin;
				break;
			}
		}
		
		if ( activePlugin == null ) {
			activePlugin = defaultPlugin;
		}
		
		if ( activePlugin == null) {
			logger.error("No MAS Application plugins found");
		} else {
			logger.info("Active Application plugin = " + activePlugin.getName());
		}
		
		this.activePlugin = activePlugin;
		
		
	}

	public IMasApplicationPluginSpi getActiveApplicationPlugin() {
		if ( activePlugin == null ) {
			return new DefaultMasApplicationPlugin();
		}
		return activePlugin;
	}
	
	

}
