package com.tts.fa.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.service.ITradingSessionAware;
import com.tts.message.constant.Constants;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest.RateRequestType;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam.QuoteDirection;
import com.tts.message.market.MarketStruct.AdditionalQuoteData;
import com.tts.message.market.MarketStruct.MkBookType;
import com.tts.message.market.MarketStruct.RawLiquidityEntry;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.message.system.SystemStatusStruct.ChangeTradingSession;
import com.tts.message.system.admin.AdapterStruct;
import com.tts.message.system.admin.AdapterStruct.AdapterStatus;
import com.tts.message.system.admin.AdapterStruct.AdapterStatusRequest;
import com.tts.message.system.admin.AdapterStruct.ChangeAdapterSessionStatus;
import com.tts.message.system.admin.AdapterStruct.FixSessionCapability;
import com.tts.message.system.admin.AdapterStruct.SessionStatus;
import com.tts.message.system.admin.AdapterStruct.Status;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.TradeMessage.Transaction;
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
import com.tts.util.AppConfig;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.chronology.ChronoConvUtil;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.exception.InvalidServiceException;
import com.tts.vo.TenorVo;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.StringField;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.HandlInst;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MsgType;
import quickfix.field.NoRelatedSym;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrderQty2;
import quickfix.field.Password;
import quickfix.field.Price;
import quickfix.field.Price2;
import quickfix.field.QuoteCondition;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.field.RawData;
import quickfix.field.RawDataLength;
import quickfix.field.SecurityType;
import quickfix.field.SettlDate;
import quickfix.field.SettlDate2;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.Username;

public class TtsCibcAdapterApp extends MessageCracker
		implements quickfix.Application, ITradingSessionAware, IMsgListener {
	private static final String EXEC_REPORT_TOPIC = String.format(Transactional.TRAN_INFO_TEMPLATE, "FA",
			Data.RateType.MARKET);
	public static final java.lang.String DEFAULT_DELIMITER = "%%%";
	public static final java.lang.String ESP_QUOTE_REF_ID_PREFIX = "ESP%%%";
	public final static SettlType SettlType_SPOT         = new SettlType(SettlType.REGULAR);
	public final static SettlType SETTL_TYPE__ON_FOR_TOD  = new SettlType("ON");
	public final static SettlType SETTL_TYPE__SPOT        = new SettlType("SP");
	public static final SettlType SETTL_TYPE__TN_FOR_TOM = new SettlType("TN");
	public final static SecurityType SecurityType_SPOT = new SecurityType("FXSPOT"); 
	public final static SecurityType SecurityType_FWD  = new SecurityType("FXFWD"); 
	public final static SecurityType SecurityType_SWAP = new SecurityType("FXSWAP"); 
	public final static quickfix.fix50.MarketDataRequest.NoMDEntryTypes FIX50_MarketDataRequest_SIDE_BID;
	public final static quickfix.fix50.MarketDataRequest.NoMDEntryTypes FIX50_MarketDataRequest_SIDE_OFFER;
	
	private final Map<String, String> requestIdTopicOutMap = new ConcurrentHashMap<>();
	private static final String NAME = "CIBC";
	private final static Logger logger = LoggerFactory.getLogger(TtsCibcAdapterApp.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

	private static final String DECIMAL_00 = ".00";
	private static final String TOPIC___ADAPTER_STATUS = String.format(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_INFO_TEMPLATE, NAME);

	static {
		quickfix.fix50.MarketDataRequest.NoMDEntryTypes noMDEntryType44 = null;
		noMDEntryType44 = new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.BID));
		FIX50_MarketDataRequest_SIDE_BID = noMDEntryType44;
		noMDEntryType44 = new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.OFFER));
		FIX50_MarketDataRequest_SIDE_OFFER = noMDEntryType44;
	}

	private final static String[] ctrlTopics = new String[] { 
			IEventMessageTypeConstant.REM.CE_TRADING_SESSION,
			"TTS.CTRL.EVENT.REQUEST.FA.*." +  NAME + ".>", 
			"TTS.TRAN.FX.*.TRANINFO.FA." +  NAME + ".>",
			"TTS.CTRL.EVENT.*.REQUEST.FA.*.*" };

	private final boolean logoutWhileChangeTs = AppConfig.getBooleanValue("fix", "logout_while_change_ts", false);
	private final boolean allowCancelEspDuringSession = AppConfig.getBooleanValue("fix", "allowCancelEspDuringSession", true);
	private final boolean autoCancelEspBeforeLogout = AppConfig.getBooleanValue("fix", "autoCancelEspBeforeLogout", true);
	private final long logonGracePeriod = AppConfig.getIntegerValue("fix", "logon_grace_period", 10) * ChronologyUtil.MILLIS_IN_SECOND;
	
	private final ArrayList<IMsgReceiver> msgReceivers = new ArrayList<IMsgReceiver>();
	private final ConcurrentHashMap<SessionID, Boolean> sessionsInApp = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> symbolRequestIdMap = new ConcurrentHashMap<>();
	private final ScheduledExecutorService schdExctr = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
			 
			@Override
			public Thread newThread(Runnable r) {
				Thread t = defaultThreadFactory.newThread(r);
				t.setName("FaRecurringTaskThread");
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
	private final String acctName;
	private final quickfix.field.OnBehalfOfCompID espOnBehalfOfCompID;
	
 	private volatile SessionID marketDataSessionID = null;
 	private volatile SessionID tradingSessionID = null;
	private volatile long marketDataSessionLastOnline = -1;
	private volatile long marketDataSessionLastOffline = -1;
	private volatile long tradingSessionLastOnline = -1;
	private volatile long tradingSessionLastOffline = -1;
 
 	private volatile IMsgSender msgSender;
 	private volatile IMsgSender execReportSender;
	
	public TtsCibcAdapterApp() {
		super();
		this.acctName = AppConfig.getValue("orderingParam", "AccountNm"); //"DUMMY_TOR";
		this.espOnBehalfOfCompID = new quickfix.field.OnBehalfOfCompID(AppConfig.getValue("orderingParam", "OnBehaveOf"));
	}

	public void postInit() {
		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		for (String ctrlTopic : ctrlTopics) {
			IMsgReceiver msgReceiver = msgReceiverFactory.getMsgReceiver(false, false);
			msgReceiver.setTopic(ctrlTopic);
			msgReceiver.setListener(this);
			msgReceiver.init();
			msgReceivers.add(msgReceiver);
		}
		IMsgSenderFactory msgSenderFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		IMsgSender msgSender = msgSenderFactory.getMsgSender(true, false);
		msgSender.init();

		IMsgSender execReportSender = msgSenderFactory.getMsgSender(false, true);
		execReportSender.init();
		this.msgSender = msgSender;
		this.execReportSender = execReportSender;
	}

	public void destroy() {
		for (IMsgReceiver msgReceiver : msgReceivers) {
			msgReceiver.destroy();
		}
	}

	@Override
	public void onCreate(SessionID sessionId) {
		logger.debug("onCreate:" + sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		logger.debug("onLogon:" + sessionId);
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE,
				AppUtils.getAppName());
		monitorAgent.logInfoNotification("IQfixApp:onLogon", topic,
				MonitorConstant.FXADT.INFO_CONNECTED_TO_MARKET_ACCESS_PLATFORM, "FA",
				"Connected to Market Access Platform, " + sessionId.toString());
		if (sessionId.toString().contains("_ESP_ORD")) {
			tradingSessionID = sessionId;
			tradingSessionLastOnline = System.currentTimeMillis();
		} else if (sessionId.toString().contains("_ESP_PRICES")) {
			marketDataSessionID = sessionId;
			marketDataSessionLastOnline = System.currentTimeMillis();
		}
		sessionsInApp.put(sessionId, Boolean.TRUE);
		
		OnlineNotificationRunnable r = new OnlineNotificationRunnable(marketDataSessionLastOnline, marketDataSessionLastOffline, sessionId);
		if ( logonGracePeriod > 0 ) {
			schdExctr.schedule(r , logonGracePeriod, TimeUnit.MILLISECONDS);
		} else {
			r.run();
		}
	}

	@Override
	public void onLogout(SessionID sessionId) {
		logger.debug("onLogout:" + sessionId);
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE,
				AppUtils.getAppName());
		monitorAgent.logError("IQfixApp:onLogout", topic,
				MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM,
				"Disconnected from Market Access Platform, " + sessionId.toString());
		
		AdapterStatus.Builder as = AdapterStatus.newBuilder();
		as.setAdapterName(NAME);
		as.addActiveSessionsBuilder();
		as.getActiveSessionsBuilder(0).setSessionName(sessionId.toString());
		as.getActiveSessionsBuilder(0).setStatus(Status.INACTIVE);
		as.getActiveSessionsBuilder(0).setSourceNm(NAME);
		if (sessionId.toString().contains("_ESP_PRICES")) {
			marketDataSessionID = null;
			marketDataSessionLastOffline = System.currentTimeMillis();

			as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.MARKET_DATA__ESP);
			as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.MARKET_DATA);
			as.getActiveSessionsBuilder(0).setLastOnlineTimestamp(marketDataSessionLastOnline);
			as.getActiveSessionsBuilder(0).setLastOfflineTimestamp(marketDataSessionLastOffline);
		} else {
			tradingSessionID = null;
			tradingSessionLastOffline = System.currentTimeMillis();
			as.getActiveSessionsBuilder(0).setLastOnlineTimestamp(tradingSessionLastOnline);
			as.getActiveSessionsBuilder(0).setLastOfflineTimestamp(tradingSessionLastOffline);
			as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
			as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.ORDERING_FOK);
			as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.ORDERING_IOC);
		}
		if ( msgSender != null ) {
			msgSender.send(TOPIC___ADAPTER_STATUS, TtMsgEncoder.encode(as.build()));
		}
		sessionsInApp.put(sessionId, Boolean.FALSE);
	}

	@Override
	public void toAdmin(quickfix.Message message, SessionID sessionId) {
		logger.debug("toAdmin:" + sessionId + ":" + message);

		String msgType = null;

		try {
			msgType = message.getHeader().getString(MsgType.FIELD);

			if (MsgType.LOGON.equals(msgType)) {
				String username = AppConfig.getValue("fix", "fix.username");
				if (username != null) {
					String password = AppConfig.getValue("fix", "fix.password");

					if (username != null && !username.isEmpty()) {
						if (FixVersions.BEGINSTRING_FIX42.equals(sessionId.getBeginString())) {
							message.setString(RawData.FIELD, username + "@" + password);
							message.setInt(RawDataLength.FIELD, username.length() + password.length() + 1);
						} else if (FixVersions.BEGINSTRING_FIX44.equals(sessionId.getBeginString())
								|| FixVersions.BEGINSTRING_FIXT11.equals(sessionId.getBeginString())) {
							message.setString(Username.FIELD, username);
							message.setString(Password.FIELD, password);
						}
					}
				}

			}
		} catch (FieldNotFound fnfExp) {
			fnfExp.printStackTrace();
		}
	}

	@Override
	public void fromAdmin(quickfix.Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.debug("fromAdmin:" + sessionId + ":" + message);

		if (message.getHeader().getString(35).equals("3")) {
			String msg = "SessionId: " + sessionId.toString() + ", Received Reject [type '3'] Message : "
					+ message.toString();

			logger.warn(msg);
			String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE,
					AppUtils.getAppName());
			monitorAgent.logInfoNotification("IQfixApp:fromAdmin", topic, MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED,
					"MDE", msg);
		}
	}

	@Override
	public void toApp(quickfix.Message message, SessionID sessionId) throws DoNotSend {
		// TODO Auto-generated method stub

	}

	@Override
	public void fromApp(quickfix.Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		crack(message, sessionId);
	}

	@Override
	public void switchTradingSession(String newTradingSessionName) throws InvalidServiceException {
		SessionID marketDataSessionID = this.marketDataSessionID;
		SessionID tradingSessionID = this.tradingSessionID;
		
		if ( logoutWhileChangeTs ) {
			if ( autoCancelEspBeforeLogout && symbolRequestIdMap != null ) {
				for (Entry<String, String> e : symbolRequestIdMap.entrySet()) {
					if ( marketDataSessionID != null ) {
						quickfix.Message m = buildCancelEspRequestFix(e.getKey(), e.getValue());
						Session sss = Session.lookupSession(marketDataSessionID);
						if ( sss != null ) {
							sss.send(m);
						}
					}
				}
			}
			Session tradingSession = null;
			Session marketDataSession = null;
			if ( tradingSessionID != null ) {
				tradingSession = Session.lookupSession(tradingSessionID);
				if (tradingSession != null ) {
					tradingSession.logout();
				}
			}
			if ( marketDataSessionID != null ) {
				marketDataSession = Session.lookupSession(marketDataSessionID);
				marketDataSession.logout();
			}
			
			symbolRequestIdMap.clear();
			
			try {
				TimeUnit.SECONDS.sleep(20L);
			} catch (InterruptedException e) {
	
			}
			if ( marketDataSessionID != null ) {
				Session.lookupSession(marketDataSessionID).logon();
			}
			try {
				TimeUnit.SECONDS.sleep(5L);
			} catch (InterruptedException e) {
	
			}
			if ( tradingSessionID != null ) {
				Session.lookupSession(tradingSessionID).logon();
			}
		}
	}

	@Handler
	public void onBusinessMessageResponse(quickfix.fix50.ExecutionReport report, SessionID sessionId)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		ExecutionReportInfo.Builder tradeExecutionStatusInfo =  ExecutionReportInfo.newBuilder();
		String orderId = report.getOrderID().getValue();
		String clientOrderId = report.getClOrdID().getValue();
		String status = null;
		
		char orderStatus = report.getOrdStatus().getValue();
		if ( orderStatus == OrdStatus.NEW) {
			status = TransStateType.TRADE_PENDING;
		} else if ( orderStatus == OrdStatus.CANCELED) {
			status = TransStateType.TRADE_CANCEL;
		} else if ( orderStatus == OrdStatus.REJECTED) {
			status = TransStateType.TRADE_REJECT;
		} else if ( orderStatus == OrdStatus.FILLED) {
			status = TransStateType.TRADE_DONE;
		} 
		
		String tradeAction = null;
		char side = report.getSide().getValue();
		
		String ccyPair = report.getSymbol().getValue();
		String notional = ccyPair.substring(0, 3);
		if ( report.isSetCurrency() ) {
			notional = report.getCurrency().getValue();
		}
		tradeExecutionStatusInfo.setCurrency(notional);
		
		
		if ( report.isSetSettlDate() ) {
			tradeExecutionStatusInfo.setSettleDate( report.getSettlDate().getValue() );
		}
		
		String product = com.tts.message.constant.Constants.ProductType.FXSPOT;

		if ( report.isSetSecurityType() ) {
			if ( "FXFWD".equals(report.getSecurityType().getValue())) {
				product = com.tts.message.constant.Constants.ProductType.FXFORWARDS;
			} else if ( "FXSWAP".equals(report.getSecurityType().getValue())) {
				product = com.tts.message.constant.Constants.ProductType.FXSWAP;
			}
		}
		tradeExecutionStatusInfo.setProduct(product);

		boolean isCcy2notional = ccyPair.indexOf(notional) > 2;


		
		if ( "FXSWAP".equals(report.getSecurityType().getValue())) {
			if ( !isCcy2notional) {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.SELL_AND_BUY;
				} else {
					tradeAction = TradeConstants.TradeAction.BUY_AND_SELL;
				}
			} else {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.BUY_AND_SELL;
				} else {
					tradeAction = TradeConstants.TradeAction.SELL_AND_BUY;
				}
			}

		} else {
			if ( !isCcy2notional) {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.BUY;
				} else if(side == Side.SELL)  { 
					tradeAction = TradeConstants.TradeAction.SELL;
				}
			} else {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.SELL;
				} else if(side == Side.SELL)  { 
					tradeAction = TradeConstants.TradeAction.BUY;
				}
			}
		}
		
		String finalPrice = convertRateToString(report.getPrice().getValue());
		if (com.tts.message.constant.Constants.ProductType.FXSWAP.equals(product) ) {
			if ( report.isSetField(640)) {
				String finalPrice2 = (report.getString(640));
				tradeExecutionStatusInfo.setFinalPrice2(finalPrice2);
			}
			
			String size2 = convertSizeToString(report.getOrderQty2().getValue());
			tradeExecutionStatusInfo.setSize2(size2);

			
		}
		if ( report.isSetSettlDate2() ) {
			String settleDate2 =  report.getSettlDate2().getValue();
			tradeExecutionStatusInfo.setSettleDate2(settleDate2);

		}
		
		tradeExecutionStatusInfo.setRefId(orderId);
		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(ccyPair);

		tradeExecutionStatusInfo.setTradeAction(tradeAction);
		tradeExecutionStatusInfo.setSize(convertSizeToString(report.getOrderQty().getValue()));
		

		if ( report.isSetText() ) {
			tradeExecutionStatusInfo.setAdditionalInfo(report.getText().getValue());
		}
		if ( report.isSetTransactTime()) {
			tradeExecutionStatusInfo.setTransactTime(
					ChronologyUtil.getDateTimeSecString(ChronoConvUtil.convertDateTime(report.getTransactTime().getValue()))
				);
		}
		ExecutionReportInfo execReport = tradeExecutionStatusInfo.build();
		execReportSender.send(EXEC_REPORT_TOPIC, TtMsgEncoder.encode(execReport));
	}

	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.MarketDataRequestReject response, SessionID sessionId) {
		long timestamp = System.currentTimeMillis();
		RawMarketBook.Builder mkBookBuilder = RawMarketBook.newBuilder();

		String requestId = null;
		try {
			requestId = response.getMDReqID().getValue();
			mkBookBuilder.setRequestId(requestId);

		} catch (FieldNotFound e) {
			e.printStackTrace();
		}
		mkBookBuilder.setBookStatusFlag(1);
		mkBookBuilder.setUpdateTimeStamp(timestamp);

		RawMarketBook mkBook = mkBookBuilder.build();
		TtMsg ttMsg = TtMsgEncoder.encode(mkBook);
		msgSender.send("TTS.MD.FX.FA.SPOT." + requestId.toUpperCase(), ttMsg);
	}
	
	@Handler
	public void onQuoteResponse(quickfix.fix50.Quote response, SessionID sessionID) throws FieldNotFound {
		long timestamp = System.currentTimeMillis();

		RawMarketBook.Builder mkBookBuilder = RawMarketBook.newBuilder();
		String requestId = response.getQuoteReqID().getValue();
		mkBookBuilder.setRequestId( requestId);
		mkBookBuilder.setQuoteId(response.getQuoteID().getValue());
		String symbol = response.getSymbol().getValue();
		// building the marketData
		mkBookBuilder.setSymbol(symbol);
		mkBookBuilder.setUpdateTimeStamp(timestamp);
		
		if (response.isSetBidPx()) {
			RawLiquidityEntry.Builder tickBuilder = RawLiquidityEntry.newBuilder();
			tickBuilder.setRate(response.getDouble(132));
			if (response.isSetBidSize() ) {
				tickBuilder.setSize((long) response.getBidSize().getValue());
			}
			if ( response.isSetBidForwardPoints()) {
				AdditionalQuoteData.Builder additional = AdditionalQuoteData.newBuilder();
				additional.setFwdPts(response.getDouble(189));
				if ( response.isSetBidSpotRate()) {
					additional.setSpotRate(response.getBidSpotRate().getValue());
				}
				tickBuilder.setAdditionalInfo(additional);
			}
			mkBookBuilder.addBidQuote(tickBuilder);
		}
		
		if (response.isSetOfferPx()) {
			RawLiquidityEntry.Builder tickBuilder = RawLiquidityEntry.newBuilder();

			tickBuilder.setRate(response.getDouble(133));
			if (response.isSetOfferSize() ) {
				tickBuilder.setSize((long) response.getOfferSize().getValue());
			}
			if ( response.isSetOfferForwardPoints()) {
				AdditionalQuoteData.Builder additional = AdditionalQuoteData.newBuilder();
				additional.setFwdPts(response.getDouble(191));
				if ( response.isSetOfferSpotRate()) {
					additional.setSpotRate(response.getOfferSpotRate().getValue());
				}
				tickBuilder.setAdditionalInfo(additional);
			}
			mkBookBuilder.addAskQuote(tickBuilder);
		}

		mkBookBuilder.setAdapter(NAME);
		mkBookBuilder.setMkBookType(MkBookType.LADDER_WITH_MULTI_HIT_ALLOW);
		RawMarketBook mkBook = mkBookBuilder.build();
		TtMsg ttMsg = TtMsgEncoder.encode(mkBook);
		
		String topic = requestIdTopicOutMap.get(requestId);
		msgSender.send(topic, ttMsg);

	}

	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.MarketDataSnapshotFullRefresh response,
			SessionID sessionId) throws FieldNotFound {
		RawMarketBook.Builder mkBookBuilder = RawMarketBook.newBuilder();
		long timestamp = System.currentTimeMillis();
		String symbol = response.getSymbol().getValue();
		
		// building the marketData
		mkBookBuilder.setSymbol(symbol);
		mkBookBuilder.setUpdateTimeStamp(timestamp);
		
		quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
		int noMDEntries = response.getNoMDEntries().getValue();

		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();
			String quoteId = noMDEntry.getMDEntryID().getValue();

			if ( QuoteCondition.CLOSED_INACTIVE.equals(noMDEntry.getQuoteCondition().getValue() )) {
				continue;
			}
			
			if ( ! noMDEntry.isSetMDEntryPx() ) {
				continue;
			}
			
			if ( size <= 0 ) {
				continue;
			}	
			// building the tick
			RawLiquidityEntry.Builder tickBuilder = RawLiquidityEntry.newBuilder();
			tickBuilder.setRate(noMDEntry.getMDEntryPx().getValue());
			tickBuilder.setQuoteId(quoteId);
			tickBuilder.setSize(size);

			// adding tick to marketData
			switch (noMDEntry.getMDEntryType().getValue()) {
				case MDEntryType.BID:
					mkBookBuilder.addBidQuote(tickBuilder);
					break;

				case MDEntryType.OFFER:
					mkBookBuilder.addAskQuote(tickBuilder);
					break;

				default:
					break;
			}
		}
		mkBookBuilder.setAdapter(NAME);
		mkBookBuilder.setMkBookType(MkBookType.LADDER_WITH_MULTI_HIT_ALLOW);
		mkBookBuilder.setRequestId(response.getMDReqID().getValue());
		RawMarketBook mkBook = mkBookBuilder.build();
		TtMsg ttMsg = TtMsgEncoder.encode(mkBook);
		msgSender.send("TTS.MD.FX.FA.SPOT." + symbol + "." + NAME + "." + NAME, ttMsg);
	}
	@Handler
	public void onBusinessMessageResponse(quickfix.fix42.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, AppUtils.getAppName(), responseMessage.toString());
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix50.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, AppUtils.getAppName(), responseMessage.toString());
	}
	@Handler
	public void onQuoteCancel(quickfix.fix50.QuoteCancel responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		logger.info("Received QuoteCancel: " + responseMessage);
	}
	@Override
	public void onMessage(TtMsg arg0, IMsgSessionInfo arg1, IMsgProperties arg2) {
		try {
			String sendTopic = arg2.getSendTopic();
			if (IEventMessageTypeConstant.REM.CE_TRADING_SESSION.equals(sendTopic)) {
				ChangeTradingSession tradingSession = ChangeTradingSession.parseFrom(arg0.getParameters());
				if (tradingSession.hasChangeTo() ) {
					String toTradingSession = tradingSession.getChangeTo().getTradingSessionNm();
					switchTradingSession(toTradingSession);
				}
			} else if (sendTopic.startsWith("TTS.CTRL.EVENT.REQUEST.FA.SUBSCRIBE." + NAME)) {
				onNewPriceSubscriptionRquest(arg0);
			} else if (sendTopic.startsWith("TTS.CTRL.EVENT.REQUEST.FA.UNSUBSCRIBE." + NAME)) {
				onUnsubscribe(arg0);
			} else if (sendTopic.startsWith("TTS.TRAN.FX.MR.TRANINFO.FA." + NAME)) {
				onNewExecutionRequest(arg0, sendTopic);
			} else if (arg0.hasParamType()) {
				if (arg0.getParamType().equals(AdapterStatusRequest.class.getName())) {

					AdapterStatusRequest asr = AdapterStatusRequest.parseFrom(arg0.getParameters());
					if (asr.hasAdapterName()
							&& asr.getAdapterName().equals(NAME)) {

						AdapterStatus.Builder adapterStatusBuilder =  buildStatus();

						msgSender.send(TOPIC___ADAPTER_STATUS, TtMsgEncoder.encode(adapterStatusBuilder.build()));
					}

				} else if (arg0.getParamType().equals(AdapterStruct.ChangeAdapterSessionStatus.class.getName())) {
					ChangeAdapterSessionStatus cass = ChangeAdapterSessionStatus.parseFrom(arg0.getParameters());
					for (Entry<SessionID, Boolean> s : sessionsInApp.entrySet()) {
						if (s.getKey().toString().equals(cass.getSessionName())) {
							Session sss = Session.lookupSession(s.getKey());

							if (sss != null && sss.isLoggedOn() && cass.getNewStatus() == Status.INACTIVE) {
								sss.logout();
							} else if (sss != null && !sss.isLoggedOn() && cass.getNewStatus() == Status.ACTIVE) {
								sss.logon();
							}
						}
					}

				}
			}
		} catch (InvalidProtocolBufferException e) {

		}
	}
	private AdapterStatus.Builder buildStatus() {
		boolean allOnline = true;
		int onlineSession = 0;
		AdapterStatus.Builder adapterStatusBuilder = AdapterStatus.newBuilder();
		adapterStatusBuilder.setAdapterName(NAME);
		for (Entry<SessionID, Boolean> s : sessionsInApp.entrySet()) {
			boolean sessionOnline = s.getValue();
			String sessionNm = s.getKey().toString();

			if ( sessionOnline ) {
				onlineSession++;
			}
			SessionStatus.Builder ss = SessionStatus.newBuilder();
			ss.setSessionName(sessionNm);
			ss.setStatus(sessionOnline ? Status.ACTIVE : Status.INACTIVE);
			ss.setSourceNm(NAME);
			if ( sessionNm.indexOf("_ESP_PRICES" ) > 0) {
				ss.addCapability(FixSessionCapability.MARKET_DATA);
				ss.addCapability(FixSessionCapability.MARKET_DATA__ESP);
				ss.setLastOnlineTimestamp(marketDataSessionLastOnline);
				ss.setLastOfflineTimestamp(marketDataSessionLastOffline);
			} else {
				ss.addCapability(FixSessionCapability.ORDERING_FOK);
				ss.addCapability(FixSessionCapability.ORDERING_IOC);
				ss.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
				ss.setLastOnlineTimestamp(tradingSessionLastOnline);
				ss.setLastOfflineTimestamp(tradingSessionLastOffline);
			}
			
			adapterStatusBuilder.addActiveSessions(ss);
			allOnline = sessionOnline && allOnline;
		}
		
		adapterStatusBuilder.setStatus(allOnline && onlineSession == 2 ? Status.ACTIVE : Status.INACTIVE);
		return adapterStatusBuilder;
	}
	private void onUnsubscribe(TtMsg arg0) {
		if ( allowCancelEspDuringSession ) {
			try {
				PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(arg0.getParameters());
				String symbol = request.getQuoteParam().getCurrencyPair();
				String requestId = request.getRequestId();
				logger.info(String.format("Unsubscribing market data for <%s>: reqId = %s", symbol, requestId));
				if ( marketDataSessionID != null ) {
					quickfix.Message m = buildCancelEspRequestFix(symbol, requestId);
					Session sss = Session.lookupSession(marketDataSessionID);
					if ( sss != null ) {
						sss.send(m);
					}
				}
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
	}

	private void onNewExecutionRequest(TtMsg arg0, String sendTopic) {
		Transaction transactionMessage = null;
		try {
			transactionMessage = Transaction.parseFrom(arg0.getParameters());
			String transId = transactionMessage.getTransId();
			logger.info(String.format("Received Hedging Request from %s for transId <%s>: %s", sendTopic, transId,
					TextFormat.shortDebugString(transactionMessage)));
			String amount = transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) > 0 ? transactionMessage.getNearDateDetail().getCurrency2Amt() : transactionMessage.getNearDateDetail().getCurrency1Amt();
			String quoteId = transactionMessage.getOrderParams().getQuoteRefId();
			String settleType = null;
			String onBehaveOf = null;
			String settleDate = transactionMessage.getNearDateDetail().getValueDate();
			String settleDate2 = transactionMessage.hasFarDateDetail() ? transactionMessage.getFarDateDetail().getValueDate() : null;
			String amountFar = transactionMessage.hasFarDateDetail() ? transactionMessage.getFarDateDetail().getCurrency1Amt() : null;

			SecurityType securityType = null;
			String product = transactionMessage.getProduct();
			String clientOrderId = transactionMessage.getTransId();
			if ( Constants.ProductType.FXSPOT.equals(product)) {
				securityType = SecurityType_SPOT;
			} else if ( Constants.ProductType.FXFORWARDS.equals(product)
					|| Constants.ProductType.FXTIMEOPTION.equals(product)) {
				securityType = SecurityType_FWD;
			} else if ( Constants.ProductType.FXSWAP.equals(product) ) {
				securityType = SecurityType_SWAP;
			}
			quickfix.fix50.NewOrderSingle message = new quickfix.fix50.NewOrderSingle();
			message.set(new OrdType(OrdType.PREVIOUSLY_QUOTED));
			message.set(new ClOrdID(clientOrderId));
			message.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC));

			message.set(new QuoteID(quoteId));
			message.set(new TimeInForce(TimeInForce.FILL_OR_KILL));
			message.set(new Symbol(transactionMessage.getSymbol()));
			message.set(securityType);
			message.set(new Currency(transactionMessage.getNotionalCurrency()));
			
			if ( TradeAction.BUY.equals(transactionMessage.getTradeAction() )) {
				message.set(new Side(Side.BUY));
			} else if ( TradeAction.SELL.equals(transactionMessage.getTradeAction() )) {
				message.set(new Side(Side.SELL));
			} else if ( TradeAction.BUY_AND_SELL.equals(transactionMessage.getTradeAction())) {
				message.set(new Side(Side.SELL));
			} else if ( TradeAction.SELL_AND_BUY.equals(transactionMessage.getTradeAction())) {
				message.set(new Side(Side.BUY));
			} 
			
			if ( settleType != null && !settleType.trim().isEmpty() ) {
				message.set(new SettlType(settleType));
			} else {
				message.set(SettlType_SPOT);
			}
			if ( amount.indexOf(".") < 0) {
				amount = amount + DECIMAL_00;
			}
			message.set(new OrderQty(Double.parseDouble(amount)));
			message.set(new SettlDate(settleDate));
			if ( settleDate2 != null ) {
				message.set(new SettlDate2(settleDate2));
			}
			if ( Constants.ProductType.FXSWAP.equals(product) ) {
				message.set(new Price2(Double.parseDouble(transactionMessage.getFarDateDetail().getTradeRate())));
				if ( amountFar.indexOf(".") < 0) {
					amountFar = amountFar + DECIMAL_00;
				}
				message.set(new OrderQty2(Double.parseDouble(amountFar)));
				message.set(new OrdType(OrdType.FOREX_SWAP));
			}
			message.set(new Account(this.acctName));
			message.set(new Price(Double.parseDouble(transactionMessage.getNearDateDetail().getTradeRate())));

			if ( transactionMessage.hasTransTime()  ) {
				message.set(new TransactTime(new java.util.Date(transactionMessage.getTransTime())));
			} else {
				message.set(new TransactTime());
			}
			message.getHeader().setField(new OnBehalfOfCompID(onBehaveOf));
			logger.debug(String.format("Prev-Quoted FOK Order from %s sent", sendTopic));
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	private void onNewPriceSubscriptionRquest(TtMsg arg0) {
		try {
			
			PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(arg0.getParameters());
			String symbol = request.getQuoteParam().getCurrencyPair();
			logger.info(String.format("Received PriceSubscriptionRequest for <%s>: %s", symbol, 
					TextFormat.shortDebugString(request)));
			if ( RateRequestType.ESP == request.getRateRequestType()) {
				doHandleEspRequest(request, symbol);
			} else 	if ( RateRequestType.RFS == request.getRateRequestType()) {
				doHandleRfsRequest(request, symbol);
			}

		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	private void doHandleRfsRequest(PriceSubscriptionRequest request, String symbol) {
		String settleDate = request.getQuoteParam().hasNearDateDetail() && request.getQuoteParam().getNearDateDetail().hasActualDate() ? 
				request.getQuoteParam().getNearDateDetail().getActualDate():
					null;
		String settleDate2 = request.getQuoteParam().hasFarDateDetail() && request.getQuoteParam().getFarDateDetail().hasActualDate() ? 
						request.getQuoteParam().getFarDateDetail().getActualDate():
							null;
		String tenor  = null;
		String tenor2 = null;
		String rfsOutTopic = request.getTopic();
		
		if ( request.getQuoteParam().hasNearDateDetail() && request.getQuoteParam().getNearDateDetail().hasPeriodCd() ) {
			TenorVo.Builder t = new TenorVo.Builder();
			t.setPeriodCd(request.getQuoteParam().getNearDateDetail().getPeriodCd());
			t.setValue(Integer.parseInt(request.getQuoteParam().getNearDateDetail().getPeriodValue()));
			tenor = t.build().toString();
		}
		if ( request.getQuoteParam().hasFarDateDetail() && request.getQuoteParam().getFarDateDetail().hasPeriodCd() ) {
			TenorVo.Builder t = new TenorVo.Builder();
			t.setPeriodCd(request.getQuoteParam().getFarDateDetail().getPeriodCd());
			t.setValue(Integer.parseInt(request.getQuoteParam().getFarDateDetail().getPeriodValue()));
			tenor2 = t.build().toString();
		}
		String requestId = request.getRequestId();

		quickfix.fix50.QuoteRequest quoteRequest = new quickfix.fix50.QuoteRequest(new QuoteReqID(requestId));
		quoteRequest.set(new NoRelatedSym(1));
		
		quickfix.fix50.QuoteRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.QuoteRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(symbol));
		
		String ccy1 = symbol.substring(0, 3);
		String ccy2 = symbol.substring(3, 6);

		noRelatedSym.set(new Currency(request.getQuoteParam().getNotionalCurrency()));
		
		noRelatedSym.set(new OrderQty(new BigDecimal(request.getQuoteParam().getSize()).longValue()));
		
		if ( request.getQuoteParam().getQuoteDirection() == QuoteDirection.BUY ) {
			noRelatedSym.set(new Side(Side.BUY));
		} else if ( request.getQuoteParam().getQuoteDirection() == QuoteDirection.SELL ) {
			noRelatedSym.set(new Side(Side.SELL));
		} else  if (request.getQuoteParam().getQuoteDirection() == QuoteDirection.BUY_AND_SELL ) {
			if ( symbol.indexOf(request.getQuoteParam().getNotionalCurrency()) > 0 ) {
				noRelatedSym.set(new Side(Side.BUY));
			} else {
				noRelatedSym.set(new Side(Side.SELL));
			}
		} else if ( request.getQuoteParam().getQuoteDirection() == QuoteDirection.SELL_AND_BUY ) {
			if ( symbol.indexOf(request.getQuoteParam().getNotionalCurrency()) > 0 ) {
				noRelatedSym.set(new Side(Side.SELL));
			} else {
				noRelatedSym.set(new Side(Side.BUY));
			}
		} else  {
			noRelatedSym.set(new Side('0'));
		}
		
		if ( Constants.ProductType.FXSWAP.equals(request.getQuoteParam().getProduct())) {
			noRelatedSym.set(SecurityType_SWAP);

			if ( settleDate == null ) {
				if ( TenorVo.NOTATION_TODAY.equals(tenor)) {
					noRelatedSym.set(SETTL_TYPE__ON_FOR_TOD);
				} else if ( TenorVo.NOTATION_TOMORROW.equals(tenor)) {
					noRelatedSym.set(SETTL_TYPE__TN_FOR_TOM);
				} else if (tenor != null && !tenor.isEmpty()){
					noRelatedSym.set(new SettlType(tenor));
				}
			}
			noRelatedSym.set(new SettlDate(settleDate));
			
			if ( settleDate2 == null ) {
				if ( TenorVo.NOTATION_TODAY.equals(tenor2)) {
					noRelatedSym.setField(new StringField(6363, "ON"));
				} else if ( TenorVo.NOTATION_TOMORROW.equals(tenor2)) {
					noRelatedSym.setField(new StringField(6363, "TN"));
				} else if (tenor2 != null && !tenor2.isEmpty()){
					noRelatedSym.setField(new StringField(6363, tenor2));
				}
			}
			noRelatedSym.set(new OrderQty2(new BigDecimal(request.getQuoteParam().getSizeFar()).longValue()));
			noRelatedSym.set(new SettlDate2(settleDate2));
		} else if ( TenorVo.NOTATION_SPOT.equals(tenor) ) {
			noRelatedSym.set(SecurityType_SPOT);
			noRelatedSym.set(SETTL_TYPE__SPOT);
			if ( settleDate != null && !settleDate.isEmpty()) {
				noRelatedSym.set(new SettlDate(settleDate));
			} else {
				//noRelatedSym.set(new SettlDate("19700101"));
			}
		} else {
			noRelatedSym.set(SecurityType_FWD);
			if ( TenorVo.NOTATION_TODAY.equals(tenor)) {
				noRelatedSym.set(SETTL_TYPE__ON_FOR_TOD);
			} else if ( TenorVo.NOTATION_TOMORROW.equals(tenor)) {
				noRelatedSym.set(SETTL_TYPE__TN_FOR_TOM);
			} else if (tenor != null && !tenor.isEmpty() && !tenor.startsWith(TenorVo.NOTATION_UNIT_IMM)){
				noRelatedSym.set(new SettlType(tenor));
			}
			noRelatedSym.set(new SettlDate(settleDate));
		}
		noRelatedSym.setField(new StringField(6065, Long.toString(135)));
		quoteRequest.addGroup(noRelatedSym);
		quoteRequest.setField(new StringField(1, this.acctName));
		Session sss = marketDataSessionID == null?  null : Session.lookupSession(marketDataSessionID);
		requestIdTopicOutMap.put(requestId, rfsOutTopic);
		
		if ( sss != null && sss.isLoggedOn() 
				&& (logonGracePeriod <= 0 || ((System.currentTimeMillis() - marketDataSessionLastOnline) >  logonGracePeriod)) ) {
			logger.info(String.format("Subscribing market data for <%s>: reqId = %s", symbol, requestId));

			sss.send(quoteRequest);
			symbolRequestIdMap.put(symbol, requestId);
		} else {
			logger.warn(String.format("NOT Subscribing market data for %s as FIX session is not online or in the grace period after logon", symbol));
		}
		
	}

	public void doHandleEspRequest(PriceSubscriptionRequest request, String symbol) {
		if ( !symbolRequestIdMap.contains(symbol)) {
			String requestId = request.getRequestId();

			quickfix.fix50.MarketDataRequest marketDataRequest = new quickfix.fix50.MarketDataRequest(
					new MDReqID(requestId),
					new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES),
					new MarketDepth(0));
			if ( espOnBehalfOfCompID != null  ) {
				marketDataRequest.getHeader().setField(espOnBehalfOfCompID);
			} 
			marketDataRequest.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
			marketDataRequest.set(new NoRelatedSym(1));

			marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_BID);
			marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_OFFER);

			quickfix.fix50.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.MarketDataRequest.NoRelatedSym();
			noRelatedSym.set(new Symbol(symbol));
			if ( symbol != null && symbol.length() >3 ) {
				String currency = symbol.substring(0, 3);
				noRelatedSym.set(new Currency(currency));
			}

			String tenor = !request.getQuoteParam().hasNearDateDetail() ? TenorVo.NOTATION_SPOT : "";
			String settleDate = "";

			if ( TenorVo.NOTATION_SPOT.equals(tenor) ) {
				noRelatedSym.set(SecurityType_SPOT);
				noRelatedSym.set(SettlType_SPOT);
			} else {
				noRelatedSym.set(SecurityType_FWD);
				if(tenor.equals(TenorVo.NOTATION_OVERNIGHT)) {
					noRelatedSym.set(new SettlType(SettlType.CASH));
				} else if(tenor.equals(TenorVo.NOTATION_TOMORROWNIGHT))   {
					noRelatedSym.set(new SettlType(SettlType.NEXT_DAY));
				} else if(tenor.equals(TenorVo.NOTATION_SPOTNIGHT))   {
					noRelatedSym.set(new SettlType(SettlType.FX_SPOT_NEXT_SETTLEMENT));
				} else {
					String revisedTenor = "";
					//Just need to move the W or M or Y to the beginning of string to get into a FIX compatible form
					if(tenor.contains(TenorVo.NOTATION_UNIT_WEEK)) {
						revisedTenor = TenorVo.NOTATION_UNIT_WEEK+tenor.replace(TenorVo.NOTATION_UNIT_WEEK,"");
					} else if(tenor.contains(TenorVo.NOTATION_UNIT_MONTH)) {
						revisedTenor = TenorVo.NOTATION_UNIT_MONTH+tenor.replace(TenorVo.NOTATION_UNIT_MONTH,"");
					} if(tenor.contains(TenorVo.NOTATION_UNIT_YEAR)) {
						revisedTenor = TenorVo.NOTATION_UNIT_YEAR+tenor.replace(TenorVo.NOTATION_UNIT_YEAR,"");
					}
					noRelatedSym.set(new SettlType(revisedTenor));

				}
				if ( TenorVo.NOTATION_SPOT.equals(tenor) ) {
					if(settleDate != null) {
						noRelatedSym.set(new SettlDate(settleDate));
					} else {
						//Put in a dummy date to get around Conditionally Required Field Missing (64) i keep getting!!
						noRelatedSym.set(new SettlDate("19700101"));
					}
				}
			}
			marketDataRequest.addGroup(noRelatedSym);
			
			@SuppressWarnings("resource")
			Session sss = marketDataSessionID == null?  null : Session.lookupSession(marketDataSessionID);
			if ( sss != null && sss.isLoggedOn() 
					&& (logonGracePeriod <= 0 || ((System.currentTimeMillis() - marketDataSessionLastOnline) >  logonGracePeriod)) ) {
				logger.info(String.format("Subscribing market data for <%s>: reqId = %s", symbol, requestId));

				sss.send(marketDataRequest);
				symbolRequestIdMap.put(symbol, requestId);
			} else {
				logger.warn(String.format("NOT Subscribing market data for %s as FIX session is not online or in the grace period after logon", symbol));
			}
				
		} else {
			logger.warn(String.format("NOT Subscribing market data for %s as we have already subscribed.", symbol));
		}
	}

	private quickfix.Message buildCancelEspRequestFix(String symbol, String requestId) {
		quickfix.fix50.MarketDataRequest marketDataRequest = new quickfix.fix50.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST),
				new MarketDepth(1));
		String sym = symbol;
		if ( symbol.indexOf('/') < 0 ) {
			sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
		}
		marketDataRequest.set(new NoRelatedSym(1));

		marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_BID);
		marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_OFFER);

		quickfix.fix50.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.MarketDataRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(sym));

		marketDataRequest.addGroup(noRelatedSym);
		logger.info(String.format("Built Esp Cancellation Request for <%s>: %s", symbol, requestId));
		return marketDataRequest;	
	}

	public static String convertRateToString(double d) {
		BigDecimal bd = new BigDecimal(d, MathContext.DECIMAL64);
		return bd.stripTrailingZeros().toPlainString();
	}

	public static String convertSizeToString(double d) {
		return String.format("%.2f", d);
	}
	private class OnlineNotificationRunnable implements Runnable {

		private final SessionID sessionId;
		private final long marketDataSessionLastOnlineLastValue;
		private final long marketDataSessionLastOfflineLastValue;
		
		private OnlineNotificationRunnable(long marketDataSessionLastOnline, long marketDataSessionLastOffline, SessionID sessionId) {
			super();
			this.sessionId = sessionId;
			this.marketDataSessionLastOfflineLastValue = marketDataSessionLastOffline;
			this.marketDataSessionLastOnlineLastValue = marketDataSessionLastOnline;
		}

		@Override
		public void run() {
			if ( marketDataSessionLastOnlineLastValue != marketDataSessionLastOnline
					|| marketDataSessionLastOfflineLastValue != marketDataSessionLastOffline) {
				//session status has changed... this task is no longer valid
				return;
			}
			AdapterStatus.Builder as = AdapterStatus.newBuilder();
			as.setAdapterName(NAME);
			as.addActiveSessionsBuilder();
			as.getActiveSessionsBuilder(0).setSessionName(sessionId.toString());
			as.getActiveSessionsBuilder(0).setStatus(Status.ACTIVE);
			as.getActiveSessionsBuilder(0).setSourceNm(NAME);

			if (sessionId.toString().contains("STREAMING")) {
				as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.MARKET_DATA__ESP);
				as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.MARKET_DATA);
				as.getActiveSessionsBuilder(0).setLastOnlineTimestamp(marketDataSessionLastOnline);
				as.getActiveSessionsBuilder(0).setLastOfflineTimestamp(marketDataSessionLastOffline);
			} else {
				as.getActiveSessionsBuilder(0).setLastOnlineTimestamp(tradingSessionLastOnline);
				as.getActiveSessionsBuilder(0).setLastOfflineTimestamp(tradingSessionLastOffline);
				as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
				as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.ORDERING_FOK);
				as.getActiveSessionsBuilder(0).addCapability(FixSessionCapability.ORDERING_IOC);
			}	
			if ( msgSender != null ) {
				msgSender.send(TOPIC___ADAPTER_STATUS, TtMsgEncoder.encode(as.build()));
			}
		}
		
	}
}
