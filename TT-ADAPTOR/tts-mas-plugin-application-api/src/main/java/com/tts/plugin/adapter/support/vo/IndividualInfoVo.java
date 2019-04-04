package com.tts.plugin.adapter.support.vo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.util.flag.IndicativeFlag.IndicativeSubStatus;

/**
 * holding a market data set for a single instrument
 * 
 *
 */
public class IndividualInfoVo {

	private final AtomicBoolean tradeDateChanged = new AtomicBoolean(false);

	private String symbol;

	private LocalDate tradeDate;

	private String tradeDateString;

	private long[] liquidities = null;

	private String[] tenors = null;

	private long refreshInterval;

	private long lastRefresh;
	
	private long lastRequest;

	private String topic;

	private volatile long indicativeFlag = IndicativeFlag.TRADABLE;

	private long indicativeSubStatus = IndicativeFlag.TRADABLE;
	
	private volatile Map<String, String> valueDateMap;
	
	private boolean isDebug = false;
	
	public IndividualInfoVo(String symbol) {
		this.symbol = symbol;
	}
	
	private IndividualInfoVo(IndividualInfoVo original) {
		this.symbol = original.symbol;
		if (original.getTradeDate() != null) {
			this.tradeDate = LocalDate.of(
					original.getTradeDate().getYear(), 
					original.getTradeDate().getMonth(), 
					original.getTradeDate().getDayOfMonth());
		}
		
		if (original.getLiquidities() != null) {
			this.liquidities = new long[original.getLiquidities().length];
			System.arraycopy(original.getLiquidities(), 0, this.liquidities, 0, original.getLiquidities().length);
		}
		
		if (original.getTenors() != null) {
			this.tenors = new String[original.getTenors().length];
			System.arraycopy(original.getTenors(), 0, this.tenors, 0, original.getTenors().length);
		}
		
		this.tradeDateString = original.tradeDateString;
		this.refreshInterval = original.refreshInterval;
		this.lastRefresh = original.lastRefresh;
		this.topic = original.topic;
		this.indicativeFlag = original.indicativeFlag;
		this.indicativeSubStatus = original.indicativeSubStatus;
		this.valueDateMap = original.valueDateMap;
		this.isDebug = original.isDebug;
	}
	
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getTradeDateString() {
		return tradeDateString;
	}

	public LocalDate getTradeDate() {
		if (this.tradeDateChanged.compareAndSet(true, false)) {
			synchronized (this) {
				this.tradeDate = ChronologyUtil
						.getLocalDateFromString(getTradeDateString());
			}
		}
		return tradeDate;
	}

	public synchronized void setTradeDateString(String tradeDateString) {
		this.tradeDateString = tradeDateString;
		tradeDateChanged.set(true);
	}

	public long[] getLiquidities() {
		return liquidities;
	}

	public void setLiquidities(long[] liquidities) {
		this.liquidities = liquidities;
	}

	public String[] getTenors() {
		return tenors;
	}

	public void setTenors(String[] tenors) {
		this.tenors = tenors;
	}

	public long getRefreshInterval() {
		return refreshInterval;
	}

	public void setRefreshInterval(long refreshInterval) {
		this.refreshInterval = refreshInterval;
	}

	public long getLastRefresh() {
		return lastRefresh;
	}

	public void setLastRefresh(long lastRefresh) {
		this.lastRefresh = lastRefresh;
	}
	
	public long getLastRequest() {
		return lastRequest;
	}

	public void setLastRequest(long lastRequest) {
		this.lastRequest = lastRequest;
	}
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public long getIndicativeFlag() {
		return indicativeFlag;
	}

	public long getIndicativeSubStatus() {
		return indicativeSubStatus;
	}

	public void addIndicativeReason(IndicativeReason reason) {
		indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, reason);
	}

	public void removeIndicativeReason(IndicativeReason reason) {
		indicativeFlag = IndicativeFlag
				.removeIndicative(indicativeFlag, reason);
	}

	public void addIndicativeSubStatus(IndicativeSubStatus subStatus) {
		indicativeSubStatus = IndicativeFlag.setIndicativeSubStatus(
				indicativeSubStatus, subStatus);
	}

	public void removeIndicativeSubStatus(IndicativeSubStatus subStatus) {
		indicativeSubStatus = IndicativeFlag.removeIndicativeSubStatus(
				indicativeSubStatus, subStatus);
	}

	public boolean isTradeDateChanged() {
		return tradeDateChanged.get();
	}
	
	public IndividualInfoVo deepClone() {
		return new IndividualInfoVo(this);
	}

	public Map<String, String> getValueDateMap() {
		return valueDateMap;
	}

	public void setValueDateMap(Map<String, String> valueDateMap) {
		this.valueDateMap = Collections.unmodifiableMap(valueDateMap);
	}

	public boolean isDebug() {
		return isDebug;
	}

	public void setDebug(boolean isDebug) {
		this.isDebug = isDebug;
	}

	
}
