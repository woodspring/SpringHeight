package com.tts.reuters.adapter.utils.ric;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedRicCodeProvider implements IRicCodeProvider {

	private static final String DEFAULT_RIC_CODE_MAP_FILE = "ricCode.csv";
	private static final Logger logger = LoggerFactory.getLogger(FileBasedRicCodeProvider.class);
	private static final String CSV_COMMENT_PREFIX = "#";
	private static final String CCY_USD = "USD";
	
	private volatile Map<String, String> ricCodeMap = Collections.emptyMap();;

	private volatile File newFile = null;
	
	public FileBasedRicCodeProvider() {
		reload();
	}
	
	@Override
	public void reload() {
		URL orgFileUrl = Thread.currentThread().getContextClassLoader()
				.getResource("env-resources/rfa/" + DEFAULT_RIC_CODE_MAP_FILE);

		try {
			if ( orgFileUrl != null ) {
				newFile = new File(orgFileUrl.toURI());
				
				ricCodeMap = reloadRicCodeFile(newFile.getAbsolutePath());
			}
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}		
	}

	@Override
	public String getRicCode(String symbol, String tenor) {
		String ric = ricCodeMap.get(symbol + tenor);
		if (ric == null) {
			StringBuilder subscriptionId = new StringBuilder();
			String tempSymbol = symbol.toUpperCase().replaceFirst(CCY_USD, "").trim();
			subscriptionId.append(tempSymbol).append(tenor.trim()).append("=");
			String formattedRICSymbol = subscriptionId.toString();
			logger.debug("RIC Code for " + symbol + tenor + " not set. Using default " + formattedRICSymbol);
			ric = formattedRICSymbol;
		}
		return ric;
	}

	public Map<String, String> reloadRicCodeFile(String newFile) {

		HashMap<String, String> s = new HashMap<String, String>();
		logger.debug("reloadFromFile: (RicCode): " + newFile + " started");

		String func = "IRicCodeProvider.reloadRicCodeFile".intern();
		File f = new File(newFile);
		if ( newFile == null || !f.exists() ) {
			return Collections.emptyMap();
		}
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(newFile)))) {
			logger.debug(func, String.format("Open: %s", newFile));

			String line = null;

			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0 || line.startsWith(CSV_COMMENT_PREFIX)) {
					continue;
				} else {
					String[] data = line.split(",");
					if (data.length >= 2) {
						String symbolTenor = data[0];
						String ricCode = data[1];
						s.put(symbolTenor, ricCode);
					}
				}

			}

		} catch (IOException e) {
			logger.error("Error in reading ric code file", e);
		} catch (Exception e) {
			logger.error("Error in reading ric code file", e);
		}

		logger.debug("reloadFromFile: (RicCode):" + newFile + " completed.");
		return Collections.unmodifiableMap(s);
	}


}
