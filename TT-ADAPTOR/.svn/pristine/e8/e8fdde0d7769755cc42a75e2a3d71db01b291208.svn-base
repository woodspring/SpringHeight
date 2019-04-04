package com.tts.ske.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

import com.tts.util.collection.formatter.DoubleFormatter;


public class BankLiquidityAdjustmentVo {
	public final static String DELIMITER = "::";
	private static final String WRITE_PATTERN = "%s" + DELIMITER + "%s" + DELIMITER + "%s" + DELIMITER + "%s";
	private final double adjustSize;
	private final double adjustment;
	private final double limitPrice;
	private final OwnerInfo ownerInfo;

	public BankLiquidityAdjustmentVo(double size, double adjustment, double limitPrice, OwnerInfo ownerInfo) {
		super();
		this.adjustSize = size;
		this.adjustment = adjustment;
		this.limitPrice = limitPrice;
		this.ownerInfo = ownerInfo;
	}

	public double getSize() {
		return adjustSize;
	}

	public double getAdjustment() {
		return adjustment;
	}

	public double getLimitPrice() {
		return limitPrice;
	}

	public OwnerInfo getOwnerInfo() {
		return ownerInfo;
	}
	
	public String objToString() {
		return String.format(WRITE_PATTERN, 
				DoubleFormatter.convertToString(adjustSize, 2, RoundingMode.UNNECESSARY), 
				DoubleFormatter.convertToString(adjustment, 5, RoundingMode.UNNECESSARY),
				DoubleFormatter.convertToString(limitPrice, 5, RoundingMode.UNNECESSARY), 
				ownerInfo == null ? "" : ownerInfo.objToString() );
	}
	
	public static BankLiquidityAdjustmentVo fromString(String s) {
		String[] segs = s.split(DELIMITER);
		return new BankLiquidityAdjustmentVo(
				new BigDecimal(segs[0]).doubleValue(), 
				new BigDecimal(segs[1]).doubleValue(), 
				new BigDecimal(segs[2]).doubleValue(), 
				segs[3].isEmpty() || segs[3].trim().isEmpty() ? null : OwnerInfo.fromString(segs[3]));
	}
	
	public static class OwnerInfo {
		private final static String DELIMITER = "~~";
		private static final String WRITE_PATTERN = "[%s"  + DELIMITER + "%s" + DELIMITER + "%s" + DELIMITER + "%s]";

		private final String ownerName;
		private final long ownerIntCustId;
		private final long ownerIntAcctId;
		private final ZonedDateTime creationTime;
		
		public OwnerInfo(String ownerName, long ownerIntCustId, long ownerIntAcctId) {
			this(ownerName, ownerIntCustId, ownerIntAcctId, ZonedDateTime.now());
		}
		
		private OwnerInfo(String ownerName, long ownerIntCustId, long ownerIntAcctId, ZonedDateTime creationTime) {
			super();
			this.ownerName = ownerName;
			this.ownerIntCustId = ownerIntCustId;
			this.ownerIntAcctId = ownerIntAcctId;
			this.creationTime = creationTime;
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

		public String objToString() {
			return String.format(WRITE_PATTERN, ownerName, ownerIntCustId, ownerIntAcctId, creationTime );
		}
		
		public static OwnerInfo fromString(String s) {
			String newInput = s.trim();
			String inline = newInput.substring(1, newInput.length() -1);
			String[] segs = inline.split(DELIMITER);
			return new OwnerInfo(segs[0], Long.parseLong(segs[1]), Long.parseLong(segs[2]), ZonedDateTime.parse(segs[3]));
		}
	}
}
