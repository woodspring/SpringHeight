package com.tts.mde.support;

import com.google.protobuf.Message;

/**
 * IExternalApp shall use this implementation to publish non-certified message
 * 
 *
 */
public interface IPublishingEndpoint {

	/**
	 * publish a message
	 * 
	 * @param topic
	 * @param message
	 */
	public void publish(String topic, Message message);
	
}
