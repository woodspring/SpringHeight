package com.tts.mde.vo;

import com.tts.vo.CounterPartyVo;

public class LiquidityProviderVo extends MarketDataProviderVo implements IMDProvider, ITradingParty {
	private final boolean isInternalTradingParty;

	private CounterPartyVo spotProvider;
	private CounterPartyVo outrightProvider;
	private CounterPartyVo swapProvider;
	
	public LiquidityProviderVo(int internalAssignedProviderId, String adapterNm, String sourceNm,
			boolean isRFSenabled, boolean isESPenabled, boolean isInternalTradingParty) {
		super(internalAssignedProviderId, adapterNm, sourceNm, isRFSenabled, isESPenabled);
		this.isInternalTradingParty = isInternalTradingParty;
	}

	@Override
	public CounterPartyVo getFxSPOTCounterParty() {
		return spotProvider;
	}

	@Override
	public CounterPartyVo getFxOutrightCounterParty() {
		return outrightProvider;
	}

	@Override
	public CounterPartyVo getFxSWAPCounterParty() {
		return swapProvider;
	}

	public void setFxSPOTCounterParty(CounterPartyVo spotProvider) {
		this.spotProvider = spotProvider;
	}

	public void setFxOutrightCounterParty(CounterPartyVo outrightProvider) {
		this.outrightProvider = outrightProvider;
	}

	public void setFxSWAPCounterParty(CounterPartyVo swapProvider) {
		this.swapProvider = swapProvider;
	}

	public boolean isInternalTradingParty() {
		return isInternalTradingParty;
	}

	
}
