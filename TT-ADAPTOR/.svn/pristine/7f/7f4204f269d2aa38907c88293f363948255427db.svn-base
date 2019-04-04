package com.tts.plugin.adapter.impl.cibc;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.app.IExternalInterfacingApp;
import com.tts.plugin.adapter.impl.base.app.AbstractInterfacingAppFactory;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultEspOnlySpotAdapterAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultFileBasedForwardAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultManualSpotPriceAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.IEspPriceRepoDependent;
import com.tts.plugin.adapter.impl.base.app.roe.CIBCROEAppImpl;
import com.tts.util.AppContext;

public class CIBCAppFactoryImpl extends AbstractInterfacingAppFactory {
	
	private final FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
	
	@Override
	public IExternalInterfacingApp createApplication(AppType appType) throws Exception {
		IExternalInterfacingApp app = null;
		if ( appType == AppType.SPOTADAPTER) {
			app =  new DefaultEspOnlySpotAdapterAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
					);
		} else if ( appType == AppType.TRADEREPORTADAPTER) {
			app =  new CIBCTradeReportingAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
					);			
		}else if ( appType == AppType.FCADAPTER) {
			String forwardAdapterType = p.getProperty("fwdadapter", "none");
			if (forwardAdapterType.equals("file")) {
				app =  new DefaultFileBasedForwardAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
						);
			} else { 
				return null;
			}
		} else if ( appType == AppType.FIXTRADEADAPTER) {
			app = new CIBCPreviouslyQuotedTradeAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(), 						
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin());

		} else if ( appType == AppType.QUOTEADAPTER ) {
			app = new CIBCQuoteAdapterImpl(
					getQfixApp(), 
					getWorker(), 
					getSessionInfo(), 						
					getPublishingEndpoint(),
					getCertifiedPublishingEndpoint(), 
					getIntegrationPlugin());
		} else if(appType == AppType.MANUALSPOTADAPTER)	{
			app = new DefaultManualSpotPriceAppImpl(
					getQfixApp(), 
					getWorker(), 
					getSessionInfo(), 
					getPublishingEndpoint(), 
					getCertifiedPublishingEndpoint(), 
					getIntegrationPlugin());
		} else if(appType == AppType.ROETRADEADAPTER)	{
			app =  new CIBCROEAppImpl(
					getQfixApp(), 
					getWorker(), 
					getSessionInfo(), 
					getPublishingEndpoint(), 
					getCertifiedPublishingEndpoint(), 
					getIntegrationPlugin());
		}
		
		if ( app instanceof IEspPriceRepoDependent ) {
			((IEspPriceRepoDependent) app).setEspRepo(getEspRepo());
		}	
		return app;
	}
}
