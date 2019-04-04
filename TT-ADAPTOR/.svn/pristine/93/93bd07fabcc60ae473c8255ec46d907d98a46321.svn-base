package com.tts.mde.spot.impl;

import java.util.List;

import com.tts.mde.algo.IMDAggAlgo;
import com.tts.mde.vo.AggPxVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.market.FullBookStruct.FullBook.Builder;

public class VwapBruteForceAggAlgo implements IMDAggAlgo {
	
	public static final String NAME = "VwapBruteForceAggAlgo";

	@Override
	public AggPxVo calculateOutPxBid(List<RawLiquidityVo> quotes, double size, int finalPrecision, String marketMode) {
		BruteForceSolver solver = new BruteForceSolver(true);
		return solver.solve(quotes, (long) size, true, finalPrecision);
	}

	@Override
	public AggPxVo calculateOutPxAsk(List<RawLiquidityVo> quotes, double size, int finalPrecision, String marketMode) {
		BruteForceSolver solver = new BruteForceSolver(true);
		return solver.solve(quotes, (long) size, false, finalPrecision);
	}

	@Override
	public Builder validateVwap(Builder fbBuilder, String marketMode) {
		return fbBuilder;
	}

}
