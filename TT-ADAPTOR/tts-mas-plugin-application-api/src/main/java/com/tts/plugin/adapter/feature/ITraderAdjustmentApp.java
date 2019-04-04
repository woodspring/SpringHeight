package com.tts.plugin.adapter.feature;

import java.time.ZonedDateTime;

import com.tts.message.common.CommonStruct.BuySellActionCd;
import com.tts.message.common.CommonStruct.SideCd;
import com.tts.message.market.FullBookStruct.FullBook;

public interface ITraderAdjustmentApp {
	FullBook.Builder adjustBook(String symbol, FullBook.Builder fbBuilder);

	CoveringResult coverAdjustment(String symbol, BuySellActionCd clientBuySellAction, double size);


	void adjustPriceRequest(String symbol, SideCd side, double bankWantedSize, double delta, double limitPrice,
			OwnerInfo ownerInfo);

	static class OwnerInfo {
		private final String ownerName;
		private final long ownerIntCustId;
		private final long ownerIntAcctId;
		private final ZonedDateTime creationTime;
		
		public OwnerInfo(String ownerName, long ownerIntCustId, long ownerIntAcctId) {
			super();
			this.ownerName = ownerName;
			this.ownerIntCustId = ownerIntCustId;
			this.ownerIntAcctId = ownerIntAcctId;
			this.creationTime = ZonedDateTime.now();
		}
		public String getOwnerName() {
			return ownerName;
		}
		public long getOwnerIntCustId() {
			return ownerIntCustId;
		}
		public long getOwnerIntAcctId() {
			return ownerIntAcctId;
		}
		public ZonedDateTime getCreationTime() {
			return creationTime;
		}
		
	}
	
	static class CoveringResult {
		OwnerInfo provider;
		double coveredAmount;
		
		public OwnerInfo getProvider() {
			return provider;
		}
		public void setProvider(OwnerInfo provider) {
			this.provider = provider;
		}
		public double getCoveredAmount() {
			return coveredAmount;
		}
		public void setCoveredAmount(double coveredAmount) {
			this.coveredAmount = coveredAmount;
		}
	
		
	}

}