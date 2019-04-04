package com.tts.mde.vo;

public class RawLiquidityVo {

	private volatile boolean valid = false;
	private final long size;
	private final long receivedTime;
	private final double rate;
	private final double spotRate;
	private final double fwdPts;
	private final String quoteId;
	private final String liquidityProviderSrc;
	private final String providerAdapter;
	private String settleDate;

	private String quoteEntryRefId;
	private transient int assignedLpAdapterSrcId;
	private LiquidityType quoteType;

	public RawLiquidityVo(long size, long receivedTime, double rate, double fwdPts, double spotRate, String quoteId, String liquidityProviderSrc, String providerAdapter,
			String quoteEntryRefId, int assignedLpAdapterSrcId, LiquidityType quoteType) {
		super();
		this.valid = true;
		this.size = size;
		this.receivedTime = receivedTime;
		this.rate = rate;
		this.quoteId = quoteId;
		this.liquidityProviderSrc = liquidityProviderSrc;
		this.quoteEntryRefId = quoteEntryRefId;
		this.assignedLpAdapterSrcId = assignedLpAdapterSrcId;
		this.quoteType = quoteType;
		this.spotRate = spotRate;
		this.fwdPts = fwdPts;
		this.providerAdapter = providerAdapter;
	}

	public void flagInvalid() {
		this.valid = false;
	}

	public boolean isValid() {
		return this.valid;
	}

	public long getReceivedTime() {
		return this.receivedTime;
	}

	public String getQuoteId() {
		return this.quoteId;
	}

	public String getLiquidityProviderSrc() {
		return liquidityProviderSrc;
	}

	public long getSize() {
		return size;
	}

	public double getRate() {
		return rate;
	}

	public String getProviderAdapter() {
		return providerAdapter;
	}

	public String getQuoteEntryRefId() {
		return quoteEntryRefId;
	}

	public double getSpotRate() {
		return spotRate;
	}

	public double getForwardPts() {
		return fwdPts;
	}

	public int getAssignedLpAdapterSrcId() {
		return assignedLpAdapterSrcId;
	}

	public void setAssignedLpAdapterSrcId(int assignedLpAdapterSrcId) {
		this.assignedLpAdapterSrcId = assignedLpAdapterSrcId;
	}	
	
	public String getSettleDate() {
		return settleDate;
	}

	public LiquidityType getType() {
		return quoteType;
	}
	
	public static enum LiquidityType {
		RAW_QUOTE, BOOK, LADDER, LADDER_WITH_MULTIHIT_ALLOWED
	}
	
	public String toString() {
		return "quoteType = " + quoteType + "\ttime = " + receivedTime + "\tsize = " + size + "\trate = " + rate + "\tspotRate = " + spotRate + "\tfwdPts = " + fwdPts
				+ "\tvalid = " + valid + "\tquoteId = " + quoteId;
	}
}
