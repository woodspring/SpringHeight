package com.tts.plugin.adapter.impl.base.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;
import com.tts.util.flag.IndicativeFlag;

public class DefaultSpotFbPublishValidator implements IPublishValidator<FullBook.Builder> {
	private final static Logger logger = LoggerFactory.getLogger(DefaultSpotFbPublishValidator.class);

	private static final String LOGMSG__FLAGGED_AS_NO_MARKET_DATA_AS_NO_ASK_TICK_OR_BID_TICK = ": Flagged as No Market Data as no ask tick or bid tick";
	
	@Override
	public FullBook.Builder validate(FullBook.Builder fbBuilder) {
		long indicativeFlag = fbBuilder.getIndicativeFlag();
		String symbol = fbBuilder.getSymbol();
		if ( fbBuilder.getAskTicksCount() ==0 && fbBuilder.getBidTicksCount() ==0 ) {
			indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
						IndicativeFlag.IndicativeReason.MA_NoData);		
			//logger.debug(symbol + LOGMSG__FLAGGED_AS_NO_MARKET_DATA_AS_NO_ASK_TICK_OR_BID_TICK);
		} 
		fbBuilder.setIndicativeFlag(indicativeFlag);
		return fbBuilder;
	}

}
