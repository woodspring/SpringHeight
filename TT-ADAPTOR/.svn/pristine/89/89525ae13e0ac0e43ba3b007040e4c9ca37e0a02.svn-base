package com.tts.plugin.adapter.impl.base.route;

import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSetting;

public class DefaultQfixRoutingAgentFactoryImpl implements IRoutingAgentFactory {
	
	private IFixSetting fixSetting;

	@Override
	public void setFixSetting(
			IFixSetting additionalSetting) {
		this.fixSetting = additionalSetting;
	}

	@Override
	public IQfixRoutingAgent createRouteAgent() {
		return new DefaultQfixRoutingAgentImpl(fixSetting);
	}

}
