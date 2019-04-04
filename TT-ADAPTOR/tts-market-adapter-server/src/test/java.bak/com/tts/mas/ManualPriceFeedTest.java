package com.tts.mas;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.ManualPriceFeed;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.config.UtilConfiguration;

public class ManualPriceFeedTest {

	static Logger logger = LoggerFactory.getLogger(ManualPriceFeedTest.class);
	
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		
		ctx.register(UtilConfiguration.class);
		ctx.register(MessageConfiguration.class);
		ctx.refresh();
		ctx.registerShutdownHook();
		
		ManualPriceFeed.Builder mpFeed = ManualPriceFeed.newBuilder();
		
		FullBook.Builder fbAEDUSD = FullBook.newBuilder();
		FullBook.Builder fbAEDCAD = FullBook.newBuilder();
		
		fbAEDUSD.setSymbol("AEDUSD");
		fbAEDCAD.setSymbol("AEDCAD");
		
		Tick.Builder bidAEDUSD = Tick.newBuilder();
		bidAEDUSD.setRate("0.0000");
		Tick.Builder askAEDUSD = Tick.newBuilder();
		askAEDUSD.setRate("0.3852");
		
		fbAEDUSD.addBidTicks(bidAEDUSD);
		fbAEDUSD.addAskTicks(askAEDUSD);
		
		Tick.Builder bidAEDCAD = Tick.newBuilder();
		bidAEDCAD.setRate("0.0000");
		Tick.Builder askAEDCAD = Tick.newBuilder();
		askAEDCAD.setRate("0.4560");
		
		fbAEDCAD.addBidTicks(bidAEDCAD);
		fbAEDCAD.addAskTicks(askAEDCAD);
		
		mpFeed.addPriceFeed(fbAEDUSD);
		mpFeed.addPriceFeed(fbAEDCAD);
		
		TtMsg ttMsg = TtMsgEncoder.encode(mpFeed.build());
				
		IMsgSenderFactory msgSenderFactory = ctx.getBean("msgSenderFactory", IMsgSenderFactory.class);			
		IMsgSender msgSender0 = msgSenderFactory.getMsgSender(true, false);
		msgSender0.send(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_MANUAL_RATE_UPDATE_REQUEST, ttMsg);
		
		ctx.close();
	}
}
