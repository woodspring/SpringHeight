package com.tts.mas.manager.plugin;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.impl.base.DefaultMasIntegrationPlugin;


public class FixIntegrationPluginManager extends  com.tts.util.plugin.AbstractPlugInManager<IFixIntegrationPluginSpi> {
	
	private final static Logger logger = LoggerFactory.getLogger(FixIntegrationPluginManager.class);
	
	private final IFixIntegrationPluginSpi activePlugin;

	public FixIntegrationPluginManager() {
		super(IFixIntegrationPluginSpi.class);
		
		List<IFixIntegrationPluginSpi> availablePlugins = new ArrayList<IFixIntegrationPluginSpi>();
		for ( IFixIntegrationPluginSpi plugin: loader) {
			availablePlugins.add(plugin);
		}
		loader = null;
		
		if ( availablePlugins.size() > 2) {
			logger.warn("More than 2 FIX Integration plugins available..." );
		}
		
		IFixIntegrationPluginSpi defaultPlugin = null;
		/**
		 * This logic is odd ... if there is > 1 active plugins that are not the default plugin then the active plugin will be determined by which one it finds first by virtue of class path/loader
		 */
		IFixIntegrationPluginSpi activePlugin = null;
		for ( IFixIntegrationPluginSpi plugin: availablePlugins) {
			if ( DefaultMasIntegrationPlugin.NAME__TTS_DEFAULT.equals(plugin.getName())) {
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
			logger.error("No FIX Integration plugins found");
		} else {
			logger.info("Active Integration plugin = " + activePlugin.getName());
		}
		
		this.activePlugin = activePlugin;
		
		
	}

	public IFixIntegrationPluginSpi getActiveIntegrationPlugin() {
		if ( activePlugin == null  ) {
			return new DefaultMasIntegrationPlugin();
		}
		return activePlugin;
	}
	
	

}
