package com.tts.plugin.adapter.impl.cibc;

import com.tts.plugin.adapter.api.IMasApplicationPluginSpi;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;
import com.tts.util.AppUtils;

public class CIBCMasApplicationPlugin implements IMasApplicationPluginSpi {

	public static final String NAME__CIBC = "CIBC";
	static final String CIBC_QUOTE_LOGGER_NAME = "CIBCQuoteLogger";
	static final String INTERNAL_QUOTE_LOGGER_NAME = "InternalQuoteLogger";


	static {
		AppUtils.createCustomRollingFileAppender("CibcQuote", CIBC_QUOTE_LOGGER_NAME, AppUtils.LOGLEVEL__INFO, "%d - %msg%n%ex");
		AppUtils.createCustomRollingFileAppender("InternalQuote", INTERNAL_QUOTE_LOGGER_NAME, AppUtils.LOGLEVEL__INFO, "%d - %msg%n%ex");
	}
	
	@Override
	public String getName() {
		return NAME__CIBC;
	}

	@Override
	public IInterfacingAppFactory getInterfacingAppFactory() {
		return new CIBCAppFactoryImpl();
	}

}
