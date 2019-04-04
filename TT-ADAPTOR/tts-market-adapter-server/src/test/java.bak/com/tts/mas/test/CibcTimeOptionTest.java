package com.tts.mas.test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
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

public class CibcTimeOptionTest {

	public static final String REQ_STR = "requestId: \"TTS.MD.FX.SR.TIMEOP.USDCAD.6894905730389526273.AP.10.1W.14899788688\" topic: \"TTS.MD.FX.FA.TIMEOP.USDCAD.1234\" rateRequestType: RFQ requestorProfile { accountId: \"10\" clientId: \"923\" customerId: \"10\" channelId: 1 priceConv: \"INTERBANK\" partySessionInfos { cmId: 6894905730389526273 userNm: \"btyyang\" userId: 923 userExtId: 923 instanceNm: \"5LZ8_2xwGGRUAtZSQK3LhoweaLGZlyW80qGfWXoq\" } } quoteParam { product: \"FxTimeOption\" quoteDirection: BOTH currencyPair: \"USDCAD\" nearDateDetail { periodCd: \"W\" periodValue: \"1\" actualDate: \"20170331\" } farDateDetail { periodCd: \"W\" periodValue: \"2\" actualDate: \"20170407\" } size: \"1000000\" notionalCurrency: \"USD\" quoteDuration: 120 rfqShowMatchedRungOnly: true } rateSource { sourcePricingType: AUTO }";
	
	public static void main(String[] args) throws ParseException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();;

		ctx.register(UtilConfiguration.class);
		ctx.register(MessageConfiguration.class);
		ctx.refresh();
		ctx.registerShutdownHook();
		
		PriceSubscriptionRequest.Builder priceSubscriptionRequest = PriceSubscriptionRequest.newBuilder();
		TextFormat.merge(REQ_STR, priceSubscriptionRequest);
		TtMsg ttMsg = TtMsgEncoder.encode(priceSubscriptionRequest.build());
		
		
		IMsgSenderFactory msgSenderFactory = ctx.getBean("msgSenderFactory", IMsgSenderFactory.class);			
		IMsgSender msgSender0 = msgSenderFactory.getMsgSender(true, false);
		msgSender0.send("TTS.CTRL.EVENT.REQUEST.USDCAD.MR.QUOTE", ttMsg);
		
		ctx.close();
	}

}
