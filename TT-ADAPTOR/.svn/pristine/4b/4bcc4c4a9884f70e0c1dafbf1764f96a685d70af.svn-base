package com.tts.mas.feature.position;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.protobuf.Message;
import com.tts.message.common.CommonStruct.BuySellActionCd;
import com.tts.message.common.CommonStruct.SideCd;
import com.tts.message.market.MarketMarkerStruct.AddMarketAdjustmentLiquidity;
import com.tts.message.market.MarketMarkerStruct.CancelMarketMakingRequest;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.config.UtilConfiguration;

public class TraderAdjustmentAppTest {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = null;
		ctx = new AnnotationConfigApplicationContext();
		ctx.register(UtilConfiguration.class);
		ctx.register(MessageConfiguration.class);
		ctx.refresh();
		
		IMsgSenderFactory msgSenderFactory = ctx.getBean(IMsgSenderFactory.class);
		IMsgSender msgSender = msgSenderFactory.getMsgSender(false, false);
		msgSender.init();
		String topic1 = String.format(IEventMessageTypeConstant.Control.Event.REQUEST_TEMPLATE_WITH_ACTIONS, 
				IEventMessageTypeConstant.BankTrader.UI_NAME, "MR_PRICE_ADJUSTMENT", "CHANGE");

		String topic2 = String.format(IEventMessageTypeConstant.Control.Event.REQUEST_TEMPLATE_WITH_ACTIONS, 
				IEventMessageTypeConstant.BankTrader.UI_NAME, "MR_PRICE_ADJUSTMENT", "STATUS_REQUEST");
		
		String topic3 = String.format(IEventMessageTypeConstant.Control.Event.REQUEST_TEMPLATE_WITH_ACTIONS, 
				IEventMessageTypeConstant.BankTrader.UI_NAME, "MR_PRICE_ADJUSTMENT", "STATUS_UNSUBSCRIBE");
		
		AddMarketAdjustmentLiquidity.Builder addMarketAdjustmentLiquidity = AddMarketAdjustmentLiquidity.newBuilder();
		addMarketAdjustmentLiquidity.setAdjustment(0.0004);
		addMarketAdjustmentLiquidity.setSymbol("USDTRY");
		addMarketAdjustmentLiquidity.setSide(SideCd.BID);
		addMarketAdjustmentLiquidity.setSize(20000000);
		addMarketAdjustmentLiquidity.setLimitPrice(3.8920);
		Message m1 = addMarketAdjustmentLiquidity.build();
		
		CancelMarketMakingRequest.Builder cancelMarketMakingRequest = CancelMarketMakingRequest.newBuilder();
		cancelMarketMakingRequest.setSymbol("EURUSD");
		cancelMarketMakingRequest.setBuySellActionCd(BuySellActionCd.SELL);
		msgSender.send(IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_CANCEL_REQUEST, TtMsgEncoder.encode(cancelMarketMakingRequest.build()));

		
		//msgSender.send(topic1, TtMsgEncoder.encode(m1));
		ctx.close();
	}

}
