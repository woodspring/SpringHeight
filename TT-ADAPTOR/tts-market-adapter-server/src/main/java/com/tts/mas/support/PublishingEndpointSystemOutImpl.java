package com.tts.mas.support;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.tts.plugin.adapter.support.IPublishingEndpoint;


public class PublishingEndpointSystemOutImpl implements IPublishingEndpoint {

	@Override
	public void publish(String topic, Message message) {
		String msg = TextFormat.shortDebugString(message);
		if ( msg.indexOf("EURUSD") > 0 ) {
			System.out.println(TextFormat.shortDebugString(message));
		}
	}
	
	public void init() {
		
	}
	
	public void destroy() {
		
	}

}
