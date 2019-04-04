package com.tts.mde.spot.impl;

import java.util.Comparator;

import com.tts.mde.vo.RawLiquidityVo;

public class RateComparator implements Comparator<RawLiquidityVo> {

	public static final RateComparator PRICE_COMPARATOR_ASCENDING = new RateComparator(false, RateComparsionFactor.PRICE_AND_THEN_SIZE);
	public static final RateComparator PRICE_COMPARATOR_DESCENDING = new RateComparator(true, RateComparsionFactor.PRICE_AND_THEN_SIZE);
	public static final RateComparator SIZE_COMPARATOR_ASCENDING = new RateComparator(false, RateComparsionFactor.SIZE_AND_THEN_PRICE);
	public static final RateComparator SIZE_COMPARATOR_DESCENDING = new RateComparator(true, RateComparsionFactor.SIZE_AND_THEN_PRICE);

	private final RateComparsionFactor factor;
	private final boolean descending;

	public RateComparator(boolean descending, RateComparsionFactor factor) {
		this.descending = descending;
		this.factor = factor;
	}

	@Override
	public int compare(RawLiquidityVo o1, RawLiquidityVo o2) {
		if (factor == RateComparsionFactor.PRICE_AND_THEN_SIZE) {
			if (o1.getRate() > 0 && o2.getRate() > 0 && o1.getRate() == o2.getRate()) {
				// sort by descending size if rates are equal
				if (o2.getSize() > o1.getSize()) {
					return 10;
				} else if (o2.getSize() < o1.getSize()) {
					return -10;
				}
				// if ( descending) {
				// return (int) ((o2.getSize() - o1.getSize()) >> 10);
				// }
				// return (int) ((o1.getSize() - o2.getSize()) >> 10);
			} else {
				if (!descending) {
					if (o1.getRate() > o2.getRate()) {
						return 10;
					} else {
						return -10;
					}
				} else {
					if (o2.getRate() > o1.getRate()) {
						return 10;
					} else {
						return -10;
					}
				}
			}
		} else {
			if (o1.getSize() == o2.getSize()) {
				if (o1.getRate() > 0 && o2.getRate() > 0) {
					if (!descending && o1.getRate() > o2.getRate()) {
						return -10;
					} else {
						return 10;
					}
				}
			} else {
				if (o2.getSize() > o1.getSize()) {
					return 10;
				} else if (o2.getSize() < o1.getSize()) {
					return -10;
				}
//				if (descending) {
//					return (int) ((o2.getSize() - o1.getSize()) >> 10);
//				}
//				return (int) ((o1.getSize() - o2.getSize()) >> 10);
			}
		}
		return 0;
	}

	public enum RateComparsionFactor {
		SIZE_AND_THEN_PRICE, PRICE_AND_THEN_SIZE;
	}
}
