package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.Map;
import java.util.function.BiFunction;

import com.google.protobuf.Message;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;

public class UpdateForwardCurveWithNoMarketData implements BiFunction<Message.Builder, Object, Message.Builder> {
	private final Map<String, IndividualInfoVo> sessionMarketSetup;

	public UpdateForwardCurveWithNoMarketData(Map<String, IndividualInfoVo> sessionMarketSetup2) {
		this.sessionMarketSetup = sessionMarketSetup2;
	}
	
	@Override
	public Message.Builder apply(Message.Builder _t, Object u) {
		ForwardCurve.Builder t = (ForwardCurve.Builder) _t;
		t.clearTenors();
		t.clearLatency();
		IndividualInfoVo config =	sessionMarketSetup.get(t.getSymbol());

		long indicativeFlag = config.getIndicativeFlag();
		config.addIndicativeReason(IndicativeReason.MA_NoData);
		indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_NoData);
		t.setIndicativeFlag(indicativeFlag);
		
		return t;
	}
	
}
