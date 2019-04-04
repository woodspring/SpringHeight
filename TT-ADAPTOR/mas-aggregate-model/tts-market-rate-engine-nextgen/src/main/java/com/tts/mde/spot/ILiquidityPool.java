package com.tts.mde.spot;

import java.util.List;

import com.tts.mde.vo.RawLiquidityVo;

public interface ILiquidityPool {

	public RawLiquidityVo[] getBidLqy(boolean ignoreAggSubspensionFlag);
	public RawLiquidityVo[] getAskLqy(boolean ignoreAggSubspensionFlag);
	public List<QuoteUpdateStatVo> getQuoteUpdateCount();

	static class QuoteUpdateStatVo {
		final String providerNm;
		final long count;
		
		public QuoteUpdateStatVo(String providerNm, long count) {
			super();
			this.providerNm = providerNm;
			this.count = count;
		}

		public String getProviderNm() {
			return providerNm;
		}

		public long getCount() {
			return count;
		}
		
		
	}
}



