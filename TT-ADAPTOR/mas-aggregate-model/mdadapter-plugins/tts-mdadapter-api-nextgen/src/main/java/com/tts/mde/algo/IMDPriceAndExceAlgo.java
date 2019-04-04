package com.tts.mde.algo;

import java.util.List;

import com.tts.mde.vo.AggPxVo;
import com.tts.mde.vo.ExecutionPlanVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.trade.TradeMessage.Transaction;

public interface IMDPriceAndExceAlgo {

	public enum BuySellActionCd {
		SELL,
		BUY
	}
	
	public enum OrdTypeCd {
		PREVIOUSLY_QUOTED,
		LIMIT,
		MARKET,
	}
	
	public enum TimeInForceCd {
		FOK,
		IOC,
	}
	
	public String getName();
	
	/** 
	 * Get a price for display
	 * 
	 * @param quotes
	 * @param buySell
	 * @param size
	 * @param finalPrecision
	 * @param marketMode
	 * @return
	 */
	public AggPxVo getVwapPrice(List<RawLiquidityVo> quotes, BuySellActionCd buySell, double size, int finalPrecision, String marketMode);
	
	public FullBook.Builder validateVwap(FullBook.Builder fbBuilder,  String marketMode);

	/**
	 * Get Execution Plan for a request.
	 *
	 * 
	 * @param currentQuotes - current set of quote at the moment 
	 * @param buySell 
	 * @param aggAlgoNm
	 * @param aggOrdType
	 * @param orgReqSize
	 * @param orgReqTargetRate
	 * @param finalPrecision
	 * @param executedTransactions
	 * @return null for no reminding execution or an execution plan
	 */
	public ExecutionPlanVo getExecutionPlan(
			List<RawLiquidityVo> currentQuotes,
			BuySellActionCd buySell, 
			String aggAlgoNm, 
			String aggOrdType, //IOC, FOK, MKT
			double orgReqSize, 
			double orgReqTargetRate,
			int finalPrecision,
			List<Transaction> executedTransactions);
}
