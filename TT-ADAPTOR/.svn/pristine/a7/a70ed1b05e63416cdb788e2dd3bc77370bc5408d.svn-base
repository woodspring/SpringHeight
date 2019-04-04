package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.tts.mde.algo.IMDAggAlgo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.mde.vo.AggPxVo;
import com.tts.mde.vo.RawLiquidityVo.LiquidityType;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.util.collection.formatter.DoubleFormatter;

public class VwapByPx101Helper implements IMDAggAlgo {
	public final static String NAME = "VwapByPx101";
	
	@Override
	public AggPxVo calculateOutPxBid(List<RawLiquidityVo> quotes, double size, int finalPrecision,
			String marketMode) {
		int n = quotes.size();
		if ( n == 0 ) { 
			return null; 
		}
		HashSet<Integer> lpIds = new HashSet<>();
		for (int i = 0; i < n; i++) {
			RawLiquidityVo rawLiquidityVo = quotes.get(i);
			if ( rawLiquidityVo.getType() == LiquidityType.LADDER 
					||  rawLiquidityVo.getType() == LiquidityType.LADDER_WITH_MULTIHIT_ALLOWED) {
				lpIds.add(rawLiquidityVo.getAssignedLpAdapterSrcId());
			}
		}
		
		int[] llp = new int[lpIds.size()];
		int idx  = 0;
		for ( Integer lpId: lpIds) {
			llp[idx++] = lpId;
		}
		return quickSolveBid(quotes, (long ) size , llp, llp.length, finalPrecision);
	}

	@Override
	public AggPxVo calculateOutPxAsk(List<RawLiquidityVo> quotes, double size, int finalPrecision,
			String marketMode) {
		int n = quotes.size();
		if ( n == 0 ) { 
			return null; 
		}
		HashSet<Integer> lpIds = new HashSet<>();
		for (int i = 0; i < n; i++) {
			RawLiquidityVo rawLiquidityVo = quotes.get(i);
			if ( rawLiquidityVo.getType() == LiquidityType.LADDER 
					||  rawLiquidityVo.getType() == LiquidityType.LADDER_WITH_MULTIHIT_ALLOWED) {
				lpIds.add(rawLiquidityVo.getAssignedLpAdapterSrcId());
			}
		}
		
		int[] llp = new int[lpIds.size()];
		int idx  = 0;
		for ( Integer lpId: lpIds) {
			llp[idx++] = lpId;
		}
		return quickSolveAsk(quotes, (long ) size , llp, llp.length, finalPrecision);
	}

	@Override
	public FullBook.Builder validateVwap(FullBook.Builder fbBuilder, String marketMode) {
		return fbBuilder;
	}

	private static AggPxVo quickSolveAsk(List<RawLiquidityVo> quotes, long l, int[] llp, int numLadders, int finalPrecision) {
	
		int n = quotes.size();
	
		long[] quoteSizes = new long[n];
		double[] quoteRates = new double[n];
		int[] quoteLPs = new int[n];
		long[] solutionFill = new long[n];
		int[][] conflictLP = new int[numLadders][n];
		int[] conflictIndex = new int[numLadders];
		long filledSize = 0;
		int quoteIndex = 0;
		boolean outOfBounds = false;
	
		// Initial Iteration
		for (int i = 0; i < n; i++) {
			quoteSizes[i] = quotes.get(i).getSize();
			quoteRates[i] = quotes.get(i).getRate();
			quoteLPs[i] = quotes.get(i).getAssignedLpAdapterSrcId();
			if (filledSize < l) {
				quoteIndex = i;
				// Get Ladder Conflicts
				for (int j = 0; j < numLadders; j++) {
					if (quoteLPs[i] == llp[j]) {
						conflictLP[j][conflictIndex[j]] = i;
						conflictIndex[j] = conflictIndex[j] + 1;
						break;
					}
				}
	
				if (filledSize + quoteSizes[i] <= l) {
					solutionFill[i] = quoteSizes[i];
					filledSize = filledSize + quoteSizes[i];
				} else {
					solutionFill[i] = l - filledSize;
					filledSize = l;
				}
			} else {
				solutionFill[i] = (long) 0;
			}
		}
		// Make -999
		for (int i = 0; i < numLadders; i++) {
			for (int j = conflictIndex[i]; j < n; j++) {
				conflictLP[i][j] = -999;
			}
		}
		// Initial Iteration Total Conflict
		boolean conflict = false;
		for (int i = 0; i < numLadders; i++) {
			conflict = (conflict || conflictIndex[i] > 1);
		}
	
		while (conflict) {
			for (int i = 0; i < numLadders; i++) {
				if (conflictIndex[i] == 1) {
					continue;
				} else {
					double minUtil = 999999;
					int nextQuoteIndex = quoteIndex;
					long[] nextSolutionFill = Arrays.copyOf(solutionFill, n);
					int[][] nextConflictLP = new int[numLadders][n];
					for (int j = 0; j < numLadders; j++) {
						nextConflictLP[j] = Arrays.copyOf(conflictLP[j], n);
					}
					int[] nextConflictIndex = Arrays.copyOf(conflictIndex, numLadders);
					int removeIndex = 0;
					for (int j = 0; j < conflictIndex[i]; j++) {
						int m = 0;
						int found = 0;
						while (conflictLP[i][m] == -999 || found < j) {
							m = m + 1;
							if (conflictLP[i][m] != -999) {
								found = found + 1;
							}
						}
						long[] tempSolutionFill = Arrays.copyOf(solutionFill, n);
						int tempQuoteIndex = quoteIndex;
						int[][] tempConflictLP = new int[numLadders][n];
						for (int k = 0; k < numLadders; k++) {
							tempConflictLP[k] = Arrays.copyOf(conflictLP[k], n);
						}
						int[] tempConflictIndex = Arrays.copyOf(conflictIndex, numLadders);
						long replaceAmount = solutionFill[conflictLP[i][m]];
						int sameLP = 0;
						double specialUtil = 999999;
						double utility = -replaceAmount * quoteRates[conflictLP[i][m]];
	
						// Largest Quote not fully filled
						if (quoteSizes[quoteIndex] - solutionFill[quoteIndex] > 0 && quoteIndex != conflictLP[i][m]) {
							if (replaceAmount <= quoteSizes[quoteIndex] - solutionFill[quoteIndex]) {
								tempSolutionFill[quoteIndex] = tempSolutionFill[quoteIndex] + replaceAmount;
								utility = utility + replaceAmount * quoteRates[quoteIndex];
								tempQuoteIndex = quoteIndex;
								tempSolutionFill[conflictLP[i][m]] = 0;
							} else {
								if (quoteSizes[quoteIndex] != solutionFill[quoteIndex]) {
									utility = utility + (quoteSizes[quoteIndex] - solutionFill[quoteIndex])
											* quoteRates[quoteIndex];
									tempSolutionFill[quoteIndex] = tempSolutionFill[quoteIndex]
											+ (quoteSizes[quoteIndex] - solutionFill[quoteIndex]);
									replaceAmount = replaceAmount - (quoteSizes[quoteIndex] - solutionFill[quoteIndex]);
									tempSolutionFill[conflictLP[i][m]] = replaceAmount;
								}
	
								int k = quoteIndex + 1;
								int special_k = 0;
								long totalReplace = 0;
								long special_Replace = 0;
								
								if (k >= n) {
									outOfBounds = true;
								}
								
								while (replaceAmount != 0 && !outOfBounds) {
	
									// Special utility
									// double specialUtil = 999999;
	
									boolean hitSameLp = false;
									for (int lps = 0; lps < numLadders; lps++) {
										if (quoteLPs[k] == llp[lps]) {
											if (lps == i) {
												// Special case
												special_k = k;
												if (sameLP < 1) {
	
													for (int q = 0; q < n; q++) {
														if (tempConflictLP[i][q] != -999) {
															totalReplace = totalReplace
																	+ tempSolutionFill[tempConflictLP[i][q]];
														}
													}
													special_Replace = totalReplace;
													while (totalReplace != 0 && special_k < n) {
														if (quoteSizes[special_k] < totalReplace) {
															special_k = special_k + 1;
															continue;
														} else {
															specialUtil = totalReplace * quoteRates[special_k]
																	- totalReplace * quoteRates[tempConflictLP[i][m]];
															totalReplace = 0;
															sameLP = sameLP + 1;
														}
													}
												}
												k = k + 1;
												hitSameLp = true;
												break;
											} else {
												int tempConflictLP_index = 0;
												while (tempConflictLP[lps][tempConflictLP_index] != -999) {
													tempConflictLP_index = tempConflictLP_index + 1;
												}
												tempConflictLP[lps][tempConflictLP_index] = k;
												tempConflictIndex[lps] = tempConflictIndex[lps] + 1;
											}
										}
	
									}
									if (hitSameLp) {
										continue;
									}
									if (quoteSizes[k] >= replaceAmount) {
										utility = utility + replaceAmount * quoteRates[k];
										if (specialUtil < utility) {
											int tempConflictLP_index = 0;
											int found2 = 0;
											while (found2 < tempConflictIndex[i]) {
												if (tempConflictLP[i][tempConflictLP_index] != -999) {
													tempSolutionFill[tempConflictLP[i][tempConflictLP_index]] = 0;
													tempConflictLP[i][tempConflictLP_index] = -999;
													found2 = found2 + 1;
												}
												tempConflictLP_index = tempConflictLP_index + 1;
											}
											tempSolutionFill[special_k] = special_Replace;
											tempConflictIndex[i] = 1;
											tempQuoteIndex = special_k;
	
										} else {
											tempSolutionFill[k] = replaceAmount;
											tempSolutionFill[conflictLP[i][m]] = 0;
											tempQuoteIndex = k;
										}
										replaceAmount = 0;
									} else {
										utility = utility + quoteSizes[k] * quoteRates[k];
										tempSolutionFill[k] = quoteSizes[k];
										replaceAmount = replaceAmount - quoteSizes[k];
										tempSolutionFill[conflictLP[i][m]] = replaceAmount;
									}
									k = k + 1;
									if (k >= n) {
										outOfBounds = true;
									}
								}
							}
						} else {
							int k = quoteIndex + 1;
							int special_k = 0;
							long totalReplace = 0;
							long special_Replace = 0;
							if (k >= n) {
								outOfBounds = true;
							}
							
							while (replaceAmount != 0 && !outOfBounds) {
	
								// Special utility
								// double specialUtil = 999999;
	
								boolean hitSameLp = false;
								for (int lps = 0; lps < numLadders; lps++) {
									if (quoteLPs[k] == llp[lps]) {
										if (lps == i) {
											// Sepcial case
											special_k = k;
											if (sameLP < 1) {
												for (int q = 0; q < n; q++) {
													if (tempConflictLP[i][q] != -999) {
														totalReplace = totalReplace
																+ tempSolutionFill[tempConflictLP[i][q]];
													}
												}
												special_Replace = totalReplace;
												while (totalReplace != 0 && special_k < n) {
													if (quoteSizes[special_k] < totalReplace) {
														special_k = special_k + 1;
														continue;
													} else {
														specialUtil = totalReplace * quoteRates[special_k]
																- totalReplace * quoteRates[conflictLP[i][m]];
														totalReplace = 0;
														sameLP = sameLP + 1;
													}
												}
											}
											k = k + 1;
											hitSameLp = true;
											break;
										} else {
											int tempConflictLP_index = 0;
											while (tempConflictLP[lps][tempConflictLP_index] != -999) {
												tempConflictLP_index = tempConflictLP_index + 1;
											}
											tempConflictLP[lps][tempConflictLP_index] = k;
											tempConflictIndex[lps] = tempConflictIndex[lps] + 1;
										}
									}
								}
								if (hitSameLp) {
									continue;
								}
								if (quoteSizes[k] >= replaceAmount) {
									utility = utility + replaceAmount * quoteRates[k];
									if (specialUtil < utility) {
										int tempConflictLP_index = 0;
										int found2 = 0;
										while (found2 < tempConflictIndex[i]) {
											if (tempConflictLP[i][tempConflictLP_index] != -999) {
												tempSolutionFill[tempConflictLP[i][tempConflictLP_index]] = 0;
												tempConflictLP[i][tempConflictLP_index] = -999;
												found2 = found2 + 1;
											}
											tempConflictLP_index = tempConflictLP_index + 1;
										}
										tempSolutionFill[special_k] = special_Replace;
										tempConflictIndex[i] = 1;
										tempQuoteIndex = special_k;
	
									} else {
										tempSolutionFill[k] = replaceAmount;
										tempSolutionFill[conflictLP[i][m]] = 0;
										tempQuoteIndex = k;
									}
									replaceAmount = 0;
								} else {
									utility = utility + quoteSizes[k] * quoteRates[k];
									tempSolutionFill[k] = quoteSizes[k];
									replaceAmount = replaceAmount - quoteSizes[k];
									tempSolutionFill[conflictLP[i][m]] = replaceAmount;
								}
								k = k + 1;
								if (k >= n) {
									outOfBounds = true;
								}
							}
						}
						// Get best solution for current conflict
						if (utility < minUtil) {
							minUtil = utility;
							nextSolutionFill = tempSolutionFill;
							nextConflictLP = tempConflictLP;
							nextConflictIndex = tempConflictIndex;
							nextQuoteIndex = tempQuoteIndex;
							removeIndex = m;
						}
	
					}
					// Updating current best solution for next conflict
					// resolution
					solutionFill = nextSolutionFill;
					conflictLP = nextConflictLP;
					conflictIndex = nextConflictIndex;
					quoteIndex = nextQuoteIndex;
					conflictLP[i][removeIndex] = -999;
					conflictIndex[i] = conflictIndex[i] - 1;
				}
	
				// Updating total conflict
				conflict = false;
				for (int j = 0; j < numLadders; j++) {
					conflict = (conflict || conflictIndex[j] > 1);
				}
				if (!conflict) {
					break;
				}
			}
		}
		
		AggPxVo retObj = getVwapVo(solutionFill, quoteRates, quotes, finalPrecision, true);
		if (outOfBounds || n == 0) {
			retObj = null;
		}
		
		return retObj;
	
	}

	private static AggPxVo quickSolveBid(List<RawLiquidityVo> quotes, long l, int[] llp, int numLadders, int finalPrecision) {
	
		int n = quotes.size();
	
		long[] quoteSizes = new long[n];
		double[] quoteRates = new double[n];
		int[] quoteLPs = new int[n];
		long[] solutionFill = new long[n];
		int[][] conflictLP = new int[numLadders][n];
		int[] conflictIndex = new int[numLadders];
		long filledSize = 0;
		int quoteIndex = 0;
		boolean outOfBounds = false;
	
		// Initial Iteration
		for (int i = 0; i < n; i++) {
			quoteSizes[i] = quotes.get(i).getSize();
			quoteRates[i] = quotes.get(i).getRate();
			quoteLPs[i] = quotes.get(i).getAssignedLpAdapterSrcId();
			if (filledSize < l) {
				quoteIndex = i;
				// Get Ladder Conflicts
				for (int j = 0; j < numLadders; j++) {
					if (quoteLPs[i] == llp[j]) {
						conflictLP[j][conflictIndex[j]] = i;
						conflictIndex[j] = conflictIndex[j] + 1;
						break;
					}
				}
	
				if (filledSize + quoteSizes[i] <= l) {
					solutionFill[i] = quoteSizes[i];
					filledSize = filledSize + quoteSizes[i];
				} else {
					solutionFill[i] = l - filledSize;
					filledSize = l;
				}
			} else {
				solutionFill[i] = (long) 0;
			}
		}
		// Make -999
		for (int i = 0; i < numLadders; i++) {
			for (int j = conflictIndex[i]; j < n; j++) {
				conflictLP[i][j] = -999;
			}
		}
		// Initial Iteration Total Conflict
		boolean conflict = false;
		for (int i = 0; i < numLadders; i++) {
			conflict = (conflict || conflictIndex[i] > 1);
		}
	
		while (conflict) {
			for (int i = 0; i < numLadders; i++) {
				if (conflictIndex[i] == 1) {
					continue;
				} else {
					double maxUtil = -999999;
					int nextQuoteIndex = quoteIndex;
					long[] nextSolutionFill = Arrays.copyOf(solutionFill, n);
					int[][] nextConflictLP = new int[numLadders][n];
					for (int j = 0; j < numLadders; j++) {
						nextConflictLP[j] = Arrays.copyOf(conflictLP[j], n);
					}
					int[] nextConflictIndex = Arrays.copyOf(conflictIndex, numLadders);
					int removeIndex = 0;
					for (int j = 0; j < conflictIndex[i]; j++) {
						int m = 0;
						int found = 0;
						while (conflictLP[i][m] == -999 || found < j) {
							m = m + 1;
							if (conflictLP[i][m] != -999) {
								found = found + 1;
							}
						}
						long[] tempSolutionFill = Arrays.copyOf(solutionFill, n);
						int tempQuoteIndex = quoteIndex;
						int[][] tempConflictLP = new int[numLadders][n];
						for (int k = 0; k < numLadders; k++) {
							tempConflictLP[k] = Arrays.copyOf(conflictLP[k], n);
						}
						int[] tempConflictIndex = Arrays.copyOf(conflictIndex, numLadders);
						long replaceAmount = solutionFill[conflictLP[i][m]];
						int sameLP = 0;
						double specialUtil = -999999;
						double utility = -replaceAmount * quoteRates[conflictLP[i][m]];
	
						// Largest Quote not fully filled
						if (quoteSizes[quoteIndex] - solutionFill[quoteIndex] > 0 && quoteIndex != conflictLP[i][m]) {
							if (replaceAmount <= quoteSizes[quoteIndex] - solutionFill[quoteIndex]) {
								tempSolutionFill[quoteIndex] = tempSolutionFill[quoteIndex] + replaceAmount;
								utility = utility + replaceAmount * quoteRates[quoteIndex];
								tempQuoteIndex = quoteIndex;
								tempSolutionFill[conflictLP[i][m]] = 0;
							} else {
								if (quoteSizes[quoteIndex] != solutionFill[quoteIndex]) {
									utility = utility + (quoteSizes[quoteIndex] - solutionFill[quoteIndex])
											* quoteRates[quoteIndex];
									tempSolutionFill[quoteIndex] = tempSolutionFill[quoteIndex]
											+ (quoteSizes[quoteIndex] - solutionFill[quoteIndex]);
									replaceAmount = replaceAmount - (quoteSizes[quoteIndex] - solutionFill[quoteIndex]);
									tempSolutionFill[conflictLP[i][m]] = replaceAmount;
								}
	
								int k = quoteIndex + 1;
								int special_k = 0;
								long totalReplace = 0;
								long special_Replace = 0;
								if (k >= n) {
									outOfBounds = true;
								}
								while (replaceAmount != 0 && !outOfBounds) {
	
									// Special utility
									// double specialUtil = -999999;
									boolean hitSameLp = false;
									for (int lps = 0; lps < numLadders; lps++) {
										if (quoteLPs[k] == llp[lps]) {
											if (lps == i) {
												// Special case
												special_k = k;
												if (sameLP < 1) {
	
													for (int q = 0; q < n; q++) {
														if (tempConflictLP[i][q] != -999) {
															totalReplace = totalReplace
																	+ tempSolutionFill[tempConflictLP[i][q]];
														}
													}
													special_Replace = totalReplace;
													while (totalReplace != 0) {
														if (quoteSizes[special_k] < totalReplace && special_k < n) {
															special_k = special_k + 1;
															continue;
														} else {
															specialUtil = totalReplace * quoteRates[special_k]
																	- totalReplace * quoteRates[tempConflictLP[i][m]];
															totalReplace = 0;
															sameLP = sameLP + 1;
														}
													}
												}
												k = k + 1;
												hitSameLp = true;
												break;
											} else {
												int tempConflictLP_index = 0;
												while (tempConflictLP[lps][tempConflictLP_index] != -999) {
													tempConflictLP_index = tempConflictLP_index + 1;
												}
												tempConflictLP[lps][tempConflictLP_index] = k;
												tempConflictIndex[lps] = tempConflictIndex[lps] + 1;
											}
										}
	
									}
									if (hitSameLp) {
										continue;
									}
									if (quoteSizes[k] >= replaceAmount) {
										utility = utility + replaceAmount * quoteRates[k];
										if (specialUtil > utility) {
											int tempConflictLP_index = 0;
											int found2 = 0;
											while (found2 < tempConflictIndex[i]) {
												if (tempConflictLP[i][tempConflictLP_index] != -999) {
													tempSolutionFill[tempConflictLP[i][tempConflictLP_index]] = 0;
													tempConflictLP[i][tempConflictLP_index] = -999;
													found2 = found2 + 1;
												}
												tempConflictLP_index = tempConflictLP_index + 1;
											}
											tempSolutionFill[special_k] = special_Replace;
											tempConflictIndex[i] = 1;
											tempQuoteIndex = special_k;
	
										} else {
											tempSolutionFill[k] = replaceAmount;
											tempSolutionFill[conflictLP[i][m]] = 0;
											tempQuoteIndex = k;
										}
										replaceAmount = 0;
									} else {
										utility = utility + quoteSizes[k] * quoteRates[k];
										tempSolutionFill[k] = quoteSizes[k];
										replaceAmount = replaceAmount - quoteSizes[k];
										tempSolutionFill[conflictLP[i][m]] = replaceAmount;
									}
									k = k + 1;
									if (k >= n) {
										outOfBounds = true;
									}
								}
							}
						} else {
							int k = quoteIndex + 1;
							int special_k = 0;
							long totalReplace = 0;
							long special_Replace = 0;
							if (k >= n) {
								outOfBounds = true;
							}
							while (replaceAmount != 0 && !outOfBounds) {
	
								// Special utility
								// double specialUtil = 999999;
	
								boolean hitSameLp = false;
								for (int lps = 0; lps < numLadders; lps++) {
									if (quoteLPs[k] == llp[lps]) {
										if (lps == i) {
											// Sepcial case
											special_k = k;
											if (sameLP < 1) {
												for (int q = 0; q < n; q++) {
													if (tempConflictLP[i][q] != -999) {
														totalReplace = totalReplace
																+ tempSolutionFill[tempConflictLP[i][q]];
													}
												}
												special_Replace = totalReplace;
												while (totalReplace != 0 && special_k < n) {
													if (quoteSizes[special_k] < totalReplace) {
														special_k = special_k + 1;
														continue;
													} else {
														specialUtil = totalReplace * quoteRates[special_k]
																- totalReplace * quoteRates[conflictLP[i][m]];
														totalReplace = 0;
														sameLP = sameLP + 1;
													}
												}
											}
											k = k + 1;
											hitSameLp = true;
											break;
										} else {
											int tempConflictLP_index = 0;
											while (tempConflictLP[lps][tempConflictLP_index] != -999) {
												tempConflictLP_index = tempConflictLP_index + 1;
											}
											tempConflictLP[lps][tempConflictLP_index] = k;
											tempConflictIndex[lps] = tempConflictIndex[lps] + 1;
										}
									}
								}
								if (hitSameLp) {
									continue;
								}
								if (quoteSizes[k] >= replaceAmount) {
									utility = utility + replaceAmount * quoteRates[k];
									if (specialUtil > utility) {
										int tempConflictLP_index = 0;
										int found2 = 0;
										while (found2 < tempConflictIndex[i]) {
											if (tempConflictLP[i][tempConflictLP_index] != -999) {
												tempSolutionFill[tempConflictLP[i][tempConflictLP_index]] = 0;
												tempConflictLP[i][tempConflictLP_index] = -999;
												found2 = found2 + 1;
											}
											tempConflictLP_index = tempConflictLP_index + 1;
										}
										tempSolutionFill[special_k] = special_Replace;
										tempConflictIndex[i] = 1;
										tempQuoteIndex = special_k;
	
									} else {
										tempSolutionFill[k] = replaceAmount;
										tempSolutionFill[conflictLP[i][m]] = 0;
										tempQuoteIndex = k;
									}
									replaceAmount = 0;
								} else {
									utility = utility + quoteSizes[k] * quoteRates[k];
									tempSolutionFill[k] = quoteSizes[k];
									replaceAmount = replaceAmount - quoteSizes[k];
									tempSolutionFill[conflictLP[i][m]] = replaceAmount;
								}
								k = k + 1;
								if (k >= n) {
									outOfBounds = true;
								}
							}
						}
						// Get best solution for current conflict
						if (utility > maxUtil) {
							maxUtil = utility;
							nextSolutionFill = tempSolutionFill;
							nextConflictLP = tempConflictLP;
							nextConflictIndex = tempConflictIndex;
							nextQuoteIndex = tempQuoteIndex;
							removeIndex = m;
						}
	
					}
					// Updating current best solution for next conflict
					// resolution
					solutionFill = nextSolutionFill;
					conflictLP = nextConflictLP;
					conflictIndex = nextConflictIndex;
					quoteIndex = nextQuoteIndex;
					conflictLP[i][removeIndex] = -999;
					conflictIndex[i] = conflictIndex[i] - 1;
	
				}
	
				// Updating total conflict
				conflict = false;
				for (int j = 0; j < numLadders; j++) {
					conflict = (conflict || conflictIndex[j] > 1);
				}
				if (!conflict) {
					break;
				}
			}
		}
	
		// Return
		AggPxVo retObj = getVwapVo(solutionFill, quoteRates, quotes, finalPrecision, false);
		if (outOfBounds || n==0) {
			retObj = null;
		}
		
		return retObj;
	}

	private static AggPxVo getVwapVo(long[] fill, double[] rates, List<RawLiquidityVo> quotes, int finalPrecision, boolean roundUp) {
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
		AggPxVo VwapVo = new AggPxVo(DoubleFormatter.roundDouble(vwap/size, finalPrecision, roundUp ? RoundingMode.CEILING : RoundingMode.FLOOR), finalWeights, orgQuotes.toArray(new RawLiquidityVo[0]));

		return VwapVo;
	}
}
