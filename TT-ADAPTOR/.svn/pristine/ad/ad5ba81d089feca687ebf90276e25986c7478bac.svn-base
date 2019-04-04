package com.tts.mlp.app;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalAppConfig {
	private static final SpreadConfig NO_CHANGE_SPREAD = new SpreadConfig("NO_CHANGE", "0.00");
	private static final SpreadConfig SLOW_SPREAD = new SpreadConfig("SLOW", "0.1");
	private static final SpreadConfig NORMAL_SPREAD = new SpreadConfig("NORMAL", "0.25");
	private static final SpreadConfig VOLATILE_SPREAD = new SpreadConfig("VOLATILE", "0.99");
	
	private static volatile SpreadConfig activeSpreadConfig = NORMAL_SPREAD;
	private static volatile boolean isRatePaused = false;
	private static volatile boolean isCrazyPriceStructure = false;
	private static volatile boolean isOrderTimeCheckEnabled = false;
	private static volatile boolean isRateFreezed = false;
	private static volatile boolean isDoubleBid = false;
	private static volatile boolean isDoubleOffer = false;
	private static volatile boolean isFillPriceMustBeDiffThanOrdPrice = false;
	private static final Map<String, String> settleDateOverrideMap = new ConcurrentHashMap<String, String>();
	private static final Map<String, String> tradeRejectMap = new ConcurrentHashMap<String, String>();
	private static final Map<String, String> tradeCancelMap = new ConcurrentHashMap<String, String>();
	private static final Map<String, String> tradePartialFillMap = new ConcurrentHashMap<String, String>();
	private static final Map<String, String> tradeDoNotReplyMap = new ConcurrentHashMap<String, String>();

	public static SpreadConfig getSpreadConfig() {
		return activeSpreadConfig;
	}
	
	public static void switchSpreadConfig(String spreadConfig) {
		if ( "NORMAL".equals(spreadConfig)) {
			activeSpreadConfig = NORMAL_SPREAD;
		} else if ( "SLOW".equals(spreadConfig)) {
			activeSpreadConfig = SLOW_SPREAD;
		} else if ( "VOLATILE".equals(spreadConfig)) {
			activeSpreadConfig = VOLATILE_SPREAD;
		} else if ( "NO_CHANGE".equals(spreadConfig)) {
			activeSpreadConfig = NO_CHANGE_SPREAD;
		}
	}

	public static class SpreadConfig {
		private final String name;
		private final String spread;
		
		public SpreadConfig(String name, String spread) {
			this.name = name;
			this.spread = spread;
		}
		
		public String getSpread() {
			return spread;
		}

		public String getName() {
			return name;
		}	
	}
	
	public static void setTradeReject(String symbol, String newValue) {
		tradeRejectMap.put(symbol, newValue);
	}
	
	public static boolean isTradeRejectForSymbol(String symbol) {
		String bs = tradeRejectMap.get(symbol);
		if ( bs == null ) {
			return false;
		} 
		Boolean b = Boolean.parseBoolean(bs);
		return b;
	}
	
	public static void setRateFreezed(boolean b) {
		isRateFreezed = b;		
	}
	
	public static boolean isRateFreezed() {
		return isRateFreezed;
	}
	
	public static void setRatePause(boolean b) {
		isRatePaused = b;		
	}
	
	public static boolean isRatePaused() {
		return isRatePaused;
	}

	public static void setOrderTimeCheck(boolean enableB) {
		isOrderTimeCheckEnabled = enableB;
	}
	
	public static boolean isOrderTimeCheckEnabled() {
		return isOrderTimeCheckEnabled;		
	}
	
	public static String getOverrideSettleDate(String symbol) {
		return settleDateOverrideMap.get(symbol);
	}
	
	public static void setOverrideSettleDate(String symbol, String date) {
		settleDateOverrideMap.put(symbol, date);
	}
	
	public static void removeOverrideSettleDate(String symbol) {
		settleDateOverrideMap.remove(symbol);
	}

	public static void setTradeDoNotReply(String symbol, String newValue) {
		tradeDoNotReplyMap.put(symbol, newValue);	
	}
	
	public static boolean isTradeDoNotReplyForSymbol(String symbol) {
		String bs = tradeDoNotReplyMap.get(symbol);
		if ( bs == null ) {
			return false;
		} 
		Boolean b = Boolean.parseBoolean(bs);
		return b;
	}

	public static void setCrazyPriceStructure(boolean enableB) {
		isCrazyPriceStructure= true;
	}

	public static boolean isCrazyPriceStructure() {
		return isCrazyPriceStructure;
	}

	public static boolean isDoubleBid() {
		return isDoubleBid;
	}

	public static boolean isDoubleOffer() {
		return isDoubleOffer;
	}
	
	public static void setDoubleSizeUp(boolean bidEnabled, boolean offerEnabled) {
		isDoubleBid = bidEnabled;
		isDoubleOffer = offerEnabled;
	}

	public static void setTradeCancel(String symbol, String newValue) {
		tradeCancelMap.put(symbol, newValue);
	}

	public static boolean isTradeCancelForSymbol(String symbol) {
		try {
			boolean f = Boolean.parseBoolean(tradeCancelMap.get(symbol));
			return f;
		} catch (Exception e) {
			
		}
		return false;
	}

	public static void setTradePartialFillEnabled(String symbol, String newValue) {
		tradePartialFillMap.put(symbol, newValue);
	}
	
	public static boolean isTradePartialFillForSymbol(String symbol) {
		try {
			boolean f = Boolean.parseBoolean(tradePartialFillMap.get(symbol));
			return f;
		} catch (Exception e) {
			
		}
		return false;
	}

	public static boolean isFillPriceMustBeDiffThanOrdPrice() {
		return isFillPriceMustBeDiffThanOrdPrice;
	}

	public static void setFillPriceMustBeDiffThanOrdPrice(boolean b) {
		isFillPriceMustBeDiffThanOrdPrice = b;
	}
}

