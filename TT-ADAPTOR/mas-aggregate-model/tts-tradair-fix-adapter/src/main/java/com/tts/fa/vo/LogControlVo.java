package com.tts.fa.vo;

public class LogControlVo {

	private volatile boolean logMarketData = true; //Boolean.parseBoolean(System.getenv("MARKET_DATA_DYNAMIC_CONTROL_DEFAULT"));

	public LogControlVo(boolean defaultLogMarketData) {
		super();
		this.logMarketData = defaultLogMarketData;
	}

	public boolean isLogMarketData() {
		return logMarketData;
	}

	public void setIsLogMarketData(boolean logMarketData) {
		this.logMarketData = logMarketData;
	}
	
	
}
