package com.tts.mde.spot.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.vo.RawLiquidityVo;

public class MultiSrcLiquidityPool implements ILiquidityPool {
	
	private final List<ILiquidityPool> pools;
	
	private final int numberOfPools;
	private int expectedNumberOfData;

	public MultiSrcLiquidityPool(List<ILiquidityPool> pools ) {
		this.pools = Collections.unmodifiableList(pools);
		this.numberOfPools = pools.size();
		expectedNumberOfData = 15 * pools.size();
	}

	@Override
	public RawLiquidityVo[] getBidLqy(boolean ignoreAggSubspensionFlag) {
		RawLiquidityVo[] total = new RawLiquidityVo[expectedNumberOfData];
		int recordedPos = 0;
		for ( ILiquidityPool p : pools ) {
			RawLiquidityVo[] thisPool = p.getBidLqy(ignoreAggSubspensionFlag);
			if ( thisPool != null ) {
				int thisPoollength = thisPool.length;
				System.arraycopy(thisPool, 0	, total, recordedPos, thisPoollength);
				recordedPos += thisPoollength;
			}
		}
		RawLiquidityVo[] totalNew = new RawLiquidityVo[recordedPos];
	    System.arraycopy(total, 0, totalNew, 0, recordedPos);
	    return totalNew;	}

	@Override
	public RawLiquidityVo[] getAskLqy(boolean ignoreAggSubspensionFlag) {
		RawLiquidityVo[] total = new RawLiquidityVo[expectedNumberOfData];
		int recordedPos = 0;
		for ( ILiquidityPool p : pools ) {
			RawLiquidityVo[] thisPool = p.getAskLqy(ignoreAggSubspensionFlag);
			if ( thisPool != null ) {
				int thisPoollength = thisPool.length;
				System.arraycopy(thisPool, 0	, total, recordedPos, thisPoollength);
				recordedPos += thisPoollength;
			}
		}
		RawLiquidityVo[] totalNew = new RawLiquidityVo[recordedPos];
	    System.arraycopy(total, 0, totalNew, 0, recordedPos);
	    return totalNew;
	}

	@Override
	public List<QuoteUpdateStatVo> getQuoteUpdateCount() {
		ArrayList<QuoteUpdateStatVo> a = new ArrayList<>(numberOfPools); 
		for ( ILiquidityPool p : pools ) {
			a.addAll(p.getQuoteUpdateCount());
		}
		return a;
	}

}
