package com.tts.mlp.app;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalAppConfig {
	private static final SpreadConfig NO_CHANGE_SPREAD = new SpreadConfig("NO_CHANGE", "0.00", 0, 0);
	private static final SpreadConfig SLOW_SPREAD = new SpreadConfig("SLOW", "0.0001", -1, 1);
	private static final SpreadConfig NORMAL_SPREAD = new SpreadConfig("NORMAL", "0.0003", -4, 4);
	private static final SpreadConfig VOLATILE_SPREAD = new SpreadConfig("VOLATILE", "0.0009", -9, 9);
	
	private static volatile SpreadConfig activeSpreadConfig = NORMAL_SPREAD;
	private static volatile boolean isRatePaused = false;
	private static volatile boolean isOrderTimeCheckEnabled = false;
	private static volatile boolean isRateFreezed = false;
	private static final Map<String, String> settleDateOverrideMap = new ConcurrentHashMap<String, String>();
	private static final Map<String, String> tradeRejectMap = new ConcurrentHashMap<String, String>();

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
		private final BigDecimal spread;
		private final int rwalkMin;
		private final int rwalkMax;
		
		public SpreadConfig(String name, String spread, int rwalkMin, int rwalkMax) {
			this.name = name;
			this.spread = new BigDecimal(spread);
			this.rwalkMin = rwalkMin;
			this.rwalkMax = rwalkMax;
		}
		
		public BigDecimal getSpread() {
			return spread;
		}

		public String getName() {
			return name;
		}

		public int getRwalkMin() {
			return rwalkMin;
		}

		public int getRwalkMax() {
			return rwalkMax;
		}	
		
		
	}
	
	public static void setTradeReject(String symbol, String newValue) {
		tradeRejectMap.put(symbol, newValue);
	}
	
	public static boolean isTradeRejectForSymbol(String s) {
		String bs = tradeRejectMap.get(s);
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
}

