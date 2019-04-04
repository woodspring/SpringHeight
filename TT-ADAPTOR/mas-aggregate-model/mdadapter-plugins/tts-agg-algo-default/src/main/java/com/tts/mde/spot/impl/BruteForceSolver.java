package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.vo.AggPxVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.util.collection.formatter.DoubleFormatter;

public class BruteForceSolver implements Iterator<List<RawLiquidityVo>>, Comparator<RawLiquidityVo> {
	
	private static final Logger log = LoggerFactory.getLogger(BruteForceSolver.class);

	private final Map<String, List<RawLiquidityVo>> ladderMap = new LinkedHashMap<>();
	private final Map<String, Integer> pointerMap = new LinkedHashMap<>();
	private final List<RawLiquidityVo> quoteList = new ArrayList<>();
	private long maxQuote;
	private boolean isBid;
	private boolean debug;

	public BruteForceSolver() {
		this(false);
	}

	public BruteForceSolver(boolean debug) {
		super();
		this.debug = debug;
	}

	public AggPxVo solve(List<RawLiquidityVo> list, long reqAmt, boolean isBid, int precision) {
		this.isBid = isBid;
		pointerMap.clear();
		ladderMap.clear();
		quoteList.clear();
		maxQuote = 0;

		filterLiquidityList(list, reqAmt);
		populateLadderMap(list);
		populateQuoteListAndMaxQuote(list);

		AggPxVo bestVwap = null;
		while (hasNext()) {
			if (debug) {
				printCombination();
			}
			List<RawLiquidityVo> currList = next();
			if (sizeRequirementMet(currList, reqAmt)) {
				currList.addAll(quoteList);
				currList.sort(this);

				AggPxVo currVwap = solveQuote(currList, reqAmt, isBid, precision);
				if (currVwap != null) {
					if (bestVwap != null) {
						if (isBid) { // sell
							if (currVwap.getVwapPrice() > bestVwap.getVwapPrice()) {
								bestVwap = currVwap;
							}
						} else { // buy
							if (currVwap.getVwapPrice() < bestVwap.getVwapPrice()) {
								bestVwap = currVwap;
							}
						}
					} else {
						bestVwap = currVwap;
					}
				}
			}
		}
		
		if (debug) {
			printQuotes();
		}
		if (bestVwap == null) {
			log.debug("Failed to find a solution...");
		}

		return bestVwap;
	}

	private void populateLadderMap(List<RawLiquidityVo> list) {
		for (RawLiquidityVo vo : list) {
			if (RawLiquidityVo.LiquidityType.LADDER.equals(vo.getType())) {
				if (!ladderMap.containsKey(vo.getLiquidityProviderSrc())) {
					ladderMap.put(vo.getLiquidityProviderSrc(), new ArrayList<>(Arrays.asList(vo)));
					pointerMap.put(vo.getLiquidityProviderSrc(), 0);
				} else {
					ladderMap.get(vo.getLiquidityProviderSrc()).add(vo);
				}
			}
		}
	}

	private void populateQuoteListAndMaxQuote(List<RawLiquidityVo> list) {
		for (RawLiquidityVo vo : list) {
			if (RawLiquidityVo.LiquidityType.RAW_QUOTE.equals(vo.getType())) {
				quoteList.add(vo);
				maxQuote += vo.getSize();
			}
		}
	}

	private static void filterLiquidityList(List<RawLiquidityVo> list, long requestAmount) {
		Iterator<RawLiquidityVo> it = list.iterator();
		Map<String, Long> largestAmountPerLadderLPMap = new HashMap<>();
		long quoteTotal = 0L;
		while (it.hasNext()) {
			RawLiquidityVo currVo = it.next();
			if (RawLiquidityVo.LiquidityType.LADDER.equals(currVo.getType())) {
				if (!largestAmountPerLadderLPMap.containsKey(currVo.getLiquidityProviderSrc())) {
					largestAmountPerLadderLPMap.put(currVo.getLiquidityProviderSrc(), currVo.getSize());
				} else {
					if (largestAmountPerLadderLPMap.get(currVo.getLiquidityProviderSrc()) >= requestAmount) {
						it.remove();
					} else {
						if (currVo.getSize() > largestAmountPerLadderLPMap.get(currVo.getLiquidityProviderSrc())) {
							largestAmountPerLadderLPMap.replace(currVo.getLiquidityProviderSrc(), currVo.getSize());
						} else {
							it.remove();
						}
					}
				}
			} else if (RawLiquidityVo.LiquidityType.RAW_QUOTE.equals(currVo.getType())) {
				if (requestAmount > quoteTotal) {
					quoteTotal += currVo.getSize();
				} else {
					it.remove();
				}
			}
		}
	}

	private AggPxVo solveQuote(List<RawLiquidityVo> voList, long reqAmt, boolean isBid, int precision) {
		if (isBid) { // sell, roundUp = false
			int n = voList.size();

			long[] quoteSizes = new long[n];
			double[] quoteRates = new double[n];
			int[] quoteLPs = new int[n];
			long[] solutionFill = new long[n];
			long filledSize = 0;

			// Initial Iteration
			for (int i = 0; i < n; i++) {
				quoteSizes[i] = voList.get(i).getSize();
				quoteRates[i] = voList.get(i).getRate();
				quoteLPs[i] = voList.get(i).getAssignedLpAdapterSrcId();
				if (filledSize < reqAmt) {
					if (filledSize + quoteSizes[i] <= reqAmt) {
						solutionFill[i] = quoteSizes[i];
						filledSize = filledSize + quoteSizes[i];
					} else {
						solutionFill[i] = reqAmt - filledSize;
						filledSize = reqAmt;
					}
				} else {
					solutionFill[i] = (long) 0;
				}
			}
			// Get VWAP Price
			return getVwapVo(solutionFill, quoteRates, voList, precision, false);
		} else { // ask (buy), roundUp = true
			int n = voList.size();

			long[] quoteSizes = new long[n];
			double[] quoteRates = new double[n];
			int[] quoteLPs = new int[n];
			long[] solutionFill = new long[n];
			long filledSize = 0;

			// Initial Iteration
			for (int i = 0; i < n; i++) {
				quoteSizes[i] = voList.get(i).getSize();
				quoteRates[i] = voList.get(i).getRate();
				quoteLPs[i] = voList.get(i).getAssignedLpAdapterSrcId();
				if (filledSize < reqAmt) {
					if (filledSize + quoteSizes[i] <= reqAmt) {
						solutionFill[i] = quoteSizes[i];
						filledSize = filledSize + quoteSizes[i];
					} else {
						solutionFill[i] = reqAmt - filledSize;
						filledSize = reqAmt;
					}
				} else {
					solutionFill[i] = (long) 0;
				}
			}
			// Get VWAP Price
			return getVwapVo(solutionFill, quoteRates, voList, precision, true);
		}
	}

	private static AggPxVo getVwapVo(long[] fill, double[] rates, List<RawLiquidityVo> quotes, int finalPrecision, boolean roundUp) {
		String method = "[getVwapVo] ";
		double vwap = 0;
		long size = 0;
		List<Long> weights = new ArrayList<Long>();
		List<RawLiquidityVo> orgQuotes = new ArrayList<RawLiquidityVo>();

		for (int i = 0; i < fill.length; i++) {
			if (fill[i] > 0) {
				size = size + fill[i];
				vwap = vwap + rates[i] * fill[i];
				weights.add(fill[i]);
				orgQuotes.add(quotes.get(i));
			}
		}

		long[] finalWeights = new long[weights.size()];
		for (int i = 0; i < weights.size(); i++) {
			finalWeights[i] = weights.get(i);
		}
		AggPxVo VwapVo = new AggPxVo(DoubleFormatter.roundDouble(vwap / size, finalPrecision, roundUp ? RoundingMode.CEILING : RoundingMode.FLOOR), finalWeights,
				orgQuotes.toArray(new RawLiquidityVo[0]));

		return VwapVo;
	}

	private boolean sizeRequirementMet(List<RawLiquidityVo> currList, long reqAmt) {
		long totalCombinationAmt = maxQuote;
		for (RawLiquidityVo vo : currList) {
			totalCombinationAmt += vo.getSize();
		}
		return totalCombinationAmt >= reqAmt;
	}

	private void printCombination() {
		log.debug("Current Testing Combination:");
		log.debug("Current RawLiquidityVo and Index list: ");
		for (String lp : pointerMap.keySet()) {
			log.debug("\tIndex = " + pointerMap.get(lp) + ", LP = " + lp + ", RawLiquidityVo: " + ladderMap.get(lp).get(pointerMap.get(lp)));
		}
	}

	private void printQuotes() {
		log.debug("Quote List:");
		for (RawLiquidityVo vo : quoteList) {
			log.debug("\tQuote: " + vo.toString());
		}
	}

	@Override
	public boolean hasNext() {
		for (String lp : pointerMap.keySet()) {
			if (pointerMap.get(lp) >= ladderMap.get(lp).size()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<RawLiquidityVo> next() {
		// get list of RawLiquidityVo that correspond with the current pointers
		List<RawLiquidityVo> currList = new ArrayList<>();
		for (String lp : pointerMap.keySet()) {
			currList.add(ladderMap.get(lp).get(pointerMap.get(lp)));
		}

		// update pointers
		Iterator<String> it = pointerMap.keySet().iterator();
		while (it.hasNext()) {
			String lp = it.next();
			if ((pointerMap.get(lp) + 1 == ladderMap.get(lp).size()) && it.hasNext()) {
				pointerMap.replace(lp, 0);
			} else {
				pointerMap.replace(lp, pointerMap.get(lp) + 1);
				break;
			}
		}

		// return currList
		return currList;
	}

	@Override
	public int compare(RawLiquidityVo o1, RawLiquidityVo o2) {
		if (isBid) { // sell
			return o1.getRate() == o2.getRate() ? o1.getSize() == o2.getSize() ? 0 : o1.getSize() > o2.getSize() ? -1 : 1 : o1.getRate() > o2.getRate() ? -1 : 1;
		} else { // ask (buy)
			return o1.getRate() == o2.getRate() ? o1.getSize() == o2.getSize() ? 0 : o1.getSize() > o2.getSize() ? -1 : 1 : o1.getRate() > o2.getRate() ? 1 : -1;
		}
	}
}
