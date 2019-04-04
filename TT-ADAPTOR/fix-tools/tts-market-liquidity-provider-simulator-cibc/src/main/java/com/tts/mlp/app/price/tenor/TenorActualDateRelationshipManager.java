package com.tts.mlp.app.price.tenor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TenorActualDateRelationshipManager {

	private final Map<String, Map<String, String>> symbolTenorActualDateMap;
	
	public TenorActualDateRelationshipManager(String[] symbols) {
		Map<String, Map<String, String>> m = new HashMap<String, Map<String, String>>();
		for (String symbol: symbols) {
			m.put(symbol,  new ConcurrentHashMap<String, String>());
		}
		this.symbolTenorActualDateMap = Collections.unmodifiableMap(m);
	}
	
	public void registerSymbolTenorDate(String symbol, String tenor, String actualDate) {
		Map<String, String> tenorActualDateMap = null;
		if ( symbol != null)  {
			tenorActualDateMap = this.symbolTenorActualDateMap.get(symbol);
			if ( tenorActualDateMap  != null 
					&& tenor != null 
					&& actualDate != null) {
				tenorActualDateMap.put(tenor, actualDate);
			}
		}
	}
	
	public String findTenorByDate(String symbol, String actualDate) {
		if ( symbol != null && actualDate != null ) {
			Map<String, String> tenorActualDateMap = this.symbolTenorActualDateMap.get(symbol);
			if ( tenorActualDateMap != null ) {
				for (Entry<String, String> e: tenorActualDateMap.entrySet()) {
					if ( actualDate.equals(e.getValue())) {
						return e.getKey();
					}
				}
			}
		}
		return null;
	}
}
