package com.tts.fa.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
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
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
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
import com.tts.message.trade.RestingOrderMessage.OrderParams;
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
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.exception.InvalidServiceException;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.AggregatedBook;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MsgType;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Password;
import quickfix.field.Price;
import quickfix.field.QuoteCondition;
import quickfix.field.RawData;
import quickfix.field.RawDataLength;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.Username;

public class TtsTradAirAdapterApp extends MessageCracker
		implements quickfix.Application, ITradingSessionAware, IMsgListener {
	private static final String EXEC_REPORT_TOPIC = String.format(Transactional.TRAN_INFO_TEMPLATE, "FA",
			Data.RateType.MARKET);

	private final static int tagForOrderRef = quickfix.field.ExecID.FIELD;

	private final static Logger logger = LoggerFactory.getLogger(TtsTradAirAdapterApp.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	private static final String NAME = "TRADAIR";

	public final static quickfix.fix44.MarketDataRequest.NoMDEntryTypes FIX44_MarketDataRequest_SIDE_BID;
	public final static quickfix.fix44.MarketDataRequest.NoMDEntryTypes FIX44_MarketDataRequest_SIDE_OFFER;
	private static final String TOPIC___ADAPTER_STATUS = String.format(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_INFO_TEMPLATE, NAME);
	private static final String DECIMAL_00 = ".00";
	private static final char DEFAULT_TIME_IN_FORCE = TimeInForce.FILL_OR_KILL;
	private static final char DEFAULT_ORD_TYPE = OrdType.FOREX_LIMIT;

	static {
		quickfix.fix44.MarketDataRequest.NoMDEntryTypes noMDEntryType44 = null;
		noMDEntryType44 = new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.BID));
		FIX44_MarketDataRequest_SIDE_BID = noMDEntryType44;
		noMDEntryType44 = new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.OFFER));
		FIX44_MarketDataRequest_SIDE_OFFER = noMDEntryType44;
	}

	private final static String[] ctrlTopics = new String[] { 
			IEventMessageTypeConstant.REM.CE_TRADING_SESSION,
			"TTS.CTRL.EVENT.REQUEST.FA.*.TRADAIR.>", 
			"TTS.TRAN.FX.*.TRANINFO.FA.TRADAIR.>", // TTS.TRAN.FX.MR.TRANINFO.FA.TRADAIR
			IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_ALL_REQUEST_EVENT };

	private final boolean logoutWhileChangeTs = AppConfig.getBooleanValue("fix", "logout_while_change_ts", true);
	private final boolean allowCancelEspDuringSession = AppConfig.getBooleanValue("fix", "allowCancelEspDuringSession", false);
	private final boolean autoCancelEspBeforeLogout = AppConfig.getBooleanValue("fix", "autoCancelEspBeforeLogout", true);
	private final long logonGracePeriod = AppConfig.getIntegerValue("fix", "logon_grace_period", 30) * ChronologyUtil.MILLIS_IN_SECOND;
	
	private final ArrayList<IMsgReceiver> msgReceivers = new ArrayList<IMsgReceiver>();
	private final ConcurrentHashMap<SessionID, Boolean> sessionsInApp = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> symbolRequestIdMap = new ConcurrentHashMap<>();
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
	
	private volatile SessionID marketDataSessionID = null;
	private volatile SessionID tradingSessionID = null;
	private volatile long marketDataSessionLastOnline = -1;
	private volatile long marketDataSessionLastOffline = -1;
	private volatile long tradingSessionLastOnline = -1;
	private volatile long tradingSessionLastOffline = -1;

	private volatile IMsgSender msgSender;
	private volatile IMsgSender execReportSender;

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
		this.schdExctr.shutdownNow();
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
		
		if (sessionId.toString().contains("STREAMING")) {
			marketDataSessionID = sessionId;
			marketDataSessionLastOnline = System.currentTimeMillis();
		} else {
			tradingSessionID = sessionId;
			tradingSessionLastOnline = System.currentTimeMillis();
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

		if (sessionId.toString().contains("STREAMING")) {
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
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {

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

	@SuppressWarnings("unused")
	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.ExecutionReport report, SessionID sessionId)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

		String orderId = "";
		String execId = "";
		String tag8000value = "";
		String cmt = null;

		if (report.isSetOrderID())
			orderId = report.getOrderID().getValue();
		if (report.isSetExecID()) {
			execId = report.getExecID().getValue();
		}
		if (report.isSetField(8000)) {
			tag8000value = report.getString(8000);
		}

		String clientOrderId = report.getClOrdID().getValue();
		String status = "";

		char orderStatus = report.getOrdStatus().getValue();
		if (orderStatus == OrdStatus.NEW) {
			status = TransStateType.TRADE_PENDING;
		} else if (orderStatus == OrdStatus.CANCELED) {
			status = TransStateType.TRADE_CANCEL;
		} else if (orderStatus == OrdStatus.REJECTED) {
			status = TransStateType.TRADE_REJECT;
		} else if (orderStatus == OrdStatus.PARTIALLY_FILLED) {
			status = TransStateType.TRADE_PARTIALLY_DONE;
		} else if (orderStatus == OrdStatus.FILLED) {
			status = TransStateType.TRADE_DONE;
		}

		String tradeAction = null;
		char side = report.getSide().getValue();
		String symbol = report.getSymbol().getValue().replaceFirst("/", "");

		if (orderStatus == OrdStatus.FILLED || orderStatus == OrdStatus.PARTIALLY_FILLED) {
			String currency1 = symbol.substring(0, 3);
			String currency2 = symbol.substring(3);
			String notionalCurrency = report.getCurrency().getValue();

			if (notionalCurrency.equalsIgnoreCase(currency1)) {
				tradeAction = (side == Side.BUY) ? TradeConstants.TradeAction.BUY : TradeConstants.TradeAction.SELL;
			}
			if (notionalCurrency.equals(currency2)) {
				tradeAction = (side == Side.BUY) ? TradeConstants.TradeAction.SELL : TradeConstants.TradeAction.BUY;
			}

			StringBuilder sb = new StringBuilder();
			sb.append(17).append('=').append(execId).append(' ');
			sb.append(37).append('=').append(orderId).append(' ');
			sb.append(8000).append('=').append(tag8000value);
			cmt = sb.toString();
		} else {
			if (side == Side.BUY) {
				tradeAction = TradeConstants.TradeAction.BUY;
			} else if (side == Side.SELL) {
				tradeAction = TradeConstants.TradeAction.SELL;
			}
			if (report.isSetText()) {
				cmt = report.getText().getValue();
			}
		}

		String finalPrice = "0";
		String orderPrice = "0";
		if (report.isSetLastPx()) {
			finalPrice = convertRateToString(report.getLastPx().getValue());
		}
		if (report.isSetPrice()) {
			orderPrice = convertRateToString(report.getPrice().getValue());
			cmt = cmt + " ordPrice=" + orderPrice;
		}
		ExecutionReportInfo.Builder tradeExecutionStatusInfo = ExecutionReportInfo.newBuilder();
		if (tagForOrderRef == quickfix.field.OrderID.FIELD) {
			tradeExecutionStatusInfo.setRefId(orderId);
		} else if (tagForOrderRef == quickfix.field.ExecID.FIELD) {
			tradeExecutionStatusInfo.setRefId(execId);
		} else if (tagForOrderRef == 8000) {
			tradeExecutionStatusInfo.setRefId(tag8000value);
		}

		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSpotRate(finalPrice);
		tradeExecutionStatusInfo.setAllInPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(symbol);
		if (report.isSetCurrency()) {
			tradeExecutionStatusInfo.setCurrency(report.getCurrency().getValue());
		}
		if (tradeAction != null) {
			tradeExecutionStatusInfo.setTradeAction(tradeAction);
		}

		double filledQty, requestQty;
		requestQty = report.getOrderQty().getValue();
		if (report.isSetLastQty()) {
			filledQty = report.getLastQty().getValue();
		} else {
			filledQty = report.getOrderQty().getValue();
		}
		if (Double.compare(filledQty, requestQty) != 0) {
			tradeExecutionStatusInfo.setSize(convertSizeToString(filledQty));
			tradeExecutionStatusInfo.setOriginalSize(convertSizeToString(requestQty));
		} else {
			tradeExecutionStatusInfo.setSize(convertSizeToString(filledQty));
			tradeExecutionStatusInfo.setOriginalSize(convertSizeToString(filledQty));
		}

		if (report.isSetOrdRejReason()) {
			cmt = report.getOrdRejReason().toString();
		}
		if (report.isSetTransactTime()) {
			tradeExecutionStatusInfo.setTransactTime(ChronologyUtil
					.getDateTimeSecString(ChronoConvUtil.convertDateTime(report.getTransactTime().getValue())));
		}

		if (report.isSetSettlDate()) {
			tradeExecutionStatusInfo.setSettleDate(report.getSettlDate().getValue());
		}

		if (cmt != null) {
			tradeExecutionStatusInfo.setAdditionalInfo(cmt);
		}
		ExecutionReportInfo execReport = tradeExecutionStatusInfo.build();
		execReportSender.send(EXEC_REPORT_TOPIC, TtMsgEncoder.encode(execReport));
	}

	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataRequestReject response, SessionID sessionId) {
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
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataSnapshotFullRefresh response,
			SessionID sessionId) {
		long timestamp = System.currentTimeMillis();
		RawMarketBook.Builder mkBookBuilder = RawMarketBook.newBuilder();

		String symbol = null;
		try {
			String requestId = response.getMDReqID().getValue();
			String sentTime = response.getHeader().getString(52);
			mkBookBuilder.setQuoteId(sentTime);
			symbol = response.getSymbol().getValue();

			if (symbol != null) {
				symbol = symbol.replaceFirst("/", "");
			}
			mkBookBuilder.setSymbol(symbol);
			mkBookBuilder.setRequestId(requestId);
			quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
			int noMDEntries = response.getNoMDEntries().getValue();

			int tradableRung = 0, indicativeRung = 0;
			long indicativeFlag = IndicativeFlag.TRADABLE;

			for (int i = 1; i <= noMDEntries; i++) {
				noMDEntry = new quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries();
				response.getGroup(i, noMDEntry);
				long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();

				if (noMDEntry.isSetQuoteCondition()) {
					if (QuoteCondition.CLOSED_INACTIVE.equals(noMDEntry.getQuoteCondition().getValue())) {
						indicativeRung++;
						continue;
					}
				}

				if (!noMDEntry.isSetMDEntryPx()) {
					continue;
				}

				tradableRung++;
				// building the tick
				RawLiquidityEntry.Builder tickBuilder = RawLiquidityEntry.newBuilder();
				tickBuilder.setRate(noMDEntry.getMDEntryPx().getValue());
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
			if (tradableRung == 0 && indicativeRung > 0) {
				indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_Indicative_Data);
			}
			mkBookBuilder.setAdapter(NAME);
			mkBookBuilder.setMkBookType(MkBookType.LADDER_WITH_MULTI_HIT_ALLOW);
			mkBookBuilder.setUpdateTimeStamp(timestamp);
		} catch (FieldNotFound e) {
			e.printStackTrace();
		}
		RawMarketBook mkBook = mkBookBuilder.build();
		TtMsg ttMsg = TtMsgEncoder.encode(mkBook);
		msgSender.send("TTS.MD.FX.FA.SPOT." + symbol + ".TRADAIR.TRADAIR", ttMsg);
	}
	@Handler
	public void onBusinessMessageResponse(quickfix.fix42.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, AppUtils.getAppName(), responseMessage.toString());
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, AppUtils.getAppName(), responseMessage.toString());
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
			} else if (sendTopic.startsWith("TTS.CTRL.EVENT.REQUEST.FA.SUBSCRIBE.TRADAIR")) {
				onNewPriceSubscriptionRquest(arg0);
			} else if (sendTopic.startsWith("TTS.CTRL.EVENT.REQUEST.FA.UNSUBSCRIBE.TRADAIR")) {
				onUnsubscribe(arg0);
			} else if (sendTopic.startsWith("TTS.TRAN.FX.MR.TRANINFO.FA.TRADAIR")) {
				onNewExecutionRequest(arg0, sendTopic);
			} else if (arg0.hasParamType()) {
				if (arg0.getParamType().equals(AdapterStatusRequest.class.getName())) {

					AdapterStatusRequest asr = AdapterStatusRequest.parseFrom(arg0.getParameters());
					if (asr.hasAdapterName()
							&& asr.getAdapterName().equals(NAME)) {

						AdapterStatus.Builder adapterStatusBuilder = buildStatus();
						adapterStatusBuilder.setRequestId(asr.getRequestId());
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

			if ( sessionNm.indexOf("-STREAMING" ) > 0) {
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
		String mktOrdPrice = "";
		Transaction transactionMessage = null;
		try {
			transactionMessage = Transaction.parseFrom(arg0.getParameters());
			String transId = transactionMessage.getTransId();
			logger.info(String.format("Received Hedging Request from %s for transId <%s>: %s", sendTopic, transId,
					TextFormat.shortDebugString(transactionMessage)));

			String amount = getMarketTradeDataInfo(transactionMessage, "AMT");
			String side = getMarketTradeDataInfo(transactionMessage, "SIDE");

			long currentTime = System.currentTimeMillis();
			String priceTime = "";
			OrderParams orderParams = null;

			if (transactionMessage.hasOrderParams()) {
				orderParams = transactionMessage.getOrderParams();
			}
			if (transactionMessage.hasOrderParams() && transactionMessage.getOrderParams().hasTargetPrice()) {
				if (transactionMessage.hasQuoteRefId()
						&& transactionMessage.getOrderParams().getQuoteRefId().length() > 1) {
					priceTime = transactionMessage.getOrderParams().getQuoteRefId();
				} else
					priceTime = transactionMessage.getOrderParams().getQuoteRefId();
				mktOrdPrice = transactionMessage.getOrderParams().getTargetPrice();
			}
			String msg = (currentTime + " MktTime(LQVP):" + priceTime);

			System.out.println(msg);
			logger.info("price latency message: " + msg);

			if ((mktOrdPrice == null) || (mktOrdPrice.trim() == "")) {
				// TODO Action when price is EMPTY
			}

			Session tradeSession = null;
			if (tradingSessionID != null && (tradeSession = Session.lookupSession(tradingSessionID)).isLoggedOn()) {
				logger.info(String.format("Executing Trade(MKT) from %s for transId <%s>: %s @ %s", sendTopic, transId,
						TextFormat.shortDebugString(transactionMessage), mktOrdPrice));
				char sideC = side.equalsIgnoreCase("B") ? Side.BUY : Side.SELL;
				String symbol = transactionMessage.getSymbol();
				String sym = symbol;
				char timeInForce = DEFAULT_TIME_IN_FORCE;
				char ordType = DEFAULT_ORD_TYPE;

				if (orderParams != null) {
					if (orderParams.hasOrdType()) {
						if (orderParams.getOrdType() == OrderParams.OrdType.Market) {
							ordType = OrdType.FOREX_MARKET;
						} else if (orderParams.getOrdType() == OrderParams.OrdType.Limit) {
							ordType = OrdType.FOREX_LIMIT;
						}
					}
					if (orderParams.hasTimeInForce()) {
						if (orderParams.getTimeInForce() == OrderParams.TimeInForce.Immediate_or_Cancel) {
							timeInForce = TimeInForce.IMMEDIATE_OR_CANCEL;
						} else {

						}
					}
				}

				if (symbol.indexOf('/') < 0) {
					sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
				}
				String amt = amount;
				if (amt.indexOf(".") < 0) {
					amt = amt + DECIMAL_00;
				}
				quickfix.fix44.NewOrderSingle message = new quickfix.fix44.NewOrderSingle();

				message.set(new ClOrdID(transactionMessage.getTransId()));
				message.set(new TransactTime(
						new java.util.Date(Long.parseLong(Long.toString(System.currentTimeMillis())))));
				message.set(new Currency(transactionMessage.getNotionalCurrency()));
				message.set(new Side(sideC));

				message.set(new Symbol(sym));
				message.set(new OrderQty(Double.parseDouble(amt)));
				message.set(new OrdType(ordType));

				if ((mktOrdPrice != null) && (mktOrdPrice.trim().length() > 0))
					message.set(new Price(Double.parseDouble(mktOrdPrice)));

				message.set(new TimeInForce(timeInForce));
				tradeSession.send(message);
			} else {
				rejectTrade(transactionMessage, "FIX Ordering Session not Online");
			}
			logger.debug(String.format("Trade(MKT) Order from %s sent", sendTopic));
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
			
			if ( !symbolRequestIdMap.contains(symbol)) {
				String requestId = request.getRequestId();

				quickfix.fix44.MarketDataRequest marketDataRequest = new quickfix.fix44.MarketDataRequest(
						new MDReqID(requestId),
						new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES), new MarketDepth(0));
				String sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
				marketDataRequest.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
				marketDataRequest.set(new AggregatedBook(true));
				marketDataRequest.set(new NoRelatedSym(1));
	
				marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_BID);
				marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_OFFER);
	
				quickfix.fix44.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix44.MarketDataRequest.NoRelatedSym();
				noRelatedSym.set(new Symbol(sym));
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
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	private void rejectTrade(Transaction transactionMessage, String reason) {
		ExecutionReportInfo.Builder executionReport = ExecutionReportInfo.newBuilder();

		String transId = transactionMessage.getTransId();

		String symbol = transactionMessage.getSymbol();
		String currency = transactionMessage.getNotionalCurrency();

		executionReport.setTransId(transId);
		executionReport.setSymbol(symbol);
		executionReport.setCurrency(currency);

		executionReport.setStatus(TransStateType.TRADE_REJECT);
		executionReport.setAdditionalInfo(reason);

		logger.warn("Rejecting trade<" + transId + ">. " + reason);
		logger.debug(String.format("Sending trade report for %s, %s", executionReport.getTransId(),
				TextFormat.shortDebugString(executionReport)));
		ExecutionReportInfo execReport = executionReport.build();
		execReportSender.send(EXEC_REPORT_TOPIC, TtMsgEncoder.encode(execReport));

	}

	private static String getMarketTradeDataInfo(Transaction transactionMessage, String dataName) {
		String dataValue = "";
		String currency1 = transactionMessage.getSymbol().substring(0, 3);
		String currency2 = transactionMessage.getSymbol().substring(3);

		String nationalCurrency = transactionMessage.getNotionalCurrency();
		if (dataName.equalsIgnoreCase("AMT")) {
			dataValue = (nationalCurrency.equalsIgnoreCase(currency1))
					? transactionMessage.getNearDateDetail().getCurrency1Amt()
					: transactionMessage.getNearDateDetail().getCurrency2Amt();
		}

		if (dataName.equalsIgnoreCase("SIDE")) {
			if (nationalCurrency.equalsIgnoreCase(currency1)) {
				dataValue = transactionMessage.getTradeAction();
			}
			if (nationalCurrency.equals(currency2)) {
				dataValue = (transactionMessage.getTradeAction().equalsIgnoreCase("B")) ? "S" : "B";
			}
		}

		return (dataValue);
	}
	
	private quickfix.Message buildCancelEspRequestFix(String symbol, String requestId) {
		quickfix.fix44.MarketDataRequest marketDataRequest = new quickfix.fix44.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST),
				new MarketDepth(1));
		String sym = symbol;
		if ( symbol.indexOf('/') < 0 ) {
			sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
		}
		marketDataRequest.set(new NoRelatedSym(1));

		marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_BID);
		marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_OFFER);

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
