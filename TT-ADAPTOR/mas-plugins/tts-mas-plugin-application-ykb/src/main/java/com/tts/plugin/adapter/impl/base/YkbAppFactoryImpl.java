package com.tts.plugin.adapter.impl.base;

import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.app.IExternalInterfacingApp;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp;
import com.tts.plugin.adapter.feature.ITraderAdjustmentDependentApp;
import com.tts.plugin.adapter.impl.base.app.AbstractInterfacingAppFactory;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultEspOnlySpotAdapterAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultFileBasedForwardAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultReutersFwdPointsAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.IEspPriceRepoDependent;
import com.tts.plugin.adapter.impl.base.app.tradeexec.DirectTradeExecWithMKTPriceAppImpl;
import com.tts.plugin.adapter.impl.base.app.tradereport.DefaultTradeReportingAppImpl;
import com.tts.util.AppContext;

public class YkbAppFactoryImpl extends AbstractInterfacingAppFactory {
	
	private final FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
	private final ITraderAdjustmentApp traderAdjustmentApp = AppContext.getContext().getBean(ITraderAdjustmentApp.class);

	
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
			app =  new DefaultTradeReportingAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
					);			
		}else if ( appType == AppType.FCADAPTER) {
			String forwardAdapterType = p.getProperty("fwdadapter", "web");
			if (forwardAdapterType.equals("reuters")) {
				app = new DefaultReutersFwdPointsAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
						);
			} else 	if (forwardAdapterType.equals("file")) {
				app =  new DefaultFileBasedForwardAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
						);
			} else { 
				app = new YkbWebServiceBasedForwardCurveAdapterImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
					);
			}
		} else  if ( appType == AppType.FIXTRADEADAPTER) {
			app = new DirectTradeExecWithMKTPriceAppImpl(
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
		
		if ( app instanceof ITraderAdjustmentDependentApp) {
			((ITraderAdjustmentDependentApp) app).setTraderAdjustmentApp(traderAdjustmentApp);
		}
		return app;
	}

}
