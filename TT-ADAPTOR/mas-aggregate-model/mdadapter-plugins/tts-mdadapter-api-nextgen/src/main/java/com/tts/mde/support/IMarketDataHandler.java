package com.tts.mde.support;

public interface IMarketDataHandler {
	void init();
	void destroy();
	
	void atPublish(long masGlobalSeq, IPublishingEndpoint publishingEndpoint);

}