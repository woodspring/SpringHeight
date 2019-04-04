package com.tts.plugin.adapter.impl.base.app;

import com.tts.fix.support.IMkQfixApp;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.SessionInfo;

public abstract class AbstractInterfacingAppFactory implements IInterfacingAppFactory {
	
	private IMkQfixApp qfixApp;
		
	private ISchedulingWorker worker;
	
	private SessionInfo sessionInfo;

	private IPublishingEndpoint endpoint;
	
	private ICertifiedPublishingEndpoint certifiedEndpoint;
	
	private IFixIntegrationPluginSpi integrationPlugin;
	
	private IEspRepo<?> espRepo;

	@Override
	public void setCertifiedPublishingEndpoint(
			ICertifiedPublishingEndpoint endpoint) {
		this.certifiedEndpoint = endpoint;
	}

	@Override
	public void setPublishingEndpoint(IPublishingEndpoint endpoint) {
		this.endpoint = endpoint;	
	}

	
	@Override
	public void setQfixApp(IMkQfixApp qfixApp) {
		this.qfixApp = qfixApp;
	}

	@Override
	public void setSchedulingWorker(ISchedulingWorker worker) {
		this.worker = worker;
	}
	
	@Override
	public void setSessionInfo(SessionInfo sessionInfo) {
		this.sessionInfo = sessionInfo;
	}
	
	@Override
	public void setActiveIntegrationPlugin(IFixIntegrationPluginSpi plugin) {
		this.integrationPlugin = plugin;		
	}
	
	public ISchedulingWorker getWorker() {
		return worker;
	}

	public IMkQfixApp getQfixApp() {
		return qfixApp;
	}

	public SessionInfo getSessionInfo() {
		return sessionInfo;
	}

	public IPublishingEndpoint getPublishingEndpoint() {
		return endpoint;
	}

	public ICertifiedPublishingEndpoint getCertifiedPublishingEndpoint() {
		return certifiedEndpoint;
	}

	public IFixIntegrationPluginSpi getIntegrationPlugin() {
		return integrationPlugin;
	}

	public IEspRepo<?> getEspRepo() {
		return espRepo;
	}

	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo = espRepo;
	}
	
	

}
