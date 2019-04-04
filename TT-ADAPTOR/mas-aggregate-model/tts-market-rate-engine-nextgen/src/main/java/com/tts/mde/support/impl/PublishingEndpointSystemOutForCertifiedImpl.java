package com.tts.mde.support.impl;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.tts.mde.support.ICertifiedPublishingEndpoint;


public class PublishingEndpointSystemOutForCertifiedImpl implements ICertifiedPublishingEndpoint {

	@Override
	public void publish(String topic, Message message) {
		System.out.println(TextFormat.printToString(message));
	}
	
	public void init() {
		
	}
	
	public void destroy() {
		
	}

}
