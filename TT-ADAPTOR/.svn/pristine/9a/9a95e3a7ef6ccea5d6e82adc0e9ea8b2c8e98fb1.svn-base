package com.tts.plugin.adapter.impl.ykb.routing;

import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSetting;

public class FixRoutingAgentFactoryImpl implements IRoutingAgentFactory {
	
	private IFixSetting fixSetting;

	@Override
	public void setFixSetting(
			IFixSetting fixSetting) {
		this.fixSetting = fixSetting;
	}

	@Override
	public IQfixRoutingAgent createRouteAgent() {
		return new YkbFixRoutingAgentImpl(fixSetting);
	}

}
