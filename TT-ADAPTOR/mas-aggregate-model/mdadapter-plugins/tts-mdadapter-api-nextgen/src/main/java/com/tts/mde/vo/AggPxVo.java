package com.tts.mde.vo;

import com.tts.mde.vo.RawLiquidityVo;

public class AggPxVo {

	private final double vwapPrice;
	private final long[] weights;
	private final RawLiquidityVo[] orgQuotes;
	
	public AggPxVo(double vwapPrice, long[] weights, RawLiquidityVo[] orgQuotes) {
		super();
		this.vwapPrice = vwapPrice;
		this.orgQuotes = orgQuotes;
		this.weights = weights;
	}
	
	public double getVwapPrice() {
		return vwapPrice;
	}

	public RawLiquidityVo[] getOrgQuotes() {
		return orgQuotes;
	}

	public long[] getWeights() {
		return weights;
	}

	public String shortDebugString() {
		StringBuilder sb= new StringBuilder(vwapPrice + "->");
		for ( int i = 0; i < orgQuotes.length; i++) {
			sb.append(orgQuotes[i].getLiquidityProviderSrc()).append(' ').append(weights[i]).append('/').append(orgQuotes[i].getSize()).append(' ').append(orgQuotes[i].getRate()).append("  ");
		}
		return sb.toString();
	}
}
