package com.tts.ske.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.service.ITradingSessionAware;
import com.tts.message.config.ConfigCommon.CoreRateGroupInstrument;
import com.tts.message.constant.Constants;
import com.tts.message.eas.request.MarketModelStruct.ChangeManualIndicative;
import com.tts.message.eas.request.MarketModelStruct.ManualIndicative;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.market.MarketMarkerStruct.AddMarketAdjustmentLiquidity;
import com.tts.message.market.MarketMarkerStruct.CancelMarketMakingRequest;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.message.system.RolloverStruct.RolloverNotification;
import com.tts.message.system.SystemStatusStruct.ChangePanicMode;
import com.tts.message.system.SystemStatusStruct.ChangeTradingSession;
import com.tts.message.system.admin.AdapterStruct.AdapterStatus;
import com.tts.message.system.admin.AdapterStruct.AdapterStatusRequest;
import com.tts.message.system.admin.AdapterStruct.FixSessionCapability;
import com.tts.message.system.admin.AdapterStruct.SessionStatus;
import com.tts.message.system.admin.AdapterStruct.Status;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.TradeMessage.BankTransaction;
import com.tts.message.trade.TradeMessage.BankTransactionSummary;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.message.trade.TradeMessage.TransactionSummary;
import com.tts.message.ui.client.constant.ClientResponseType;
import com.tts.message.util.TtMsgEncoder;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.protocol.platform.event.IEventMessageTypeConstant.Data;
import com.tts.protocol.platform.event.IEventMessageTypeConstant.Transactional;
import com.tts.service.biz.calendar.ICachedFxCalendarBizServiceApi;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.db.ICustomerProfileService;
import com.tts.service.db.TradingSessionManager;
import com.tts.service.sa.transaction.TransWrapper;
import com.tts.ske.app.feature.InternalLiquidityHandler;
import com.tts.ske.app.price.subscription.IMdSubscriber;
import com.tts.ske.app.price.subscription.InternalConsumer;
import com.tts.ske.db.RetryBuilder;
import com.tts.ske.vo.SessionInfoVo;
import com.tts.service.biz.transactions.vo.TransactionDetailVo;
import com.tts.service.biz.transactions.vo.TransactionVo;
import com.tts.util.AppConfig;
import com.tts.util.AppContext;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.exception.InvalidServiceException;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.AssetClassProductVo;
import com.tts.vo.CounterPartyVo;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;

public class DataFlowController implements ITradingSessionAware, IMsgListener {
	private static final boolean USE_SELF_GEN_ID = false;
	private static final String EXEC_REPORT_TOPIC = String.format(Transactional.TRAN_INFO_TEMPLATE, "FA",
			Data.RateType.MARKET);
	private static final Logger logger = LoggerFactory.getLogger(DataFlowController.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	private static final long CHANNEL_ID= 1L;
	private static final String NAME = "INTERNAL";
	private static final String TOPIC___ADAPTER_STATUS = String.format(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_INFO_TEMPLATE, NAME);

	private static final String[] CTRL_TOPICS_INTERESTED = new String[] {
			IEventMessageTypeConstant.REM.CE_TRADING_SESSION,
			IEventMessageTypeConstant.System.FX_ROLLOVER_EVENT,
			IEventMessageTypeConstant.MarketMaking.ALL_MARKET_ADJUSTMENT_REQUEST,
			"TTS.CTRL.EVENT.REQUEST.FA.*.INTERNAL.>", 
			"TTS.TRAN.FX.*.TRANINFO.FA.INTERNAL.>", // TTS.TRAN.FX.MR.TRANINFO.FA.TRADAIR
			IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_ALL_REQUEST_EVENT,
			IEventMessageTypeConstant.REM.ALL_EVENTS
		};
	private final ICachedFxCalendarBizServiceApi fxCalendarBizService;
	private final ArrayList<IMsgReceiver> ctrlMsgReceivers = new ArrayList<>();
	private final IMsgSender ctrlMsgSender;
	private final IMsgSender marketDataSender;
	private final CounterPartyVo extClient;
	private final CounterPartyVo internalLP;
	private final RetryBuilder db = new RetryBuilder();
	private final SessionInfoVo sessionInfo = new SessionInfoVo();
	private final String tradingCustNm;
	private final String tradingAcctNm;
	private final String providerCustNm;
	private final String providerAcctNm;
	private final long startUpTime = System.currentTimeMillis();
	private volatile long tradingSessionId = -1L;
	private Map<String, InternalLiquidityHandler> internalLiquidityHandlerMap;
	private Map<String, IMdSubscriber> mdSubscriberMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService schdExctr = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = defaultThreadFactory.newThread(r);
            t.setName("SKERecurringTaskThread");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    });
	

	public DataFlowController() {
		ICachedFxCalendarBizServiceApi fxCalendarBizServiceApi = AppContext.getContext().getBean(ICachedFxCalendarBizServiceApi.class);
		this.fxCalendarBizService = fxCalendarBizServiceApi;
		
		IMsgSenderFactory msgSenderFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		IMsgSender msgSender1 = msgSenderFactory.getMsgSender(false, false, false);
		IMsgSender msgSender2 = msgSenderFactory.getMsgSender(true, false, false);

		msgSender1.init();
		msgSender2.init();
		
		ISymbolIdMapper symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
		List<String> symbols = symbolIdMapper.getSymbols();
		HashMap<String, InternalLiquidityHandler> _handlers = new HashMap<>(); 
		for ( String symbol : symbols) {
			_handlers.put(symbol, new InternalLiquidityHandler(symbol, msgSender2, msgSender1, fxCalendarBizService, sessionInfo));
		}
		
		this.tradingCustNm = AppConfig.getValue("transactionBooking", "tradingCustNm");
		this.tradingAcctNm = AppConfig.getValue("transactionBooking", "tradingAcctNm");
		this.providerCustNm = AppConfig.getValue("transactionBooking", "providerCustNm");
		this.providerAcctNm = AppConfig.getValue("transactionBooking", "providerAcctNm");
		ICustomerProfileService customerProfileService = (ICustomerProfileService) AppContext.getContext().getBean("customerProfileService");
		List<CounterPartyVo> tradingParties = customerProfileService.findCounterPartyByNm(tradingCustNm, tradingAcctNm, "EXT"	, CHANNEL_ID);
		List<CounterPartyVo> providerParties = customerProfileService.findCounterPartyByNm(providerCustNm, providerAcctNm, "INT"	, CHANNEL_ID);
				
		this.internalLiquidityHandlerMap = Collections.unmodifiableMap(_handlers);
		this.ctrlMsgSender = msgSender1;
		this.marketDataSender = msgSender2;
		this.internalLP =providerParties.size() > 0  ? providerParties.get(0) : null ;
		this.extClient =tradingParties.size() > 0  ? tradingParties.get(0) : null ;
		
		if ( extClient == null ) {
			logger.error("External Counterparty UNDEFINED!!");
		}
		if ( internalLP == null ) {
			logger.error("Internal Counterparty UNDEFINED!!");
		}
	}
	
	public void init() {
		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		
		for ( String topic: CTRL_TOPICS_INTERESTED ) {
			IMsgReceiver  msgReceiver = msgReceiverFactory.getMsgReceiver(false, false);
			msgReceiver.setListener(this);
			msgReceiver.setTopic(topic);
			msgReceiver.init();
			this.ctrlMsgReceivers.add(msgReceiver);
		}
			
		IMsgReceiver msgReceiver = msgReceiverFactory.getMsgReceiver(true, false);
		msgReceiver.setTopic("TTS.MD.FX.FA.SPOT.*." + AppConfig.getValue("marketData", "incomingIdentifer").toUpperCase());
		msgReceiver.setListener(this);
		msgReceiver.init();
		schdExctr.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				Collection<InternalLiquidityHandler> c = internalLiquidityHandlerMap.values();
				if ( c.size() > 0 ) {
					for ( InternalLiquidityHandler  i : c) {
						i.notifyTraderPeriodically(false);
					}
				}
			}
			
		}, 250, 250, TimeUnit.MILLISECONDS);
		
		this.tradingSessionId = TradingSessionManager.getInstance().getActiveSession().getPk();

		AdapterStatus.Builder adapterStatusBuilder = buildStatus();
		ctrlMsgSender.send(TOPIC___ADAPTER_STATUS, TtMsgEncoder.encode(adapterStatusBuilder.build()));	
	}
	
	@Override
	public void switchTradingSession(String newTradingSessionName) throws InvalidServiceException {
		
	}	

	@Override
	public void onMessage(TtMsg message, IMsgSessionInfo sessionInfo, IMsgProperties msgProperties) {
		String eventType = msgProperties.getSendTopic();
		
		try {
	
			if (IEventMessageTypeConstant.REM.CE_TRADING_SESSION.equals(eventType)) {
				monitorAgent.debug(String.format("OnEvent(SYSTEM): %s", eventType));

				ChangeTradingSession tradingSession = ChangeTradingSession.parseFrom(message.getParameters());
				if (tradingSession.hasChangeTo() && tradingSession.getChangeTo().getStatus() == 100  ) {
					String toTradingSession = tradingSession.getChangeTo().getTradingSessionNm();
					switchTradingSession(toTradingSession);
					this.tradingSessionId = tradingSession.getChangeTo().getTradingSessionId();
				}		
			} else if (IEventMessageTypeConstant.System.FX_ROLLOVER_EVENT.equals(eventType)) {
				monitorAgent.debug(String.format("OnEvent(SYSTEM): %s", eventType));

				RolloverNotification rolloverNotification = RolloverNotification.parseFrom(message.getParameters());
				fxCalendarBizService.onRolloverEvent(rolloverNotification);
			} else if (IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_SUBMIT_REQUEST.equals(eventType)) {
				AddMarketAdjustmentLiquidity adjustmentLqy = AddMarketAdjustmentLiquidity
						.parseFrom(message.getParameters());
				monitorAgent.debug(String.format("OnEvent(BT): %s: %s", eventType, TextFormat.shortDebugString(adjustmentLqy)));

				InternalLiquidityHandler h = internalLiquidityHandlerMap.get(adjustmentLqy.getSymbol());
				h.submitInternalLiquidity(adjustmentLqy);
			} else if (eventType.startsWith("TTS.MD.FX.FA.SPOT")){ 
				RawMarketBook mb = RawMarketBook.parseFrom(message.getParameters());
				InternalLiquidityHandler h = internalLiquidityHandlerMap.get(mb.getSymbol());
				h.onNewMarketData(mb);
				
			}  else if (eventType.startsWith("TTS.CTRL.EVENT.REQUEST.FA.SUBSCRIBE.INTERNAL")) {
				monitorAgent.debug(String.format("OnEvent(AGG): %s", eventType));

				PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(message.getParameters());
				String symbol = request.getQuoteParam().getCurrencyPair();
				logger.info(String.format("Received PriceSubscriptionRequest for <%s>: %s", symbol, 
						TextFormat.shortDebugString(request)));
				IMdSubscriber mdSubscriber = new InternalConsumer(request.getRequestId(), marketDataSender);
				mdSubscriberMap.put(request.getRequestId(), mdSubscriber);
				registerListener(symbol, mdSubscriber);
			}  else if (eventType.startsWith("TTS.CTRL.EVENT.REQUEST.FA.UNSUBSCRIBE.INTERNAL")) {
				monitorAgent.debug(String.format("OnEvent(AGG): %s", eventType));

				PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(message.getParameters());
				String symbol = request.getQuoteParam().getCurrencyPair();
				logger.info(String.format("Received PriceSubscriptionRequest for <%s>: %s", symbol, 
						TextFormat.shortDebugString(request)));
				IMdSubscriber mdSubscriber = mdSubscriberMap.get(request.getRequestId());
				if ( mdSubscriber!= null ) { 
					unregisterListener(symbol, mdSubscriber);
				}
			} else if (IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_CANCEL_REQUEST.equals(eventType)) {
				CancelMarketMakingRequest request = CancelMarketMakingRequest.parseFrom(message.getParameters());
				monitorAgent.debug(String.format("OnEvent(BT): %s: %s", eventType, TextFormat.shortDebugString(request)));

				InternalLiquidityHandler h = internalLiquidityHandlerMap.get(request.getSymbol());
				h.cancelInternalLiquidity(request);
			} else if ( "TTS.TRAN.FX.MR.TRANINFO.FA.INTERNAL.INTERNAL".equals(eventType) ) {
				Transaction transactionMessage = Transaction.parseFrom(message.getParameters());
				String transId = transactionMessage.getTransId();
				logger.info(String.format("Received Hedging Request from %s for transId <%s>: %s", eventType, transId,
						TextFormat.shortDebugString(transactionMessage)));

				ExecutionReportInfo execReport = requestForHedge(transactionMessage.getTransId(), 
						transactionMessage.getSymbol(), 
						transactionMessage.getNotionalCurrency(),
						transactionMessage.getTradeAction(),
						transactionMessage.getNearDateDetail().getCurrency1Amt(),
						transactionMessage.getNearDateDetail().getCurrency2Amt(),
						transactionMessage.getOrderParams().getTargetPrice(),
						true,
						false);
				TtMsg ttMsg = TtMsgEncoder.encode(execReport);
				
				ctrlMsgSender.send(EXEC_REPORT_TOPIC, ttMsg);
			} else if (IEventMessageTypeConstant.REM.CE_PANIC_MODE.equals(eventType)) {
					ChangePanicMode panicMode = ChangePanicMode.parseFrom(message.getParameters());
					if (panicMode.hasPanicMode()) {
						if ( panicMode.getPanicMode().getIsPanic() ) {
							this.sessionInfo.addGlobalIndicativeReason(IndicativeReason.CRE_PanicMode);
							logger.info("all instruments change to indicative");
						} else {
							this.sessionInfo.removeGlobalIndicativeReason(IndicativeReason.CRE_PanicMode);
							logger.info("all instruments change to tradable");
						}
					}
			} else if (IEventMessageTypeConstant.REM.CE_INSTRUMENT_INDICATIVE.equals(eventType)) {
				ChangeManualIndicative chrMmanualIndicative = ChangeManualIndicative.parseFrom(message.getParameters());
				if (chrMmanualIndicative.hasManualIndicative()) {
					ManualIndicative manualIndicative = chrMmanualIndicative.getManualIndicative();
					boolean isIndicative = false;
					if (manualIndicative.hasIsIndicative() && manualIndicative.getIsIndicative()) {
						isIndicative = true;
					}
					for (CoreRateGroupInstrument crgInstrument : manualIndicative.getCoreGroupInstrumentsList()) {
						String instrument = crgInstrument.getInstrument();
						InternalLiquidityHandler h = internalLiquidityHandlerMap.get(instrument);
						if ( isIndicative ) {
							h.addIndicativeReason(IndicativeReason.CRE_ManualIndicative);
							logger.info(instrument + " change to indicative");
						} else {
							h.removeIndicativeReason(IndicativeReason.CRE_ManualIndicative);
							logger.info(instrument + " change to tradable");
						}
					}

				}
			} else if (message.hasParamType() && message.getParamType().equals(AdapterStatusRequest.class.getName())) {

					AdapterStatusRequest asr = AdapterStatusRequest.parseFrom(message.getParameters());
					if (asr.hasAdapterName()
							&& asr.getAdapterName().equals(NAME)) {

						AdapterStatus.Builder adapterStatusBuilder = buildStatus();
						adapterStatusBuilder.setRequestId(asr.getRequestId());
						ctrlMsgSender.send(TOPIC___ADAPTER_STATUS, TtMsgEncoder.encode(adapterStatusBuilder.build()));

					}

			} else {
				logger.info("Unknown event, " + eventType);
			}
		} catch (Exception ex) {
			monitorAgent.logError("SkewRateEngine.onEvent", eventType, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, ex.getMessage(), ex);
		}
	}
	
	private AdapterStatus.Builder buildStatus() {

		AdapterStatus.Builder adapterStatusBuilder = AdapterStatus.newBuilder();
		adapterStatusBuilder.setAdapterName(NAME);

		SessionStatus.Builder ss = SessionStatus.newBuilder();
		ss.setStatus(Status.ACTIVE);
		ss.setSourceNm(NAME);

		ss.addCapability(FixSessionCapability.MARKET_DATA);
		ss.addCapability(FixSessionCapability.MARKET_DATA__ESP);
		ss.addCapability(FixSessionCapability.ORDERING_FOK);
		ss.addCapability(FixSessionCapability.ORDERING_IOC);
		ss.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
		ss.setLastOnlineTimestamp(startUpTime);
		ss.setLastOfflineTimestamp(-1);
		
		adapterStatusBuilder.addActiveSessions(ss);
		adapterStatusBuilder.setStatus(Status.ACTIVE );
		return adapterStatusBuilder;
	}

	public ExecutionReportInfo requestForHedge(
			String clientOrderId,
			String symbol, 
			String notionalCurrency, 
			String tradeAction, 
			String currency1Amt, 
			String currency2Amt,
			String limitPrice,
			boolean isOrdTypeFOK,
			boolean isExternal) {
		InternalLiquidityHandler h = internalLiquidityHandlerMap.get(symbol);
		return h.requestForHedge(clientOrderId, symbol, notionalCurrency, tradeAction, currency1Amt, currency2Amt, limitPrice, isOrdTypeFOK, isExternal);
	}
	
	public void registerListener(String symbol, IMdSubscriber mdSubscriber) {
		InternalLiquidityHandler h = internalLiquidityHandlerMap.get(symbol);
		h.registerListener(mdSubscriber);
	}	
	
	public void unregisterListener(String symbol, IMdSubscriber mdSubscriber) {
		InternalLiquidityHandler h = internalLiquidityHandlerMap.get(symbol);
		h.unregisterListener(mdSubscriber);
	}
	
	public void destroy() {
		for ( IMsgReceiver  msgReceiver : this.ctrlMsgReceivers) {
			msgReceiver.destroy();
		}
		this.ctrlMsgSender.destroy();
		this.marketDataSender.destroy();
		this.schdExctr.shutdownNow();
	}

	/**
	 * TODO: return TTS-ORDER_ID
	 * 
	 * @param extClientOrderId
	 * @param symbol
	 * @param notionalCurrency
	 * @param tradeAction
	 * @param currency1Amt
	 * @param currency2Amt
	 * @param limitPrice
	 * @param b
	 * @param c
	 * @return
	 */
	public TransactionVo registerExternalDeal(
			String extClientOrderId, 	
			String ttsInternalAcc,
			String symbol, 
			String notionalCurrency, 
			String clientTradeAction, 
			String currency1Amt, 
			String currency2Amt,
			String limitPrice,
			boolean isOrdTypeFOK, 
			boolean isExternal) {
		TransactionVo transaction = new TransactionVo();
		transaction.setNearDateDetail(new TransactionDetailVo());
		transaction.setProduct(Constants.ProductType.FXSPOT);
		transaction.setIsAutoCover(false);
		transaction.setSettlement(false);
		transaction.setAllocation(false);
		transaction.getNearDateDetail().setTradeDate(fxCalendarBizService.getCurrentBusinessDay(symbol));
		transaction.setTradingSessionId(tradingSessionId);
		transaction.getNearDateDetail().setSelectedTenor(TenorVo.NOTATION_SPOT);
		transaction.setTradeAction(clientTradeAction);
		String providerTradeAction = TradeAction.BUY.equals(clientTradeAction) ? TradeAction.SELL : TradeAction.BUY;
		transaction.setProviderTradeAction(providerTradeAction);
    	transaction.setNotionalCurrency(notionalCurrency);
    	transaction.setAccountId(Long.toString(extClient.getAccountId()));
    	transaction.setCustomerId(Long.toString(extClient.getCustomerId()));
    	transaction.setProviderAccountId(Long.toString(internalLP.getAccountId()));
    	transaction.setProviderCustomerId(Long.toString(internalLP.getCustomerId()));
    	transaction.setSymbol(symbol);
		transaction.setAccountNm(this.tradingAcctNm);
		transaction.setCustomerNm(this.tradingCustNm);
		transaction.setProviderAccountNm(this.providerAcctNm);
		transaction.setProviderCustomerNm(this.providerCustNm);
    	transaction.setRequestType(Constants.RequestType.EXTERNAL_INCOMING_ORDER_FROM_MARKET);
		transaction.setUserId(1);	
		transaction.setChannelId(1L);
		transaction.setChannelNm("SDP");
		transaction.setProduct(Constants.ProductType.FXSPOT);
		transaction.setUserName("system");;;
		transaction.getNearDateDetail().setSpotRate(limitPrice);

		if (USE_SELF_GEN_ID ) {
	    	transaction.setTransRef(UUID.randomUUID().toString());
	    } else {
			try {
				transaction.setStatus(TransStateType.TRADE_PENDING);
				transaction.getNearDateDetail().setCurrency1Amt(currency1Amt);
				transaction.getNearDateDetail().setTradeRate(limitPrice);
				
				TransWrapper tw = db.submitTradeFull(transaction, this.tradingSessionId, 1L, null, internalLP, 1L, null);
				transaction.setTransRef(Long.toString(tw.getTrade().getPk()));
				transaction.setTransId(Long.toString(tw.getTrade().getPk()));
			} catch (Exception e) {
				logger.warn("Error creating transaction record, ", e);
			}
		}
		return transaction;
	}
	
	public void recordCompletedExternalDeal(TransactionVo transaction, String cmt) {
		if ( USE_SELF_GEN_ID) {
			try {
				String dbCmt = cmt;
				if ( dbCmt != null && dbCmt.length() > 255 ) {
					dbCmt = dbCmt.substring(0, 255);
					logger.warn("Comment in DB TRUNCATED!! orgValue: " + cmt);
				}
				TransWrapper tw = db.submitTradeFull(transaction, this.tradingSessionId, 1L, null, internalLP, 1L, dbCmt);
				transaction.setTransId(Long.toString(tw.getTrade().getPk()));
				transaction.setChannelId(tw.getTrade().getChannelId());
				transaction.setChannelNm("SDP");
				transaction.setProduct(Constants.ProductType.FXSPOT);
				transaction.getNearDateDetail().setCurrency2Amt(new NumberVo(tw.getFxTransLegs().get(0).getCurrency2Amt(),  tw.getFxTransLegs().get(0).getCurrency2AmtDec().intValue()).getValue());
				transaction.getNearDateDetail().setSpotRate(transaction.getNearDateDetail().getTradeRate());
				transaction.setStatusMessage(cmt);
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			NumberVo ccy2Amt = NumberVo.getInstance(transaction.getNearDateDetail().getCurrency1Amt()).multiply(NumberVo.getInstance(transaction.getNearDateDetail().getTradeRate()));
			String dbCmt = cmt;
			if ( dbCmt != null && dbCmt.length() > 255 ) {
				dbCmt = dbCmt.substring(0, 255);
				logger.warn("Comment in DB TRUNCATED!! orgValue: " + cmt);
			}
			String ccy2amtStr = ccy2Amt.toPercisionString(2);
			db.updateTrade(
					Long.parseLong(transaction.getTransId()), 
					transaction.getStatus(), 
					(int) AssetClassProductVo.FXSPOT.getAssetClassProductId(), 
					false, 
					dbCmt, 
					transaction.getNearDateDetail().getTradeRate(), 
					"0.0", 
					transaction.getNearDateDetail().getTradeRate(), 
					transaction.getNearDateDetail().getCurrency1Amt(), 
					ccy2amtStr,
					transaction.getNearDateDetail().getValueDate(),
					null,
					null, 
					1L);
			transaction.getNearDateDetail().setCurrency2Amt(ccy2amtStr);
			transaction.getNearDateDetail().setSpotRate(transaction.getNearDateDetail().getTradeRate());
			transaction.setStatusMessage(cmt);
			transaction.setTransTime(System.currentTimeMillis());
		}
		broadcastTransaction(transaction.toMessage(), false, false);
	}
	
	private void broadcastTransaction(Transaction transaction, boolean isUpdate, boolean isClientTrader) {
		try {
			logger.info("Send broadcast transaction to BT." +TextFormat.shortDebugString(transaction));

			String msgType = ClientResponseType.ADD_TRANSACTION_ENTRY;
			if (isUpdate)
				msgType = ClientResponseType.UPDATE_TRANSACTION_ENTRY;

			TransactionSummary.Builder transSummBuilder = TransactionSummary.newBuilder();
			transSummBuilder.addTransactions(transaction);

			BankTransactionSummary.Builder bankTransSummBuilder = BankTransactionSummary.newBuilder();
			// bankTransSummBuilder.setUserSessionInfo(userSession.getUserSessionInfo());
			BankTransaction.Builder bTransBuilder = BankTransaction.newBuilder();
			bTransBuilder.setTransaction(transaction);
			bankTransSummBuilder.addTransaction(bTransBuilder.build());

			TtMsg ttMsg = TtMsgEncoder.encode(msgType, bankTransSummBuilder.build());

			ctrlMsgSender.send(IEventMessageTypeConstant.BankTrader.TRADE_EVENT, ttMsg);

			if (isClientTrader) {
				logger.info("Send to " + IEventMessageTypeConstant.ClientTrader.TRADE_EVENT);
				ttMsg = TtMsgEncoder.encode(msgType, transSummBuilder.build());
				ctrlMsgSender.send(IEventMessageTypeConstant.ClientTrader.TRADE_EVENT, ttMsg);
			}
		} catch (Exception ex) {
			logger.error("Unable to broadcast transaction", ex);
		} finally {
			// safeClose(msgSender);
		}

	}
}
