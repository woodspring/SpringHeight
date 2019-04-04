package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.fix.support.util.ConfigProperty;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.plugin.adapter.support.IReutersRFAMsgListener;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.ForwardCurveBuilderFullScanStructureDifferentiator;
import com.tts.service.biz.price.structure.diff.full.IFullScanStructureDifferentiator;
import com.tts.util.AppContext;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.util.flag.RateChangeIndFlag;
import com.tts.util.flag.RateChangeIndFlag.Change;


public class DefaultReutersFwdPointsAppImpl extends AbstractPublishingApp implements IEspPriceRepoDependent, IReutersRFAMsgListener {

	public final static String NAME_TTS_FWD_ADAPTER = "REUTERS_ADAPTER_FWD";
	private static final String KEY_SEPERATOR       = ".";
	private static Logger _rfaLogger                = LoggerFactory.getLogger("ReutersAppLogger");
	private static Logger _rfaMsgLogger             = LoggerFactory.getLogger("ReutersMsgLogger");
	private static Logger _rfaDfltLogger            = LoggerFactory.getLogger(DefaultReutersFwdPointsAppImpl.class);
	
	private IReutersApp objRFAAdapter = null;
	private Map<String, ForwardCurve.Builder> hashFwdPoint = null;
	private Hashtable<String, String> hashSubIdStore = null;
	
	private final long REUTERS_ALLOWED_RATE_TIME_FRAME;
	private final boolean checkNoData;
	
	private IEspRepo<?> espRepo;
	private volatile RefreshRequesterReutersFwdPoints requester;
	private final String[] _symbols;
	private final String[] _tenors;
	private final ISymbolIdMapper symbolIdMapper;
	private final PriceStructureRepo<ForwardCurve.Builder> structureRepo;
	private final IFullScanStructureDifferentiator<ForwardCurve.Builder> differentiator;
		
	public DefaultReutersFwdPointsAppImpl(IMkQfixApp qfixApp, ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint, ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) throws Exception {
		
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint, iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
		this.objRFAAdapter  = AppContext.getContext().getBean(IReutersApp.class);
		this.hashFwdPoint   = new ConcurrentHashMap<>();
		this.hashSubIdStore = new Hashtable<>();
		
		this._tenors        = new String[] {"ON", "TN"};
		this._symbols       = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.FCADAPTER.getPublishingFormatType().toString());
		
		this.REUTERS_ALLOWED_RATE_TIME_FRAME = TimeUnit.MINUTES.toMillis(this.objRFAAdapter.getMaxAllowedMsgTimeInterval());
				
		this.symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
		this.structureRepo  = new PriceStructureRepo<ForwardCurve.Builder>(symbolIdMapper);
		this.differentiator = new ForwardCurveBuilderFullScanStructureDifferentiator();
		this.checkNoData    = ConfigProperty.getSystemProperty("MARKET_FWDADAPTER_REUTERS_CHECK_INCOMING_TIMESTAMP", true);
	}

	@Override
	public synchronized void onFwdRICMessage(String SubscriptionID, ForwardCurve.Builder fwdPrice) {
		String symbol =  fwdPrice.getSymbol();
		String tenor  =  fwdPrice.getTenors(0).getName();
		
		final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(AppType.FCADAPTER.getPublishingFormatType().toString(), symbol);
		individualInfo.removeIndicativeReason(IndicativeReason.MA_NoData);
		
		_rfaLogger.debug("RFA Price Update Received. SubID: " + SubscriptionID + ", SYM: " + symbol + ", TENOR: " + tenor);
		hashFwdPoint.put((symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()), fwdPrice);		
	}

	@Override
	public void atPublish(long masGlobalSeq) {
		_rfaLogger.info("Publishing RFA Updted prices to the END Point...");
		
		try	{
			long currentTimeStamp = System.currentTimeMillis();
			for(String symbol: this._symbols)	{
				
				final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(AppType.FCADAPTER.getPublishingFormatType().toString(), symbol);
				if((individualInfo == null) || (individualInfo.getTopic() == null))	{
					continue;
				}
				
				
				ForwardCurve.Builder fwdPrice = ForwardCurve.newBuilder();
				fwdPrice.setSymbol(symbol)
						.setTradingSession(getSessionInfo().getTradingSessionId());
							
				
				long receiveTimeStamp = 0L;
				Latency newLatency    = null;
				for(String tenor: _tenors)	{
					ForwardCurve.Builder fwdPriceTenor = hashFwdPoint.get((symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()));
					if((fwdPriceTenor.hasLatency()) && (fwdPriceTenor.getLatency().getFaReceiveTimestamp() > receiveTimeStamp))	{
						receiveTimeStamp = fwdPriceTenor.getLatency().getFaReceiveTimestamp();
						newLatency       = fwdPriceTenor.getLatency();
					}
					
					if((fwdPriceTenor.getTenorsCount() > 0) )  {
						Tenor.Builder tenor0 = fwdPriceTenor.getTenorsBuilder(0);
						if ( tenor0.hasAskSwapPoints() && tenor0.getAskSwapPoints().trim().length() > 0
							&&	tenor0.hasBidSwapPoints() && tenor0.getBidSwapPoints().trim().length() > 0 ) {	
							if ( !checkNoData || (currentTimeStamp - fwdPriceTenor.getLatency().getFaReceiveTimestamp() < REUTERS_ALLOWED_RATE_TIME_FRAME)) {
								fwdPrice.addTenors(fwdPriceTenor.getTenors(0));
							}
						}
					}						
				}
				
				if(newLatency != null)	{
					fwdPrice.setLatency(Latency.newBuilder(newLatency));
					fwdPrice.setUpdateTimestamp(receiveTimeStamp);
				}
				
				final String tradeDateStr = individualInfo.getTradeDateString();
				long indicativeFlag       = individualInfo.getIndicativeFlag();
				long indicativeSubStatus  = individualInfo.getIndicativeSubStatus();
				
				if(tradeDateStr == null) {
					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
					indicativeSubStatus =IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus, IndicativeFlag.IndicativeSubStatus.CONFIGURATION_TradeDateNotDefined);
				} else {
					fwdPrice.setTradeDate(tradeDateStr);
				}
												
				long lastRefresh         = individualInfo.getLastRefresh();
				boolean isReceiveRefresh = (receiveTimeStamp - lastRefresh) > -1;
				long currentTime         = System.currentTimeMillis();
				
				
				if(symbol.equalsIgnoreCase("USDTRY"))	{
					_rfaDfltLogger.debug("@@USDTRY checkNoData: " + checkNoData + ", isReceiveRefresh: " + isReceiveRefresh + ", currentTime: " + currentTime + ", lastRefresh: "  + lastRefresh);
					_rfaDfltLogger.debug("@@USDTRY " + (checkNoData && !isReceiveRefresh && currentTime > lastRefresh + getSessionInfo().getTimeoutInterval()));
					_rfaDfltLogger.debug("@@USDTRY " + TextFormat.shortDebugString(fwdPrice.build()));
				}
				
				if (checkNoData && !isReceiveRefresh && currentTime > lastRefresh + getSessionInfo().getTimeoutInterval()) {
					indicativeFlag = IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE, IndicativeFlag.IndicativeReason.MA_NoData);
					_rfaLogger.debug("NO DATA @ DefaultReutersFwdPointsAppImpl for " + symbol + " ==>> " + lastRefresh + "--" + receiveTimeStamp + "--" + isReceiveRefresh);	
				}
				
				boolean isStructureChanged = structureRepo.hasStructureChanged(symbol, differentiator, fwdPrice);
				int rateChangeIndFlag      = RateChangeIndFlag.NO_CHANGE;
				if(isStructureChanged)	{
					rateChangeIndFlag = RateChangeIndFlag.setChanged(rateChangeIndFlag, Change.Structure);
				}
				fwdPrice.setRateChangeInd(rateChangeIndFlag);
				
				fwdPrice.setIndicativeFlag(indicativeFlag);
				fwdPrice.setIndicativeSubFlag(indicativeSubStatus);
				fwdPrice.getLatencyBuilder().setFaSendTimestamp(System.currentTimeMillis());
				fwdPrice.setSequence(masGlobalSeq);
				
				_rfaLogger.debug("@@atPublish <<" + symbol + "--" + individualInfo.getTopic() + ">> " + TextFormat.shortDebugString(fwdPrice.build()));
				getPublishingEndpoint().publish(individualInfo.getTopic(), fwdPrice.build());
			}
		}
		catch(Exception exp)	{
			_rfaLogger.error("DefaultReutersFwdPointsAppImpl Exception @ atPublish", exp);
		}
	}

	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}
	
	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxForwards;
	}
	
	@Override
	public String getName() {
		return NAME_TTS_FWD_ADAPTER;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	@Override
	public void start() {
		String subscriptionId = null;
		
		_rfaLogger.info("Sending Price Subscription REQ. to RFA. SYMBOLS: " + Arrays.toString(this._symbols));
		try	{
			for(String symbol: this._symbols)	{
				
				for(String tenor: this._tenors)	{
					final long subSendTime = System.currentTimeMillis();
					
					ForwardCurve.Builder fwdPriceBuilder = ForwardCurve.newBuilder();
					fwdPriceBuilder.setSymbol(symbol)
								   .getLatencyBuilder().setFaSendTimestamp(subSendTime)
													   .setFaReceiveTimestamp(0L);
					
					hashFwdPoint.put((symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()), fwdPriceBuilder);
					subscriptionId = null;
					subscriptionId = objRFAAdapter.subscribeToFwdRIC(symbol, tenor, this);
					hashSubIdStore.put((symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()), ((subscriptionId == null)? "-": subscriptionId));

					final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(AppType.FCADAPTER.getPublishingFormatType().toString(), symbol);
					individualInfo.setLastRefresh(subSendTime);
					individualInfo.setLastRequest(subSendTime);

				}
			}
		}
		catch(Exception exp)	{
			_rfaLogger.error("DefaultReutersFwdPointsAppImpl Exception @ start", exp);
		}
		
		RefreshRequesterReutersFwdPoints requester = new RefreshRequesterReutersFwdPoints(getSessionInfo(), 
																this._symbols, 
																this._tenors,
																this.objRFAAdapter, 
																getWorker(), 
																this);
		this.requester = requester;
		requester.init();
	}
	
	public ForwardCurve.Builder getLatests(String symbol, String tenor)	{
		return(hashFwdPoint.get(symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()));
	}

	@Override
	public void stop() {
		if(this.requester != null)
			this.requester.destory();
		
		hashSubIdStore.clear();
		hashFwdPoint.clear();
	}

	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo =  espRepo;
	}
	
	public IEspRepo<?> getEspRepo() {
		return(this.espRepo);
	}
	
	public String getSubscriptionID(String symbol, String tenor)	{
		String subId = null;
		
		subId = hashSubIdStore.get((symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()));
		subId = ((subId == null) || (subId.equalsIgnoreCase("-")))? null: subId;
		
		return(subId);
	}
	
	public void updateNewSubscriptionID(String symbol, String tenor, String subscriptionId)	{
		hashSubIdStore.put((symbol + KEY_SEPERATOR + tenor.trim().toUpperCase()), ((subscriptionId == null)? "-": subscriptionId));
	}
}