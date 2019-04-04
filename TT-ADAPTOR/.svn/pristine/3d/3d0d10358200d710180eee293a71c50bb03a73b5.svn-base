package com.tts.mde.algo;

import java.util.List;

import com.tts.mde.vo.ExecutionPlanVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.trade.TradeMessage.Transaction;

public abstract class AbstractMDPriceAndExceAlgo implements IMDPriceAndExceAlgo {

	@Override
	public FullBook.Builder validateVwap(FullBook.Builder fbBuilder, String marketMode) {
		//no validation
		return fbBuilder;
	}

	@Override
	public ExecutionPlanVo getExecutionPlan(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			String aggAlgoNm, String aggOrdType, double orgReqSize, double orgReqTargetRate, int finalPrecision,
			List<Transaction> executedTransactions) {
		if ( "RAW".equals(aggAlgoNm)) {
			return doRawExecution(currentQuotes, buySell, aggAlgoNm, aggOrdType, orgReqSize, orgReqTargetRate, finalPrecision, executedTransactions );
		} else if ( "CONSOLIDATED".equals(aggAlgoNm)) {
			return doConsolidatedExecution(currentQuotes, buySell, aggAlgoNm, aggOrdType, orgReqSize, orgReqTargetRate, finalPrecision, executedTransactions );
		} else if ( "VWAP".equals(aggAlgoNm)) {
			return doVwapExecution(currentQuotes, buySell, aggAlgoNm, aggOrdType, orgReqSize, orgReqTargetRate, finalPrecision, executedTransactions );

		}
		return null;
	}

	public abstract ExecutionPlanVo doVwapExecution(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			String aggAlgoNm, String aggOrdType, double orgReqSize, double orgReqTargetRate, int finalPrecision,
			List<Transaction> executedTransactions);

	public abstract ExecutionPlanVo doConsolidatedExecution(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			String aggAlgoNm, String aggOrdType, double orgReqSize, double orgReqTargetRate, int finalPrecision,
			List<Transaction> executedTransactions);
	

	public ExecutionPlanVo doRawExecution(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			String aggAlgoNm, String aggOrdType, double orgReqSize, double orgReqTargetRate, int finalPrecision,
			List<Transaction> executedTransactions) {
		if ( executedTransactions != null && executedTransactions.size() > 0 ) {
			//one iteration only, STOP
			return null;
		}
		return null;
	}

}
