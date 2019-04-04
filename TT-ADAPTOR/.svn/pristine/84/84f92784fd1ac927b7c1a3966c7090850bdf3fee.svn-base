package com.tts.ske.app.price.subscription;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.platform.IMsgSender;

public class InternalConsumer implements IMdSubscriber {
	private final  String reqId;
	private final  IMsgSender marketDataSender;
	public InternalConsumer(String reqId, IMsgSender marketDataSender) {
		this.marketDataSender = marketDataSender;
		this.reqId = reqId;
	}

	@Override
	public void onNewMarketData(String symbol, RawMarketBook mb) {
		RawMarketBook.Builder bookB = RawMarketBook.newBuilder(mb);
		bookB.setRequestId(reqId);
		bookB.setUpdateTimeStamp(System.currentTimeMillis());
		RawMarketBook book = bookB.build();
		TtMsg ttMsg = TtMsgEncoder.encode(book);
		marketDataSender.send("TTS.MD.FX.FA.SPOT." + symbol + ".INTERNAL.INTERNAL", ttMsg);
	}
}
