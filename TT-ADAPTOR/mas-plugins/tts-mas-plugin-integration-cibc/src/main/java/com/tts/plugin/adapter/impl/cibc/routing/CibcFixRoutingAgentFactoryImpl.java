package com.tts.plugin.adapter.impl.cibc.routing;

import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSetting;

public class CibcFixRoutingAgentFactoryImpl implements IRoutingAgentFactory {
	
	private IFixSetting fixSetting;

	@Override
	public IQfixRoutingAgent createRouteAgent() {
		return new CibcFixRoutingAgentImpl(fixSetting);
	}

	@Override
	public void setFixSetting(IFixSetting arg0) {
		this.fixSetting = arg0;
		
	}

}
