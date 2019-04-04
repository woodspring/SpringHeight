package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.ManualPriceFeed;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.ISubscribingApp;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.db.ManualPriceDataServices;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.flag.IndicativeFlag;
import com.tts.vo.RateNumVo;
import com.tts.vo.TickBookVo;
import com.tts.vo.TickVo;

public class DefaultManualSpotPriceAppImpl extends AbstractPublishingApp implements IEspPriceRepoDependent, ISubscribingApp {
	private static final Logger logger = LoggerFactory.getLogger(DefaultManualSpotPriceAppImpl.class);
	
	public final static String NAME_TTS_MANUAL_SPOT = "MANUAL_SPOT_RATE";
	
	private IEspRepo<?> espRepo;
	private final String[] _symbols;
	private Map<String, TickBookVo> mapManualPrice;
	
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	public DefaultManualSpotPriceAppImpl(IMkQfixApp qfixApp, ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint, ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) throws Exception {

		super(qfixApp, worker, sessionInfo, iPublishingEndpoint, iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
		
		this.mapManualPrice = new ConcurrentHashMap<>();
		this._symbols = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.MANUALSPOTADAPTER.getPublishingFormatType().toString());
		logger.info("Default Manual Spot Price Successfully Initialized.... " + _symbols.length);
	}
	
	@Override
	public void atPublish(long masGlobalSeq) {
		logger.trace("Publishing Manual SPOT Prices to the END Point...");
		
		for(String symbol: _symbols)	{

			final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(AppType.MANUALSPOTADAPTER.getPublishingFormatType().toString(), symbol);
			if((individualInfo == null) || (individualInfo.getTopic() == null))	{
				continue;
			}
			
			final String tradeDateStr = individualInfo.getTradeDateString();
			long indicativeFlag       = individualInfo.getIndicativeFlag();
			long indicativeSubStatus  = individualInfo.getIndicativeSubStatus();
			
			FullBook.Builder fullBook = FullBook.newBuilder();
			fullBook.setSymbol(symbol);
			
			TickBookVo tickBook = mapManualPrice.get(symbol);
			if(tickBook != null)	{
				for(TickVo tick: tickBook.getBidTicks())	{
					Tick.Builder bidTick = Tick.newBuilder();
					bidTick.setSize(tick.getSize());
					bidTick.setRate(tick.getRate().getValue());
					
					fullBook.addBidTicks(bidTick);
				}
				for(TickVo tick: tickBook.getAskTicks())	{
					Tick.Builder askTick = Tick.newBuilder();
					askTick.setSize(tick.getSize());
					askTick.setRate(tick.getRate().getValue());
					
					fullBook.addAskTicks(askTick);
				}
			}
			else	
				indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeFlag.IndicativeReason.MA_NoData);
			
			
			if(tradeDateStr == null) {
				indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
				indicativeSubStatus =IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus, IndicativeFlag.IndicativeSubStatus.CONFIGURATION_TradeDateNotDefined);
			} else {
				fullBook.setTradeDate(tradeDateStr);
			}
			
			fullBook.setTopOfBookIdx(0);
			fullBook.setIndicativeFlag(indicativeFlag);
			fullBook.setIndicativeSubFlag(indicativeSubStatus);
			
			fullBook.setTradingSession(getSessionInfo().getTradingSessionId());
			fullBook.getLatencyBuilder().setFaSendTimestamp(System.currentTimeMillis());
			fullBook.setSequence(masGlobalSeq);
			
			logger.trace("@@atPublish <<" + symbol + "--" + individualInfo.getTopic() + ">> " + TextFormat.shortDebugString(fullBook.build()));
			getPublishingEndpoint().publish(individualInfo.getTopic(), fullBook.build());
		}
	}

	@Override
	public PublishingFormatType getPublishingFormatType() {
		return(PublishingFormatType.FxSpotManual);
	}

	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return(ChangeTradingSessionBehavior.REFRESH);
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return(NAME_TTS_MANUAL_SPOT);
	}

	@Override
	public void start() {
		logger.info("Manual SPOT Price. Reading Inital Price Data...");
		List<TickBookVo> listPrices = getManualPriceDataServices().findAllManualSpotPrice();
		for(TickBookVo price: listPrices)	{
			mapManualPrice.put(price.getSymbol(), price);
		}
		logger.info("Manual SPOT Price. Prices Found for " + listPrices.size() + " Symbol(s).");
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo =  espRepo;
	}
	
	public IEspRepo<?> getEspRepo() {
		return(this.espRepo);
	}
	
	
	@Override
	public void onRequest(String topic, TtMsg message) {
		ManualPriceFeed manualPrice = null;
		
		try	{
			if(topic.equals(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_MANUAL_RATE_UPDATE_REQUEST))	{
				logger.info("Manual Price Feed Update Received @ " + System.currentTimeMillis());
				
				manualPrice = ManualPriceFeed.parseFrom(message.getParameters());
				logger.debug("Manual Price Update Message: " + TextFormat.printToString(manualPrice));
				
				List<FullBook> listPrices = manualPrice.getPriceFeedList();
				if((listPrices == null) || (listPrices.isEmpty()))	{
					logger.warn("Manul Price Feed Update Not Found...." + TextFormat.printToString(manualPrice));
					return;
				}
				
				for(FullBook priceFeed: listPrices)	{
					TickBookVo existingPrice = mapManualPrice.get(priceFeed.getSymbol());
					String newBidRate = priceFeed.getBidTicks(0).getRate();
					String newAskRate = priceFeed.getAskTicks(0).getRate();
					
					for(TickVo tick: existingPrice.getBidTicks())	{
						tick.setRate(RateNumVo.fromString(newBidRate));
					}
					
					for(TickVo tick: existingPrice.getAskTicks())	{
						tick.setRate(RateNumVo.fromString(newAskRate));
					}
					
					mapManualPrice.put(priceFeed.getSymbol(), existingPrice);
				}
				
				String msgFormat = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
				String msg = "Manual Price Update Request by " + manualPrice.getUserSessionInfo().getUserNm() + ". No. Of Currency Pairs: " + listPrices.size();
		    	monitorAgent.logWarnNotification("ManualPriceUpdate:onRequest", msgFormat,  MonitorConstant.FXADT.INFO_MANUAL_PRICE_FEED_UPDATE, AppUtils.getAppName(), msg);
				
				logger.info("Manual Price Feed Update Completed Successfully....");
			}
		}
		catch(InvalidProtocolBufferException ipbExp)	{
			logger.error("InvalidProtocolBufferException While Parsing Manual Price Feed. " + ipbExp.getMessage());
			logger.error("InvalidProtocolBufferException: ", ipbExp);
			ipbExp.printStackTrace();
		}
	}

	@Override
	public String[] getDesiredTopics() {
		return new String[] { IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_MANUAL_RATE_UPDATE_REQUEST };
	}
	
	
	public ManualPriceDataServices getManualPriceDataServices()	{
		return((ManualPriceDataServices)AppContext.getContext().getBean("manualPriceDataServices"));
	}
}
