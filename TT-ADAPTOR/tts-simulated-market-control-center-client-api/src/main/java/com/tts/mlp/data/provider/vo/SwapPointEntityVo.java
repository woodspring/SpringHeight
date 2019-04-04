package com.tts.mlp.data.provider.vo;

public class SwapPointEntityVo {

	private String symbol;
	private String tenorNm;
	private double bidSwapPoint;
	private double askSwapPoint;
	private double bidSpotRefRate;
	private double askSpotRefRate;
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getTenorNm() {
		return tenorNm;
	}
	public void setTenorNm(String tenorNm) {
		this.tenorNm = tenorNm;
	}
	public double getBidSwapPoint() {
		return bidSwapPoint;
	}
	public void setBidSwapPoint(double bidSwapPoint) {
		this.bidSwapPoint = bidSwapPoint;
	}
	public double getAskSwapPoint() {
		return askSwapPoint;
	}
	public void setAskSwapPoint(double askSwapPoint) {
		this.askSwapPoint = askSwapPoint;
	}
	public double getBidSpotRefRate() {
		return bidSpotRefRate;
	}
	public void setBidSpotRefRate(double bidSpotRefRate) {
		this.bidSpotRefRate = bidSpotRefRate;
	}
	public double getAskSpotRefRate() {
		return askSpotRefRate;
	}
	public void setAskSpotRefRate(double askSpotRefRate) {
		this.askSpotRefRate = askSpotRefRate;
	}
	
	
}
