package com.tts.mlp.app.price.data;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.tts.mlp.rate.provider.vo.Instrument;

public class DefaultRandomMarketPriceProvider extends AbstractMarketPriceProvider implements IRandomMarketPriceProvider {

	
	private final Set<String> supportedSymbols;
	private final Map<String, Instrument> initialInstrumentPricesMap;
	private final Map<String, Instrument> latestInstrumentPricesMap;
	
	public DefaultRandomMarketPriceProvider(String defaultPriceUrl, String instrumentConfigUrl) {
		Properties instrumentProperties = null;
		try {
			instrumentProperties = loadInstrumentProperties(instrumentConfigUrl);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.supportedSymbols = buildSupportedSymbolSet(instrumentProperties);
		this.initialInstrumentPricesMap = buildPrices(defaultPriceUrl, instrumentProperties);
		this.latestInstrumentPricesMap = new ConcurrentHashMap<String, Instrument>(initialInstrumentPricesMap);
	}
	
	@Override
	public Instrument getCurrentPrice(String symbol) {
		Instrument instrument = latestInstrumentPricesMap.get(symbol);
		if ( instrument == null ) {
			 instrument = initialInstrumentPricesMap.get(symbol);
		}
		return instrument;
	}

	@Override
	public Instrument getNextMarketPrice(String symbol) {
		Instrument _instrument = latestInstrumentPricesMap.get(symbol);

		if ( _instrument != null ) {
			Instrument instrument = _instrument.deepClone();
			instrument.randomWalk();
			latestInstrumentPricesMap.put(symbol, instrument);
			return  instrument;
		}

		return _instrument;
	}

	
	public Map<String, Instrument> getLatestInstrumentPricesMap() {
		return latestInstrumentPricesMap;
	}

	public Map<String, Instrument> getInitialInstrumentPricesMap() {
		return initialInstrumentPricesMap;
	}

	private Map<String, Instrument> buildPrices(
			String defaultPriceUrl,
			Properties instrumentProperties) {
		Map<String, Instrument> rte = new ConcurrentHashMap<String, Instrument>();
		Map<String, BigDecimal> seedMap = loadRandomData(defaultPriceUrl);
		for ( String symbol: supportedSymbols) {
			int precision = getInstrumentPrecision(symbol, instrumentProperties);
			int pointValue = getInstrumentPointValue(symbol, instrumentProperties);
			Instrument instrument = new Instrument(symbol, precision, pointValue);

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
					
			rte.put(symbol, instrument);
		}
		return rte;
	}
	
}
