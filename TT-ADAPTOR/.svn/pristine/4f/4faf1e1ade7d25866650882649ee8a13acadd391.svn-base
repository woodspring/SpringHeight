package com.tts.mlp.app.price.data;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.tts.mlp.rate.provider.vo.Instrument;


public class DefaultPreGeneratedMarketPriceProvider extends AbstractMarketPriceProvider implements IPreGeneratedPriceProvider {

	private final Set<String> supportedSymbols;
	private final Map<String, Instrument[]> instrumentPricesMap;
	
	public DefaultPreGeneratedMarketPriceProvider(String defaultPriceUrl, String instrumentConfigUrl) {
		Properties instrumentProperties = null;
		try {
			instrumentProperties = loadInstrumentProperties(instrumentConfigUrl);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.supportedSymbols = buildSupportedSymbolSet(instrumentProperties);
		this.instrumentPricesMap = buildPrices(defaultPriceUrl, instrumentProperties);
	}
	
	@Override
	public Instrument[] getGeneratedPrices(String symbol) {
		return instrumentPricesMap.get(symbol);
	}

	private Map<String, Instrument[]> buildPrices(
			String defaultPriceUrl,
			Properties instrumentProperties) {
		Map<String, Instrument[]> rte = new HashMap<String, Instrument[]>();
		Map<String, BigDecimal> seedMap = loadRandomData(defaultPriceUrl);
		for ( String symbol: supportedSymbols) {
			int precision = getInstrumentPrecision(symbol, instrumentProperties);
			int pointValue = getInstrumentPointValue(symbol, instrumentProperties);


			Instrument instrument = new Instrument(symbol, precision, pointValue); 
			if (symbol.substring(3,6).equals("JPY")) {
				instrument = new Instrument(symbol, 3, 2);
			}
			
			BigDecimal rate = null;
			if (seedMap.containsKey(symbol)) {
				rate = seedMap.get(symbol);
			} else {
				rate = new BigDecimal(DEFAULT_RATE);
			}
			instrument.setTobMidPrice(rate.doubleValue());

			long[] structure = getInstrumentLiquidities(symbol, instrumentProperties);
			if (structure != null) {
				instrument.setLqLevels(structure);
			}
			
			
				instrument.randomWalk();
						
		}
		return rte;
	}

	
	public void init() {
		
	}

	
	public static void main(String[] args) {
		DefaultPreGeneratedMarketPriceProvider p = new DefaultPreGeneratedMarketPriceProvider(
				"app-resources/random.seed.data.txt", 
				"instrument.cfg");
		
		
		int priceIdx =0 ;
		for ( priceIdx = 1; priceIdx < PRICE_VARIATION; priceIdx++ ) {
			Instrument i = p.getGeneratedPrices("AUDCAD")[priceIdx];
			System.out.println(priceIdx);

			System.out.println(i.getBidTicks().get(0).getPrice());
			System.out.println(i.getAskTicks().get(0).getPrice());
		}

	}
}

