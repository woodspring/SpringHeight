package com.tts.mas;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.constant.Constants;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam.QuoteDirection;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.util.config.UtilConfiguration;

public class QuoteTimeOutTest {

	static Logger logger = LoggerFactory.getLogger(QuoteTimeOutTest.class);
	
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();;

		ctx.register(UtilConfiguration.class);
		ctx.register(MessageConfiguration.class);
		ctx.refresh();
		ctx.registerShutdownHook();
		
		QuoteParam.Builder qp = QuoteParam.newBuilder();
		qp.setCurrencyPair("USDCAD")
		  .setNotionalCurrency("USD")
		  .setProduct(Constants.ProductType.FXSPOT)
		  .setQuoteDirection(QuoteDirection.BOTH)
		  .setSize("1000000");
		
		
		PriceSubscriptionRequest.Builder priceSubscriptionRequest = PriceSubscriptionRequest.newBuilder();
		priceSubscriptionRequest.setQuoteParam(qp);
		
		TtMsg ttMsg = TtMsgEncoder.encode(priceSubscriptionRequest.build());
		
		
		IMsgSenderFactory msgSenderFactory = ctx.getBean("msgSenderFactory", IMsgSenderFactory.class);			
		IMsgSender msgSender0 = msgSenderFactory.getMsgSender(true, false);
		msgSender0.send("TTS.CTRL.EVENT.REQUEST.USDCAD.MR.QUOTE", ttMsg);
		
		ctx.close();
	}
}
