package com.tts.plugin.adapter.impl.base.app.fxprice;

import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.FullBook.Builder;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp;
import com.tts.plugin.adapter.feature.ITraderAdjustmentDependentApp;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.SessionInfo;

public class DefaultEspOnlySpotAdapterAppImpl extends AbstractPublishingApp implements IEspPriceRepoDependent, ITraderAdjustmentDependentApp {
	
	public final static String NAME_TTS_SPOT_ADAPTER = "FIX_ADAPTER_SPOT";
	
	private final MasterPriceStore<FullBook.Builder> priceStores;
	
	private volatile RefreshRequesterSpot requester;
	private volatile FbPricePublishHandler pricePublishHandler;

	private IEspRepo<?> espRepo;

	private ITraderAdjustmentApp traderAdjustmentApp;

	private IFixIntegrationPluginSpi fixIntegrationPluginSpi;


	public DefaultEspOnlySpotAdapterAppImpl(
			IMkQfixApp qfixApp,
			ISchedulingWorker worker, 
			SessionInfo sessionInfo, 
			IPublishingEndpoint iPublishingEndpoint, 
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint, 
			IFixIntegrationPluginSpi fixIntegrationPluginSpi) throws Exception {
		super(
				qfixApp, 
				worker, 
				sessionInfo, 
				iPublishingEndpoint, 
				iCertifiedPublishingEndpoint,
				fixIntegrationPluginSpi
		);
		
		String[] symbols = getMarketDataSet().getAvailableSymbolsToArrayByType(AppType.SPOTADAPTER.getPublishingFormatType().toString());
		this.priceStores = new MasterPriceStore<FullBook.Builder>(symbols, AppType.SPOTADAPTER);
		this.fixIntegrationPluginSpi = fixIntegrationPluginSpi;
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}
	
	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxSpot;
	}
	
	@Override
	public String getName() {
		return NAME_TTS_SPOT_ADAPTER;
	}

	@Override
	public void atPublish(long masGlobalSeq) {
		if ( pricePublishHandler != null) {
			priceStores.applyToAll(pricePublishHandler, masGlobalSeq);
		}
	}
	
	@Override
	public void init() {
		
	}

	@Override
	public void start() {
		String[] symbols = getMarketDataSet().getAvailableSymbolsToArrayByType(AppType.SPOTADAPTER.getPublishingFormatType().toString());

		RefreshRequesterSpot requester = new RefreshRequesterSpot(
				getSessionInfo(), symbols, priceStores, getWorker(), getQfixApp(), getIntegrationPlugin(), espRepo);
		requester.init();
		
		this.requester = requester;
		IPublishValidator<FullBook.Builder> validator = (IPublishValidator<Builder>) fixIntegrationPluginSpi.getValidator(AppType.SPOTADAPTER.getPublishingFormatType());
		this.pricePublishHandler = new FbPricePublishHandler(getPublishingEndpoint(), getSessionInfo(), validator, traderAdjustmentApp );
	}

	@Override
	public void stop() {
		if ( this.requester != null ) {
			this.requester.destroy();		
		}
	}

	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo = espRepo;
		
	}
	
	protected IEspRepo<?> getEspRepo() {
		return this.espRepo;
	}

	@Override
	public void setTraderAdjustmentApp(ITraderAdjustmentApp traderAdjustmentApp) {
		this.traderAdjustmentApp = traderAdjustmentApp;
		
	}


	
}
