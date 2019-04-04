package com.tts.fa;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.util.config.UtilConfiguration;

public class SubscribeTestMain {

	public static void main(String[] args) {
		
		String topic = "TTS.CTRL.EVENT.REQUEST.FA.SUBSCRIBE.TRADAIR";

		AnnotationConfigApplicationContext ctx = null;				

		try  {
			ctx = new AnnotationConfigApplicationContext();
			ctx.register(UtilConfiguration.class);
			ctx.register(MessageConfiguration.class);

			ctx.refresh();
			ctx.registerShutdownHook();
			
			IMsgSenderFactory msgSenderFactory = ctx.getBean(IMsgSenderFactory.class);
			IMsgSender msgSender = msgSenderFactory.getMsgSender(false, false);
			
			msgSender.init();
			
			PriceSubscriptionRequest.Builder reqB = PriceSubscriptionRequest.newBuilder();
			reqB.getQuoteParamBuilder().setCurrencyPair("USDTRY");
			
			PriceSubscriptionRequest req = reqB.build();
			TtMsg ttMsg = TtMsgEncoder.encode(req);

			msgSender.send(topic, ttMsg);
			
			Thread.sleep(5000L);


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
