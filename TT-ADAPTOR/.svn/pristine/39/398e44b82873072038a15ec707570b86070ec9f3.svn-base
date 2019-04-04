package com.tts.mde.vo;

import java.util.ArrayList;
import java.util.List;

import com.tts.mde.algo.IMDPriceAndExceAlgo.BuySellActionCd;
import com.tts.mde.algo.IMDPriceAndExceAlgo.OrdTypeCd;
import com.tts.mde.algo.IMDPriceAndExceAlgo.TimeInForceCd;

public class ExecutionPlanVo {

	private final List<ExecutionDetailVo> executions = new ArrayList<>(5);
	
	public void addExecution(ExecutionDetailVo executionDetail) {
		this.executions.add(executionDetail);
	}
	
	public List<ExecutionDetailVo> getPlannedExecutions() {
		return this.executions;
	}
	
	public static class ExecutionDetailVo {
		private double size;
		private BuySellActionCd buySellAction;
		private OrdTypeCd ordType;
		private TimeInForceCd timeInForce;
		private RawLiquidityVo quote;

		public ExecutionDetailVo(double size, BuySellActionCd buySellAction, OrdTypeCd ordType,
				TimeInForceCd timeInForce, RawLiquidityVo quote) {
			super();
			this.size = size;
			this.buySellAction = buySellAction;
			this.ordType = ordType;
			this.timeInForce = timeInForce;
			this.quote = quote;
		}
		
		public double getSize() {
			return size;
		}
		public void setSize(double size) {
			this.size = size;
		}
		public RawLiquidityVo getQuote() {
			return quote;
		}
		public void setQuote(RawLiquidityVo quote) {
			this.quote = quote;
		}
		public BuySellActionCd getBuySellAction() {
			return buySellAction;
		}
		public void setBuySellAction(BuySellActionCd buySellAction) {
			this.buySellAction = buySellAction;
		}
		public OrdTypeCd getOrdType() {
			return ordType;
		}
		public void setOrdType(OrdTypeCd ordType) {
			this.ordType = ordType;
		}
		public TimeInForceCd getTimeInForce() {
			return timeInForce;
		}
		public void setTimeInForce(TimeInForceCd timeInForce) {
			this.timeInForce = timeInForce;
		}
	}
}
