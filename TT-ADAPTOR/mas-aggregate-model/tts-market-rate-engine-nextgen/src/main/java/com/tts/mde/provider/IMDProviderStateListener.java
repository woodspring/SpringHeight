package com.tts.mde.provider;

public interface IMDProviderStateListener {
	static final String CONSTANT_SESSIONTYPE_RESTING_ORDER = "RESTING_ORDER";
	static final String CONSTANT_SESSIONTYPE_IMMEDIATE_ORDER = "IMMEDIATE_ORDER";
	static final String CONSTANT_SESSIONTYPE_RFS = "RFS";
	static final String CONSTANT_SESSIONTYPE_ESP = "ESP";
	
	public void doWhenOnline(String adapterNm, String sourceNm);
	
	public void doWhenOffline(String adapterNm, String sourceNm);

}
