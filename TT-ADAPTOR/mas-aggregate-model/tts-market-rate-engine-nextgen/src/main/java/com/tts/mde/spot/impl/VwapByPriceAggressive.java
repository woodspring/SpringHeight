package com.tts.mde.spot.impl;

import com.tts.mde.vo.RawLiquidityVo;
import com.tts.mde.vo.AggPxVo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class VwapByPriceAggressive {
	private static final int MAX_INTERATIONS =  20;
	
	public static AggPxVo calculateVWAP(List<RawLiquidityVo> ticks, long l, boolean isBid, int finalPrecision) {
		long reminding = l;
		double avgPrice = 0.0D;
		long[] weights = new long[20];
		List<RawLiquidityVo> quotes = new ArrayList<>();
		boolean[] usedInd = new boolean[20];
		int useLpCount = 0;
		HashSet<Integer> lpIds = new HashSet<>();
		int weightIdx = 0;
		if (reminding > 0L) {
			quotes.clear();
			reminding = l;
			avgPrice = 0.0D;
			weights = new long[20];
			usedInd = new boolean[20];
			long lookFor = l / 2L;
			weightIdx = 0;
			for (int i = 0; (reminding > 0L) && (i < ticks.size()); i++) {
				RawLiquidityVo tick = (RawLiquidityVo) ticks.get(i);
				lpIds.add(tick.getAssignedLpAdapterSrcId());
				if ((usedInd[tick.getAssignedLpAdapterSrcId()] == false) && (tick.getSize() >= lookFor)) {
					quotes.add(tick);
					long use = Math.min(tick.getSize(), reminding);
					weights[(weightIdx++)] = use;
					reminding -= use;
					usedInd[tick.getAssignedLpAdapterSrcId()] = true;
					useLpCount++;
					break;
				}
			}
			for (int i = 0; (reminding > 0L) && (i < ticks.size()); i++) {
				RawLiquidityVo tick = (RawLiquidityVo) ticks.get(i);
				if (usedInd[tick.getAssignedLpAdapterSrcId()] == false) {
					quotes.add(tick);
					long use = Math.min(tick.getSize(), reminding);
					weights[(weightIdx++)] = use;
					reminding -= use;
					usedInd[tick.getAssignedLpAdapterSrcId()] = true;
					useLpCount++;
				}
			}
			if (reminding > 0L) {
				quotes.clear();
				reminding = l;
				avgPrice = 0.0D;
				weights = new long[20];
				usedInd = new boolean[20];
				weightIdx = 0;
				useLpCount=0;
				lookFor = l / 2L;
				int iteration = 0;
				while ( reminding > 0 && iteration < MAX_INTERATIONS ) {
					for (int i = 0; (reminding > 0L) && (i < ticks.size()); i++) {
						RawLiquidityVo tick = (RawLiquidityVo) ticks.get(i);
						if ((usedInd[tick.getAssignedLpAdapterSrcId()] == false) && (tick.getSize() >= lookFor)) {
							quotes.add(tick);
							long use = Math.min(tick.getSize(), reminding);
							weights[(weightIdx++)] =use;
							reminding -= use;
							usedInd[tick.getAssignedLpAdapterSrcId()] = true;
							useLpCount++;
							break;
						}
					}
					if ( (lpIds.size() - useLpCount) <= 1) {
						lookFor = reminding;
					} else {
						lookFor = reminding / 2;
					}
					iteration++;
				}

				for (int i = 0; (reminding > 0L) && (i < ticks.size()); i++) {
					RawLiquidityVo tick = (RawLiquidityVo) ticks.get(i);
					if (usedInd[tick.getAssignedLpAdapterSrcId()] == false) {
						quotes.add(tick);
						long use = Math.min(tick.getSize(), reminding);
						weights[(weightIdx++)] = use;
						reminding -= use;
						usedInd[tick.getAssignedLpAdapterSrcId()] = true;
					}
				}
				if (reminding > 0L) {
					return null;
				}
			}
		}
		for (int i = 0; i < quotes.size(); i++) {
			avgPrice = ((RawLiquidityVo) quotes.get(i)).getRate() * weights[i] / l + avgPrice;
		}
		return new AggPxVo(avgPrice, Arrays.copyOf(weights, quotes.size()),
				(RawLiquidityVo[]) quotes.toArray(new RawLiquidityVo[0]));
	}

	public static void sortTickByPrice(List<RawLiquidityVo> ticks, boolean descending) {
		if (descending) {
			Collections.sort(ticks, RateComparator.PRICE_COMPARATOR_DESCENDING);
		} else {
			Collections.sort(ticks, RateComparator.PRICE_COMPARATOR_ASCENDING);
		}
	}
}
