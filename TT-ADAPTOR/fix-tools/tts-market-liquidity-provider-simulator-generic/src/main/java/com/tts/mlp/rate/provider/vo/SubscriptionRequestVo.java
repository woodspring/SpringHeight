package com.tts.mlp.rate.provider.vo;

public class SubscriptionRequestVo {
	public static enum QuoteSide {
		BUY,
		SELL,
		BOTH
	}
	
	public static enum StreamType {
		ESP,
		RFS
	}
	
	private String clientReqId;
	
	private String symbol;
	
	private String notionalCurrency;
		
	private String tenor;
	
	private String tenorFar;
	
	private StreamType streamType;
	
	private QuoteSide quoteSide;

	private long size;
	
	private long sizeFar;

	private long expiryTime;
	
	private String settleDate;
	
	private String settleDateFar;
	
	private String onBehaveOf;
	
	private String securityType;
	
	public String getClientReqId() {
		return clientReqId;
	}

	public void setClientReqId(String clientReqId) {
		this.clientReqId = clientReqId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
	

	public String getNotionalCurrency() {
		return notionalCurrency;
	}

	public void setNotionalCurrency(String notionalCurrency) {
		this.notionalCurrency = notionalCurrency;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public QuoteSide getQuoteSide() {
		return quoteSide;
	}

	public void setQuoteSide(QuoteSide quoteSide) {
		this.quoteSide = quoteSide;
	}

	public String getTenor() {
		return tenor;
	}

	public void setTenor(String tenor) {
		this.tenor = tenor;
	}

	public long getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(long expiryTime) {
		this.expiryTime = expiryTime;
	}

	public StreamType getStreamType() {
		return streamType;
	}

	public void setStreamType(StreamType streamType) {
		this.streamType = streamType;
	}

	public String getSettleDate() {
		return settleDate;
	}

	public void setSettleDate(String settleDate) {
		this.settleDate = settleDate;
	}

	public String getOnBehaveOf() {
		return onBehaveOf;
	}

	public void setOnBehaveOf(String onBehaveOf) {
		this.onBehaveOf = onBehaveOf;
	}

	public String getTenorFar() {
		return tenorFar;
	}

	public void setTenorFar(String tenorFar) {
		this.tenorFar = tenorFar;
	}

	public long getSizeFar() {
		return sizeFar;
	}

	public void setSizeFar(long sizeFar) {
		this.sizeFar = sizeFar;
	}

	public String getSettleDateFar() {
		return settleDateFar;
	}

	public void setSettleDateFar(String settleDateFar) {
		this.settleDateFar = settleDateFar;
	}

	public String getSecurityType() {
		return securityType;
	}

	public void setSecurityType(String securityType) {
		this.securityType = securityType;
	}

	
	
	
	
}
