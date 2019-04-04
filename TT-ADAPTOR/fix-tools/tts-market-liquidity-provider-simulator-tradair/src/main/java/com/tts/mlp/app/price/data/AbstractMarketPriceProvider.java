package com.tts.mlp.app.price.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractMarketPriceProvider {
	private final static Logger logger = LoggerFactory.getLogger(AbstractMarketPriceProvider.class);
	protected static final String SEPARATOR = ",";

	protected static final String LIQUIDITY = ".liquidity";
	protected static final String PRECISION = ".precision";
	protected static final String POINTVALUE = ".pointValue";
	protected static final String INCREMENT = ".increment";

	protected static final String DEFAULT_RATE = "1.35851";
	protected static final double DEFAULT_SPREAD_RANGE = 2;
	
	protected Map<String, BigDecimal> loadData(String dataFile) {
		Map<String, BigDecimal> retVal = new HashMap<String, BigDecimal>();
		InputStream dataFileIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(dataFile);
		BufferedReader reader = null;
		try  {
			reader = new BufferedReader(new InputStreamReader(dataFileIn));
			String line = reader.readLine();
			while (line != null) {
				String[] values = line.split(SEPARATOR);
				if (values.length > 1) {
					retVal.put(values[0].trim().toUpperCase(), new BigDecimal(values[1].trim()));
				}
				line = reader.readLine();
			}
		} catch (Exception e) {
			logger.error("Error reading " + dataFile);
		} finally {
			try {
				if ( reader != null ) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return retVal;
	}

	protected Set<String> buildSupportedSymbolSet(Properties instrumentProperties) {
		Set<String> symbolSet = new HashSet<String>();
		Set<Object> instrumentPropertyKeys = instrumentProperties.keySet();
		for ( Object _instrumentPropertyKey :  instrumentPropertyKeys) {
			String instrumentPropertyKey = ( String) _instrumentPropertyKey;
			int idx = instrumentPropertyKey.indexOf(".");
			symbolSet.add(instrumentPropertyKey.substring(0, idx));
		}
		return Collections.unmodifiableSet(symbolSet);
	}
	

	protected Properties loadInstrumentProperties(String instrumentConfigUrl) throws IOException {
		Properties p = new Properties();
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(instrumentConfigUrl)) {
			p.load(is);
		} finally {
			
		}
			
		return p;
	}
	

	protected String getInstrumentIncrement(String symbol,
			Properties instrumentProperties) {
		try {
			return (String) instrumentProperties.get(symbol + INCREMENT);
		} catch ( Exception e) {
			return null;
		}
	}

	protected int getInstrumentPointValue(String symbol,
			Properties instrumentProperties) {
		try {
			return Integer.parseInt((String) instrumentProperties.get(symbol + POINTVALUE));
		} catch ( Exception e) {
			return -1;
		}
	}

	protected String[] getInstrumentLiquidities(String symbol, Properties instrumentProperties) {
		try {
			return ((String) instrumentProperties.get(symbol + LIQUIDITY)).split(",");
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
