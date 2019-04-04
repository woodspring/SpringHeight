package com.tts.plugin.adapter.impl.ykb;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSettingBuilder;
import com.tts.plugin.adapter.impl.base.DefaultMasIntegrationPlugin;
import com.tts.plugin.adapter.impl.ykb.dialect.YkbRequestDialectHelper;
import com.tts.plugin.adapter.impl.ykb.dialect.YkbResponseDialectHelper;
import com.tts.plugin.adapter.impl.ykb.routing.FixRoutingAgentFactoryImpl;
import com.tts.plugin.adapter.impl.ykb.setting.YkbFixSettingBuilderImpl;
import com.tts.util.AppContext;

public class YkbMasIntegrationPlugin extends DefaultMasIntegrationPlugin {
	
	public final static String NAME_YKB = "YKB";

	@Override
	public IRequestDialectHelper getRequestDialectHelper() {
		return new YkbRequestDialectHelper();
	}
	
	@Override
	public FixVersion getDefaultFixMsgVersion() {
		return FixVersion.FIX44;
	}

	@Override
	public IResponseDialectHelper getResponseDialectHelper() {
		return new YkbResponseDialectHelper(AppContext.getContext().getBean(FixApplicationProperties.class));
	}

	@Override
	public IFixSettingBuilder getFixSettingBuilder() {
		return new YkbFixSettingBuilderImpl();
	}

	@Override
	public IRoutingAgentFactory getRoutingAgentFactory() {
		return new FixRoutingAgentFactoryImpl();
	}

	@Override
	public String getName() {
		return NAME_YKB;
	}

	
}
