package com.tts.mde.plugin;

import java.math.BigDecimal;
import java.util.List;

import com.tts.mde.algo.AbstractMDPriceAndExceAlgo;
import com.tts.mde.spot.impl.VwapByPx101Helper;
import com.tts.mde.vo.AggPxVo;
import com.tts.mde.vo.ExecutionPlanVo;
import com.tts.mde.vo.ExecutionPlanVo.ExecutionDetailVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.vo.NumberVo;

public class VwapByPx101 extends AbstractMDPriceAndExceAlgo {
	private final static String VwapByPx101 = "VwapByPx101";
	private final VwapByPx101Helper helper = new VwapByPx101Helper();
	
	@Override
	public String getName() {
		return VwapByPx101;
	}

	@Override
	public AggPxVo getVwapPrice(List<RawLiquidityVo> quotes, BuySellActionCd buySell, double size, int finalPrecision,
			String marketMode) {
		if ( buySell == BuySellActionCd.SELL) {
			return helper.calculateOutPxBid(quotes, size, finalPrecision, marketMode);
		}
		return helper.calculateOutPxAsk(quotes, size, finalPrecision, marketMode);
	}

	@Override
	public ExecutionPlanVo doVwapExecution(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			String aggAlgoNm, String aggOrdType, double orgReqSize, double orgReqTargetRate, int finalPrecision,
			List<Transaction> executedTransactions) {
		
		if (executedTransactions == null || executedTransactions.size() == 0  ) {
			return doGetIterationExecPlan(currentQuotes, buySell, orgReqSize, orgReqTargetRate, finalPrecision, aggOrdType);
		}
		NumberVo execAmt = NumberVo.getInstance("0.00"), execVwap = NumberVo.getInstance("0.00000");
		for ( Transaction transaction : executedTransactions) {
			if (TransStateType.TRADE_PARTIALLY_DONE.equals(transaction.getStatus()) || TransStateType.TRADE_DONE.equals(transaction.getStatus())) {
				execAmt = execAmt.plus(transaction.getNearDateDetail().getCurrency1Amt());
				execVwap = execVwap.plus( NumberVo.getInstance(transaction.getNearDateDetail().getTradeRate()).multiply(transaction.getNearDateDetail().getCurrency1Amt()));
			}
		}
		execVwap = execVwap.divide(execAmt);
		NumberVo outstandingAmt = NumberVo.fromBigDecimal(BigDecimal.valueOf(orgReqSize)).minus(execAmt);
		NumberVo outstandingTargetRate = NumberVo.fromBigDecimal(BigDecimal.valueOf(orgReqTargetRate)).multiply(orgReqSize).minus(execVwap.multiply(execAmt)).divide(outstandingAmt);
		return doGetIterationExecPlan(currentQuotes, buySell, Double.parseDouble(outstandingAmt.getValue()), Double.parseDouble(outstandingTargetRate.getValue()), finalPrecision, aggOrdType);
	
	}

	@Override
	public ExecutionPlanVo doConsolidatedExecution(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			String aggAlgoNm, String aggOrdType, double orgReqSize, double orgReqTargetRate, int finalPrecision,
			List<Transaction> executedTransactions) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private ExecutionPlanVo doGetIterationExecPlan(List<RawLiquidityVo> currentQuotes, BuySellActionCd buySell,
			double outstandingQuantity, double outstandingTargetRate, int finalPrecision, String aggOrdType) {
		AggPxVo vwapRate = null;
		if ( buySell == BuySellActionCd.SELL) {
			vwapRate = helper.calculateOutPxBid(currentQuotes, outstandingQuantity, finalPrecision, null);
		} else {
			vwapRate = helper.calculateOutPxAsk(currentQuotes, outstandingQuantity, finalPrecision, null);	
		}
		
		ExecutionPlanVo execPlan = new ExecutionPlanVo();
		if ( vwapRate != null ) {
			int contributedQuoteTotalCount = vwapRate.getOrgQuotes().length;
			for ( int i = 0; i < contributedQuoteTotalCount; i++ ) {
				double quoteRate = vwapRate.getOrgQuotes()[i].getRate();
				if ( "IOC".equals(aggOrdType  )  ) {
					if ( buySell == BuySellActionCd.BUY && quoteRate <= outstandingTargetRate) {
						execPlan.addExecution(new ExecutionDetailVo(vwapRate.getWeights()[i], buySell, OrdTypeCd.LIMIT, TimeInForceCd.FOK, vwapRate.getOrgQuotes()[i]));
					} else if ( buySell == BuySellActionCd.SELL && quoteRate >= outstandingTargetRate) {
						execPlan.addExecution(new ExecutionDetailVo(vwapRate.getWeights()[i], buySell, OrdTypeCd.LIMIT, TimeInForceCd.FOK, vwapRate.getOrgQuotes()[i]));
					}
				} else {
					execPlan.addExecution(new ExecutionDetailVo(vwapRate.getWeights()[i], buySell, OrdTypeCd.LIMIT, TimeInForceCd.FOK, vwapRate.getOrgQuotes()[i]));
				}
			}
		}
		return execPlan;
	}
}
