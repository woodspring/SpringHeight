package com.tts.mde.vo;

public class RawLiquidityVo {

	private boolean valid = false;
	private long size;
	private long receivedTime;
	private double rate;
	private String quoteId;
	private String liquidityProviderIdentifier;
	
	
	
	public RawLiquidityVo() {
		super();
	}

	private RawLiquidityVo(RawLiquidityVo org) {
		super();
		this.valid = org.valid;
		this.size = org.size;
		this.receivedTime = org.receivedTime;
		this.rate = org.rate;
		this.quoteId = org.quoteId;
		this.liquidityProviderIdentifier = org.liquidityProviderIdentifier;
	}

	public synchronized void update(long size, double rate, String quoteId, long receivedTime, String liquidityProviderIdentifier) {
		this.size = size;
		this.rate = rate;
		this.receivedTime = receivedTime;
		this.valid = true;
		this.quoteId = quoteId;
		this.liquidityProviderIdentifier = liquidityProviderIdentifier;
	}
	
	public synchronized void flagInvalid() {
		this.valid = false;
	}

	public synchronized boolean isValid() {
		return this.valid;
	}

	public synchronized long getReceivedTime() {
		return this.receivedTime;
	}
	
	public synchronized String getQuoteId() {
		return this.quoteId;
	}
		
	public synchronized String getLiquidityProviderIdentifier() {
		return liquidityProviderIdentifier;
	}

	public synchronized long getSize() {
		return size;
	}

	public synchronized double getRate() {
		return rate;
	}

	public RawLiquidityVo deepClone() {
		return new RawLiquidityVo(this);
	}
	
	public void setSize(long size) {
		this.size = size;
	}
}	
