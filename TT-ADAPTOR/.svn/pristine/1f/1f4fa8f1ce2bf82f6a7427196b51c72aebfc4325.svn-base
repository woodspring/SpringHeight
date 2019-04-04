package com.tts.mde.provider;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.tts.mde.support.config.MarketDataSetConfig;

public class SessionInfoProvider {
	
	public MarketDataSetConfig getOrReloadMarketDataSetConfig() {
		MarketDataSetConfig cfg = null;
		try {
			cfg = read("marketDataSet.xml");
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return cfg;
	}
	
	private static MarketDataSetConfig read(String file) throws JAXBException {
		JAXBContext contextObj = JAXBContext.newInstance(MarketDataSetConfig.class);  
		Unmarshaller u = contextObj.createUnmarshaller();
		MarketDataSetConfig cfg = (MarketDataSetConfig) u.unmarshal(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));
		return cfg;
	}
}
