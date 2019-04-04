package com.tts.plugin.adapter.impl.cibc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.entity.system.SystemProperty;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.constant.Constants;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam.QuoteDirection;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.IPublishingApp;
import com.tts.plugin.adapter.api.app.ISubscribingApp;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.AbstractMasApp;
import com.tts.plugin.adapter.impl.base.app.QuoteStackRepo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IFxCalendarBizServiceApi;
import com.tts.plugin.adapter.support.IInstrumentDetailProperties;
import com.tts.plugin.adapter.support.IInstrumentDetailProvider;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.price.PriceUtil;
import com.tts.service.db.RuntimeDataService;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.util.constant.SysProperty;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;

public class CIBCQuoteAdapterImpl extends AbstractMasApp implements IPublishingApp, ISubscribingApp {
	private final static int MAX_SIMULTANEOUS_QUOTE = 2000;
	private final static long DEFAULT_MAX_TIME_OPTION_DURATION = 90l;
	private final static AtomicInteger reqId = new AtomicInteger((int) (AppUtils.getSequencePrefix() /10000));
	
	protected final static Logger QUOTE_LOGGER = LoggerFactory.getLogger(CIBCMasApplicationPlugin.CIBC_QUOTE_LOGGER_NAME);
	protected final static Logger INTERNAL_QUOTE_LOGGER = LoggerFactory.getLogger(CIBCMasApplicationPlugin.INTERNAL_QUOTE_LOGGER_NAME);

	protected final static Logger logger = LoggerFactory.getLogger(CIBCQuoteAdapterImpl.class);
	private final QuoteSubscriptionNode[] nodes = new QuoteSubscriptionNode[MAX_SIMULTANEOUS_QUOTE];

	private final IPublishingEndpoint iPublishingEndpoint;
	private final IResponseDialectHelper dialect;
	private final QuoteStackRepo quoteStackRepo;
	protected final IFxCalendarBizServiceApi fxCalendarBizServiceApi;
	private final IFxCalendarBizService fxCalendarBizService;
	private final Map<String, String> symbolsAllowPassCutOff;
	
	protected final IInstrumentDetailProvider instrumentDetailProvider;
	private final ISchedulingWorker worker;
	private final int maxTimeOptionDuration;
	private volatile int currentMaxQuoteSubscriptionIdx = 0;

	public CIBCQuoteAdapterImpl(IMkQfixApp qfixApp, ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint, ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint, iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
		this.dialect = IFixIntegrationPluginSpi.getResponseDialectHelper();
		this.iPublishingEndpoint = iPublishingEndpoint;
		this.quoteStackRepo = AppContext.getContext().getBean(QuoteStackRepo.class);
		this.fxCalendarBizServiceApi = AppContext.getContext().getBean(IFxCalendarBizServiceApi.class);
		this.instrumentDetailProvider = AppContext.getContext().getBean(IInstrumentDetailProvider.class);
		this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);

		this.worker =  worker;
		
		for ( int i = 0; i < nodes.length; i++ ) {
			this.nodes[i] = new QuoteSubscriptionNode();
		}
		
		Long _maxTimeOptionDuration = RuntimeDataService.getLongRunTimeData(SysProperty.GroupCd.FXPARMS, SysProperty.Key1.FX_TIME_OPTION, SysProperty.Key2.FX_TIME_OPTION_MAX_DURATION);
		if (_maxTimeOptionDuration == null) {
			_maxTimeOptionDuration = DEFAULT_MAX_TIME_OPTION_DURATION;
		} 
		this.maxTimeOptionDuration = _maxTimeOptionDuration.intValue();
		//	Changes for S-02108
		HashMap<String, String> _symbolsAllowPassCutOff = new HashMap<String, String>();
		List<SystemProperty> listAllowPassCutOff = RuntimeDataService.getRunTimeDataList(SysProperty.GroupCd.FXPARMS, SysProperty.Key1.ALLOW_TOD_PASS_CUTOFF_TIME, null);
		for(SystemProperty sysProp: listAllowPassCutOff)	{
			if((sysProp.getKey2() != null) && (sysProp.getValue() != null) && (sysProp.getValue().trim().equalsIgnoreCase("Y")))
				_symbolsAllowPassCutOff.put(sysProp.getKey2().trim(), "");
		}
		
		List<SystemProperty> listCutOffTime      = RuntimeDataService.getRunTimeDataList(SysProperty.GroupCd.FXPARMS, SysProperty.Key1.TOD_CUTOFF_TIME, null);
		for(SystemProperty sysProp: listCutOffTime)	{
			if((sysProp.getKey2() != null) && (_symbolsAllowPassCutOff.containsKey(sysProp.getKey2().trim())))
				if ( _symbolsAllowPassCutOff.containsKey(sysProp.getKey2().trim())) {
					_symbolsAllowPassCutOff.put(sysProp.getKey2().trim(), sysProp.getValue().trim());
				}
		}
		this.symbolsAllowPassCutOff = Collections.unmodifiableMap(_symbolsAllowPassCutOff);
		
		StringBuilder sb= new StringBuilder();
		for ( Entry<String, String> e : _symbolsAllowPassCutOff.entrySet()) {
			sb.append(e.getKey() ).append(' ' ).append(e.getValue()).append('\n');
		}
		logger.info(sb.toString());
	}

	@Override
	public String getName() {
		return "CIBCQuoteAdapter";
	}
	
	@Override
	public void init() {
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.NO_CHANGE;
	}
	
	@Override
	public void onRequest(String topic, TtMsg ttMsg) {
		long currentTime = System.currentTimeMillis();
				
		if ( topic.indexOf("REQUEST") > 0 ) {
			try {
				PriceSubscriptionRequest priceSubscriptionRequest = PriceSubscriptionRequest.parseFrom(ttMsg.getParameters());
				logger.debug(String.format("Received message from topic<%s>: %s", topic, TextFormat.shortDebugString(priceSubscriptionRequest)));

				QuoteParam quoteParam = priceSubscriptionRequest.getQuoteParam();
				String tenor1 = null;
				String tenor2 = null;
				String ovrSettleDate1 = null;
				
				QuoteSide side = null;
				long expiryTime = -1L;
				
				String quotingProduct = quoteParam.getProduct();
				if ( Constants.ProductType.FXSPOT.equals(quotingProduct )) {
					tenor1 = TenorVo.NOTATION_SPOT;
				} else {
					try {
						TenorVo.Builder t = new TenorVo.Builder();
						t.setPeriodCd(quoteParam.getNearDateDetail().getPeriodCd());
						t.setValue(Integer.parseInt(quoteParam.getNearDateDetail().getPeriodValue()));
						tenor1 = t.toString(); 
					} catch (Exception e) {
						try {
							TenorVo.Builder t = new TenorVo.Builder();
							t.setPeriodCd(quoteParam.getNearDateDetail().getPeriodCd());
							t.setValue(-1);
							tenor1 = t.toString(); 
						} catch (Exception e1) {
							
						}
					}
					
					if ( Constants.ProductType.FXTIMEOPTION.equals(quotingProduct ) 
							|| Constants.ProductType.FXSWAP.equals(quotingProduct) ) {
						try {
							TenorVo.Builder t = new TenorVo.Builder();
							t.setPeriodCd(quoteParam.getFarDateDetail().getPeriodCd());
							t.setValue(Integer.parseInt(quoteParam.getFarDateDetail().getPeriodValue()));
							tenor2 = t.toString(); 
						} catch (Exception e) {
							try {
								TenorVo.Builder t = new TenorVo.Builder();
								t.setPeriodCd(quoteParam.getFarDateDetail().getPeriodCd());
								t.setValue(-1);
								tenor2 = t.toString(); 
							} catch (Exception e1) {
								
							}
						}
					}
				}
				
				if ( QuoteDirection.BOTH.equals(quoteParam.getQuoteDirection()) ) {
					side = QuoteSide.BOTH;
				} else if ( QuoteDirection.BUY.equals(quoteParam.getQuoteDirection()) ) { 
					side = QuoteSide.BUY;
				} else if (	QuoteDirection.SELL.equals(quoteParam.getQuoteDirection()) ) { 
					side = QuoteSide.SELL;
				} else if ( QuoteDirection.BUY_AND_SELL.equals(quoteParam.getQuoteDirection())) {
					side = QuoteSide.SWAP__BUY_AND_SELL;
				} else if ( QuoteDirection.SELL_AND_BUY.equals(quoteParam.getQuoteDirection())) {
					side = QuoteSide.SWAP__SELL_AND_BUY;
				}
					
					
				if (quoteParam.hasQuoteDuration() ) {
					expiryTime = quoteParam.getQuoteDuration();
					expiryTime += 10;
				}
				
				BigDecimal bd = new BigDecimal(quoteParam.getSize());
				BigDecimal bdFar = null;
				if ( quoteParam.hasSizeFar() ) {
					bdFar = new BigDecimal(quoteParam.getSizeFar());
				}

				String settleDate1 = quoteParam.getNearDateDetail().getActualDate();
				String settleDate2 = null;
				String currencyPair = quoteParam.getCurrencyPair();	
				QuoteSubscriptionNode quoteServiceNode = findAndReserveAvailableServicingNode();
				
				FullBook.Builder fbBuilder = quoteServiceNode.getFbBuilder();
				fbBuilder.clearAskTicks();
				fbBuilder.clearBidTicks();
				fbBuilder.clearTenors();
				fbBuilder.setSymbol( currencyPair);

				quoteServiceNode.setOutboundTopic(priceSubscriptionRequest.getTopic());
				quoteServiceNode.setExpireTime(currentTime + expiryTime * ChronologyUtil.MILLIS_IN_SECOND);
				quoteServiceNode.setInstrumentDetail(instrumentDetailProvider.getInstrumentDetail(currencyPair));
				quoteServiceNode.setQuote1(null);
				quoteServiceNode.setQuote2(null);

				if ( settleDate1 == null && tenor1 != null ) {
					settleDate1 = fxCalendarBizServiceApi.getForwardValueDate(currencyPair, tenor1);
					if ( TenorVo.NOTATION_SPOT.equals(tenor1)) {
						//settleDate = "0";
					}
				}
				if ( Constants.ProductType.FXTIMEOPTION.equals(quotingProduct)
						|| Constants.ProductType.FXSWAP.equals(quotingProduct)  ) {
					settleDate2 = quoteParam.getFarDateDetail().getActualDate();
				
					if ( settleDate2 == null && tenor1 != null ) {
						settleDate2 = fxCalendarBizServiceApi.getForwardValueDate(currencyPair, tenor2);
						if ( TenorVo.NOTATION_SPOT.equals(tenor1)) {
							//settleDate = "0";
						}
					}
				}
				
				//	Changes for S-02108
				if(!Constants.ProductType.FXTIMEOPTION.equals(quotingProduct ) 
						&& !Constants.ProductType.FXSWAP.equals(quotingProduct ) 
						&& TenorVo.NOTATION_TODAY.equals(tenor1)
						&& performPassCutOffTimeValidation(currentTime, currencyPair))	{
					tenor1 = TenorVo.NOTATION_SPOT;
					ovrSettleDate1 = settleDate1;

					LocalDate sp = fxCalendarBizService.getSpotValueDate(currencyPair, LocalDate.now(), PricingConventionConstants.INTERBANK);
					settleDate1 = ChronologyUtil.getDateString(sp);
				}
				quoteServiceNode.setProduct(quotingProduct);
				quoteServiceNode.setSettleDate1(settleDate1);
				quoteServiceNode.setSettleDate2(settleDate2);
				quoteServiceNode.setOvrSettleDate1(ovrSettleDate1);
				quoteServiceNode.setQuoteSide(side);
				String notionalCurrency;
				if ( quoteParam.hasNotionalCurrency() ) {
					notionalCurrency = quoteParam.getNotionalCurrency();
				} else {
					notionalCurrency = quoteParam.getCurrencyPair().substring(0, 3);
				}
								
				String qReqId = Integer.toString(reqId.getAndIncrement());
				if ( Constants.ProductType.FXSWAP.equals(quotingProduct ) ) {
					qReqId = "FXSWAP" + settleDate1 + settleDate2 + qReqId;  
				} else if ( Constants.ProductType.FXTIMEOPTION.equals(quotingProduct ) ) {
					qReqId = "TIMEOP" + settleDate1 + settleDate2 + qReqId;  
				} else {
					qReqId = "SPOUTRT" + settleDate1 + qReqId;  
				}
				
				IFixListener quoteListener = 
						new SubscriptionNodeFixUpdater(
								qReqId,
								quoteServiceNode, 
								dialect,
								quoteStackRepo, 
								worker,
								currencyPair);

				int duration = 0;
				if ( Constants.ProductType.FXTIMEOPTION.equals(quotingProduct) ) {
					LocalDate ld1 = ChronologyUtil.getLocalDateFromString(settleDate1);
					LocalDate ld2 = ChronologyUtil.getLocalDateFromString(settleDate2);
					duration = ChronologyUtil.daysBetween(ld1, ld2);
				}
				
				if (  Constants.ProductType.FXTIMEOPTION.equals(quotingProduct)  && duration > maxTimeOptionDuration) {
					logger.warn("TimeOption quote request has a period greater than 90 days, not sending to CIBC, " + settleDate1 + " " + settleDate2);
				} else {
					String requestId1 = null;
					if ( Constants.ProductType.FXSWAP.equals(quotingProduct)) {
						if ( bdFar == null ) {
							if ( quoteParam.hasSizeFar() ) {
								System.out.println( quoteParam.getSizeFar());
							}
							bdFar = bd;
						}
						requestId1 = getQfixApp().sendRfsRequestForSwap(
								bd.longValue(), 
								currencyPair,
								notionalCurrency,
								tenor1,
								settleDate1, 
								side, 
								bdFar.longValue(),
								tenor2,
								settleDate2,
								expiryTime, 
								quoteListener,		
								AppType.QUOTEADAPTER);
					} else {
						requestId1 = getQfixApp().sendRfsRequest(
								bd.longValue(), 
								currencyPair,
								notionalCurrency,
								tenor1,
								settleDate1, 
								side, 
								expiryTime, 
								quoteListener,		
								AppType.QUOTEADAPTER);
					}
					String requestId2 = null;
					
					if ( Constants.ProductType.FXTIMEOPTION.equals(quotingProduct)) {
						requestId2 = getQfixApp().sendRfsRequest(
								bd.longValue(), 
								currencyPair,
								notionalCurrency,
								tenor2,
								settleDate2, 
								side, 
								expiryTime, 
								quoteListener,									
								AppType.QUOTEADAPTER);
					}
					logger.debug("QuoteRequest(s) sent, requestId1 = "  + requestId1 +  " requestId2 = "  + requestId2);
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		} else if (topic.indexOf("AMEND") > 0) {
			try {
				PriceSubscriptionRequest priceSubscriptionRequest = PriceSubscriptionRequest.parseFrom(ttMsg.getParameters());
				logger.debug(String.format("Received message from topic<%s>: %s", topic, TextFormat.shortDebugString(priceSubscriptionRequest)));
				String tenor = null;

				QuoteParam quoteParam = priceSubscriptionRequest.getQuoteParam();
				if ( Constants.ProductType.FXSPOT.equals(quoteParam.getProduct() )) {
					tenor = TenorVo.NOTATION_SPOT;
				} else {
					String periodCode = quoteParam.getNearDateDetail().getPeriodCd();
					String periodValue = quoteParam.getNearDateDetail().getPeriodValue();
					if ( "-1".equals(periodValue) ) {
						tenor = periodCode;
					} else {
						tenor = periodValue + periodCode;
					}
				}
				getQfixApp().cancelEspRequest(
						quoteParam.getCurrencyPair(), 
						tenor,
						quoteParam.getNearDateDetail().getActualDate(), 
						priceSubscriptionRequest.getRequestId(),
						AppType.QUOTEADAPTER);

			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public String[] getDesiredTopics() {
		return new String[] { IEventMessageTypeConstant.Market.TOPIC_QUOTE_ALL };
	}

	@Override
	public void atPublish(long masGlobalSeq) {
		long publishTime = System.currentTimeMillis();
		for (int i =0 ;  i <= currentMaxQuoteSubscriptionIdx; i++ ) {
			QuoteSubscriptionNode quoteSubscriptionNode = nodes[i];

			try {
				if (quoteSubscriptionNode.isInUse() && quoteSubscriptionNode.isFirstPriceActived()) {
					FullBook.Builder fbBuilder = FullBook.newBuilder(quoteSubscriptionNode.getFbBuilder().build());
					LocalDate dt = fxCalendarBizService.getCurrentBusinessDay(fbBuilder.getSymbol());

					fbBuilder.setSequence(masGlobalSeq);
					fbBuilder.setTradeDate(ChronologyUtil.getDateString(dt));
					fbBuilder.getLatencyBuilder().setFaSendTimestamp(publishTime);
					fbBuilder.setTradingSession(getSessionInfo().getTradingSessionId());
					FullBook internalQuote = fbBuilder.build();
					INTERNAL_QUOTE_LOGGER.info(String.format("%s:%s", quoteSubscriptionNode.getOutboundTopic(), TextFormat.shortDebugString(internalQuote)));
					iPublishingEndpoint.publish(quoteSubscriptionNode.getOutboundTopic(), internalQuote);
				} 
			} catch (Exception e) {
				logger.warn("Exception while publishing quote, ", e);
			}
			
		}
		for (int i =0 ;  i <= currentMaxQuoteSubscriptionIdx; i++ ) {
			if (nodes[i].isInUse()
					&& nodes[i].getExpireTime() > 0
					&& publishTime > nodes[i].getExpireTime() ) {
				nodes[i].setInUse(false);
				if ( i == currentMaxQuoteSubscriptionIdx) {
					boolean found = false;
					for (int j = currentMaxQuoteSubscriptionIdx ;  !found && j >=0; j--) {
						if (nodes[j].isInUse() ) {
							currentMaxQuoteSubscriptionIdx = j;
							found = true;
						}
					}
					if ( !found ) {
						currentMaxQuoteSubscriptionIdx = 0;
					}
				}
			}
			
		}
	}
	
	

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxSpot;
	}
	
	public boolean performPassCutOffTimeValidation(long currentTime, String currencyPair)	{
		boolean passCutOffTime = false;
		final ZoneId appZone   = ZoneId.of("America/New_York");
		final LocalTime maxAllowedTime = LocalTime.parse("17:00");
		final DateTimeFormatter frmt   = DateTimeFormatter.ofPattern("HH:mm");
		
		logger.info("PassCutOffTimeValidation. Begins...");
		
		if(symbolsAllowPassCutOff.containsKey(currencyPair))	{
			String symbolValue = symbolsAllowPassCutOff.get(currencyPair);
			String cutOffTime  = symbolValue.substring(0, 5);
						
			logger.info("PassCutOffTimeValidation info: " + symbolValue + " " + currentTime);
			LocalTime localCutOffTime = LocalTime.parse(cutOffTime);
			
			LocalDateTime requestDateTime = LocalDateTime.now( appZone);
			LocalTime requestTime         = requestDateTime.toLocalTime();
			
			logger.info("PassCutOffTimeValidation RequestTime: " + requestTime.format(frmt) + ", CutOffTime: " + localCutOffTime.format(frmt) 
			                            + ", MaxTime: " + maxAllowedTime.format(frmt));
			if( requestDateTime.getDayOfWeek() != DayOfWeek.SUNDAY
					&& requestTime.isAfter(localCutOffTime) && requestTime.isBefore(maxAllowedTime))
				passCutOffTime = true;
		}
				
		logger.info("PassCutOffTimeValidation. Ends... " + String.valueOf(passCutOffTime));
		return(passCutOffTime);
	}

	
	protected synchronized QuoteSubscriptionNode findAndReserveAvailableServicingNode() {
		boolean found = false;
		for (int i =0 ; !found && i < MAX_SIMULTANEOUS_QUOTE; i++ ) {
			QuoteSubscriptionNode quoteSubscriptionNode = nodes[i];
			if (!quoteSubscriptionNode.isInUse()) {
				quoteSubscriptionNode.setInUse( true);
				quoteSubscriptionNode.setFirstPriceActived(false);
				found = true;
			}
			
			if (found) {
				if ( i > currentMaxQuoteSubscriptionIdx) {
					currentMaxQuoteSubscriptionIdx = i;
				}
				return quoteSubscriptionNode;
			}
		}
		return null;
	}
	
	public static class QuoteSubscriptionNode {
		private final FullBook.Builder fbBuilder = FullBook.newBuilder();
		
		private volatile boolean firstPriceActived = false;
		private volatile boolean inUse = false;
		private volatile long expireTime = -1L;
		private volatile String outboundTopic;
		private volatile String product;
		private volatile String settleDate1;
		private volatile String settleDate2;	
		private volatile String ovrSettleDate1;
		private volatile QuoteSide quoteSide;
		private volatile QuoteVo quote1;
		private volatile QuoteVo quote2;
		private volatile IInstrumentDetailProperties instrumentDetail;
		
		public boolean isInUse() {
			return inUse;
		}
		public IInstrumentDetailProperties getInstrumentDetail() {
			return instrumentDetail;
		}
		public void setInstrumentDetail(IInstrumentDetailProperties instrumentDetail) {
			this.instrumentDetail = instrumentDetail;
		}
		public void setOutboundTopic(String topic) {
			this.outboundTopic = topic;			
		}
		public String getOutboundTopic() {
			return outboundTopic;
		}
		public void setInUse(boolean inUse) {
			this.inUse = inUse;
		}
		public long getExpireTime() {
			return expireTime;
		}
		public void setExpireTime(long expireTime) {
			this.expireTime = expireTime;
		}
		public FullBook.Builder getFbBuilder() {
			return fbBuilder;
		}
		public boolean isFirstPriceActived() {
			return firstPriceActived;
		}
		public void setFirstPriceActived(boolean firstPriceActived) {
			this.firstPriceActived = firstPriceActived;
		}
		public String getProduct() {
			return product;
		}
		public void setProduct(String product) {
			this.product = product;
		}
		public String getSettleDate1() {
			return settleDate1;
		}
		public void setSettleDate1(String settleDate1) {
			this.settleDate1 = settleDate1;
		}
		public String getSettleDate2() {
			return settleDate2;
		}
		public void setSettleDate2(String settleDate2) {
			this.settleDate2 = settleDate2;
		}
		public QuoteVo getQuote1() {
			return quote1;
		}
		public void setQuote1(QuoteVo quote1) {
			this.quote1 = quote1;
		}
		public QuoteVo getQuote2() {
			return quote2;
		}
		public void setQuote2(QuoteVo quote2) {
			this.quote2 = quote2;
		}
		public String getOvrSettleDate1() {
			return ovrSettleDate1;
		}
		public void setOvrSettleDate1(String ovrSettleDate1) {
			this.ovrSettleDate1 = ovrSettleDate1;
		}
		public QuoteSide getQuoteSide() {
			return quoteSide;
		}
		public void setQuoteSide(QuoteSide quoteSide) {
			this.quoteSide = quoteSide;
		}
		
	}
	
	private static class SubscriptionNodeFixUpdater implements IFixListener, Runnable {
		private static final double ZERO_PTS_VALUE = 0.00000d;
		private static final String ZERO_PTS = "0.00000";

		private static final PriceUtil	priceUtil = new PriceUtil();

		private final String quoteReqId;
		private final QuoteSubscriptionNode subscriptionNode;
		private final IResponseDialectHelper dialect;
		private final QuoteStackRepo quoteStackRepo;
		private volatile ScheduledFuture<?> scheduled = null;
		private final String currencyPair;
		
		
		private final static Logger logger = LoggerFactory.getLogger(SubscriptionNodeFixUpdater.class);
		private final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

		private final AtomicInteger seq;
		
		static String quoteTimeOutDelay = RuntimeDataService.getRunTimeData(SysProperty.GroupCd.FXPARMS, SysProperty.Key1.QUOTE_TIME_OUT_INTERVAL, null); 
				
		public SubscriptionNodeFixUpdater(
				String quoteReqId,
				QuoteSubscriptionNode subscriptionNode, 
				IResponseDialectHelper dialect,
				QuoteStackRepo quoteStackRepo,
				ISchedulingWorker worker,
				String currencyPair) {
			super();
			this.quoteReqId = quoteReqId;
			this.subscriptionNode = subscriptionNode;
			this.dialect = dialect;
			this.quoteStackRepo = quoteStackRepo;
			this.currencyPair = currencyPair;
			if((quoteTimeOutDelay == null) || (quoteTimeOutDelay.trim().length() <= 0))
				quoteTimeOutDelay = String.valueOf(SysProperty.DefaultValue.QUOTE_TIME_OUT_INTERVAL);
			
			this.scheduled = worker.scheduleAtFixedDelay(this, Integer.parseInt(quoteTimeOutDelay));
			this.seq = new AtomicInteger(1);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try	{
				String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
				monitorAgent.logWarnNotification("IQFix:QuoteTimeOut", topic, MonitorConstant.FXADT.NO_MARKET_RATE, "MDE", "Quote Request for " + currencyPair + " Timed Out.");
			}
			catch(Exception exp){
				
			}
		}

		@Override
		public void onMessage(Message message, IQfixRoutingAgent routingAgent)
				throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
			QuoteVo incomingQuote = null;
			
			//	Cancel the future job for sending message on TimeOut.
			if((scheduled != null)){
				scheduled.cancel(false);
				scheduled = null;
			}
			
			QUOTE_LOGGER.info(message.toString());
			
			if ( message instanceof quickfix.fix50.Quote) {
				incomingQuote = dialect.convert((quickfix.fix50.Quote) message);
			} else if ( message instanceof quickfix.fix44.Quote) {
				incomingQuote = dialect.convert((quickfix.fix44.Quote) message);
			}
			

			if ( incomingQuote != null ) { 
				String bidSettleDate = incomingQuote.getSettleDate();
				String askSettleDate = incomingQuote.getSettleDate();
				QuoteVo quote = null;
				
				if ( Constants.ProductType.FXTIMEOPTION.equals(subscriptionNode.getProduct()) ) {
					if ( incomingQuote.getBidForwardPoints() == null ) {
						incomingQuote.setBidForwardPoints(ZERO_PTS_VALUE);
					}
					if ( incomingQuote.getOfferForwardPoints() == null ) {
						incomingQuote.setOfferForwardPoints(ZERO_PTS_VALUE);
					}
					
					if ( subscriptionNode.getSettleDate1().equals(incomingQuote.getSettleDate())) {
						subscriptionNode.setQuote1(incomingQuote);
					} else 	if ( subscriptionNode.getSettleDate2().equals(incomingQuote.getSettleDate()))  {
						subscriptionNode.setQuote2(incomingQuote);
					}
					
					if ( subscriptionNode.getQuote1() != null && subscriptionNode.getQuote2() != null ){
						NumberVo quote1Bid = NumberVo.getInstance(subscriptionNode.getQuote1().getBidPx());
						NumberVo quote2Bid = NumberVo.getInstance(subscriptionNode.getQuote2().getBidPx());
						NumberVo quote1Ask = NumberVo.getInstance(subscriptionNode.getQuote1().getOfferPx());
						NumberVo quote2Ask = NumberVo.getInstance(subscriptionNode.getQuote2().getOfferPx());
						
						quote = subscriptionNode.getQuote2().deepClone();
						bidSettleDate = subscriptionNode.getQuote2().getSettleDate();
						askSettleDate = subscriptionNode.getQuote2().getSettleDate();
						
						if ( quote1Bid.isLess(quote2Bid) ) {
							quote.setBidForwardPoints(Double.parseDouble(subscriptionNode.getQuote1().getBidForwardPoints()));
							quote.setBidPx(Double.parseDouble(subscriptionNode.getQuote1().getBidPx()));
							quote.setBidSpotRate(Double.parseDouble(subscriptionNode.getQuote1().getBidSpotRate()));
							bidSettleDate  = subscriptionNode.getQuote1().getSettleDate(); 
						}
						
						if ( quote1Ask.isGreater(quote2Ask)) {
							quote.setOfferForwardPoints(Double.parseDouble(subscriptionNode.getQuote1().getOfferForwardPoints()));
							quote.setOfferPx(Double.parseDouble(subscriptionNode.getQuote1().getOfferPx()));
							quote.setOfferSpotRate(Double.parseDouble(subscriptionNode.getQuote1().getOfferSpotRate()));
							askSettleDate = subscriptionNode.getQuote1().getSettleDate();  
						}
						
						quote.setSettleDate(bidSettleDate + askSettleDate);
						quote.setOrigQuote1(subscriptionNode.getQuote1());
						quote.setOrigQuote2(subscriptionNode.getQuote2());
						quote.setQuoteId(QuoteStackRepo.PREFIX_FOR_USE_SUB_QID + seq.getAndIncrement());

					}
					
				} else {
					quote = incomingQuote;
				}
				
				FullBook.Builder fbBuilder = subscriptionNode.getFbBuilder();
				fbBuilder.setSymbol(quote == null ? subscriptionNode.getInstrumentDetail().getSymbol() : quote.getSymbol());
				fbBuilder.setIndicativeFlag(IndicativeFlag.TRADABLE);
				fbBuilder.clearAskTicks();
				fbBuilder.clearBidTicks();
				fbBuilder.clearTenors();
				fbBuilder.getLatencyBuilder().setFaReceiveTimestamp(System.currentTimeMillis());
				if ( quote != null ) {
					fbBuilder.setQuoteRefId(quoteReqId + IFixConstants.DEFAULT_DELIMITER + quote.getQuoteId());
					int pointValue = subscriptionNode.getInstrumentDetail().getPointValue();

					if ( subscriptionNode.getQuoteSide() == QuoteSide.SWAP__SELL_AND_BUY ) {
						if ( quote.getBidPx() != null ) {
							Tick.Builder bidTick = Tick.newBuilder();
							bidTick.setRate(quote.getBidPx());
							bidTick.setSize(NumberVo.getInstance(quote.getBidSize()).getLongValueFloored());
							bidTick.setSpotRate(quote.getBidSpotRate());
							fbBuilder.addBidTicks(bidTick);
						}
						if ( quote.getOfferPx2() != null ) {
							Tick.Builder askTick = Tick.newBuilder();
							askTick.setRate(quote.getOfferPx2());
							askTick.setSize(NumberVo.getInstance(quote.getOfferSize2()).getLongValueFloored());
							askTick.setSpotRate(quote.getBidSpotRate());
							fbBuilder.addAskTicks(askTick);
						}
						if ( quote.getBidForwardPoints() != null || quote.getOfferForwardPoints2() != null ) {
							bidSettleDate = quote.getSettleDate();
							askSettleDate = quote.getSettleDate2();
								Tenor.Builder tenor1 = Tenor.newBuilder();
								if ( quote.getBidForwardPoints() != null ) {
									BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
									tenor1.setBidSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									tenor1.setActualDate(bidSettleDate);
									fbBuilder.addTenors(tenor1);	
								}
								Tenor.Builder tenor2 = Tenor.newBuilder();
								if ( quote.getOfferForwardPoints2() != null ) {
									BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints2(), MathContext.DECIMAL64);
									tenor2.setAskSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									tenor2.setActualDate(askSettleDate);
									fbBuilder.addTenors(tenor2);	
								}
							
						} 
					} else if ( subscriptionNode.getQuoteSide() == QuoteSide.SWAP__BUY_AND_SELL ) {
						if ( quote.getBidPx2() != null ) {
							Tick.Builder bidTick = Tick.newBuilder();
							bidTick.setRate(quote.getBidPx2());
							bidTick.setSize(NumberVo.getInstance(quote.getBidSize2()).getLongValueFloored());
							bidTick.setSpotRate(quote.getOfferSpotRate());
							fbBuilder.addBidTicks(bidTick);
						}
						if ( quote.getOfferPx() != null ) {
							Tick.Builder askTick = Tick.newBuilder();
							askTick.setRate(quote.getOfferPx());
							askTick.setSize(NumberVo.getInstance(quote.getOfferSize()).getLongValueFloored());
							askTick.setSpotRate(quote.getOfferSpotRate());
							fbBuilder.addAskTicks(askTick);
						}
						if ( quote.getBidForwardPoints2() != null || quote.getOfferForwardPoints() != null ) {
							bidSettleDate = quote.getSettleDate2();
							askSettleDate = quote.getSettleDate();
								Tenor.Builder tenor1 = Tenor.newBuilder();
								if ( quote.getBidForwardPoints2() != null ) {
									BigDecimal bd = new BigDecimal(quote.getBidForwardPoints2(), MathContext.DECIMAL64);
									tenor1.setBidSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									tenor1.setActualDate(bidSettleDate);
									fbBuilder.addTenors(tenor1);	
								}
								Tenor.Builder tenor2 = Tenor.newBuilder();
								if ( quote.getOfferForwardPoints() != null ) {
									BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
									tenor2.setAskSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									tenor2.setActualDate(askSettleDate);
									fbBuilder.addTenors(tenor2);	
								}
							
						} 
					} else {
						if ( quote.getBidPx() != null ) {
							Tick.Builder bidTick = Tick.newBuilder();
							bidTick.setRate(quote.getBidPx());
							bidTick.setSize(NumberVo.getInstance(quote.getBidSize()).getLongValueFloored());
							bidTick.setSpotRate(quote.getBidSpotRate());
							fbBuilder.addBidTicks(bidTick);
						}
						if ( quote.getOfferPx() != null ) {
							Tick.Builder askTick = Tick.newBuilder();
							askTick.setRate(quote.getOfferPx());
							askTick.setSize(NumberVo.getInstance(quote.getOfferSize()).getLongValueFloored());
							askTick.setSpotRate(quote.getOfferSpotRate());
							fbBuilder.addAskTicks(askTick);
						}
						
						
						if ( quote.getBidForwardPoints() != null || quote.getOfferForwardPoints() != null ) {
							
							if ( bidSettleDate.equals(askSettleDate)) {
								Tenor.Builder tenor = Tenor.newBuilder();
								if ( quote.getBidForwardPoints() != null ) {
									BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
									tenor.setBidSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									
								}
								if ( quote.getOfferForwardPoints() != null ) {
									BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
									tenor.setAskSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
								}
								tenor.setActualDate(bidSettleDate);
								fbBuilder.addTenors(tenor);
							} else {
								Tenor.Builder tenor1 = Tenor.newBuilder();
								if ( quote.getBidForwardPoints() != null ) {
									BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
									tenor1.setBidSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									tenor1.setActualDate(bidSettleDate);
									fbBuilder.addTenors(tenor1);	
								}
								Tenor.Builder tenor2 = Tenor.newBuilder();
								if ( quote.getOfferForwardPoints() != null ) {
									BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
									tenor2.setAskSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
									tenor2.setActualDate(askSettleDate);
									fbBuilder.addTenors(tenor2);	
								}
							}
						} 
					}
					fbBuilder.setTopOfBookIdx(0);
				}

				if ( subscriptionNode.getOvrSettleDate1() != null ){ 
					if ( fbBuilder.getTenorsCount() == 0 ) {
						Tenor.Builder t = Tenor.newBuilder();
						t.setAskSwapPoints(ZERO_PTS);
						t.setBidSwapPoints(ZERO_PTS);
						fbBuilder.addTenors(t);	
					}
					fbBuilder.getTenorsBuilder(0).setActualDate(subscriptionNode.getOvrSettleDate1());
				}

				quoteStackRepo.addQuote(quoteReqId, quote);
				subscriptionNode.setFirstPriceActived(true);
				
			} else  if ( message instanceof quickfix.fix50.QuoteCancel) {
				@SuppressWarnings("unused")
				quickfix.fix50.QuoteCancel quoteCancel = (quickfix.fix50.QuoteCancel) message;
				FullBook.Builder fbBuilder = subscriptionNode.getFbBuilder();
				fbBuilder.setIndicativeFlag(IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE, IndicativeReason.MA_NoData));
				fbBuilder.clearQuoteRefId();
				fbBuilder.clearAskTicks();
				fbBuilder.clearBidTicks();
				fbBuilder.clearTenors();
				Latency.Builder latency = Latency.newBuilder();
				fbBuilder.setLatency(latency);
				
				latency.setFaReceiveTimestamp(System.currentTimeMillis());
			}
			
		}

		@Override
		public void onFixSessionLogoff() {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onFixSessionLogon() {
			// TODO Auto-generated method stub
		}
		

		private IResponseDialectHelper getDialect() {
			return this.dialect;
		}
	}
}
