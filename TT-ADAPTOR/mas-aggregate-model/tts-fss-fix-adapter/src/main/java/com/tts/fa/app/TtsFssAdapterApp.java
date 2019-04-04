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
import quickfix.UnsupportedMessageType;
import quickfix.field.BidForwardPoints;
import quickfix.field.BidPx;
import quickfix.field.BidSize;
import quickfix.field.BidSpotRate;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.MDEntryID;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MsgType;
import quickfix.field.NoRelatedSym;
import quickfix.field.OfferForwardPoints;
import quickfix.field.OfferPx;
import quickfix.field.OfferSize;
import quickfix.field.OfferSpotRate;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.PartyID;
import quickfix.field.PartyIDSource;
import quickfix.field.PartyRole;
import quickfix.field.Password;
import quickfix.field.Price;
import quickfix.field.QuoteCondition;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.field.QuoteRespID;
import quickfix.field.QuoteRespType;
import quickfix.field.RawData;
import quickfix.field.RawDataLength;
import quickfix.field.SecondaryExecID;
import quickfix.field.SecondaryOrderID;
import quickfix.field.SecurityType;
import quickfix.field.SettlDate;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.Username;
import quickfix.fix44.component.Parties;

public class TtsFssAdapterApp extends MessageCracker
		implements quickfix.Application, ITradingSessionAware, IMsgListener {
	private static final String EXEC_REPORT_TOPIC = String.format(Transactional.TRAN_INFO_TEMPLATE, "FA",
			Data.RateType.MARKET);
	public static final java.lang.String DEFAULT_DELIMITER = "%%%";
	public static final java.lang.String ESP_QUOTE_REF_ID_PREFIX = "ESP%%%";
	public final static SettlType SettlType_SPOT = new SettlType(SettlType.REGULAR);
	public final static SettlType SETTL_TYPE__ON_FOR_TOD = new SettlType("ON");
	public final static SettlType SETTL_TYPE__SPOT = new SettlType("SP");
	public static final SettlType SETTL_TYPE__TN_FOR_TOM = new SettlType("TN");
	public final static SecurityType SecurityType_SPOT = new SecurityType("FXSPOT");
	public final static SecurityType SecurityType_FWD = new SecurityType("FXFWD");
	public final static SecurityType SecurityType_SWAP = new SecurityType("FXSWAP");
	public final static quickfix.fix50.MarketDataRequest.NoMDEntryTypes FIX50_MarketDataRequest_SIDE_BID;
	public final static quickfix.fix50.MarketDataRequest.NoMDEntryTypes FIX50_MarketDataRequest_SIDE_OFFER;

	private final Map<String, String> requestIdTopicOutMap = new ConcurrentHashMap<>();
	private static final String NAME = "FSS";
	private final static Logger logger = LoggerFactory.getLogger(TtsFssAdapterApp.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

	private static final String DECIMAL_00 = ".00";
	private static final String TOPIC___ADAPTER_STATUS = String
			.format(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_INFO_TEMPLATE, NAME);

	static {
		quickfix.fix50.MarketDataRequest.NoMDEntryTypes noMDEntryType44 = null;
		noMDEntryType44 = new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.BID));
		FIX50_MarketDataRequest_SIDE_BID = noMDEntryType44;
		noMDEntryType44 = new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.OFFER));
		FIX50_MarketDataRequest_SIDE_OFFER = noMDEntryType44;
	}

	private final static String[] ctrlTopics = new String[] { IEventMessageTypeConstant.REM.CE_TRADING_SESSION,
			"TTS.CTRL.EVENT.REQUEST.FA.*." + NAME + ".>", "TTS.TRAN.FX.*.TRANINFO.FA." + NAME,
			"TTS.CTRL.EVENT.*.REQUEST.FA.*.*" };

	private final boolean logoutWhileChangeTs = AppConfig.getBooleanValue("fix", "logout_while_change_ts", false);
	private final boolean allowCancelEspDuringSession = AppConfig.getBooleanValue("fix", "allowCancelEspDuringSession",
			true);
	private final boolean autoCancelEspBeforeLogout = AppConfig.getBooleanValue("fix", "autoCancelEspBeforeLogout",
			true);
	private final long logonGracePeriod = AppConfig.getIntegerValue("fix", "logon_grace_period", 10)
			* ChronologyUtil.MILLIS_IN_SECOND;

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

	private final String[] srcProviders;

	private volatile SessionID marketDataSessionID = null;
	private volatile SessionID tradingSessionID = null;
	private volatile SessionID rfsMarketDataSessionID = null;
	private volatile SessionID rfsTradingSessionID = null;
	private volatile long marketDataSessionLastOnline = -1;
	private volatile long marketDataSessionLastOffline = -1;
	private volatile long tradingSessionLastOnline = -1;
	private volatile long tradingSessionLastOffline = -1;
	private volatile long rfsMarketDataSessionLastOnline = -1;
	private volatile long rfsMarketDataSessionLastOffline = -1;
	private volatile long rfsTradingSessionLastOnline = -1;
	private volatile long rfsTradingSessionLastOffline = -1;

	private volatile IMsgSender msgSender;
	private volatile IMsgSender execReportSender;

	public TtsFssAdapterApp() {
		super();
		String liquidityProvidersEnabled = AppConfig.getValue("liquidityProviderConfiguration", "liquidityProviders"); // "DUMMY_TOR";
		this.srcProviders = liquidityProvidersEnabled.split(",");
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
		String sessionIdStr = sessionId.toString();
		String senderCompId = sessionId.getSenderCompID();
		if (sessionIdStr.contains("RFS")) {
			if (senderCompId.startsWith("TRD")) {
				rfsTradingSessionID = sessionId;
				rfsTradingSessionLastOnline = System.currentTimeMillis();
			} else if (senderCompId.startsWith("STR")) {
				rfsMarketDataSessionID = sessionId;
				rfsMarketDataSessionLastOnline = System.currentTimeMillis();
			}
		} else {
			if (senderCompId.startsWith("TRD")) {
				tradingSessionID = sessionId;
				tradingSessionLastOnline = System.currentTimeMillis();
			} else if (senderCompId.startsWith("STR")) {
				marketDataSessionID = sessionId;
				marketDataSessionLastOnline = System.currentTimeMillis();
			}
		}

		sessionsInApp.put(sessionId, Boolean.TRUE);

		OnlineNotificationRunnable r = new OnlineNotificationRunnable(marketDataSessionLastOnline,
				marketDataSessionLastOffline, sessionId);
		if (logonGracePeriod > 0) {
			schdExctr.schedule(r, logonGracePeriod, TimeUnit.MILLISECONDS);
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
		for (int i = 0; i < srcProviders.length; i++) {
			as.addActiveSessionsBuilder();
			as.getActiveSessionsBuilder(i).setSessionName(sessionId.toString());
			as.getActiveSessionsBuilder(i).setStatus(Status.INACTIVE);
			as.getActiveSessionsBuilder(i).setSourceNm(srcProviders[i]);
			String sessionIdStr = sessionId.toString();
			String senderCompId = sessionId.getSenderCompID();
			if (sessionIdStr.contains("RFS")) {
				if (senderCompId.startsWith("STR")) {
					rfsMarketDataSessionID = null;
					rfsMarketDataSessionLastOffline = System.currentTimeMillis();

					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA__ESP);
					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA);
					as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(rfsMarketDataSessionLastOnline);
					as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(rfsMarketDataSessionLastOffline);
				} else {
					rfsTradingSessionID = null;
					rfsTradingSessionLastOffline = System.currentTimeMillis();
					as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(rfsTradingSessionLastOnline);
					as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(rfsTradingSessionLastOffline);
					as.getActiveSessionsBuilder(i)
							.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_FOK);
					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_IOC);
				}
			} else {
				if (senderCompId.startsWith("STR")) {
					marketDataSessionID = null;
					marketDataSessionLastOffline = System.currentTimeMillis();

					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA__ESP);
					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA);
					as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(marketDataSessionLastOnline);
					as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(marketDataSessionLastOffline);
				} else {
					tradingSessionID = null;
					tradingSessionLastOffline = System.currentTimeMillis();
					as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(tradingSessionLastOnline);
					as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(tradingSessionLastOffline);
					as.getActiveSessionsBuilder(i)
							.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_FOK);
					as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_IOC);
				}
			}
		}

		if (msgSender != null) {
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
		SessionID rfsMarketDataSessionID = this.rfsMarketDataSessionID;
		SessionID rfsTradingSessionID = this.rfsTradingSessionID;

		if (logoutWhileChangeTs) {
			if (autoCancelEspBeforeLogout && symbolRequestIdMap != null) {
				for (Entry<String, String> e : symbolRequestIdMap.entrySet()) {
					if (marketDataSessionID != null) {
						quickfix.Message m = buildCancelEspRequestFix(e.getKey(), e.getValue());
						Session sss = Session.lookupSession(marketDataSessionID);
						if (sss != null) {
							sss.send(m);
						}
					}
				}
			}
			Session tradingSession = null;
			Session rfsTradingSession = null;
			Session marketDataSession = null;
			Session rfsMarketDataSession = null;
			if (tradingSessionID != null) {
				tradingSession = Session.lookupSession(tradingSessionID);
				if (tradingSession != null) {
					tradingSession.logout();
				}
			}
			if (rfsTradingSessionID != null) {
				rfsTradingSession = Session.lookupSession(rfsTradingSessionID);
				if (rfsTradingSession != null) {
					rfsTradingSession.logout();
				}
			}
			if (rfsMarketDataSessionID != null) {
				rfsMarketDataSession = Session.lookupSession(rfsMarketDataSessionID);
				rfsMarketDataSession.logout();
			}
			if (rfsMarketDataSessionID != null) {
				marketDataSession = Session.lookupSession(marketDataSessionID);
				marketDataSession.logout();
			}
			symbolRequestIdMap.clear();

			try {
				TimeUnit.SECONDS.sleep(20L);
			} catch (InterruptedException e) {

			}
			if (marketDataSessionID != null) {
				Session.lookupSession(marketDataSessionID).logon();
			}
			if (rfsMarketDataSessionID != null) {
				Session.lookupSession(rfsMarketDataSessionID).logon();
			}
			try {
				TimeUnit.SECONDS.sleep(5L);
			} catch (InterruptedException e) {

			}
			if (tradingSessionID != null) {
				Session.lookupSession(tradingSessionID).logon();
			}
			if (rfsTradingSessionID != null) {
				Session.lookupSession(rfsTradingSessionID).logon();
			}
		}
	}

	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.ExecutionReport report, SessionID sessionId)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		logger.debug("onExecutionReport:" + report);

		ExecutionReportInfo.Builder tradeExecutionStatusInfo = ExecutionReportInfo.newBuilder();
		String orderId = report.getOrderID().getValue();
		String clientOrderId = report.isSetClOrdID() ? report.getClOrdID().getValue() : report.getQuoteRespID().getValue();

		String bankOrderId = null;
		String fssExecId = null;
		String bankExecId = null;
		String status = null;
		String providerComment = null;
		String provider = null;
		StringBuilder tradeCmt = new StringBuilder();
		char orderStatus = report.getExecType().getValue();
		if (orderStatus == ExecType.PENDING_NEW) {
			status = TransStateType.TRADE_PENDING;
		} else if (orderStatus == ExecType.CANCELED) {
			status = TransStateType.TRADE_CANCEL;
		} else if (orderStatus == ExecType.REJECTED) {
			status = TransStateType.TRADE_REJECT;
		} else if (orderStatus == ExecType.FILL || orderStatus == ExecType.PARTIAL_FILL) {
			status = orderStatus == ExecType.PARTIAL_FILL ? TransStateType.TRADE_PARTIALLY_DONE
					: TransStateType.TRADE_DONE;
			SecondaryOrderID bankOrdID = new SecondaryOrderID();
			report.get(bankOrdID);
			bankOrderId = bankOrdID.getValue();
			ExecID fssExecID = new ExecID();
			report.get(fssExecID);
			fssExecId = fssExecID.getValue();
			SecondaryExecID bankExecID = new SecondaryExecID();
			report.get(bankExecID);
			bankExecId = bankExecID.getValue();
			if (report.isSetField(30)) {
				provider = report.getString(30);
			}
		}
		if (report.isSetText()) {
			providerComment = report.getText().toString();
		}
		String tradeAction = null;
		char side = report.getSide().getValue();

		String ccyPair = report.getSymbol().getValue().replaceAll("/", "");
		String notional = ccyPair.substring(0, 3);
		if (report.isSetCurrency()) {
			notional = report.getCurrency().getValue();
		}
		tradeExecutionStatusInfo.setCurrency(notional);

		if (report.isSetSettlDate()) {
			tradeExecutionStatusInfo.setSettleDate(report.getSettlDate().getValue());
		}

		String product = com.tts.message.constant.Constants.ProductType.FXSPOT;

		tradeExecutionStatusInfo.setProduct(product);

		boolean isCcy2notional = ccyPair.indexOf(notional) > 2;

		if (!isCcy2notional) {
			if (side == Side.BUY) {
				tradeAction = TradeConstants.TradeAction.BUY;
			} else if (side == Side.SELL) {
				tradeAction = TradeConstants.TradeAction.SELL;
			}
		} else {
			if (side == Side.BUY) {
				tradeAction = TradeConstants.TradeAction.SELL;
			} else if (side == Side.SELL) {
				tradeAction = TradeConstants.TradeAction.BUY;
			}
		}

		if (bankOrderId != null)
			tradeCmt.append("bankOrdId:").append(bankOrderId).append(' ');
		if (bankExecId != null)
			tradeCmt.append("bankExecId:").append(bankExecId).append(' ');
		if (providerComment != null)
			tradeCmt.append("cmt:").append(providerComment).append(' ');

		String finalPrice = "0";
		String orderPrice = convertRateToString(report.getPrice().getValue());
		if (report.isSetLastPx()) {
			finalPrice = convertRateToString(report.getLastPx().getValue());
		}
		if (orderPrice != null)
			tradeCmt.append("ordPx:").append(orderPrice).append(' ');
		if (fssExecId != null)
			tradeCmt.append("fssExecId:").append(fssExecId).append(' ');

		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSpotRate(finalPrice);
		tradeExecutionStatusInfo.setAllInPrice(finalPrice);
		if (com.tts.message.constant.Constants.ProductType.FXSWAP.equals(product)) {
			if (report.isSetField(640)) {
				String finalPrice2 = (report.getString(640));
				tradeExecutionStatusInfo.setFinalPrice2(finalPrice2);
			}

			String size2 = convertSizeToString(report.getOrderQty2().getValue());
			tradeExecutionStatusInfo.setSize2(size2);

		}
		if (report.isSetSettlDate2()) {
			String settleDate2 = report.getSettlDate2().getValue();
			tradeExecutionStatusInfo.setSettleDate2(settleDate2);

		}
		double filledQty, requestQty;
		if (report.isSetLastQty()) {
			filledQty = report.getLastQty().getValue();
		} else {
			filledQty = report.getOrderQty().getValue();
		}
		requestQty = report.getOrderQty().getValue();
		tradeExecutionStatusInfo.setRefId(orderId);
		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(ccyPair);
		if (provider != null) {
			tradeExecutionStatusInfo.setProvider(provider);
		}
		tradeExecutionStatusInfo.setTradeAction(tradeAction);
		if (Double.compare(filledQty, requestQty) != 0) {
			tradeExecutionStatusInfo.setSize(convertSizeToString(filledQty));
			tradeExecutionStatusInfo.setOriginalSize(convertSizeToString(requestQty));
		} else {
			tradeExecutionStatusInfo.setSize(convertSizeToString(filledQty));
			tradeExecutionStatusInfo.setOriginalSize(convertSizeToString(filledQty));
		}

		if (report.isSetTransactTime()) {
			tradeExecutionStatusInfo.setTransactTime(ChronologyUtil
					.getDateTimeSecString(ChronoConvUtil.convertDateTime(report.getTransactTime().getValue())));
		}

		tradeExecutionStatusInfo.setAdditionalInfo(tradeCmt.toString());
		ExecutionReportInfo execReport = tradeExecutionStatusInfo.build();
		execReportSender.send(EXEC_REPORT_TOPIC, TtMsgEncoder.encode(execReport));
		logger.debug("onExecutionReport<end>:" + TextFormat.shortDebugString(execReport));

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
	public void onQuoteMass(quickfix.fix44.MassQuote response, SessionID sessionID) throws FieldNotFound {
		long timestamp = System.currentTimeMillis();
		String requestId = null;
		int numberOfQuotes = 0;
		RawMarketBook.Builder mkBookBuilder = RawMarketBook.newBuilder();
		requestId = response.getQuoteReqID().getValue();
		mkBookBuilder.setRequestId(requestId);

		Symbol s = new Symbol();
		response.getField(s);
		String symbol = s.getValue().replace("/", "");

		// building the marketData
		mkBookBuilder.setSymbol(symbol);
		mkBookBuilder.setUpdateTimeStamp(timestamp);
		
		quickfix.field.NoQuoteEntries e = new quickfix.field.NoQuoteEntries();
		response.getField(e);
		numberOfQuotes = e.getValue();

		for (int i = 1; i <= numberOfQuotes; i++) {
			quickfix.fix44.MassQuote.NoQuoteSets.NoQuoteEntries qsGroup = new quickfix.fix44.MassQuote.NoQuoteSets.NoQuoteEntries();
			response.getGroup(i, qsGroup);

			String quoteEntryID = qsGroup.getString(299);
			char side = qsGroup.getChar(54);
			double bidpx = qsGroup.isSetField(132) ? qsGroup.getDouble(132) : -1.0d;
			double askpx = qsGroup.isSetField(133) ? qsGroup.getDouble(133) : -1.0d;
			double bidSize = qsGroup.isSetField(134) ? qsGroup.getDouble(134) : -1.0d;
			double askSize = qsGroup.isSetField(135) ? qsGroup.getDouble(135) : -1.0d;
			double bidspotpx = qsGroup.isSetField(188) ? qsGroup.getDouble(188) : -1.0d;
			double askspotpx = qsGroup.isSetField(190) ? qsGroup.getDouble(190) : -1.0d;
			double bidfwdpts = qsGroup.isSetField(189) ? qsGroup.getDouble(189) : -1.0d;
			double askfwdpts = qsGroup.isSetField(191) ? qsGroup.getDouble(191) : -1.0d;

			String originator = qsGroup.getString(282);
			RawLiquidityEntry.Builder tickBuilder = RawLiquidityEntry.newBuilder();
			tickBuilder.setQuoteId("RFS||" + requestId + "||" + quoteEntryID);
			tickBuilder.setSource(originator);

			if (side == '1') {
				tickBuilder.setRate(bidpx);
				tickBuilder.setSize(BigDecimal.valueOf(bidSize).longValue());
				if (bidspotpx > 0 && bidfwdpts > 0) {
					AdditionalQuoteData.Builder additional = AdditionalQuoteData.newBuilder();
					additional.setFwdPts(bidfwdpts);
					additional.setSpotRate(bidspotpx);
					tickBuilder.setAdditionalInfo(additional);
				}
				mkBookBuilder.addBidQuote(tickBuilder);
			}
			if (side == '2') {
				tickBuilder.setRate(askpx);
				tickBuilder.setSize(BigDecimal.valueOf(askSize).longValue());
				if (askspotpx > 0 && askfwdpts > 0) {
					AdditionalQuoteData.Builder additional = AdditionalQuoteData.newBuilder();
					additional.setFwdPts(askfwdpts);
					additional.setSpotRate(askspotpx);
					tickBuilder.setAdditionalInfo(additional);
				}
				mkBookBuilder.addAskQuote(tickBuilder);
			}
		}
		mkBookBuilder.setAdapter(NAME);
		mkBookBuilder.setMkBookType(MkBookType.RAW);

		RawMarketBook mkBook = mkBookBuilder.build();

		TtMsg ttMsg = TtMsgEncoder.encode(mkBook);

		String topic = requestIdTopicOutMap.get(requestId);
		msgSender.send(topic, ttMsg);
	}

	@Handler
	public void onQuoteAck(quickfix.fix44.MassQuoteAcknowledgement ack, SessionID sessionID) throws FieldNotFound {
		logger.debug(ack.toString());
	}

	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataSnapshotFullRefresh response, SessionID sessionId)
			throws FieldNotFound {
		RawMarketBook.Builder mkBookBuilder = RawMarketBook.newBuilder();
		long timestamp = System.currentTimeMillis();
		String symbol = response.getSymbol().getValue();

		symbol = symbol.substring(0, 3) + symbol.substring(4);
		// building the marketData
		mkBookBuilder.setSymbol(symbol);
		mkBookBuilder.setUpdateTimeStamp(timestamp);

		quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
		int noMDEntries = response.getNoMDEntries().getValue();
		String originator = null;
		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();
			String quoteId = noMDEntry.getString(MDEntryID.FIELD);

			if (noMDEntry.isSetQuoteCondition()
					&& QuoteCondition.CLOSED_INACTIVE.equals(noMDEntry.getQuoteCondition().getValue())) {
				continue;
			}

			if (!noMDEntry.isSetMDEntryPx()) {
				continue;
			}

			if (size <= 0) {
				continue;
			}
			// building the tick
			RawLiquidityEntry.Builder tickBuilder = RawLiquidityEntry.newBuilder();
			tickBuilder.setRate(noMDEntry.getMDEntryPx().getValue());
			tickBuilder.setQuoteId(quoteId);
			tickBuilder.setSize(size);
			originator = noMDEntry.getMDEntryOriginator().getValue();
			tickBuilder.setSource(originator);

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
		mkBookBuilder.setMkBookType(MkBookType.RAW);
		mkBookBuilder.setRequestId(response.getMDReqID().getValue());
		RawMarketBook mkBook = mkBookBuilder.build();
		TtMsg ttMsg = TtMsgEncoder.encode(mkBook);
		msgSender.send("TTS.MD.FX.FA.SPOT." + symbol + "." + NAME + "." + originator, ttMsg);
	}

	@Handler
	public void onBusinessMessageResponse(quickfix.fix42.BusinessMessageReject responseMessage, SessionID sessionId)
			throws FieldNotFound {
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE,
				AppUtils.getAppName());
		monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,
				MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, AppUtils.getAppName(), responseMessage.toString());
	}

	@Handler
	public void onBusinessMessageResponse(quickfix.fix50.BusinessMessageReject responseMessage, SessionID sessionId)
			throws FieldNotFound {
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE,
				AppUtils.getAppName());
		monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,
				MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, AppUtils.getAppName(), responseMessage.toString());
	}

	@Handler
	public void onQuoteCancel(quickfix.fix50.QuoteCancel responseMessage, SessionID sessionId) throws FieldNotFound {
		logger.info("Received QuoteCancel: " + responseMessage);
	}

	@Override
	public void onMessage(TtMsg arg0, IMsgSessionInfo arg1, IMsgProperties arg2) {
		try {
			String sendTopic = arg2.getSendTopic();
			if (IEventMessageTypeConstant.REM.CE_TRADING_SESSION.equals(sendTopic)) {
				ChangeTradingSession tradingSession = ChangeTradingSession.parseFrom(arg0.getParameters());
				if (tradingSession.hasChangeTo()) {
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
					if (asr.hasAdapterName() && asr.getAdapterName().equals(NAME)) {

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
		AdapterStatus.Builder adapterStatusBuilder = AdapterStatus.newBuilder();
		adapterStatusBuilder.setAdapterName(NAME);
		for (String src : this.srcProviders) {
			SessionStatus.Builder ss = SessionStatus.newBuilder();
			ss.setSessionName(marketDataSessionID.toString());
			ss.setStatus(sessionsInApp.get(marketDataSessionID) ? Status.ACTIVE : Status.INACTIVE);
			ss.setSourceNm(src);
			ss.addCapability(FixSessionCapability.MARKET_DATA);
			ss.addCapability(FixSessionCapability.MARKET_DATA__ESP);
			ss.setLastOnlineTimestamp(marketDataSessionLastOnline);
			ss.setLastOfflineTimestamp(marketDataSessionLastOffline);
			adapterStatusBuilder.addActiveSessions(ss);

			SessionStatus.Builder ss2 = SessionStatus.newBuilder();
			ss2.setSessionName(tradingSessionID.toString());
			ss2.setStatus(sessionsInApp.get(tradingSessionID) ? Status.ACTIVE : Status.INACTIVE);
			ss2.setSourceNm(src);
			ss2.addCapability(FixSessionCapability.ORDERING_FOK);
			ss2.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
			ss2.setLastOnlineTimestamp(tradingSessionLastOnline);
			ss2.setLastOfflineTimestamp(tradingSessionLastOffline);
			adapterStatusBuilder.addActiveSessions(ss2);

		}
		allOnline = sessionsInApp.get(marketDataSessionID) && sessionsInApp.get(tradingSessionID);

		adapterStatusBuilder.setStatus(allOnline ? Status.ACTIVE : Status.INACTIVE);
		return adapterStatusBuilder;
	}

	private void onUnsubscribe(TtMsg arg0) {
		if (allowCancelEspDuringSession) {
			try {
				PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(arg0.getParameters());
				String symbol = request.getQuoteParam().getCurrencyPair();
				String requestId = request.getRequestId();
				logger.info(String.format("Unsubscribing market data for <%s>: reqId = %s", symbol, requestId));
				if (marketDataSessionID != null) {
					quickfix.Message m = buildCancelEspRequestFix(symbol, requestId);
					Session sss = Session.lookupSession(marketDataSessionID);
					if (sss != null) {
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
			logger.info(String.format("Received New Order Request from %s for transId <%s>: %s", sendTopic, transId,
					TextFormat.shortDebugString(transactionMessage)));
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}

		if (transactionMessage != null) {
			if (transactionMessage.getOrderParams().getQuoteRefId().startsWith("RFS")) {
				onNewExecutionRequestRFS(transactionMessage, sendTopic);
			} else {
				onNewExecutionRequestESP(transactionMessage, sendTopic);
			}
		} else {
			logger.error("Protobug parse error");
		}
	}

	private void onNewExecutionRequestRFS(Transaction transactionMessage, String sendTopic) {
		String amount = transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) > 0
				? transactionMessage.getNearDateDetail().getCurrency2Amt()
				: transactionMessage.getNearDateDetail().getCurrency1Amt();
		String s = transactionMessage.getOrderParams().getQuoteRefId();
		int idx = s.indexOf("||");
		String s1 = s.substring(idx+2);
		idx = s1.indexOf("||");
		String quoteReqId = s1.substring(0, idx);
		String quoteId = s1.substring(idx+2);

		String clientOrderId = transactionMessage.getTransId();
		String symbol = transactionMessage.getSymbol();
		String ccy1 = symbol.substring(0, 3);
		String ccy2 = symbol.substring(3, 6);
		quickfix.fix44.QuoteResponse response = new quickfix.fix44.QuoteResponse();
		response.set(new QuoteRespID( clientOrderId));
		response.set(new QuoteID( quoteId	));
		response.set(new QuoteRespType(QuoteRespType.HIT_LIFT));
		response.set(new Symbol(ccy1 + '/' + ccy2));
		response.set(new Currency(transactionMessage.getNotionalCurrency()));
		response.set(new TransactTime());
		response.setString(131,quoteReqId	);
		response.set(new SettlType(transactionMessage.getNearDateDetail().getValueDate()));
		if (TradeAction.BUY.equals(transactionMessage.getTradeAction())) {
			response.set(new Side(Side.BUY));
			response.set(new OfferPx(new BigDecimal(transactionMessage.getNearDateDetail().getTradeRate()).doubleValue()));
			response.set(new OfferSize(new BigDecimal(amount).doubleValue()));
			response.set(new OfferSpotRate(new BigDecimal(transactionMessage.getNearDateDetail().getSpotRate()).doubleValue()));
			response.set(new OfferForwardPoints(new BigDecimal(transactionMessage.getNearDateDetail().getForwardPoints()).doubleValue()));

		} else if (TradeAction.SELL.equals(transactionMessage.getTradeAction())) {
			response.set(new Side(Side.SELL));
			response.set(new BidPx(new BigDecimal(transactionMessage.getNearDateDetail().getTradeRate()).doubleValue()));
			response.set(new BidSize(new BigDecimal(amount).doubleValue()));
			response.set(new BidSpotRate(new BigDecimal(transactionMessage.getNearDateDetail().getSpotRate()).doubleValue()));
			response.set(new BidForwardPoints(new BigDecimal(transactionMessage.getNearDateDetail().getForwardPoints()).doubleValue()));
		}	
		
		quickfix.Session ssss = Session.lookupSession(rfsTradingSessionID);
		if (ssss.isLoggedOn()) {
			ssss.send(response);
			logger.debug(String.format("Order(QuoteResponse) from %s sent, id=%s", sendTopic, clientOrderId));
		}
		
	}

	private void onNewExecutionRequestESP(Transaction transactionMessage, String sendTopic) {

		String amount = transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) > 0
				? transactionMessage.getNearDateDetail().getCurrency2Amt()
				: transactionMessage.getNearDateDetail().getCurrency1Amt();
		String quoteId = transactionMessage.getOrderParams().getQuoteRefId();
		String clientOrderId = transactionMessage.getTransId();
		String symbol = transactionMessage.getSymbol();
		String ccy1 = symbol.substring(0, 3);
		String ccy2 = symbol.substring(3, 6);

		quickfix.fix44.NewOrderSingle message = new quickfix.fix44.NewOrderSingle();
		message.set(new OrdType(OrdType.PREVIOUSLY_QUOTED));
		message.set(new ClOrdID(clientOrderId));
		message.set(new TimeInForce(TimeInForce.FILL_OR_KILL));

		message.set(new Symbol(ccy1 + '/' + ccy2));
		message.set(new Currency(transactionMessage.getNotionalCurrency()));
		if (TradeAction.BUY.equals(transactionMessage.getTradeAction())) {
			message.set(new Side(Side.BUY));
		} else if (TradeAction.SELL.equals(transactionMessage.getTradeAction())) {
			message.set(new Side(Side.SELL));
		}
		message.setString(MDEntryID.FIELD, quoteId); // MDEntryID (tag 278)
		if (amount.indexOf(".") < 0) {
			amount = amount + DECIMAL_00;
		}
		message.set(new OrderQty(Double.parseDouble(amount)));
		message.set(new Price(Double.parseDouble(transactionMessage.getNearDateDetail().getTradeRate())));

		String lp = transactionMessage.getOrderParams().getSpecificLP(0);
		int spaceIndx = lp.indexOf(" ");
		if (spaceIndx > 0) {
			lp = lp.substring(0, spaceIndx - 1);
		}
		Parties parties = new Parties();
		Parties.NoPartyIDs liquidityProviderParty = new Parties.NoPartyIDs();
		liquidityProviderParty.set(new PartyID(lp));
		liquidityProviderParty.set(new PartyRole(PartyRole.LIQUIDITY_PROVIDER));
		liquidityProviderParty.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
		parties.addGroup(liquidityProviderParty);
		message.setGroups(parties);

		if (transactionMessage.hasTransTime()) {
			message.set(new TransactTime(new java.util.Date(transactionMessage.getTransTime())));
		} else {
			message.set(new TransactTime());
		}

		quickfix.Session s = Session.lookupSession(tradingSessionID);
		if (s.isLoggedOn()) {
			s.send(message);
			logger.debug(String.format("Prev-Quoted FOK Order from %s sent, id=%s", sendTopic, clientOrderId));
		}

	}

	private void onNewPriceSubscriptionRquest(TtMsg arg0) {
		try {

			PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(arg0.getParameters());
			String symbol = request.getQuoteParam().getCurrencyPair();
			logger.info(String.format("Received PriceSubscriptionRequest for <%s>: %s", symbol,
					TextFormat.shortDebugString(request)));
			if (RateRequestType.ESP == request.getRateRequestType()) {
				doHandleEspRequest(request, symbol);
			} else if (RateRequestType.RFS == request.getRateRequestType()) {
				doHandleRfsRequest(request, symbol);
			}

		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	private void doHandleRfsRequest(PriceSubscriptionRequest request, String symbol) {
		String settleDate = request.getQuoteParam().hasNearDateDetail()
				&& request.getQuoteParam().getNearDateDetail().hasActualDate()
						? request.getQuoteParam().getNearDateDetail().getActualDate() : null;

		String tenor = null;
		String rfsOutTopic = request.getTopic();

		if (request.getQuoteParam().hasNearDateDetail() && request.getQuoteParam().getNearDateDetail().hasPeriodCd()) {
			TenorVo.Builder t = new TenorVo.Builder();
			t.setPeriodCd(request.getQuoteParam().getNearDateDetail().getPeriodCd());
			t.setValue(Integer.parseInt(request.getQuoteParam().getNearDateDetail().getPeriodValue()));
			tenor = t.build().toString();
		}

		String requestId = request.getRequestId();

		quickfix.fix44.QuoteRequest quoteRequest = new quickfix.fix44.QuoteRequest(new QuoteReqID(requestId));

		Parties parties = new Parties();
		for (String src : request.getRateSource().getSpecificSourceNmList()) {
			Parties.NoPartyIDs liquidityProviderParty = new Parties.NoPartyIDs();
			liquidityProviderParty.set(new PartyID(src));
			liquidityProviderParty.set(new PartyRole(PartyRole.LIQUIDITY_PROVIDER));
			liquidityProviderParty.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
			parties.addGroup(liquidityProviderParty);
		}
		quoteRequest.setGroups(parties);

		quoteRequest.set(new NoRelatedSym(1));

		quickfix.fix44.QuoteRequest.NoRelatedSym noRelatedSym = new quickfix.fix44.QuoteRequest.NoRelatedSym();

		String ccy1 = symbol.substring(0, 3);
		String ccy2 = symbol.substring(3, 6);
		noRelatedSym.set(new Symbol(ccy1 + '/' + ccy2));

		noRelatedSym.set(new Currency(request.getQuoteParam().getNotionalCurrency()));

		noRelatedSym.set(new OrderQty(new BigDecimal(request.getQuoteParam().getSize()).longValue()));

		if (request.getQuoteParam().getQuoteDirection() == QuoteDirection.BUY_AND_SELL) {
			if (symbol.indexOf(request.getQuoteParam().getNotionalCurrency()) > 0) {
				noRelatedSym.set(new Side(Side.BUY));
			} else {
				noRelatedSym.set(new Side(Side.SELL));
			}
		} else if (request.getQuoteParam().getQuoteDirection() == QuoteDirection.SELL_AND_BUY) {
			if (symbol.indexOf(request.getQuoteParam().getNotionalCurrency()) > 0) {
				noRelatedSym.set(new Side(Side.SELL));
			} else {
				noRelatedSym.set(new Side(Side.BUY));
			}
		} 
		noRelatedSym.set(new SettlType(settleDate));
//		if (TenorVo.NOTATION_SPOT.equals(tenor)) {
//			if (settleDate != null && !settleDate.isEmpty()) {
//				noRelatedSym.set(new SettlDate(settleDate));
//			} else {
//				// noRelatedSym.set(new SettlDate("19700101"));
//			}
//		} else {
//			noRelatedSym.set(SecurityType_FWD);
//			if (TenorVo.NOTATION_TODAY.equals(tenor)) {
//				noRelatedSym.set(SETTL_TYPE__ON_FOR_TOD);
//			} else if (TenorVo.NOTATION_TOMORROW.equals(tenor)) {
//				noRelatedSym.set(SETTL_TYPE__TN_FOR_TOM);
//			} else if (tenor != null && !tenor.isEmpty() && !tenor.startsWith(TenorVo.NOTATION_UNIT_IMM)) {
//				noRelatedSym.set(new SettlType(tenor));
//			}
//			noRelatedSym.set(new SettlDate(settleDate));
//		}
		quoteRequest.addGroup(noRelatedSym);

		Session sss = rfsMarketDataSessionID == null ? null : Session.lookupSession(rfsMarketDataSessionID);
		requestIdTopicOutMap.put(requestId, rfsOutTopic);

		if (sss != null && sss.isLoggedOn() && (logonGracePeriod <= 0
				|| ((System.currentTimeMillis() - rfsMarketDataSessionLastOnline) > logonGracePeriod))) {
			logger.info(String.format("Subscribing rfs data for <%s>: reqId = %s", symbol, requestId));

			sss.send(quoteRequest);
			symbolRequestIdMap.put(symbol, requestId);
		} else {
			logger.warn(String.format(
					"NOT Subscribing market data for %s as FIX session is not online or in the grace period after logon",
					symbol));
		}

	}

	public void doHandleEspRequest(PriceSubscriptionRequest request, String symbol) {
		if (!symbolRequestIdMap.contains(symbol)) {
			String requestId = request.getRequestId();

			final quickfix.fix44.MarketDataRequest mdr = new quickfix.fix44.MarketDataRequest();
			mdr.set(new MDReqID(requestId));
			mdr.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));

			Parties parties = new Parties();
			for (String src : request.getRateSource().getSpecificSourceNmList()) {
				Parties.NoPartyIDs liquidityProviderParty = new Parties.NoPartyIDs();
				liquidityProviderParty.set(new PartyID(src));
				liquidityProviderParty.set(new PartyRole(PartyRole.LIQUIDITY_PROVIDER));
				liquidityProviderParty.set(new PartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE));
				parties.addGroup(liquidityProviderParty);
			}
			mdr.setGroups(parties);

			final quickfix.fix44.MarketDataRequest.NoRelatedSym symbolF = new quickfix.fix44.MarketDataRequest.NoRelatedSym();
			String ccy1 = symbol.substring(0, 3);
			String ccy2 = symbol.substring(3, 6);
			symbolF.set(new Symbol(ccy1 + '/' + ccy2));
			mdr.addGroup(symbolF);
			mdr.set(new MarketDepth(0)); // Full depth book
			mdr.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));

			@SuppressWarnings("resource")
			Session sss = marketDataSessionID == null ? null : Session.lookupSession(marketDataSessionID);
			if (sss != null && sss.isLoggedOn() && (logonGracePeriod <= 0
					|| ((System.currentTimeMillis() - marketDataSessionLastOnline) > logonGracePeriod))) {
				logger.info(String.format("Subscribing market data for <%s>: reqId = %s", symbol, requestId));

				sss.send(mdr);
				symbolRequestIdMap.put(symbol, requestId);
			} else {
				logger.warn(String.format(
						"NOT Subscribing market data for %s as FIX session is not online or in the grace period after logon",
						symbol));
			}

		} else {
			logger.warn(String.format("NOT Subscribing market data for %s as we have already subscribed.", symbol));
		}
	}

	private quickfix.Message buildCancelEspRequestFix(String symbol, String requestId) {
		final quickfix.fix44.MarketDataRequest mdr = new quickfix.fix44.MarketDataRequest();
		mdr.set(new MDReqID(requestId));
		mdr.set(new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST));
		final quickfix.fix44.MarketDataRequest.NoRelatedSym symbolF = new quickfix.fix44.MarketDataRequest.NoRelatedSym();
		String ccy1 = symbol.substring(0, 3);
		String ccy2 = symbol.substring(3, 6);
		symbolF.set(new Symbol(ccy1 + '/' + ccy2));
		mdr.addGroup(symbolF);
		mdr.set(new MarketDepth(0)); // Full depth book
		mdr.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
		logger.info(String.format("Built Esp Cancellation Request for <%s>: %s", symbol, requestId));
		return mdr;
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

		private OnlineNotificationRunnable(long marketDataSessionLastOnline, long marketDataSessionLastOffline,
				SessionID sessionId) {
			super();
			this.sessionId = sessionId;
			this.marketDataSessionLastOfflineLastValue = marketDataSessionLastOffline;
			this.marketDataSessionLastOnlineLastValue = marketDataSessionLastOnline;
		}

		@Override
		public void run() {
			if (marketDataSessionLastOnlineLastValue != marketDataSessionLastOnline
					|| marketDataSessionLastOfflineLastValue != marketDataSessionLastOffline) {
				// session status has changed... this task is no longer valid
				return;
			}
			AdapterStatus.Builder as = AdapterStatus.newBuilder();
			as.setAdapterName(NAME);
			for (int i = 0; i < srcProviders.length; i++) {

				as.addActiveSessionsBuilder();
				as.getActiveSessionsBuilder(i).setSessionName(sessionId.toString());
				as.getActiveSessionsBuilder(i).setStatus(Status.ACTIVE);
				as.getActiveSessionsBuilder(i).setSourceNm(srcProviders[i]);

				String sessionIdStr = sessionId.toString();
				String senderCompId = sessionId.getSenderCompID();
				if (sessionIdStr.contains("RFS")) {
					if (senderCompId.startsWith("STR")) {
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA__ESP);
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA);
						as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(rfsMarketDataSessionLastOnline);
						as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(rfsMarketDataSessionLastOffline);
					} else {
						as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(rfsTradingSessionLastOnline);
						as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(rfsTradingSessionLastOffline);
						as.getActiveSessionsBuilder(i)
								.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_FOK);
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_IOC);
					}
				} else {
					if (senderCompId.startsWith("STR")) {
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA__ESP);
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.MARKET_DATA);
						as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(marketDataSessionLastOnline);
						as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(marketDataSessionLastOffline);
					} else {
						as.getActiveSessionsBuilder(i).setLastOnlineTimestamp(tradingSessionLastOnline);
						as.getActiveSessionsBuilder(i).setLastOfflineTimestamp(tradingSessionLastOffline);
						as.getActiveSessionsBuilder(i)
								.addCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_FOK);
						as.getActiveSessionsBuilder(i).addCapability(FixSessionCapability.ORDERING_IOC);
					}
				}
			}

			if (msgSender != null) {
				msgSender.send(TOPIC___ADAPTER_STATUS, TtMsgEncoder.encode(as.build()));
			}
		}

	}
}
