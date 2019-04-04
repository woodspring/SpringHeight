package com.tts.mas.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.fix.support.IQfixApp;
import com.tts.mas.function.RequestIdBuilderFunction;
import com.tts.mas.vo.LogControlVo;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.constant.Constants;
import com.tts.message.system.SystemStatusStruct.ChangeOrderMode;
import com.tts.message.system.SystemStatusStruct.ChangeOrderMode.OrderModeRequestor;
import com.tts.message.system.SystemStatusStruct.ChangeOrderMode.OrderModeType;
import com.tts.message.system.SystemStatusStruct.OrderMode;
import com.tts.message.system.UserSessionStruct.UserSessionInfo;
import com.tts.message.system.admin.AdapterStruct;
import com.tts.message.system.admin.AdapterStruct.AdapterStatus;
import com.tts.message.system.admin.AdapterStruct.AdapterStatusRequest;
import com.tts.message.system.admin.AdapterStruct.ChangeAdapterLogControl;
import com.tts.message.system.admin.AdapterStruct.ChangeAdapterSessionStatus;
import com.tts.message.system.admin.AdapterStruct.SessionStatus;
import com.tts.message.system.admin.AdapterStruct.Status;
import com.tts.message.trade.RestingOrderMessage.OrderParams;
import com.tts.message.trade.RestingOrderMessage.RestingOrder;
import com.tts.message.trade.RestingOrderMessage.RestingOrderSummaryRequest;
import com.tts.message.util.TtMsgEncoder;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi.FixVersion;
import com.tts.plugin.adapter.api.dialect.IMkRequestDialectHelper;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.route.IRoutingAgentFactory;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.constant.RestingOrderConstants;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.vo.TenorVo;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.MsgType;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.Password;
import quickfix.field.RawData;
import quickfix.field.RawDataLength;
import quickfix.field.Side;
import quickfix.field.TimeInForce;
import quickfix.field.Username;


public class MarketQFixApp extends MessageCracker implements Application, IMkQfixApp, IMsgListener {
	private final static String NAME__QUICKFIX      = "QFIXApp";
	private static final String MAINTENANCE_TS_NAME = "<Maintenance Trading Session>";
	private static final String SEPARATOR           = ".";
	private static final String SYSTEM_USER         = "system";
	private static final long SYSTEM_USER_ID        = 1L;
	private static final int SENDERCOMPID           = 49;
	private static final int TARGETCOMPID           = 56;
	
	private static final Set<String> ALL_SYMBOLS    = Collections.emptySet();
	private final static Logger logger              = LoggerFactory.getLogger(IQfixApp.class);
	private final static Logger roeLogger		    = LoggerFactory.getLogger("FIXROELogger");
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	private final boolean cancel_all_unknown_response;
	private final RequestIdRegistry requestIdRegistry   = new RequestIdRegistry();
	private final ConcurrentHashMap<SessionID, Boolean> sessionsInApp = new ConcurrentHashMap<SessionID, Boolean>();

	private final Object fixListenerMonitor = new Object();
	private final Map<String, SessionID> fixAppSessions     = new ConcurrentHashMap<String, SessionID>();
	private final Map<String, IFixListener> fixAppListeners = new ConcurrentHashMap<String, IFixListener>() {
		
		private static final long serialVersionUID = -1128332071248182213L;

		@Override
		public IFixListener remove(Object key) {
			logger.debug("Unregistering FixListener, " + key);
			return super.remove(key);
		}

		@Override
		public IFixListener put(String key, IFixListener value) {
			logger.debug("Registering FixListener, " + key);
			return super.put(key, value);
		}		
	};
	
	
	private final IFixSetting fixSetting;
	private final FixVersion fixVersion;
	private final Set<String> symbolFilter;
	private final IQfixRoutingAgent routingAgent;
	private final HashMap<SessionID, LinkedList<IFixListener>> sessionFixListeners;
	private final IFixIntegrationPluginSpi integrationPlugin;
	private final LogControlVo logControl;
	private IMsgSenderFactory msgSenderFact;
	private IMsgSender msgSender;
	
	private volatile IMkRequestDialectHelper dialectHelper;
	private volatile String currentTradingSessionName;
	private volatile Set<Session> maintenanceLogoutSessions = Collections.emptySet();
	private volatile IFixListener executionReportListener;

	
	public MarketQFixApp(IFixSetting fixSetting, IFixIntegrationPluginSpi integrationPlugin, LogControlVo logControl)	{
		super();
		logger.debug("<<< MarketQFixApp >>>");
		
		String filteredSymbolList = System.getProperty("SYMBOL.FILTER");
		if(filteredSymbolList == null || filteredSymbolList.equals("")) {
			this.symbolFilter = ALL_SYMBOLS;
		} 
		else {
			String[] symbols  = filteredSymbolList.split(",");
			this.symbolFilter = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(symbols)));
		}
		
		IRoutingAgentFactory routingAgentFactory = integrationPlugin.getRoutingAgentFactory();
		routingAgentFactory.setFixSetting(fixSetting);

		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		IMsgReceiver msgReceiver1 = msgReceiverFactory.getMsgReceiver(false, false);
		msgReceiver1.setListener(this);
		msgReceiver1.setTopic("TTS.CTRL.EVENT.*.REQUEST.FA.*.*");
		msgReceiver1.start();
		
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		this.cancel_all_unknown_response = p.getProperty("spotadapter.cancel_unknown_esp", true);
		
		this.msgSenderFact = AppContext.getContext().getBean(IMsgSenderFactory.class);
		this.msgSender     = this.msgSenderFact.getMsgSender(false, false, false);
		
		this.fixSetting          = fixSetting;
		this.integrationPlugin   = integrationPlugin;
		this.dialectHelper       = (IMkRequestDialectHelper) integrationPlugin.getRequestDialectHelper();
		this.fixVersion          = integrationPlugin.getDefaultFixMsgVersion();
		this.routingAgent        = routingAgentFactory.createRouteAgent();
		this.sessionFixListeners = new HashMap<SessionID, LinkedList<IFixListener>>();
		this.logControl = logControl;
		this.fixAppSessions.clear();
		this.fixAppListeners.clear();
	}
	
	@Override
	public boolean isLoggedOn(AppType appType) {
		return routingAgent.isRequiredSessionConnected(appType);
	}
	
	@Override
	public boolean isLoggedOn(String target) {
		return routingAgent.isRequiredSessionConnected(target);
	}
	
	public boolean isAllSessionsLoggedOn() {
		boolean allOn = true;
		for (AppType app: AppType.values()) {
			allOn = allOn || routingAgent.isRequiredSessionConnected(app);
			if ( allOn == false ) {
				return false;
			}
		}
		return allOn;
	}
	
	public boolean isAllTargetSessionsLoggedOn() {
		boolean allOn = true;
		for(String target: fixAppSessions.keySet())	{
			allOn = allOn || routingAgent.isRequiredSessionConnected(target);
			if(allOn == false ) {
				return false;
			}
		}
		return allOn;
	}

	@Override
	public void setExecutionReportListener(IFixListener fixListener) {
		this.executionReportListener = fixListener;
	}
	
	@Override
	public IFixListener getExecutionReportListener() {
		return executionReportListener;
	}
	
	@Override
	public IFixListener getSessionResponseProcessor(String sessionIdentifier) {
		return(fixAppListeners.get(sessionIdentifier));
	}

	@Override
	public void setSessionResponseProcessor(String sessionIdentifier, IFixListener fixListener) {
		fixAppListeners.put(sessionIdentifier, fixListener);
	}

	@Override
	public String getName() {
		return NAME__QUICKFIX;
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
	public void switchTradingSession(String tradingSessionName) {
		String fromTradingSession   = currentTradingSessionName;
		boolean switchToMaintenance = MAINTENANCE_TS_NAME.equals(tradingSessionName);
		boolean switchFromMaintence = MAINTENANCE_TS_NAME.equals(fromTradingSession);
		
		if(switchFromMaintence && !switchToMaintenance) {
			for(Session session: maintenanceLogoutSessions) {
				session.logon();
			}		
			this.maintenanceLogoutSessions = Collections.emptySet();
			
		}
		
		this.routingAgent.switchTradingSession(tradingSessionName);
		
		if(switchToMaintenance && !switchFromMaintence ) {
			Set<Session> newlyLoggedOutSessions = new HashSet<Session>();
			for(String target: fixAppSessions.keySet()) {
				Session session = Session.lookupSession(fixAppSessions.get(target)); 
				if(session.isEnabled() &&  session.isLoggedOn() ) {
					//session.logout();
					//newlyLoggedOutSessions.add(session);
				}
			}
			
			this.maintenanceLogoutSessions = Collections.unmodifiableSet(newlyLoggedOutSessions);
		}
		this.currentTradingSessionName = tradingSessionName;
		this.dialectHelper       = (IMkRequestDialectHelper) integrationPlugin.getRequestDialectHelper();
	}

	@Override
	public String sendRfsRequest(long size, String symbol, String notionalCurrency, String tenor, String settleDate,
			QuoteSide side, long expiryTime, IFixListener listener, AppType source) {
		Message message     = null;
		SessionID sessionID = null;
		
		String requestId = requestIdRegistry.register(listener, new RequestIdBuilderFunction(symbol, tenor, this.dialectHelper));
		fixAppListeners.put(requestId, listener);
				
		if(fixVersion == FixVersion.FIX50) {
			message = dialectHelper.buildRfsRequestFix50(size, symbol, notionalCurrency, tenor, settleDate, side, expiryTime, requestId, null, -1, null, null);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildRfsRequestFix44(size, symbol, notionalCurrency, tenor, settleDate, side, expiryTime, requestId, null, -1, null, null);
		}
		
		
		String rfsMsgTragets = System.getProperty("QFIX.RFS.REQ.TARGETS");
		if((rfsMsgTragets == null) || (rfsMsgTragets.trim().length() <= 0))	{
			sessionID = routingAgent.send(source, message);
		}
		else	{		
			String[] targets = rfsMsgTragets.split(";");
			for(String target: targets)	{
				sessionID = routingAgent.send(message, target);
			}
		}
		
		
		if(sessionID == null ) {
			return null;
		}		
		synchronized(fixListenerMonitor) {
			sessionFixListeners.get(sessionID).add(listener);
		}
	
		return requestId;
	}
	
	@Override
	public String sendRfsRequestForSwap(long size, String symbol, String notionalCurrency, String tenor, String settleDate, QuoteSide side, long size2, String tenor2, String settleDate2,  long expiryTime, IFixListener listener, AppType source) {
		Message message     = null;
		SessionID sessionID = null;
		
		String requestId = requestIdRegistry.register(listener, new RequestIdBuilderFunction(symbol, tenor, this.dialectHelper));
		fixAppListeners.put(requestId, listener);
				
		if(fixVersion == FixVersion.FIX50) {
			message = dialectHelper.buildRfsRequestFix50(size, symbol, notionalCurrency, tenor, settleDate, side, expiryTime, requestId, Constants.ProductType.FXSWAP, size2, tenor2, settleDate2);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildRfsRequestFix44(size, symbol, notionalCurrency, tenor, settleDate, side, expiryTime, requestId, Constants.ProductType.FXSWAP, size2, tenor2, settleDate2);
		}
		
		
		String rfsMsgTragets = System.getProperty("QFIX.RFS.REQ.TARGETS");
		if((rfsMsgTragets == null) || (rfsMsgTragets.trim().length() <= 0))	{
			sessionID = routingAgent.send(source, message);
		}
		else	{		
			String[] targets = rfsMsgTragets.split(";");
			for(String target: targets)	{
				sessionID = routingAgent.send(message, target);
			}
		}
		
		
		if(sessionID == null ) {
			return null;
		}		
		synchronized(fixListenerMonitor) {
			sessionFixListeners.get(sessionID).add(listener);
		}
	
		return requestId;
	}

	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.Quote responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getQuoteReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.Quote responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getQuoteReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.QuoteCancel responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getQuoteReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
    	
		requestIdRegistry.unregister(requestId);
		fixAppListeners.remove(requestId);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.QuoteCancel responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getQuoteReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
    	
		requestIdRegistry.unregister(requestId);
		fixAppListeners.remove(requestId);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.QuoteRequestReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getQuoteReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
    	
		requestIdRegistry.unregister(requestId);
		fixAppListeners.remove(requestId);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.QuoteRequestReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getQuoteReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
    	
		requestIdRegistry.unregister(requestId);
		fixAppListeners.remove(requestId);
	}
	
	
	
	@Override
	public String sendEspRequest(String symbol, String tenor, String settleDate, IFixListener listener, AppType source) {
		Message message     = null;
		SessionID sessionID = null;
		
		String requestId = requestIdRegistry.register(listener, new RequestIdBuilderFunction(symbol, tenor, this.dialectHelper));
		fixAppListeners.put(requestId, listener);
		
		if(this.symbolFilter != ALL_SYMBOLS) {
    		if(!this.symbolFilter.contains(symbol)) {
    			return requestId;
    		}
    	}
		
		if(fixVersion == FixVersion.FIX50) {
			message = dialectHelper.buildEspRequestFix50(symbol, tenor, settleDate, requestId);
		}
		else if ( fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildEspRequestFix44(symbol, tenor, settleDate, requestId);
		}
		
		String espMsgTragets = System.getProperty("QFIX.ESP.REQ.TARGETS");
		if((espMsgTragets == null) || (espMsgTragets.trim().length() <= 0))	{
			sessionID = routingAgent.send(source, message);
		}
		else	{		
			String[] targets = espMsgTragets.split(";");
			for(String target: targets)	{
				sessionID = routingAgent.send(message, target);
			}
		}
		
		if(sessionID == null) {
			return null;
		}
		synchronized(fixListenerMonitor) {
			sessionFixListeners.get(sessionID).add(listener);
		}
		
		return requestId;
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataSnapshotFullRefresh responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getMDReqID().getValue();
		boolean success = sendRespMessageToListener(requestId, responseMessage);
		
		if(!success && cancel_all_unknown_response)	{
			cancelEspRequest(responseMessage.getSymbol().getValue(), 
					TenorVo.NOTATION_SPOT, 
					null, responseMessage.getMDReqID().getValue(), 
					AppType.SPOTADAPTER);
		} 
		if ( !success ) {
			logger.debug("Unable to find fixListener, requestId=" + requestId  + " .. but cancel_unknown_reponse flag is set to false");
		}
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataIncrementalRefresh responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getMDReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.MarketDataSnapshotFullRefresh responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getMDReqID().getValue();
		boolean success = sendRespMessageToListener(requestId, responseMessage);
		
		if(!success && cancel_all_unknown_response)	{
			cancelEspRequest(responseMessage.getSymbol().getValue(), 
					TenorVo.NOTATION_SPOT, 
					null, responseMessage.getMDReqID().getValue(), 
					AppType.SPOTADAPTER);
		}
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.MarketDataIncrementalRefresh responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getMDReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix44.MarketDataRequestReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getMDReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
		
		fixAppListeners.remove(requestId);
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, "MDE", responseMessage.toString());
	}
	
	@Handler
	public void onMarketDataResponseMessage(quickfix.fix50.MarketDataRequestReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String requestId = responseMessage.getMDReqID().getValue();
		sendRespMessageToListener(requestId, responseMessage);
		
		fixAppListeners.remove(requestId);
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, "MDE", responseMessage.toString());
	}
	
	
	
	@Override
	public boolean cancelEspRequest(String symbol, String tenor, String settleDate, String requestId, AppType source) {
		Message message     = null;
		SessionID sessionID = null;
    	
		if(this.symbolFilter != ALL_SYMBOLS) {
    		if(!this.symbolFilter.contains(symbol)) {
    			return true;
    		}
    	}
		
		if(symbol == null || tenor == null || requestId == null ) {
			logger.warn((String.format("Parameters not set in cancelEspRequest, symbol<%s>, tenor<%s>, requestId<%s>", symbol, tenor, requestId)));
			return false;
		}
		
		if(fixVersion == FixVersion.FIX50) {
			message = dialectHelper.buildCancelEspRequestFix50(symbol, tenor, settleDate, requestId);
		} 
		else if ( fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildCancelEspRequestFix44(symbol, tenor, settleDate, requestId);
		}
		
		String espMsgTragets = System.getProperty("QFIX.ESP.REQ.TARGETS");
		if((espMsgTragets == null) || (espMsgTragets.trim().length() <= 0))	{
			sessionID = routingAgent.send(source, message);
		}
		else	{		
			String[] targets = espMsgTragets.split(";");
			for(String target: targets)	{
				sessionID = routingAgent.send(message, target);
			}
		}
		
		boolean requestedCancelOnRemoteEnd =  (sessionID != null);
		if(requestedCancelOnRemoteEnd )
			requestIdRegistry.unregister(requestId);
		
		fixAppListeners.remove(requestId);
		return requestedCancelOnRemoteEnd;
	}

	@Override
	public boolean sendTradeExecRequest(String product, String price, String amount, String currency, String symbol,
			String tenor, String settleDate, String priceFar, String amountFar, String tenorFar, String settleDateFar, OrderParams orderParams, QuoteSide side, String clientOrderId, String quoteId,
			AppType requestSource, String transactTime, IFixListener listener, String target, String comment) {
		Message message     = null;
		SessionID sessionID = null;
		
		String requestId = clientOrderId;
		if(listener != null)	fixAppListeners.put(requestId, listener);

		if(fixVersion == FixVersion.FIX50) {
			message = dialectHelper.buildTradeExecRequestFix50(product, price, amount, currency, symbol, tenor, settleDate, priceFar, amountFar, tenorFar, settleDateFar, clientOrderId, quoteId, orderParams, side, transactTime, comment);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildTradeExecRequestFix44(product, price, amount, currency, symbol, tenor, settleDate, priceFar, amountFar, tenorFar, settleDateFar, clientOrderId, quoteId, orderParams, side, transactTime, comment);
		}
		
		String tradeOrdTragets = System.getProperty("QFIX.TRADE.EXEC.TARGETS");
		if((tradeOrdTragets == null) || (tradeOrdTragets.trim().length() <= 0))	{
			sessionID = routingAgent.send(requestSource, message);
		}
		else	{
			String[] targets = tradeOrdTragets.split(";");
			for(String tempTarget: targets)	{
				sessionID = routingAgent.send(message, tempTarget);
			}
		}
		
		return (sessionID != null);
	}

	@Override
	public boolean sendOMSNewOrderSingleRequest(RestingOrder roeMessage, String msgTarget, AppType requestSource, 
																			String possDupFlag, long expiryDateTime) {
		Message message     = null;
		SessionID sessionID = null;
		boolean sessionConnected = false;
		
		String clOrdID      = String.valueOf(roeMessage.getOrderId());
		double orderQty     = Double.parseDouble(roeMessage.getSize());
		char side           = getTradeAction(roeMessage);
		String ordType      = roeMessage.getOrderType();
		double price        = Double.parseDouble(roeMessage.getMarketTargetRate());
		char timeInForce    = getTimeInForce(roeMessage.getExpiryDateTypeCd());		
		String currency     = roeMessage.getNotionalCurrency();
		String symbol       = roeMessage.getSymbol();
		long transactTime   = roeMessage.getCreateTimestamp();
		String settlType    = roeMessage.getSettlementType();
		String settlDate    = roeMessage.getSettlementDate();
		String securityType = roeMessage.getSecurityType();
				
		if(fixVersion == FixVersion.FIX50) {
			message = dialectHelper.buildOMSNewOrderSingleFix50(clOrdID, orderQty, side, ordType, price, timeInForce, currency, 
														symbol, expiryDateTime, transactTime, settlType, settlDate, securityType, possDupFlag);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildOMSNewOrderSingleFix44(clOrdID, orderQty, side, ordType, price, timeInForce, currency, 
														symbol, expiryDateTime, transactTime, settlType, settlDate, securityType, possDupFlag);
		}
		
		roeLogger.info("ROE New Order Single FIX Message: " + message.toString() + " Target: " + msgTarget);
				
		if((msgTarget == null) || (msgTarget.trim().isEmpty()))	{
			roeLogger.error("MISSING SYSTEM PROPERTY - MARKET_ROE_EXEC_TARGETS. USING DEFAULT SETTING REQUEST SOURCE FOR NEW ORDER SINGLE REQ");
			
			sessionConnected = routingAgent.isRequiredSessionConnected(requestSource);
			if(sessionConnected)
				sessionID = routingAgent.send(requestSource, message);
		}
		else	{
			sessionConnected = routingAgent.isRequiredSessionConnected(msgTarget);
			if(sessionConnected)
				sessionID = routingAgent.send(message, msgTarget);
		}
		
		return (sessionID != null);
	}
	
	@Override
	public boolean sendOMSOrderCancelRequest(String clOrdID, RestingOrder roeMessage, String msgTarget, AppType requestSource)	{
		Message message     = null;
		SessionID sessionID = null;
		boolean sessionConnected = false;
		
		char side          = getTradeAction(roeMessage);
		String symbol      = roeMessage.getSymbol();
		String origClOrdID = String.valueOf(roeMessage.getExternalOrderId());
		long transactTime  = roeMessage.getCreateTimestamp();
				
		if(fixVersion == FixVersion.FIX50) {
			message =  dialectHelper.buildOMSOrderCancelFix50(clOrdID, side, symbol, origClOrdID, transactTime);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildOMSOrderCancelFix44(clOrdID, side, symbol, origClOrdID, transactTime);
		}
		
		roeLogger.info("ROE Order Cancel Request FIX Message: " + message.toString() + " Target: " + msgTarget);
				
		if((msgTarget == null) || (msgTarget.trim().isEmpty()))	{
			roeLogger.error("MISSING SYSTEM PROPERTY - MARKET_ROE_EXEC_TARGETS. USING DEFAULT SETTING REQUEST SOURCE FOR CANCEL REQ");
			
			sessionConnected = routingAgent.isRequiredSessionConnected(requestSource);
			if(sessionConnected)
				sessionID = routingAgent.send(requestSource, message);
		}
		else	{
			sessionConnected = routingAgent.isRequiredSessionConnected(msgTarget);
			if(sessionConnected)
				sessionID = routingAgent.send(message, msgTarget);
		}
		
		return (sessionID != null);
	}
	
	@Override
	public boolean sendOrderHeatBandNotification(RestingOrder roeMessage, String msgTarget, AppType requestSource) {
		Message message     = null;
		SessionID sessionID = null;
		boolean sessionConnected = false;
		
		String clOrdID     = String.valueOf(roeMessage.getOrderId());
		String origClOrdID = String.valueOf(roeMessage.getExternalOrderId());
		int temperature    = roeMessage.getTemperature();
		origClOrdID        = (Integer.parseInt(origClOrdID) <= 0)? clOrdID: origClOrdID;
		
		if(fixVersion == FixVersion.FIX50) {
			message =  dialectHelper.buildOrderHeatBandMessageFix50(clOrdID, origClOrdID, temperature);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildOrderHeatBandMessageFix44(clOrdID, origClOrdID, temperature);
		}
		
		//roeLogger.debug("Order Heat Band Update Request FIX Message: " + message.toString() + " Target: " + heatBandTragets);
						
		if((msgTarget == null) || (msgTarget.trim().isEmpty()))	{
			roeLogger.error("MISSING SYSTEM PROPERTY - MARKET_ROE_HEAT_BAND_TARGETS");
			/*sessionConnected = routingAgent.isRequiredSessionConnected(requestSource);
			if(sessionConnected)
				sessionID = routingAgent.send(requestSource, message);	*/
		}
		else	{
			sessionConnected = routingAgent.isRequiredSessionConnected(msgTarget);
			if(sessionConnected)
				sessionID = routingAgent.send(message, msgTarget);
		}
		
		return (sessionID != null);
	}
	
	@Override
	public boolean sendOMSOrderStatusRequest(String orderID, String clOrdID, String msgTarget, AppType requestSource) {
		Message message     = null;
		SessionID sessionID = null;
		boolean sessionConnected = false;
		
		if(fixVersion == FixVersion.FIX50) {
			message =  dialectHelper.buildOMSOrderStatusFix50(orderID, clOrdID);
		} 
		else if(fixVersion == FixVersion.FIX44) {
			message = dialectHelper.buildOMSOrderStatusFix44(orderID, clOrdID);
		}
		
		roeLogger.info("ROE Order Status Request FIX Message: " + message.toString() + " Target: " + msgTarget);
		
		if((msgTarget == null) || (msgTarget.trim().isEmpty()))	{
			roeLogger.error("MISSING SYSTEM PROPERTY - MARKET_ROE_EXEC_TARGETS. USING DEFAULT SETTING REQUEST SOURCE FOR STATUS REQ");
			
			sessionConnected = routingAgent.isRequiredSessionConnected(requestSource);
			if(sessionConnected)
				sessionID = routingAgent.send(requestSource, message);
		}
		else	{
			sessionConnected = routingAgent.isRequiredSessionConnected(msgTarget);
			if(sessionConnected)
				sessionID = routingAgent.send(message, msgTarget);
		}
		
		return (sessionID != null);
	}
	
	
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.ExecutionReport responseMessage, SessionID sessionId) 
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue	{

		String listenerKey    = getListenerKey(responseMessage);
		IFixListener listener = fixAppListeners.get(listenerKey);
		String clOrdID        = (responseMessage.isSetField(790))? responseMessage.getOrdStatusReqID().getValue()
				 												 : responseMessage.getClOrdID().getValue();
		
		logger.info("ORDER_ID: " + clOrdID + " being Processed By " 
		                         + listenerKey + ". " + String.valueOf(listener == null) + " >>> " + responseMessage.toString());
		
		if(listener != null)
			sendRespMessageToListener(listenerKey, responseMessage);
		else if(executionReportListener != null)
			executionReportListener.onMessage(responseMessage, routingAgent);
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix50.ExecutionReport responseMessage, SessionID sessionId) 
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue	{

		String listenerKey    = getListenerKey(responseMessage);
		IFixListener listener = fixAppListeners.get(listenerKey);
		String clOrdID        = (responseMessage.isSetField(790))? responseMessage.getOrdStatusReqID().getValue()
																 : responseMessage.getClOrdID().getValue();
		
		logger.info("ORDER_ID: " + clOrdID + " being Processed By " 
                				 + listenerKey + ". " + String.valueOf(listener == null) + " >>> " + responseMessage.toString());
		
		if(listener != null)
			sendRespMessageToListener(listenerKey, responseMessage);
		else if(executionReportListener != null)
			executionReportListener.onMessage(responseMessage, routingAgent);
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.OrderCancelReject rejectMessage, SessionID sessionId)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue	{

		String listenerKey    = getListenerKey(rejectMessage);
		IFixListener listener = fixAppListeners.get(listenerKey);
		logger.info("ORDER_ID: " + rejectMessage.getClOrdID().getValue() + " being Processed By " 
								 + listenerKey + ". " + String.valueOf(listener == null) + " >>> " + rejectMessage.toString());
		
		if(listener != null)
			sendRespMessageToListener(listenerKey, rejectMessage);
		else if(executionReportListener != null)
			executionReportListener.onMessage(rejectMessage, routingAgent);
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix50.OrderCancelReject rejectMessage, SessionID sessionId)	
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue	{
		
		String listenerKey    = getListenerKey(rejectMessage);
		IFixListener listener = fixAppListeners.get(listenerKey);
		logger.info("ORDER_ID: " + rejectMessage.getClOrdID().getValue() + " being Processed By " 
                			     + listenerKey + ". " + String.valueOf(listener == null) + " >>> " + rejectMessage.toString());
		
		if(listener != null)
			sendRespMessageToListener(listenerKey, rejectMessage);
		else if(executionReportListener != null)
			executionReportListener.onMessage(rejectMessage, routingAgent);
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix44.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, "MDE", responseMessage.toString());
	}
	
	@Handler
	public void onBusinessMessageResponse(quickfix.fix50.BusinessMessageReject responseMessage, SessionID sessionId) 
			throws FieldNotFound	{
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
    	monitorAgent.logWarnNotification("IQfixApp:BusinessMessageReject", topic,  MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, "MDE", responseMessage.toString());
	}
	
	
	
	public boolean sendRespMessageToListener(String requestId, Message message)	{
		IFixListener listener = fixAppListeners.get(requestId);
    	if(listener != null)	{
    		try	{
    			listener.onMessage(message, null);
    			if ( logger.isTraceEnabled())
    				logger.trace(listener.getClass().getName() + " Processing " + message.toString());
    		}
    		catch(FieldNotFound | UnsupportedMessageType | IncorrectTagValue fuiExp) {
    			logger.error("QFiXException", fuiExp);
    		}
    		catch(Exception exp) {
    			logger.error("Exception @ FIX Message Process. " + exp.getMessage());
    			logger.error("Exception @ FIX Message Process: ", exp);
    			
    			String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());

    			monitorAgent.logError("FIX message process error", topic, MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, 
    					"Error processing FIX message, " + message , exp);
    		}
    	}    	
    	return(listener != null);
	}
	
	public char getTradeAction(RestingOrder message)	{
		char ordAction = Side.BUY;
		String CCY1    = message.getSymbol().substring(0, 3);
		String CCY2    = message.getSymbol().substring(3);
		String NCCY    = message.getNotionalCurrency();
		
		if(NCCY.equalsIgnoreCase(CCY1))	{
			ordAction = (TradeAction.BUY.equals(message.getTradeAction()))? Side.BUY: Side.SELL;
		}
		if(NCCY.equals(CCY2))	{
			ordAction = (TradeAction.BUY.equals(message.getTradeAction()))? Side.SELL: Side.BUY;
		}
		
		return(ordAction);
	}
	
	public char getTimeInForce(String timeInForce)	{
		char tifValue = TimeInForce.GOOD_TILL_CANCEL;
		
		if(RestingOrderConstants.OrderDurationType.GOOD_TIL_CANCELLED.equalsIgnoreCase(timeInForce))
			tifValue = TimeInForce.GOOD_TILL_CANCEL;
		
		if(RestingOrderConstants.OrderDurationType.GOOD_TIL_DATE.equalsIgnoreCase(timeInForce))
			tifValue = TimeInForce.GOOD_TILL_DATE;
			
		return(tifValue);
	}
	
	private String getListenerKey(Message message)	{
		String listenerKey = null;
		
		try {
			String msgSenderId = message.getHeader().getString(SENDERCOMPID);
			String msgTargetId = message.getHeader().getString(TARGETCOMPID);
			listenerKey        = msgTargetId + SEPARATOR + msgSenderId;
		} 
		catch (FieldNotFound fnfExp) {
			logger.error("FieldNotFound getting Listener Key from Message Header. " + fnfExp.getMessage());
			logger.error("FieldNotFound: ", fnfExp);
		}
		catch (Exception exp) {
			logger.error("Exception getting Listener Key from Message Header. " + exp.getMessage());
			logger.error("Exception: ", exp);
		}
		
		return(listenerKey);
	}
	
	
	
	
	
	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.debug("fromAdmin:" + sessionId + ":" + message);
		
		if(message.getHeader().getString(35).equals("3"))	{
			String msg = "SessionId: " + sessionId.toString()
					   + ", Received Reject [type '3'] Message : " + message.toString(); 
			
			logger.warn(msg);
			String topic = String.format(IEventMessageTypeConstant.Control.Notification.WARN_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logInfoNotification("IQfixApp:fromAdmin", topic, MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, "MDE", msg);
		}
		
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		crack(message, sessionId);
	}

	@Override
	public void onCreate(SessionID sessionId) {
		logger.debug("onCreate:" + sessionId);
		String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
		fixAppSessions.put(key, sessionId);
		sessionsInApp.put(sessionId, Boolean.FALSE);

	}

	@Override
	public void onLogon(SessionID sessionId) {
		logger.debug("onLogon:" + sessionId);
		
		synchronized(fixListenerMonitor) {
			routingAgent.registerSession(sessionId);
			sessionFixListeners.put(sessionId, new LinkedList<IFixListener>());
		}
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
		monitorAgent.logInfoNotification("IQfixApp:onLogon", topic, MonitorConstant.FXADT.INFO_CONNECTED_TO_MARKET_ACCESS_PLATFORM, 
				"MDE", "Connected to Market Access Platform, " + sessionId.toString());
		
		sessionsInApp.put(sessionId, Boolean.TRUE);
		
		//		Notify ROE Listener About Logon
		String roeOrdSession    = System.getenv("MARKET_ROE_EXEC_TARGETS");
		String roeHbSession     = System.getenv("MARKET_ROE_HEAT_BAND_TARGETS");
		String roeOrdStsSession = System.getenv("MARKET_ROE_STATUS_TARGETS");
		String senderReceiver   = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
		
		
		if((roeHbSession != null) && (!roeHbSession.trim().isEmpty()) && (roeHbSession.equals(senderReceiver)) 
								  && (routingAgent.isRequiredSessionConnected(roeHbSession)))	{
			roeLogger.info("ROE:onLogon SESSION: " + sessionId.toString() + " KEY: " + senderReceiver);
			requestForROEHeatBandUpdate(sessionId);
		}
		
		if((roeOrdSession != null) && (roeOrdSession.equals(senderReceiver)) && (routingAgent.isRequiredSessionConnected(roeOrdSession)))	{
			roeLogger.info("ROE:onLogon SESSION: " + sessionId.toString() + " KEY: " + senderReceiver);
			
			notifySessionDisconnection(true);
						
			topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logError("IQfixApp:onLogon", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
					"Resting Order Connected to Market Access Platform, " + sessionId.toString());
		}
		
		if((roeOrdStsSession != null) && (roeOrdStsSession.equals(senderReceiver)) && (routingAgent.isRequiredSessionConnected(roeOrdStsSession)))	{
			roeLogger.info("ROE:onLogon SESSION: " + sessionId.toString() + " KEY: " + senderReceiver);
			
			requestToInitOrdStatusReq(sessionId);
						
			topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logError("IQfixApp:onLogon", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
					"Resting Order Status Session Connected to Market Access Platform, " + sessionId.toString());
		}
	}

	@Override
	public void onLogout(SessionID sessionId) {
		logger.debug("onLogout:" + sessionId);
		
		synchronized(fixListenerMonitor) {
			routingAgent.unregisterSession(sessionId);
			LinkedList<IFixListener> list = sessionFixListeners.get(sessionId);
			if(list != null) {
				list.forEach(new Consumer<IFixListener>() {					
					@Override
					public void accept(IFixListener t) {
						t.onFixSessionLogoff();				
					}
				});
			}
			sessionFixListeners.remove(sessionId);
		}
		
		if(routingAgent.isSessionDisconnectExcepted(sessionId)) {
			String topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logInfoNotification("IQfixApp:onLogout", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
					"MDE", "Disconnected from Market Access Platform, " + sessionId.toString() );
		} 
		else {
			String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logError("IQfixApp:onLogout", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
					"Disconnected from Market Access Platform, " + sessionId.toString() );
		}
		
		sessionsInApp.put(sessionId, Boolean.FALSE);
		
					
		//	Notify ROE Listener About Disconnection
		String roeOrdSession  = System.getenv("MARKET_ROE_EXEC_TARGETS");
		String senderReceiver = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
		
		if((roeOrdSession != null) && (roeOrdSession.equals(senderReceiver)) && (!routingAgent.isSessionDisconnectExcepted(sessionId)))	{
			roeLogger.info("ROE:onLogout SESSION: " + sessionId.toString() + " KEY: " + senderReceiver);
			
			notifySessionDisconnection(false);
						
			String topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logError("IQfixApp:onLogout", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
					"Resting Order Disconnected from Market Access Platform, " + sessionId.toString());
		}
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		logger.debug("toAdmin:" + sessionId + ":" + message);
		if(fixSetting.getUserName(sessionId) != null ) {
			String username = fixSetting.getUserName(sessionId);
			String msgType;
			
			try {
				msgType = message.getHeader().getString(MsgType.FIELD);
				if(MsgType.LOGON.equals(msgType)) {
					if(username != null && !username.isEmpty()) {				
						if(FixVersions.BEGINSTRING_FIX42.equals(sessionId.getBeginString())) {
							message.setString(RawData.FIELD, username + "@" + fixSetting.getPassword(sessionId));
							message.setInt(RawDataLength.FIELD, username.length() + fixSetting.getPassword(sessionId).length() + 1);
						} 
						else if (FixVersions.BEGINSTRING_FIX44.equals(sessionId.getBeginString()) 
								|| FixVersions.BEGINSTRING_FIXT11.equals(sessionId.getBeginString())) {
							message.setString(Username.FIELD, username);
							message.setString(Password.FIELD, fixSetting.getPassword(sessionId));
						}
					}
				}
			} 
			catch(FieldNotFound fnfExp) {
				fnfExp.printStackTrace();
			}
		}
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		if(fixSetting.getOnBehalfOfName(sessionId) != null && !message.getHeader().isSetField(115) ) {
			message.getHeader().setField(new OnBehalfOfCompID(fixSetting.getOnBehalfOfName(sessionId)));
		}
	}
	
	@Override
	public void onMessage(TtMsg arg0, IMsgSessionInfo arg1, IMsgProperties arg2) {
		try {
			if ( arg0.hasParamType()  ) {
				if ( arg0.getParamType().equals(AdapterStatusRequest.class.getName())) {
					
					AdapterStatusRequest asr  = AdapterStatusRequest.parseFrom(arg0.getParameters());
					if ( asr.hasAdapterName() && asr.getAdapterName().equals(Constants.FixAdapterNameType.DEFAULT_FIX_ADAPTER)) {

						AdapterStatus.Builder adapterStatusBuilder = AdapterStatus.newBuilder();
						adapterStatusBuilder.setAdapterName(Constants.FixAdapterNameType.DEFAULT_FIX_ADAPTER);
						for ( Entry<SessionID, Boolean> s: sessionsInApp.entrySet() ) {
							SessionStatus.Builder ss = SessionStatus.newBuilder();
							ss.setSessionName(s.getKey().toString());
							ss.setStatus(s.getValue() ? Status.ACTIVE : Status.INACTIVE);
							adapterStatusBuilder.addActiveSessions(ss);
						}
						
						arg1.getReplySender().sendReply(TtMsgEncoder.encode(adapterStatusBuilder.build()), arg2);
					}
				} else if ( arg0.getParamType().equals(AdapterStruct.ChangeAdapterLogControl.class.getName())) {
					ChangeAdapterLogControl calc = ChangeAdapterLogControl.parseFrom(arg0.getParameters());
					if ( calc.hasSessionType() && calc.hasNewStatus() && "MD".equals(calc.getSessionType())) {
						logControl.setIsLogMarketData(calc.getNewStatus() == Status.ACTIVE);
					}
				} else if ( arg0.getParamType().equals(AdapterStruct.ChangeAdapterSessionStatus.class.getName())) {
					ChangeAdapterSessionStatus cass = ChangeAdapterSessionStatus.parseFrom(arg0.getParameters());
					for ( Entry<SessionID, Boolean> s: sessionsInApp.entrySet() ) {
						if ( s.getKey().toString().equals(cass.getSessionName())) {
							Session sss = Session.lookupSession(s.getKey());
							
							if ( sss.isLoggedOn() && cass.getNewStatus() == Status.INACTIVE) {
								sss.logout();
							} else if ( !sss.isLoggedOn() && cass.getNewStatus() == Status.ACTIVE ) {
								sss.logon();
							}								
						}
					}
				} 
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
	
	private void notifySessionDisconnection(boolean enableOrder)	{
		ChangeOrderMode.Builder chngOrderMode = ChangeOrderMode.newBuilder();
		
		UserSessionInfo.Builder userSession = UserSessionInfo.newBuilder();
		userSession.setUserNm(SYSTEM_USER);
		userSession.setUserId(SYSTEM_USER_ID);
		userSession.setCmId(System.currentTimeMillis());
		userSession.setUserExtId(SYSTEM_USER_ID);
		userSession.setInstanceNm("FIXROEAppImpl");
		
		OrderMode.Builder ordMode = OrderMode.newBuilder();
		ordMode.setIsEnable(enableOrder);		
		
		chngOrderMode.setOrderModeType(OrderModeType.SUBMIT);
		chngOrderMode.setRequestor(OrderModeRequestor.FIX);
		chngOrderMode.setUserSession(userSession);
		chngOrderMode.setOrderMode(ordMode);
		chngOrderMode.setSequence(System.currentTimeMillis());
		
		TtMsg msgOrderMode = TtMsgEncoder.encode(chngOrderMode.build());
		msgSender.send(IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ROR_ORDERMODE, msgOrderMode);
		
		roeLogger.info("Notify ROE about FIX Logon/Logoff. EnableOrder: " + TextFormat.printToString(chngOrderMode.build()));
	}
	
	private void requestForROEHeatBandUpdate(SessionID sessionId)	{
		RestingOrderSummaryRequest.Builder summaryRequest = RestingOrderSummaryRequest.newBuilder();
		
		summaryRequest.setRequestor("FXADT-HB");
		roeLogger.info("Requesting Heat Band Update for All Active Orders. " + TextFormat.printToString(summaryRequest.build()));
		
		TtMsg msgSummaryReq = TtMsgEncoder.encode(summaryRequest.build());
		msgSender.send(IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ROR_ORDERINFO, msgSummaryReq);
	}
	
	private void requestToInitOrdStatusReq(SessionID sessionId)	{
		RestingOrderSummaryRequest.Builder summaryRequest = RestingOrderSummaryRequest.newBuilder();
		
		summaryRequest.setRequestor("FXADT-ORDSTS");
		roeLogger.info("Requesting for Initiating Order Status Request. " + TextFormat.printToString(summaryRequest.build()));
		
		TtMsg msgSummaryReq = TtMsgEncoder.encode(summaryRequest.build());
		msgSender.send(IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ROR_ORDERINFO, msgSummaryReq);
	}
}