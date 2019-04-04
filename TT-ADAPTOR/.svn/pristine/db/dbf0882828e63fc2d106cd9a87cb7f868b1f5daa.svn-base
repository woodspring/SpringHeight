package com.tts.mlp.data.provider;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.tts.mlp.data.provider.vo.InstrumentRateVo;

public class MarketRateBookProvider {

	private long dataRefreshTimestamp = -1;

	private final List<String> symbols;
	private final ConcurrentHashMap<String, InstrumentRateVo> cache;
	private final  IMarketRawDataProvider marketRawDataProvider;
	private final  InstrumentDefinitionProvider instrumentDefinitionProvider;
	
	public MarketRateBookProvider(List<String> symbols, IMarketRawDataProvider marketRawDataProvider, InstrumentDefinitionProvider instrumentDefinitionProvider) {
		this.symbols = symbols;
		this.marketRawDataProvider = marketRawDataProvider;
		this.instrumentDefinitionProvider = instrumentDefinitionProvider;
		this.cache = new ConcurrentHashMap<>(symbols.size());
	}
	
	public InstrumentRateVo getInstrumentRateBook(String symbol) {
		refresh();
		return cache.get(symbol);
	}
	public void refresh() {
		if ( marketRawDataProvider.getDataRefreshTimestamp() != this.dataRefreshTimestamp  ) {
			for ( String symbol : symbols) {
				SimulatedInstrumentRateVo rateBook = new SimulatedInstrumentRateVo(instrumentDefinitionProvider.getInstrumentDefinition(symbol));
				rateBook.randomWalk(marketRawDataProvider.getSpotData(symbol));				
				this.cache.put(symbol, rateBook);
			}
			this.dataRefreshTimestamp = marketRawDataProvider.getDataRefreshTimestamp();
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		MarketDataProvider p = new MarketDataProvider();
		InstrumentDefinitionProvider dp = new InstrumentDefinitionProvider();
		
		MarketRateBookProvider bp = new MarketRateBookProvider(Arrays.asList(new String[]{ "USDCAD" }), p, dp);
		InstrumentRateVo r = bp.getInstrumentRateBook("USDCAD");
		
		Gson gson = new Gson();
		System.out.println(gson.toJson(r));
	}
}
