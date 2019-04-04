package com.tts.plugin.adapter.impl.cibc;

import com.tts.plugin.adapter.api.app.IApp.PublishingFormatType;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSettingBuilder;
import com.tts.plugin.adapter.impl.base.DefaultMasIntegrationPlugin;
import com.tts.plugin.adapter.impl.cibc.dialect.CibcRequestDialectHelper;
import com.tts.plugin.adapter.impl.cibc.dialect.CibcResponseDialectHelper;
import com.tts.plugin.adapter.impl.cibc.routing.CibcFixRoutingAgentFactoryImpl;
import com.tts.plugin.adapter.impl.cibc.setting.CibcFixSettingBuilderImpl;
import com.tts.plugin.adapter.impl.cibc.validate.CIBCSpotPublishValidator;

public class CibcMasIntegrationPlugin extends DefaultMasIntegrationPlugin {
	
	public final static String NAME__CIBC = "CIBC";

	@Override
	public IRequestDialectHelper getRequestDialectHelper() {
		return new CibcRequestDialectHelper();
	}
	
	

	@Override
	public IResponseDialectHelper getResponseDialectHelper() {
		return new CibcResponseDialectHelper();
	}



	@Override
	public IFixSettingBuilder getFixSettingBuilder() {
		return new CibcFixSettingBuilderImpl();
	}

	@Override
	public IRoutingAgentFactory getRoutingAgentFactory() {
		return new CibcFixRoutingAgentFactoryImpl();
	}
	
	
	@Override
	public IPublishValidator<?> getValidator(PublishingFormatType formatType) {
		if ( formatType == PublishingFormatType.FxSpot) {
			return new CIBCSpotPublishValidator();
		}
		return super.getValidator(formatType);
	}

	@Override
	public String getName() {
		return NAME__CIBC;
	}

	
}
