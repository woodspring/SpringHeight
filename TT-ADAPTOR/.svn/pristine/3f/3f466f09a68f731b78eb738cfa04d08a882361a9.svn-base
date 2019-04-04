package com.tts.plugin.adapter.impl.base.app;

import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.app.IExternalInterfacingApp;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultEspOnlyFwdAdapterAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultEspOnlySpotAdapterAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultFileBasedForwardAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultManualSpotPriceAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.DefaultReutersFwdPointsAppImpl;
import com.tts.plugin.adapter.impl.base.app.fxprice.IEspPriceRepoDependent;
import com.tts.plugin.adapter.impl.base.app.interest.DefaultFileBasedInterestRateAppImpl;
import com.tts.plugin.adapter.impl.base.app.quote.QuoteAdapterImpl;
import com.tts.plugin.adapter.impl.base.app.roe.DefaultROEAppImpl;
import com.tts.plugin.adapter.impl.base.app.tradeexec.DirectTradeExecWithMKTPriceAppImpl;
import com.tts.plugin.adapter.impl.base.app.tradeexec.DirectTradeExecWithQuoteIdAppImpl;
import com.tts.plugin.adapter.impl.base.app.tradereport.DefaultTradeReportingAppImpl;
import com.tts.plugin.adapter.impl.base.app.volcurve.DefaultVolCurveAppImpl;
import com.tts.util.AppUtils;

public class DefaultAppFactoryImpl extends AbstractInterfacingAppFactory {

	@Override
	public  IExternalInterfacingApp createApplication(AppType appType) throws Exception {
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
			String forwardAdapterType = System.getenv("MARKET_FWDADAPTER");
			if(forwardAdapterType == null) {
				forwardAdapterType = "ESP";
			}
			
			if(forwardAdapterType.equals("REUTERS"))
				app = new DefaultReutersFwdPointsAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
						);
			else if(forwardAdapterType.equals("ESP"))
				app =  new DefaultEspOnlyFwdAdapterAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
						);
			else if(forwardAdapterType.equals("FILE"))
				app =  new DefaultFileBasedForwardAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(),
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin()
						);
			else {
				app = null;
			}
		}else if ( appType == AppType.IRADAPTER) {
			if ( AppUtils.getActiveEnvironment().contains("qa") 
					&& !AppUtils.getActiveEnvironment().equals("qa6")
					&& !AppUtils.getActiveEnvironment().equals("qa5") 					) {
				return null;
			}
			app =  new DefaultFileBasedInterestRateAppImpl(
					getQfixApp(), 
					getWorker(), 
					getSessionInfo(),
					getPublishingEndpoint(),
					getCertifiedPublishingEndpoint(), 
					getIntegrationPlugin()
				);			
		}
		if ( appType == AppType.FIXTRADEADAPTER) {
			if ( AppUtils.getActiveEnvironment().contains("ykb")) {				
				app = new DirectTradeExecWithMKTPriceAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(), 
						getPublishingEndpoint(), 
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin());
			}
			else	{
				app = new DirectTradeExecWithQuoteIdAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(), 						
						getPublishingEndpoint(),
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin());
			}
		} else if ( appType == AppType.QUOTEADAPTER ) {
			app = new QuoteAdapterImpl(
					getQfixApp(), 
					getWorker(), 
					getSessionInfo(), 						
					getPublishingEndpoint(),
					getCertifiedPublishingEndpoint(), 
					getIntegrationPlugin());
		} else if(appType == AppType.ROETRADEADAPTER)	{
			if (  System.getenv("MARKET_ROE_EXEC_TARGETS") != null) {
				app =  new DefaultROEAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(), 
						getPublishingEndpoint(), 
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin());
			}
		} else if(appType == AppType.MANUALSPOTADAPTER)	{
			boolean enableShoudler  = AppUtils.getActiveEnvironment().startsWith("atb") || "Y".equals(System.getenv("ENABLE_SHOULDER_CCY"));
			if ( enableShoudler ) {
				app = new DefaultManualSpotPriceAppImpl(
						getQfixApp(), 
						getWorker(), 
						getSessionInfo(), 
						getPublishingEndpoint(), 
						getCertifiedPublishingEndpoint(), 
						getIntegrationPlugin());
			}
		} else if(appType == AppType.VOLCURVEADAPTER)	{
			app = new DefaultVolCurveAppImpl(
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