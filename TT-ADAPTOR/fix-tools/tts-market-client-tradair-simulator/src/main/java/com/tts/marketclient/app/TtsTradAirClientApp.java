package com.tts.marketclient.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.util.AppConfig;
import com.tts.util.PasswordUtil;

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
import quickfix.field.HandlInst;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MsgType;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Password;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.Username;

public class TtsTradAirClientApp extends MessageCracker
		implements quickfix.Application {

	private final static Logger logger = LoggerFactory.getLogger(TtsTradAirClientApp.class);
	public final static quickfix.fix42.MarketDataRequest.NoMDEntryTypes FIX44_MarketDataRequest_SIDE_BID;
	public final static quickfix.fix42.MarketDataRequest.NoMDEntryTypes FIX44_MarketDataRequest_SIDE_OFFER;
	private static final String DECIMAL_00 = ".00";
	private static final char DEFAULT_TIME_IN_FORCE = TimeInForce.FILL_OR_KILL;
	private final static boolean  INITIATOR_MODE; 

	static {
		quickfix.fix42.MarketDataRequest.NoMDEntryTypes noMDEntryType44 = null;
		noMDEntryType44 = new quickfix.fix42.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.BID));
		FIX44_MarketDataRequest_SIDE_BID = noMDEntryType44;
		noMDEntryType44 = new quickfix.fix42.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType(MDEntryType.OFFER));
		FIX44_MarketDataRequest_SIDE_OFFER = noMDEntryType44;
		
		String initiatorOrAcceptor = AppConfig.getValue("fix", "operatingMode");
		INITIATOR_MODE =  initiatorOrAcceptor.equalsIgnoreCase("initiator");
	}

	private final ConcurrentHashMap<SessionID, Boolean> sessionsInApp = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> symbolRequestIdMap = new ConcurrentHashMap<>();

	private volatile SessionID marketDataSessionID = null;
	private volatile SessionID tradingSessionID = null;
	private volatile long mklogonMill = -1;



	@Override
	public void onCreate(SessionID sessionId) {
		logger.info("onCreate:" + sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		logger.info("onLogon:" + sessionId);

		if (sessionId.toString().contains("STREAMING")) {
			marketDataSessionID = sessionId;
			mklogonMill = System.currentTimeMillis();
		} else {
			tradingSessionID = sessionId;
		}
		sessionsInApp.put(sessionId, Boolean.TRUE);
	}

	@Override
	public void onLogout(SessionID sessionId) {
		logger.info("onLogout:" + sessionId);
		if (sessionId.toString().contains("STREAMING")) {
			marketDataSessionID = null;
		} else {
			tradingSessionID = null;
		}
		sessionsInApp.put(sessionId, Boolean.FALSE);
	}

	@Override
	public void toAdmin(quickfix.Message message, SessionID sessionId) {
		logger.info("toAdmin:" + sessionId + ":" + message);
		String msgType;
		try {
			msgType = message.getHeader().getString(MsgType.FIELD);
			if (INITIATOR_MODE && MsgType.LOGON.equals(msgType)) {
				String username = AppConfig.getValue("fix", "fix.username");
				if (username != null) {
					String password = AppConfig.getValue("fix", "fix.password");

					if (username != null && !username.isEmpty()) {
						if (FixVersions.BEGINSTRING_FIX42.equals(sessionId.getBeginString())
								|| FixVersions.BEGINSTRING_FIX44.equals(sessionId.getBeginString())
								|| FixVersions.BEGINSTRING_FIXT11.equals(sessionId.getBeginString())) {
							message.setString(Username.FIELD, username);
							message.setString(Password.FIELD, password);
						}
					}
				}

			}
		} catch (FieldNotFound e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fromAdmin(quickfix.Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.debug("fromAdmin:" + sessionId + ":" + message);
		char msgType = message.getHeader().getChar(35); 

		if (msgType == '3') {
			String msg = "SessionId: " + sessionId.toString() + ", Received Reject [type '3'] Message : "
					+ message.toString();

			logger.warn(msg);
		}
		if ( '0' != msgType)  {
			logger.debug("fromAdmin: message = " + message + " " + sessionId);
			String enablePasswordCheckStr = AppConfig.getValue("fix", "enablePasswordCheck");
			boolean enablePasswordCheck = Boolean.parseBoolean(enablePasswordCheckStr);
			if ( !INITIATOR_MODE && enablePasswordCheck && 'A' == msgType) {
		        String incomingUsername = message.getString(quickfix.field.Username.FIELD);
		        String incomingPassword = message.getString(quickfix.field.Password.FIELD);
				String expectedUsername = AppConfig.getValue("fix", "fix.username");
				String expectedPassword = AppConfig.getValue("fix", "fix.password");

				if ( expectedPassword.startsWith(PasswordUtil.TTS_PASS_PREFIX)){
					String k = expectedPassword.substring(PasswordUtil.TTS_PASS_PREFIX.length());
					expectedPassword = PasswordUtil.decrypt(k);
				}
		        if (incomingPassword.equals(expectedPassword) && incomingUsername.equals(expectedUsername)) {
					logger.info("fromAdmin: message = " + incomingUsername + " logging on FIX channel. SessionID: " + sessionId.toString());
		        } else {
		        	throw new RejectLogon();
		        }
			}
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

	@Handler
	public void onBusinessMessageResponse(quickfix.fix42.ExecutionReport report, SessionID sessionId)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		System.out.println(report);
	}

	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.ExecutionReport report, SessionID sessionId)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		System.out.println(report);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix42.MarketDataRequestReject response, SessionID sessionId) {
		long timestamp = System.currentTimeMillis();
		System.out.println(response);
	}
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataRequestReject response, SessionID sessionId) {
		long timestamp = System.currentTimeMillis();
		System.out.println(response);
	}
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix42.MarketDataSnapshotFullRefresh response,
			SessionID sessionId) {
		
	}
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataSnapshotFullRefresh response,
			SessionID sessionId) {
		
	}
	@Handler
	public void onBusinessMessageResponse(quickfix.fix42.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
    	logger.warn("IQfixApp:BusinessMessageReject " + responseMessage.toString());
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
    	logger.warn("IQfixApp:BusinessMessageReject " + responseMessage.toString());
	}
	

	public void onUnsubscribe(String symbol, String requestId) {

				logger.info(String.format("Unsubscribing market data for <%s>: reqId = %s", symbol, requestId));
				if ( marketDataSessionID != null ) {
					quickfix.Message m = buildCancelEspRequestFix(symbol, requestId);
					Session sss = Session.lookupSession(marketDataSessionID);
					if ( sss != null ) {
						sss.send(m);
					}
				}

		
	}

	public void onNewExecutionRequest(String transId, String symbol, String notionalCurrency, String amount, String side, String mktOrdPrice, String timeInForceStr) {


			long currentTime = System.currentTimeMillis();
			String priceTime = "";

			String msg = (currentTime + " MktTime(LQVP):" + priceTime);

			System.out.println(msg);
			logger.info("price latency message: " + msg);

			if ((mktOrdPrice == null) || (mktOrdPrice.trim() == "")) {
				// TODO Action when price is EMPTY
			}

			Session tradeSession = null;
			if (tradingSessionID != null && (tradeSession = Session.lookupSession(tradingSessionID)).isLoggedOn()) {

				char sideC = side.equalsIgnoreCase("B") ? Side.BUY : Side.SELL;
				String sym = symbol;
				char timeInForce = DEFAULT_TIME_IN_FORCE;
				char ordType = OrdType.LIMIT;

				if ("IOC".equals(timeInForceStr) ) {
							timeInForce = TimeInForce.IMMEDIATE_OR_CANCEL;
				}
				
				
				if (symbol.indexOf('/') < 0) {
					sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
				}
				String amt = amount;
				if (amt.indexOf(".") < 0) {
					amt = amt + DECIMAL_00;
				}
				quickfix.fix42.NewOrderSingle message = new quickfix.fix42.NewOrderSingle();

				message.set(new ClOrdID(transId));
				message.set(new TransactTime(
						new java.util.Date(Long.parseLong(Long.toString(System.currentTimeMillis())))));
				message.set(new Currency(notionalCurrency));
				message.set(new Side(sideC));
				message.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC));
				message.set(new Symbol(sym));
				message.set(new OrderQty(Double.parseDouble(amt)));
				message.set(new OrdType(ordType));

				if ((mktOrdPrice != null) && (mktOrdPrice.trim().length() > 0))
					message.set(new Price(Double.parseDouble(mktOrdPrice)));

				message.set(new TimeInForce(timeInForce));
				tradeSession.send(message);
			} 

	}

	public void onNewPriceSubscriptionRquest(String requestId, String symbol) {

			

				quickfix.fix42.MarketDataRequest marketDataRequest = new quickfix.fix42.MarketDataRequest(
						new MDReqID(requestId),
						new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES), new MarketDepth(1));
				String sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
				marketDataRequest.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
				marketDataRequest.set(new AggregatedBook(true));
				marketDataRequest.set(new NoRelatedSym(1));
	
				marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_BID);
				marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_OFFER);
	
				quickfix.fix42.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix42.MarketDataRequest.NoRelatedSym();
				noRelatedSym.set(new Symbol(sym));
				marketDataRequest.addGroup(noRelatedSym);
				
				@SuppressWarnings("resource")
				Session sss = marketDataSessionID == null?  null : Session.lookupSession(marketDataSessionID);
				if ( sss != null && sss.isLoggedOn() ) {
					logger.info(String.format("Subscribing market data for <%s>: reqId = %s", symbol, requestId));

					sss.send(marketDataRequest);
					symbolRequestIdMap.put(symbol, requestId);
				} else {
					logger.warn(String.format("NOT Subscribing market data for %s as FIX session is not online or in the grace period after logon", symbol));
				}
					


	}

	private quickfix.Message buildCancelEspRequestFix(String symbol, String requestId) {
		quickfix.fix42.MarketDataRequest marketDataRequest = new quickfix.fix42.MarketDataRequest(
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

		quickfix.fix42.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix42.MarketDataRequest.NoRelatedSym();
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
}
