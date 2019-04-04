package com.tts.mlp.app.price.data;

import com.tts.mlp.rate.provider.vo.Instrument;

public interface IPreGeneratedPriceProvider {
	
	public final static int PRICE_VARIATION = 71;

	public Instrument[] getGeneratedPrices(String symbol);
	
}
