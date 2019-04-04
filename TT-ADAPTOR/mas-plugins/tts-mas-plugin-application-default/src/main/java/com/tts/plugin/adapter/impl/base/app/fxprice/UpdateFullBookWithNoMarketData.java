package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;

public class UpdateFullBookWithNoMarketData implements BiFunction<Message.Builder, Object, Message.Builder> {
	private final static Logger logger = LoggerFactory.getLogger(UpdateFullBookWithNoMarketData.class);
	private final Map<String, IndividualInfoVo> sessionMarketSetup;

	public UpdateFullBookWithNoMarketData(Map<String, IndividualInfoVo> sessionMarketSetup2) {
		this.sessionMarketSetup = sessionMarketSetup2;
	}
	
	@Override
	public Message.Builder apply(Message.Builder _t, Object u) {
		FullBook.Builder t = (FullBook.Builder) _t;
		t.clearAskTicks();
		t.clearBidTicks();
		t.clearLatency();
		IndividualInfoVo config =	sessionMarketSetup.get(t.getSymbol());
		config.setLastRefresh(0L);
		long indicativeFlag = config.getIndicativeFlag();
		config.addIndicativeReason(IndicativeReason.MA_NoData);
		indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_NoData);
		t.setIndicativeFlag(indicativeFlag);
		t.clearQuoteRefId();
		t.clearTenors();
		logger.debug("Setting " + t.getSymbol() + " as MA_NoData");	
		return t;
	}
	
}
