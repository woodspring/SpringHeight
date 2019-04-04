package com.tts.mlp.app.price.data;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.rate.provider.vo.Instrument;

public class UpdatableMarketPriceProvider extends DefaultRandomMarketPriceProvider
		implements IUpdatableMarketPriceProvider {

	public UpdatableMarketPriceProvider(String defaultPriceUrl, String defaultSpreadUrl, String instrumentConfigUrl) {
		super(defaultPriceUrl, defaultSpreadUrl, instrumentConfigUrl);
	}

	@Override
	public void updateMarketLiquidityStructure(String symbol, String defaultRate, String[] structure) {
		Instrument instrument = getCurrentPrice(symbol).deepClone();
		instrument.getAskTicks().clear();
		instrument.getBidTicks().clear();
		fillInstrumentLiquidityWithRate(
				instrument, 
				Double.parseDouble(GlobalAppConfig.getSpreadConfig().getSpread()), 
				defaultRate, 
				structure);
		getInitialInstrumentPricesMap().put(instrument.getSymbol(), instrument);
		getLatestInstrumentPricesMap().remove(instrument.getSymbol());
	}

}
