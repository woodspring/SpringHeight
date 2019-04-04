package com.tts.mde.support.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tts.mde.vo.IMDProvider;
import com.tts.mde.vo.ISessionInfo;
import com.tts.mde.vo.LiquidityProviderVo;
import com.tts.message.config.AggConfigStruct.AggInstrumentConfigResponse;
import com.tts.message.constant.Constants.MarketMode;
import com.tts.util.flag.IndicativeFlag;

public class SessionInfoVo implements ISessionInfo {

	private volatile long tradingSessionId = -1L;
	private volatile long globalIndicativeFlag = IndicativeFlag.TRADABLE;

	private volatile AggInstrumentConfigResponse.Builder aggInstrumentConfigResponse = null;
	private List<LiquidityProviderVo> internalTradingRepParties;
	private Map<String, IMDProvider> mdProviderMap;
	private List<IMDProvider> mdProviderList;
	
	@Override
	public long getTradingSessionId() {
		return tradingSessionId;
	}

	public void setTradingSessionId(long tradingSessionId) {
		this.tradingSessionId = tradingSessionId;
	}

	public AggInstrumentConfigResponse.Builder getAggInstrumentConfigResponse() {
		return aggInstrumentConfigResponse;
	}

	public void setAggInstrumentConfigResponse(AggInstrumentConfigResponse.Builder aggInstrumentConfigResponse) {
		this.aggInstrumentConfigResponse = aggInstrumentConfigResponse;
	}

	public void setGlobalIndicativeFlag(long globalIndicativeFlag) {
		this.globalIndicativeFlag = globalIndicativeFlag;
	}

	@Override
	public long getGlobalIndicativeFlag() {
		return globalIndicativeFlag;
	}

	@Override
	public String getMarketMode() {
		return MarketMode.NORMAL;
	}

	public void setMDproviderExternal(List<IMDProvider> mdProviders) {
		HashMap<String, IMDProvider> mdProviderMap = new HashMap<>();
		for (IMDProvider p :  mdProviders) {
			mdProviderMap.put(p.getSourceNm(), p);
		}
		this.mdProviderMap = Collections.unmodifiableMap(mdProviderMap);
		this.mdProviderList = mdProviders;
	}

	public IMDProvider getMDproviderBySourceNm(String sourceNm) {
		return this.mdProviderMap.get(sourceNm);
	}

	public List<IMDProvider> getMDprovidersExternal() {
		return this.mdProviderList;
	}

	public void setInternalTradingRepParties(List<LiquidityProviderVo> internalTradingRepParties) {
		this.internalTradingRepParties =  internalTradingRepParties;
		
	}

	public List<LiquidityProviderVo> getInternalTradingParties() {
		return this.internalTradingRepParties;
	}
	
	
}
