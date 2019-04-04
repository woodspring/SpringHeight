package com.tts.mlp.app.price.data;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.Tick;
import com.tts.vo.NumberVo;

public class DefaultRandomMarketPriceProvider extends AbstractMarketPriceProvider implements IRandomMarketPriceProvider {

	
	private final double spreadRange = DEFAULT_SPREAD_RANGE;

	private final Set<String> supportedSymbols;
	private final Map<String, Instrument> initialInstrumentPricesMap;
	private final Map<String, Instrument> latestInstrumentPricesMap;

	
	public DefaultRandomMarketPriceProvider(String defaultPriceUrl, String defaultSpreadUrl, String instrumentConfigUrl) {
		Properties instrumentProperties = null;
		try {
			instrumentProperties = loadInstrumentProperties(instrumentConfigUrl);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.supportedSymbols = buildSupportedSymbolSet(instrumentProperties);
		this.initialInstrumentPricesMap = buildPrices(defaultPriceUrl, defaultSpreadUrl, instrumentProperties);
		this.latestInstrumentPricesMap = new ConcurrentHashMap<String, Instrument>(supportedSymbols.size());
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
		Instrument _instrument = initialInstrumentPricesMap.get(symbol);
		
		if ( _instrument != null ) {
			Instrument instrument = _instrument.deepClone();
			instrument.randomWalk();
			latestInstrumentPricesMap.put(symbol, instrument);
			return  instrument;
		}

		return null;
	}

	
	public Map<String, Instrument> getLatestInstrumentPricesMap() {
		return latestInstrumentPricesMap;
	}

	public Map<String, Instrument> getInitialInstrumentPricesMap() {
		return initialInstrumentPricesMap;
	}

	private Map<String, Instrument> buildPrices(
			String defaultPriceUrl,
			String defaultSpreadUrl,
			Properties instrumentProperties) {
		Map<String, Instrument> rte = new ConcurrentHashMap<String, Instrument>();
		Map<String, BigDecimal> seedMap = loadData(defaultPriceUrl);
		Map<String, BigDecimal> spreadMap = loadData(defaultSpreadUrl);

		for ( String symbol: supportedSymbols) {
			double spreadRange = this.spreadRange;
			
			if ( spreadMap.get(symbol) != null ) {
				spreadRange = spreadMap.get(symbol).doubleValue();
			}
			
			int precision = getInstrumentPrecision(symbol, instrumentProperties);
			int pointValue = getInstrumentPointValue(symbol, instrumentProperties);
			String increment = getInstrumentIncrement(symbol, instrumentProperties);

			String default_increment = new NumberVo(Double.toString(1.111111 * Math.pow(10, -1 * pointValue)), precision).toPercisionString();

			Instrument instrument = new Instrument(symbol);
			instrument.setPrecision(precision);
			instrument.setPointValue(pointValue);
			instrument.setIncrement(increment == null? default_increment: increment);
			
			BigDecimal rate = null;
			if (seedMap.containsKey(symbol)) {
				rate = seedMap.get(symbol);
			} else {
				rate = new BigDecimal(DEFAULT_RATE);
			}

			String[] structure = getInstrumentLiquidities(symbol, instrumentProperties);
			if (structure != null) {
				fillInstrumentLiquidityWithRate(instrument, spreadRange, rate.toString(), structure);
			}
					
			rte.put(symbol, instrument);
		}
		return rte;
	}
	
	

	public static void fillInstrumentLiquidityWithRate(
			Instrument instrument, 
			double spreadRange,
			String defaultRateStr,
			String[] structure) {
		int level = 1;
		int precision = instrument.getPrecision();
		int pointValue = instrument.getPointValue();
		NumberVo defaultRate = NumberVo.getInstance(defaultRateStr);
		for (String liquidityStr : structure) {
			long liquidity = Long.parseLong(liquidityStr);
			NumberVo multiplier = (new NumberVo(liquidity, 0)).divide(100000);
			NumberVo bid = defaultRate.minus((new NumberVo(Double.toString(spreadRange * Math.pow(10, -1 * pointValue)), precision)).multiply(multiplier).divide(10));
			NumberVo ask = defaultRate.plus((new NumberVo(Double.toString(spreadRange * Math.pow(10, -1 * pointValue)), precision)).multiply(multiplier).divide(10));

			instrument.addBidTick(new Tick(Double.valueOf(bid.getValue()), liquidity, level));
			instrument.addAskTick(new Tick(Double.valueOf(ask.getValue()), liquidity, level));
			level ++;
		}
	}
	
}
