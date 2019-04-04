package com.tts.plugin.adapter.impl.base.app;

import com.tts.fix.support.IMkQfixApp;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.ISubscribingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;

public abstract class AbstractSubscribingApp extends AbstractMasApp implements
		ISubscribingApp {

	public AbstractSubscribingApp(
			IMkQfixApp qfixApp,
			ISchedulingWorker worker,
			SessionInfo sessionInfo, 
			IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint,
				iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
