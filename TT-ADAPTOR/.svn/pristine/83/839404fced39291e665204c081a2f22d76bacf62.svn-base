package com.tts.mde.vo;

import java.util.List;

import com.tts.mde.support.config.Adapter;
import com.tts.mde.support.config.Adapter.SourceConfig;
import com.tts.mde.support.config.MDSubscription;

public class SubscriptionWithSourceVo {
	
	private final List<SourceConfig> sourceConfigs;
	private final Adapter adapter;
	private final MDSubscription mdSubscription;
	private final String symbol;
	
	public SubscriptionWithSourceVo(String symbol, MDSubscription subscription, List<SourceConfig> sourceConfigs, Adapter adapter) {
		super();
		this.sourceConfigs = sourceConfigs;
		this.adapter = adapter;
		this.mdSubscription = subscription;
		this.symbol = symbol;
	}
	
	public List<SourceConfig> getSourceConfigs() {
		return sourceConfigs;
	}
	
	public Adapter getAdapter() {
		return adapter;
	}

	public MDSubscription getMdSubscription() {
		return mdSubscription;
	}

	public String getSymbol() {
		return symbol;
	}

}
