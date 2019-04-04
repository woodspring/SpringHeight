package com.tts.plugin.adapter.api.factory;

import com.tts.fix.support.IMkQfixApp;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.app.IExternalInterfacingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.SessionInfo;

public interface IInterfacingAppFactory {

	void setQfixApp(IMkQfixApp qfixApp);

	void setSchedulingWorker(ISchedulingWorker worker);

	void setSessionInfo(SessionInfo sessionInfo);
	
	void setCertifiedPublishingEndpoint(ICertifiedPublishingEndpoint endpoint);
	
	void setPublishingEndpoint(IPublishingEndpoint endpoint);
	
	void setActiveIntegrationPlugin(IFixIntegrationPluginSpi plugin);
	
	void setEspRepo(IEspRepo<?> espRepo);

	IExternalInterfacingApp createApplication(AppType AppType) throws Exception;

}
