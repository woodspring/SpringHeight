package com.tts.plugin.adapter.impl.base.app.quote;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
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
import com.tts.util.constant.SysProperty;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;

public class QuoteAdapterImpl extends AbstractMasApp implements IPublishingApp, ISubscribingApp {
	private final static int MAX_SIMULTANEOUS_QUOTE = 2000;
	private final static AtomicInteger reqId = new AtomicInteger((int) (AppUtils.getSequencePrefix() /10000));

	protected final static Logger logger = LoggerFactory.getLogger(QuoteAdapterImpl.class);
	private final QuoteSubscriptionNode[] nodes = new QuoteSubscriptionNode[MAX_SIMULTANEOUS_QUOTE];

	private final IPublishingEndpoint iPublishingEndpoint;
	private final IResponseDialectHelper dialect;
	private final QuoteStackRepo quoteStackRepo;
	protected final IFxCalendarBizServiceApi fxCalendarBizServiceApi;
	private final IFxCalendarBizService fxCalendarBizService;

	protected final IInstrumentDetailProvider instrumentDetailProvider;
	private final ISchedulingWorker worker;
	private volatile int currentMaxQuoteSubscriptionIdx = 0;

	public QuoteAdapterImpl(IMkQfixApp qfixApp, ISchedulingWorker worker, SessionInfo sessionInfo,
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
		
	}

	@Override
	public String getName() {
		return "QuoteAdapter";
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
				String tenor = null;
				QuoteSide side = null;
				long expiryTime = -1L;
				
				if ( Constants.ProductType.FXSPOT.equals(quoteParam.getProduct() )) {
					tenor = TenorVo.NOTATION_SPOT;
				} else {
					try {
						TenorVo.Builder t = new TenorVo.Builder();
						t.setPeriodCd(quoteParam.getNearDateDetail().getPeriodCd());
						t.setValue(Integer.parseInt(quoteParam.getNearDateDetail().getPeriodValue()));
						tenor = t.toString(); 
					} catch (Exception e) {
						try {
							TenorVo.Builder t = new TenorVo.Builder();
							t.setPeriodCd(quoteParam.getNearDateDetail().getPeriodCd());
							t.setValue(-1);
							tenor = t.toString(); 
						} catch (Exception e1) {
							
						}
					}
				}
				
				if ( QuoteDirection.BOTH.equals(quoteParam.getQuoteDirection()) ) {
					side = QuoteSide.BOTH;
				} else if ( QuoteDirection.BUY.equals(quoteParam.getQuoteDirection()) ) { 
					side = QuoteSide.BUY;
				} else if (	QuoteDirection.SELL.equals(quoteParam.getQuoteDirection()) ) { 
					side = QuoteSide.SELL;
				}
					
					
				if (quoteParam.hasQuoteDuration() ) {
					expiryTime = quoteParam.getQuoteDuration();
					expiryTime += 10;
				}
				
				BigDecimal bd = new BigDecimal(quoteParam.getSize());
				String settleDate = quoteParam.getNearDateDetail().getActualDate();
				String currencyPair = quoteParam.getCurrencyPair();	
				QuoteSubscriptionNode n = findAndReserveAvailableServicingNode();
				
				FullBook.Builder fbBuilder = n.getFbBuilder();
				fbBuilder.clearAskTicks();
				fbBuilder.clearBidTicks();
				fbBuilder.clearTenors();
				fbBuilder.setSymbol( currencyPair);

				n.setOutboundTopic(priceSubscriptionRequest.getTopic());
				n.setExpireTime(currentTime + expiryTime * ChronologyUtil.MILLIS_IN_SECOND);
				n.setInstrumentDetail(instrumentDetailProvider.getInstrumentDetail(currencyPair));
				if ( settleDate == null && tenor != null ) {
					settleDate = fxCalendarBizServiceApi.getForwardValueDate(currencyPair, tenor);
					if ( TenorVo.NOTATION_SPOT.equals(tenor)) {
						//settleDate = "0";
					}
				}
				String notionalCurrency;
				if ( quoteParam.hasNotionalCurrency() ) {
					notionalCurrency = quoteParam.getNotionalCurrency();
				} else {
					notionalCurrency = quoteParam.getCurrencyPair().substring(0, 3);
				}

				String requestId = getQfixApp().sendRfsRequest(
						bd.longValue(), 
						currencyPair,
						notionalCurrency,
						tenor,
						settleDate, 
						side, 
						expiryTime, 
						new SubscriptionNodeFixUpdater(
								settleDate + "-" + Integer.toString(reqId.getAndIncrement()),
								n, 
								dialect,
								quoteStackRepo, 
								worker,
								currencyPair),									
						AppType.QUOTEADAPTER);
				logger.debug("sent, requestId = "  + requestId);
				
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
					iPublishingEndpoint.publish(quoteSubscriptionNode.getOutboundTopic(), fbBuilder.build());
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
	
	protected QuoteSubscriptionNode findAndReserveAvailableServicingNode() {
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
		
	}
	
	private static class SubscriptionNodeFixUpdater implements IFixListener, Runnable {
		private static final PriceUtil	priceUtil = new PriceUtil();

		private final String quoteReqId;
		private final QuoteSubscriptionNode subscriptionNode;
		private final IResponseDialectHelper dialect;
		private final QuoteStackRepo quoteStackRepo;
		private volatile ScheduledFuture<?> scheduled = null;
		private final String currencyPair;
		
		
		private final static Logger logger = LoggerFactory.getLogger(SubscriptionNodeFixUpdater.class);
		private final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
		
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
			QuoteVo quote = null;
			
			//	Cancel the future job for sending message on TimeOut.
			if((scheduled != null)){
				scheduled.cancel(false);
				scheduled = null;
			}
			
			
			if ( message instanceof quickfix.fix50.Quote) {
				quote = dialect.convert((quickfix.fix50.Quote) message);
			} else if ( message instanceof quickfix.fix44.Quote) {
				quote = dialect.convert((quickfix.fix44.Quote) message);
			}
				
			if ( quote != null ) { 
				FullBook.Builder fbBuilder = subscriptionNode.getFbBuilder();
				fbBuilder.setSymbol(quote.getSymbol());
				fbBuilder.setIndicativeFlag(IndicativeFlag.TRADABLE);
				fbBuilder.setQuoteRefId(quoteReqId + IFixConstants.DEFAULT_DELIMITER + quote.getQuoteId());
				fbBuilder.clearAskTicks();
				fbBuilder.clearBidTicks();
				fbBuilder.clearTenors();
				fbBuilder.getLatencyBuilder().setFaReceiveTimestamp(System.currentTimeMillis());
				
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
				
				fbBuilder.setTopOfBookIdx(0);
				
				if ( quote.getBidForwardPoints() != null || quote.getOfferForwardPoints() != null ) {
					int pointValue = subscriptionNode.getInstrumentDetail().getPointValue();
					
					Tenor.Builder tenor = Tenor.newBuilder();
					if ( quote.getBidForwardPoints() != null ) {
						BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
						tenor.setBidSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
						
					}
					if ( quote.getOfferForwardPoints() != null ) {
						BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
						tenor.setAskSwapPoints(priceUtil.toPipConvert(bd.toPlainString(), pointValue));
					}
					tenor.setActualDate(quote.getSettleDate());
					fbBuilder.addTenors(tenor);
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
