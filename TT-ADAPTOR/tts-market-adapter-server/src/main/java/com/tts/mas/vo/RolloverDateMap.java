package com.tts.mas.vo;

import java.util.HashMap;

public class RolloverDateMap {

	private final HashMap<String, String> map = new HashMap<String, String>();
	
	long receivedTimestamp;
	
	public String getTradeDate(String symbol) {
		return map.get(symbol);
	}
	
	public void setTradeDate(String symbol, String tradeDate) {
		map.put(symbol, tradeDate);
	}

	public long getReceivedTimestamp() {
		return receivedTimestamp;
	}

	public void setReceivedTimestamp(long receivedTimestamp) {
		this.receivedTimestamp = receivedTimestamp;
	}
	
	
}
