package com.tts.mde.spot;

import com.tts.mde.spot.vo.MdConditionVo;
import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.mde.vo.RawLiquidityVo;

public interface IDirectLiquidityPool extends ILiquidityPool {

	void replaceAllQuotes(RawLiquidityVo[] bid, RawLiquidityVo[] ask);
	
	void setIsSubspendForAggregation(boolean isSubspendForAggregation);

	MdConditionVo validate();
	
	MdSubscriptionVo getSubscription();
}