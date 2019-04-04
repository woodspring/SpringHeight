package com.tts.plugin.adapter.impl.base;

import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.IApp.PublishingFormatType;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSettingBuilder;
import com.tts.plugin.adapter.impl.base.dialect.DefaultMarketRequestDialectHelper;
import com.tts.plugin.adapter.impl.base.dialect.DefaultResponseDialectHelper;
import com.tts.plugin.adapter.impl.base.route.DefaultQfixRoutingAgentFactoryImpl;
import com.tts.plugin.adapter.impl.base.setting.DefaultFixSettingBuilderImpl;
import com.tts.plugin.adapter.impl.base.validate.DefaultFwdCPublishValidator;
import com.tts.plugin.adapter.impl.base.validate.DefaultInterestRatePublishValidator;
import com.tts.plugin.adapter.impl.base.validate.DefaultSpotFbPublishValidator;

public class DefaultMasIntegrationPlugin implements IFixIntegrationPluginSpi {
	
	public final static String NAME__TTS_DEFAULT = "TTS_DEFAULT";

	@Override
	public FixVersion getDefaultFixMsgVersion() {
		return FixVersion.FIX50;
	}

	@Override
	public IRequestDialectHelper getRequestDialectHelper() {
		return new DefaultMarketRequestDialectHelper();
	}
	
	@Override
	public IResponseDialectHelper getResponseDialectHelper() {
		return new DefaultResponseDialectHelper();
	}
	
	@Override
	public String getName() {
		return NAME__TTS_DEFAULT;
	}

	@Override
	public IFixSettingBuilder getFixSettingBuilder() {
		return new DefaultFixSettingBuilderImpl();
	}
	
	@Override
	public IRoutingAgentFactory getRoutingAgentFactory() {
		return new DefaultQfixRoutingAgentFactoryImpl();
	}

	@Override
	public IPublishValidator<?> getValidator(PublishingFormatType formatType) {
		if ( formatType == PublishingFormatType.FxSpot) {
			return new DefaultSpotFbPublishValidator();
		} else if (formatType == PublishingFormatType.FxForwards ){
			return new DefaultFwdCPublishValidator();
		} else if ( formatType == PublishingFormatType.FxInterestRate) {
			return new DefaultInterestRatePublishValidator();
		}
		
		return null;
	}


}
