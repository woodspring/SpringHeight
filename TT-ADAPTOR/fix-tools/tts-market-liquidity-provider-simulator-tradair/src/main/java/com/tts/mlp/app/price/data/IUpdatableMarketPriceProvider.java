package com.tts.mlp.app.price.data;

public interface IUpdatableMarketPriceProvider extends IRandomMarketPriceProvider {

	
	public void updateMarketLiquidityStructure(String symbol, String defaultRate, String[] structure);
}
