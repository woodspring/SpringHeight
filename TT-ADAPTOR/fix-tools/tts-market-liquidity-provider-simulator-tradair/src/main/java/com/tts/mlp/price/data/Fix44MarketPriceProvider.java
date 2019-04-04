package com.tts.mlp.price.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.tts.mlp.app.price.data.IMarketPriceProvider;

import quickfix.fix44.MarketDataSnapshotFullRefresh;


public class Fix44MarketPriceProvider implements IMarketPriceProvider<quickfix.fix44.MarketDataSnapshotFullRefresh> {
	
	private final Map<String, quickfix.fix44.MarketDataSnapshotFullRefresh[]> data;
	private final Map<String, Integer> currentIdx = new ConcurrentHashMap<String, Integer>();
	
	
	public Fix44MarketPriceProvider(Map<String, MarketDataSnapshotFullRefresh[]> data) {
		super();
		this.data = data;
		
		for ( String sym : data.keySet()) {
			currentIdx.put(sym, 0);
		}
	}

	@Override
	public MarketDataSnapshotFullRefresh getCurrentPrice(String symbol) {
		if ( symbol.indexOf("/") > 0) { symbol= symbol.replace("/", ""); }
		int i = currentIdx.get(symbol);
		quickfix.fix44.MarketDataSnapshotFullRefresh[] d = data.get(symbol);
		
		return d[i];
	}

	@Override
	public MarketDataSnapshotFullRefresh getNextMarketPrice(String symbol) {
		
		if ( symbol.indexOf("/") > 0) { symbol= symbol.replace("/", ""); }

		int i = currentIdx.get(symbol);
		quickfix.fix44.MarketDataSnapshotFullRefresh[] d = data.get(symbol);
		i++;
		
		if ( i >= d.length) {
			i = i % d.length;
		}
		currentIdx.put(symbol, i);
		return d[i];
	}

	public static IMarketPriceProvider<quickfix.fix44.MarketDataSnapshotFullRefresh> from(
			HashMap<String, LinkedList<quickfix.fix44.MarketDataSnapshotFullRefresh>> repo) {
		HashMap<String, quickfix.fix44.MarketDataSnapshotFullRefresh[]> newRepo = new HashMap<String, quickfix.fix44.MarketDataSnapshotFullRefresh[]>();
		for (Entry<String, LinkedList<quickfix.fix44.MarketDataSnapshotFullRefresh>> e : repo.entrySet()) {
			quickfix.fix44.MarketDataSnapshotFullRefresh[] a = e.getValue().toArray(new quickfix.fix44.MarketDataSnapshotFullRefresh[0]);
			String symbol = e.getKey();
			if ( symbol.indexOf("/") > 0) { symbol= symbol.replace("/", ""); }

			newRepo.put(symbol, a);
		}
		return new Fix44MarketPriceProvider(Collections.unmodifiableMap(newRepo));
	}

}
