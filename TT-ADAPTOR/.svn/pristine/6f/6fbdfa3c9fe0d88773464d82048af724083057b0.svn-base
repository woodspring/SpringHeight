package com.tts.mlp.data.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.tts.mlp.data.provider.vo.InstrumentDefinitionVo;

public class InstrumentDefinitionProvider {
	protected static final String LIQUIDITY = ".liquidity";
	protected static final String PRECISION = ".precision";
	protected static final String POINTVALUE = ".pointValue";
	protected static final String INCREMENT = ".increment";
	
	private final Map<String, InstrumentDefinitionVo> instrumentDefMap;
	
	public InstrumentDefinitionVo getInstrumentDefinition(String symbol) {
		return instrumentDefMap.get(symbol);
	}
	
	public InstrumentDefinitionProvider() {
		Properties p = null;
		try {
			p = loadInstrumentProperties("instrument.cfg");
		} catch (IOException e) {
			e.printStackTrace();
		}
		HashMap<String, InstrumentDefinitionVo> instrumentDefMap = new HashMap<String, InstrumentDefinitionVo>();
		if ( p != null ) {
			
			Set<String> symbolSet = new HashSet<String>();
			Set<Object> instrumentPropertyKeys = p.keySet();
			for ( Object _instrumentPropertyKey :  instrumentPropertyKeys) {
				String instrumentPropertyKey = ( String) _instrumentPropertyKey;
				int idx = instrumentPropertyKey.indexOf(".");
				symbolSet.add(instrumentPropertyKey.substring(0, idx));
			}
			symbolSet =  Collections.unmodifiableSet(symbolSet);
			int count = 1;
			for ( String symbol : symbolSet) {
				int pointValue = getInstrumentPointValue(symbol, p);
				int spotPrecision = getInstrumentPrecision(symbol, p);
				long[] lqyStructure = getInstrumentLiquidities(symbol, p);
				
				InstrumentDefinitionVo i = new InstrumentDefinitionVo(count++, symbol, spotPrecision, pointValue);
				i.setLqyStructure(lqyStructure);
				instrumentDefMap.put(symbol, i);
			}
		}
		this.instrumentDefMap = Collections.unmodifiableMap(instrumentDefMap);
	}
	
	
	protected Properties loadInstrumentProperties(String instrumentConfigUrl) throws IOException {
		Properties p = new Properties();
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(instrumentConfigUrl)) {
			p.load(is);
		} finally {
			
		}
			
		return p;
	}


	
	protected int getInstrumentPointValue(String symbol,
			Properties instrumentProperties) {
		try {
			return Integer.parseInt((String) instrumentProperties.get(symbol + POINTVALUE));
		} catch ( Exception e) {
			return -1;
		}
	}

	protected long[] getInstrumentLiquidities(String symbol, Properties instrumentProperties) {
		try {
			String[] settings =  ((String) instrumentProperties.get(symbol + LIQUIDITY)).split(",");
			long[] ls = new long[settings.length];
			for ( int i = 0; i < settings.length; i++) {
				ls[i] = Long.parseLong(settings[i]);
			}
			return ls;
		} catch ( Exception e) {
			return null;
		}
	}

	protected int getInstrumentPrecision(String symbol, Properties instrumentProperties) {
		try {
			return Integer.parseInt((String) instrumentProperties.get(symbol + PRECISION));
		} catch ( Exception e) {
			return -1;
		}
	}
}
