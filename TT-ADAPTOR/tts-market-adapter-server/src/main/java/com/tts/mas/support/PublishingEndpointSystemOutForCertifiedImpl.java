package com.tts.mas.support;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;


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
