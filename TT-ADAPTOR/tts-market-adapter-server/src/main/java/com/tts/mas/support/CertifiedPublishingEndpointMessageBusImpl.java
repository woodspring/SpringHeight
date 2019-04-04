package com.tts.mas.support;

import com.google.protobuf.Message;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.util.TtMsgEncoder;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.util.AppContext;

public class CertifiedPublishingEndpointMessageBusImpl implements ICertifiedPublishingEndpoint {
	
	private final IMsgSender msgSender;
	
	public CertifiedPublishingEndpointMessageBusImpl() {
		IMsgSenderFactory msgSenderFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		msgSender = msgSenderFactory.getMsgSender(false, false, true);
	}
	
	public void init() {
		msgSender.init();
	}
	
	public void destroy() {
		msgSender.destroy();
	}

	@Override
	public void publish(String topic, Message message) {
		TtMsg ttMsg = TtMsgEncoder.encode(message);
		msgSender.send(topic, ttMsg);
	}

}
