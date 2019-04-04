package com.tts.mlp.app.price.data;

import com.tts.mlp.rate.provider.vo.Instrument;

public interface IUpdatableMarketPriceProvider extends IRandomMarketPriceProvider {

	
	void updateMarketLiquidityStructure(String symbol, String defaultRate, String[] structure);
	
	void updateMarketBook(String symbol, Instrument i);
}
