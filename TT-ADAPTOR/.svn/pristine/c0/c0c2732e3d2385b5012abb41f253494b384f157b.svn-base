package com.tts.mde.algo;

import java.util.List;

import com.tts.mde.vo.RawLiquidityVo;
import com.tts.mde.vo.AggPxVo;
import com.tts.message.market.FullBookStruct.FullBook;

public interface IMDAggAlgo {
	
	public enum AggType {
		RAW("RAW"),
		CONSOLIDATED("CONSOLIDATED"),
		VWAP("VWAP");
		
		private final String s;
		
		private AggType(final String s) {
			this.s = s;
		}
		
		public static AggType fromString(String s) {
			for (AggType a : values() ) {
				if ( a.s.startsWith(s)) {
					return a;
				}
			}
			return null;
		}

		public String getValue() {
			return s;
		}
	}

	public AggPxVo calculateOutPxBid(List<RawLiquidityVo> quotes, double size, int finalPrecision, String marketMode);
	
	public AggPxVo calculateOutPxAsk(List<RawLiquidityVo> quotes, double size, int finalPrecision, String marketMode);
	
	public FullBook.Builder validateVwap(FullBook.Builder fbBuilder,  String marketMode);

}
