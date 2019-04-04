package com.tts.plugin.adapter.impl.base.app;

import com.tts.fix.support.IMkQfixApp;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.MarketDatasetVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;

public class AbstractMasApp {

	private final IMkQfixApp qfixApp;
		
	private final ISchedulingWorker worker;
	
	private final SessionInfo sessionInfo;
	
	private final IPublishingEndpoint publishingEndpoint;
	
	private final ICertifiedPublishingEndpoint certifiedPublishingEndpoint;

	private final IFixIntegrationPluginSpi integrationPlugin;
	
	public AbstractMasApp(
				IMkQfixApp qfixApp,
				ISchedulingWorker worker,
				SessionInfo sessionInfo, 
				IPublishingEndpoint iPublishingEndpoint, 
				ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint, 
				IFixIntegrationPluginSpi IFixIntegrationPluginSpi
			) {
		super();
		this.qfixApp = qfixApp;
		this.worker = worker;
		this.sessionInfo = sessionInfo;
		this.publishingEndpoint = iPublishingEndpoint;
		this.certifiedPublishingEndpoint = iCertifiedPublishingEndpoint;
		this.integrationPlugin = IFixIntegrationPluginSpi;
	}

	public IMkQfixApp getQfixApp() {
		return qfixApp;
	}

	public MarketDatasetVo getMarketDataSet() {
		return sessionInfo.getMarketDataset();
	}

	public ISchedulingWorker getWorker() {
		return worker;
	}

	public SessionInfo getSessionInfo() {
		return sessionInfo;
	}

	public IPublishingEndpoint getPublishingEndpoint() {
		return publishingEndpoint;
	}

	public ICertifiedPublishingEndpoint getCertifiedPublishingEndpoint() {
		return certifiedPublishingEndpoint;
	}

	public IFixIntegrationPluginSpi getIntegrationPlugin() {
		return integrationPlugin;
	}
	
	
	
}
