package com.tts.plugin.adapter.support.vo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * holding the market data set for all instruments
 *
 */
public class MarketDatasetVo {

	private final Map<String, Map<String, IndividualInfoVo>> marketDatasetMap;
	private volatile Set<String> marketDataType;
	
	/**
	 * constructor for MarketDatasetVo
	 * 
	 * @param marketDatasetMap
	 */
	public MarketDatasetVo() {
		this.marketDatasetMap = new ConcurrentHashMap<String,  Map<String, IndividualInfoVo>>();
	}
		
	/**
	 * @return
	 */
	public Set<String> getDestinationTypes() {
		return marketDataType;
	}
	
	/**
	 * @param type
	 * @return
	 */
	public Map<String, IndividualInfoVo> getMarketStructuresByType(String type) {
		return marketDatasetMap.get(type);
	}
	
	/**
	 * @param type
	 * @param marketDatasest
	 */
	public void setMarketStructuresByType(String type, Map<String, IndividualInfoVo> marketDatasest) {
		marketDatasetMap.put(type, marketDatasest);
	}
	
	
	/**
	 * @param type
	 * @return
	 */
	public Set<String> getAvailableSymbolsByType(String type) {
		Set<String> retVal = null;
		
		Map<String, IndividualInfoVo> marketDataset = marketDatasetMap.get(type);
		if (marketDataset != null) {
			retVal = marketDataset.keySet();
		} else {
			retVal = Collections.emptySet();
		}
		
		return retVal;
	}
	
	/**
	 * @param type
	 * @return
	 */
	public String[] getAvailableSymbolsToArrayByType(String type) {
		return getAvailableSymbolsByType(type).toArray(new String[0]);
	}

	/**
	 * @param type
	 * @param symbol
	 * @return
	 */
	public IndividualInfoVo getMarketStructureByTypeAndSymbol(String type, String symbol) {
		IndividualInfoVo retVal = null;
		
		Map<String, IndividualInfoVo> marketDataset = marketDatasetMap.get(type);
		if (marketDataset != null) {
			retVal = marketDataset.get(symbol);
		}
		
		return retVal;
	}

	public void doImmuatably() {
		this.marketDataType = Collections.unmodifiableSet( marketDatasetMap.keySet());
		
	}
}
