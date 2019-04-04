package com.tts.mas;

import java.util.Calendar;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.message.trade.TradeMessage.TransactionDetail;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.util.config.UtilConfiguration;

public class MKTOrderTest {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();;

		ctx.register(UtilConfiguration.class);
		ctx.register(MessageConfiguration.class);
		ctx.refresh();
		ctx.registerShutdownHook();
		
		int iCnt = 1;
		String side = "B";
		String crncy = "EUR";
		int change = 1;
		
		while(iCnt <= 12)	{
			
			TransactionDetail.Builder transactionDetails = TransactionDetail.newBuilder();
			transactionDetails.setCurrency1Amt("109655")
							  .setCurrency2Amt("109814");						  
			
			
			Transaction.Builder transactionMessage = Transaction.newBuilder();
			transactionMessage.setSymbol("EURUSD")
							  .setNotionalCurrency(crncy)
			                  .setTransId(Long.toString(Calendar.getInstance().getTimeInMillis()))
			                  .setNearDateDetail(transactionDetails)
			                  .setTradeAction(side);
			
			TtMsg ttMsg = TtMsgEncoder.encode(transactionMessage.build());
			
			IMsgSenderFactory msgSenderFactory = ctx.getBean("msgSenderFactory", IMsgSenderFactory.class);			
			IMsgSender msgSender0 = msgSenderFactory.getMsgSender(true, false);
			msgSender0.send("TTS.TRAN.FX.MKTORD.TRANINFO.MR", ttMsg);
			
			side = (side.equalsIgnoreCase("B"))? "S": "B";
			change++;
			iCnt++;
			
			if(change > 2)	{
				change = 1;
				crncy = (crncy.equalsIgnoreCase("EUR"))? "USD": "EUR";
			}
		}
		
		
		ctx.close();
		System.out.println("Message SEND.....");
	}

}
