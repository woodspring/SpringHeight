package com.tts.mlp.app.price.data;

import com.tts.mlp.rate.provider.vo.Instrument;

public class UpdatableMarketPriceProvider extends DefaultRandomMarketPriceProvider
		implements IUpdatableMarketPriceProvider {

	public UpdatableMarketPriceProvider(String defaultPriceUrl, String instrumentConfigUrl) {
		super(defaultPriceUrl, instrumentConfigUrl);
	}

	@Override
	public void updateMarketLiquidityStructure(String symbol, String defaultRate, String[] structure) {
		Instrument instrument = getCurrentPrice(symbol);
		instrument.setTobMidPrice(Double.parseDouble(defaultRate));
		
		long[] ls = new long[structure.length];
		for ( int i = 0; i < structure.length; i++) {
			ls[i] = Long.parseLong(structure[i]);
		}
		instrument.setLqLevels(ls);
	}

	@Override
	public void updateMarketBook(String symbol, Instrument i) {
		getLatestInstrumentPricesMap().put(symbol, i);		
	}

}
