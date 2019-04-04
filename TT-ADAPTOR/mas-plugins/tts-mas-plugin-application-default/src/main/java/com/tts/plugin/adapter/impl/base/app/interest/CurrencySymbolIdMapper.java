package com.tts.plugin.adapter.impl.base.app.interest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tts.service.biz.instrument.util.ISymbolIdMapper;

public class CurrencySymbolIdMapper implements ISymbolIdMapper {
	
	private final List<String> symbols;
	private final Map<String, Integer> map;
	
	public CurrencySymbolIdMapper(String[] symbols){
		HashMap<String, Integer> _map = new HashMap<String, Integer>(symbols.length);
		for (int i = 0; i < symbols.length; i++ ) {
			_map.put(symbols[i], new Integer(i));
		}
		this.map = Collections.unmodifiableMap(_map);
		this.symbols = Arrays.asList(symbols);
	}

	@Override
	public List<String> getSymbols() {
		return this.symbols;
	}

	@Override
	public int map(String symbol) {
		return map.get(symbol);
	}

	@Override
	public int getTotalAvailableInstruments() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String mapSymbol(int symbolId) {
		// TODO Auto-generated method stub
		return null;
	}

}
