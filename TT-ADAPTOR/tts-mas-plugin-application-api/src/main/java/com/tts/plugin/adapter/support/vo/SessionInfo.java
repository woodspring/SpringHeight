package com.tts.plugin.adapter.support.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session setting for the Market adapter server, not the session setting of 
 * quickfix engine
 * 
 *
 */
public class SessionInfo {

	private static final Logger log = LoggerFactory.getLogger(SessionInfo.class);

	private boolean active;
	private String[] activePlugins;
	private volatile String tradingSessionName;
	private long timeoutInterval;
	private volatile long tradingSessionId;

	private final MarketDatasetVo marketDataset = new MarketDatasetVo();
	private final MarketDatasetVo previousMarketDataset = new MarketDatasetVo();
	
	public void reconfigure(String destinationType, String[] symbols) {
		log.debug(String.format("Reconfigure destination type: %s ...", destinationType));

		Map<String, IndividualInfoVo> previousMktStructures = marketDataset.getMarketStructuresByType(destinationType);
		if (previousMktStructures != null) {
			previousMarketDataset.setMarketStructuresByType(destinationType, previousMktStructures);
		}
		
		Map<String, IndividualInfoVo> mktStructures = new HashMap<String, IndividualInfoVo>();
		for (String symbol : symbols) {
			IndividualInfoVo individualInfo = new IndividualInfoVo(symbol);
			mktStructures.put(symbol, individualInfo);
		}
		
		marketDataset.setMarketStructuresByType(destinationType, Collections.unmodifiableMap(mktStructures));
		
		log.debug(String.format("Reconfigure destination type: %s ... DONE", destinationType));
	}
	
	public String[] getMergedSymbols() {
		return doBuildMergedSymbols();
	}
		

	
	public String[] getActivePlugins() {
		return activePlugins;
	}

	public void setActivePlugins(String[] activePlugins) {
		this.activePlugins = activePlugins;
	}

	public long getTradingSessionId() {
		return tradingSessionId;
	}

	public void setTradingSessionId(long tradingSessionId) {
		this.tradingSessionId = tradingSessionId;
	}

	public String getTradingSessionName() {
		return tradingSessionName;
	}

	public void setTradingSessionName(String tradingSessionName) {
		this.tradingSessionName = tradingSessionName;
	}

	public long getTimeoutInterval() {
		return timeoutInterval;
	}

	public void setTimeoutInterval(long timeoutInterval) {
		this.timeoutInterval = timeoutInterval;
	}

	public MarketDatasetVo getMarketDataset() {
		return marketDataset;
	}

	public MarketDatasetVo getPreviousMarketDataset() {
		return previousMarketDataset;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
		if ( active == true ) {
			marketDataset.doImmuatably();
		}
	}
	
	private String[] doBuildMergedSymbols() {
		Set<String> retVal = new HashSet<String>();
		
		Set<String> destinationTypes = marketDataset.getDestinationTypes();
		for (String destinationType : destinationTypes) {
			retVal.addAll(marketDataset.getAvailableSymbolsByType(destinationType));
		}
		
		List<String> mergedList = new ArrayList<String>(retVal);
		return mergedList.toArray(new String[0]);
	}
}

