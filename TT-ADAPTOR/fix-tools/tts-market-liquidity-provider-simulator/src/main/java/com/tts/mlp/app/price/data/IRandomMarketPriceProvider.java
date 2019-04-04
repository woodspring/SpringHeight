package com.tts.mlp.app.price.data;

import com.tts.mlp.rate.provider.vo.Instrument;

public interface IRandomMarketPriceProvider {

	public Instrument getCurrentPrice(String symbol);
	
	public Instrument getNextMarketPrice(String symbol);
}
