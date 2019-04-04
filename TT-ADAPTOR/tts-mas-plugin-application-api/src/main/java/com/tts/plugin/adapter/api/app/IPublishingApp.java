package com.tts.plugin.adapter.api.app;

/**
 * IPublishingApp is "app" that will publish market data in a periodical manner
 * 
 */
public interface IPublishingApp extends IExternalInterfacingApp {

	/**
	 * Action for "app" to implement for what to do at publishing time
	 * 
	 * @param masGlobalSeq Market Adapter Server sequence
	 */
	public void atPublish(long masGlobalSeq);
	
	
	public IApp.PublishingFormatType getPublishingFormatType();
}
