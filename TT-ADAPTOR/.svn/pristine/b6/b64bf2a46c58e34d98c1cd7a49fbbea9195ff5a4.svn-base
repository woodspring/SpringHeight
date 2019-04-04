package com.tts.plugin.adapter.impl.base.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tts.vo.RateNumVo;
import com.tts.vo.TickBookVo;
import com.tts.vo.TickVo;

public class VWAPUtil {

	public static TickBookVo calculateVWAP(TickBookVo raw, long[] ls) {
		sortTickByPrice(raw);
		TickBookVo vwap = new TickBookVo();
		vwap.setSymbol(raw.getSymbol());
		
		for ( long l : ls ) {
			RateNumVo vAvgAskPrice = calculateVWAP(raw.getAskTicks(), l);
			RateNumVo vAvgBidPrice = calculateVWAP(raw.getBidTicks(), l);

			if ( vAvgAskPrice != null && vAvgBidPrice != null ) {
				TickVo askTick = new TickVo();
				askTick.setSize(l);
				askTick.setRate(vAvgAskPrice);
				vwap.getAskTicks().add(askTick);

				TickVo bidTick = new TickVo();
				bidTick.setSize(l);
				bidTick.setRate(vAvgBidPrice);
				vwap.getBidTicks().add(bidTick);
			}
		}
		return vwap;
	}

	public static RateNumVo calculateVWAP(List<TickVo> ticks, long l) {
		long reminding = l;
		RateNumVo avgPrice = new RateNumVo(0, 1);
		List<RateNumVo> prices = new ArrayList<RateNumVo>();
		List<Long> weights = new ArrayList<Long>();
		
		for ( int i = 0; reminding > 0 && i < ticks.size(); i++) {
			TickVo tick = ticks.get(i);
			prices.add(tick.getRate());
			weights.add(Math.min(tick.getSize(), reminding));
			reminding -= tick.getSize();
		}
		
		if ( reminding > 0 ) {
			return null;
		}
		for ( int i = 0; i < prices.size(); i++ ) {
			RateNumVo factor = new RateNumVo(weights.get(i), l);
			RateNumVo price = prices.get(i);
			avgPrice =  price.multiply(factor).add(avgPrice);
		}
	
		return avgPrice;
	}

	public static void sortTickByPrice(TickBookVo fbB) {
		sortTickByPrice(fbB.getAskTicks(), false);
		sortTickByPrice(fbB.getBidTicks(), true);
		
	}

	private static void sortTickByPrice(List<TickVo> ticks, boolean descending) {
		if (descending) {
			Collections.sort(ticks, TickVoComparator.PRICE_COMPARATOR_DESCENDING);
		} else {
			Collections.sort(ticks, TickVoComparator.PRICE_COMPARATOR_ASCENDING);
		}
	}

	public static class TickVoComparator implements Comparator<TickVo> {
		
		public static final TickVoComparator PRICE_COMPARATOR_ASCENDING = new TickVoComparator(false, TickComparsionFactor.PRICE_AND_THEN_SIZE);
		public static final TickVoComparator PRICE_COMPARATOR_DESCENDING = new TickVoComparator(true, TickComparsionFactor.PRICE_AND_THEN_SIZE);
		public static final TickVoComparator SIZE_COMPARATOR_ASCENDING = new TickVoComparator(false, TickComparsionFactor.SIZE_AND_THEN_PRICE);
		public static final TickVoComparator SIZE_COMPARATOR_DESCENDING = new TickVoComparator(true, TickComparsionFactor.SIZE_AND_THEN_PRICE);

		
		private final TickComparsionFactor factor;
		private final boolean descending;
		
		public TickVoComparator(boolean descending, TickComparsionFactor factor) {
			this.descending = descending;
			this.factor = factor;
		}

		@Override
		public int compare(TickVo o1, TickVo o2) {
			if ( factor == TickComparsionFactor.PRICE_AND_THEN_SIZE) {
				if (  o1.getRate() != null && o2.getRate() != null 
						&& o1.getRate().isEqual(o2.getRate())) {
					if ( descending) {
						return (int) ((o2.getSize()  - o1.getSize()) >> 10);
					}
					return (int) ((o1.getSize()  - o2.getSize()) >> 10);
				} else {
					if ( !descending ) {
						if  ( o1.getRate().isGreater(o2.getRate())) {
							return 10;
						} else {
							return -10;
						}
					} else{
						if  ( o2.getRate().isGreater(o1.getRate())) {
							return 10;
						} else {
							return -10;
						}
					}
				}
			} else {
				if ( o1.getSize() == o2.getSize() ) {
					if (  o1.getRate() != null && o2.getRate() != null ) {
						if  ( !descending && o1.getRate().isGreater(o2.getRate())) {
							return -10;
						} else {
							return 10;
						}
					}
				} else {
					if ( descending) {
						return (int) ((o2.getSize()  - o1.getSize()) >> 10);
					}
					return (int) ((o1.getSize()  - o2.getSize()) >> 10);
				}
			}
			return 0;
		}
		
		public enum TickComparsionFactor {
			SIZE_AND_THEN_PRICE,
			PRICE_AND_THEN_SIZE;
		}
	}

}
