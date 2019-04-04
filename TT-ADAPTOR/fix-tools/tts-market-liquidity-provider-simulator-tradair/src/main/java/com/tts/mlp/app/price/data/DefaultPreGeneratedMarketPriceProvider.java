package com.tts.mlp.app.price.data;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.Tick;
import com.tts.vo.NumberVo;

public class DefaultPreGeneratedMarketPriceProvider extends AbstractMarketPriceProvider implements IPreGeneratedPriceProvider {


	
	private final double spreadRange = DEFAULT_SPREAD_RANGE;

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
		Map<String, BigDecimal> seedMap = loadData(defaultPriceUrl);
		for ( String symbol: supportedSymbols) {
			int priceIdx = 0;
			double spreadRange = this.spreadRange;
			if ( "EURUSD".equals(symbol)) {
				spreadRange = 1.0;
			}
			int precision = getInstrumentPrecision(symbol, instrumentProperties);
			int pointValue = getInstrumentPointValue(symbol, instrumentProperties);
			String increment = getInstrumentIncrement(symbol, instrumentProperties);

			String default_increment = new NumberVo(Double.toString(1.111111 * Math.pow(10, -1 * pointValue)), precision).toPercisionString();

			Instrument[] prices = new Instrument[PRICE_VARIATION];
			Instrument instrument = new Instrument(symbol);
			instrument.setPrecision(precision);
			instrument.setIncrement(increment == null? default_increment: increment);
			
			BigDecimal rate = null;
			if (seedMap.containsKey(symbol)) {
				rate = seedMap.get(symbol);
			} else {
				rate = new BigDecimal(DEFAULT_RATE);
			}

			NumberVo value = new NumberVo(rate.toString(), precision);
			int level = 1;
			String[] structure = getInstrumentLiquidities(symbol, instrumentProperties);
			if (structure != null) {
				for (String liquidityStr : structure) {
					long liquidity = Long.parseLong(liquidityStr);
					NumberVo multiplier = (new NumberVo(liquidity, 0)).divide(1000000);
					NumberVo bid = value.minus((new NumberVo(Double.toString(spreadRange * Math.pow(10, -1 * pointValue)), precision)).multiply(multiplier));
					NumberVo ask = value.plus((new NumberVo(Double.toString(spreadRange * Math.pow(10, -1 * pointValue)), precision)).multiply(multiplier));
	
					instrument.addBidTick(new Tick(Double.valueOf(bid.getValue()), liquidity, level));
					instrument.addAskTick(new Tick(Double.valueOf(ask.getValue()), liquidity, level));
					level ++;
				}
			}
			
			prices[priceIdx] = instrument.deepClone();
			
			for ( priceIdx = 1; priceIdx < PRICE_VARIATION; priceIdx++ ) {
				instrument.randomWalk();
				prices[priceIdx] = instrument.deepClone();
			}
			
			rte.put(symbol, prices);
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

