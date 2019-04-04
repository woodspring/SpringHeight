package com.tts.plugin.adapter.impl.base.app.trade;

import java.util.HashMap;
import java.util.Map;

import com.tts.plugin.adapter.impl.base.vo.TradeRequestWithQuoteVo;

public class TradeQuoteRepo {

	private final Map<String, TradeRequestWithQuoteVo> repo = new HashMap<String, TradeRequestWithQuoteVo>();
	
	public synchronized void associateQuote(String transID, TradeRequestWithQuoteVo quote) {
		repo.put(transID, quote);
	}
	
	public synchronized TradeRequestWithQuoteVo unassociateQuote(String transID) {
		return repo.remove(transID);
	}

	public synchronized TradeRequestWithQuoteVo find(String transId) {
		return repo.get(transId);		
	}
}
