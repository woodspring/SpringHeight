package com.tts.mde.spot.impl;

import com.tts.mde.algo.IMDAggAlgo;
import com.tts.mde.algo.IMDAggAlgoProvider;

public class DefaultMDAggAlgoProvider implements IMDAggAlgoProvider {

	@Override
	public IMDAggAlgo getAggAlgoByName(String name) {
		if ( VwapByPx101Helper.NAME.equalsIgnoreCase(name)) {
			return new VwapByPx101Helper();
		} else if (VwapBruteForceAggAlgo.NAME.equalsIgnoreCase(name)) {
			return new VwapBruteForceAggAlgo();
		}
		return null;
	}

}
