package com.tts.ske.app.price.subscription;

import com.tts.message.market.MarketStruct.RawMarketBook;

public interface IMdSubscriber {

	public void onNewMarketData(String symbol, RawMarketBook mb) ;
}
