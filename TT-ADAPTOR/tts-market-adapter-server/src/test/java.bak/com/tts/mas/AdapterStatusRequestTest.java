package com.tts.mas;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.mas.config.MasReutersConfiguration;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.constant.Constants;
import com.tts.message.system.admin.AdapterStruct.AdapterStatus;
import com.tts.message.system.admin.AdapterStruct.AdapterStatusRequest;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.util.config.UtilConfiguration;

public class AdapterStatusRequestTest {

	
	public static void main(String[] args) throws InvalidProtocolBufferException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		ctx.register(UtilConfiguration.class);
		ctx.register(MessageConfiguration.class);
		ctx.refresh();
		ctx.registerShutdownHook();
		
		IMsgSenderFactory msgSenderFactory = ctx.getBean(IMsgSenderFactory.class);
		IMsgSender msgSender = msgSenderFactory.getMsgSender(false, false);
		msgSender.init();
		
		AdapterStatusRequest.Builder b = AdapterStatusRequest.newBuilder();
		b.setAdapterName(Constants.FixAdapterNameType.DEFAULT_FIX_ADAPTER);
		
		TtMsg ttReply = msgSender.sendRequest("TTS.CTRL.EVENT.MNT.REQUEST.FA.STATUS", TtMsgEncoder.encode(b.build()));
		
		AdapterStatus s = AdapterStatus.parseFrom(ttReply.getParameters());
		
		System.out.println(TextFormat.shortDebugString(s));
	}
}
