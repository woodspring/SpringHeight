package com.tts.mas.test.fwd.dummy;

import java.util.Arrays;
import java.util.List;

import com.tts.service.biz.instrument.util.ISymbolIdMapper;

public class DummySymbolMapper implements ISymbolIdMapper {

	@Override
	public List<String> getSymbols() {
		return Arrays.asList(new String[] { "USDTRY", "EURUSD", "USDJPY"});
	}

	@Override
	public int map(String arg0) {
		if ( "USDTRY".equals(arg0) ) {
			return 0;
		} else 		if ( "EURUSD".equals(arg0) ) {
			return 1;
		}
		return 2; 
		
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
