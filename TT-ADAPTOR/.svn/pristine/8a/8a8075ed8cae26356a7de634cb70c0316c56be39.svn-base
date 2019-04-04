package com.tts.ske.app;

import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.ske.app.price.push.PricePushWorker;
import com.tts.ske.app.price.subscription.AbstractSubscriptionHandler;
import com.tts.ske.app.price.subscription.EspHandler;
import com.tts.ske.app.price.subscription.PriceSubscriptionHandlerFactory;
import com.tts.ske.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.ske.vo.SubscriptionRequestVo;
import com.tts.ske.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.ske.vo.SubscriptionRequestVo.StreamType;
import com.tts.service.biz.transactions.vo.TransactionVo;
import com.tts.util.AppConfig;
import com.tts.util.AppUtils;
import com.tts.util.PasswordUtil;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;
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
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.FutSettDate;
import quickfix.field.LastPx;
import quickfix.field.LastShares;
import quickfix.field.LeavesQty;
import quickfix.field.MDEntryType;
import quickfix.field.MsgType;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrigClOrdID;
import quickfix.field.Password;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.Username;

public class TtsTradairQfixApplication extends MessageCracker implements Application {
	private final static Logger logger = LoggerFactory.getLogger(TtsTradairQfixApplication.class);
	private static final com.tts.monitor.agent.api.IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

	private final PriceSubscriptionHandlerFactory handlerFactory = new PriceSubscriptionHandlerFactory();

	private final PriceSubscriptionRegistry registry;
	private final String tradAirAccInTts;
	private final PricePushWorker pushWorker = new PricePushWorker();
	private final DataFlowController controller;
	private final boolean isInitiatorMode;

	public TtsTradairQfixApplication(PriceSubscriptionRegistry registry, DataFlowController dataFlowController) {
		super();
		String initiatorOrAcceptor = AppConfig.getValue("fix", "operatingMode");
		String enableSelfInjection = AppConfig.getValue("marketData", "enableSelfInjection");
		
		if ( "TRUE".equalsIgnoreCase(enableSelfInjection)) {
			this.pushWorker.start();
		}
		this.registry = registry;
		this.controller = dataFlowController;
		this.tradAirAccInTts = AppConfig.getValue("ordering", "acctNmInTts");
		
		this.isInitiatorMode =  initiatorOrAcceptor.equalsIgnoreCase("initiator");
	}

	@Override
	public void fromAdmin(Message arg0, SessionID arg1)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		try {
			char msgType = arg0.getHeader().getChar(35); 
			if ( '0' != msgType)  {
				logger.debug("fromAdmin: message = " + arg1 + " " + arg0);
				if ( !isInitiatorMode && 'A' == msgType) {
			        String incomingUsername = arg0.getString(quickfix.field.Username.FIELD);
			        String incomingPassword = arg0.getString(quickfix.field.Password.FIELD);
					String expectedUsername = AppConfig.getValue("fix", "fix.username");
					String expectedPassword = AppConfig.getValue("fix", "fix.password");

					if ( expectedPassword.startsWith(PasswordUtil.TTS_PASS_PREFIX)){
						String k = expectedPassword.substring(PasswordUtil.TTS_PASS_PREFIX.length());
						expectedPassword = PasswordUtil.decrypt(k);
					}
			        if (incomingPassword.equals(expectedPassword) && incomingUsername.equals(expectedUsername)) {
						logger.info("fromAdmin: message = " + incomingUsername + " logging on FIX channel. SessionID: " + arg1.toString());
			        } else {
			        	throw new RejectLogon();
			        }
				}
			}
		} catch (FieldNotFound e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fromApp(Message arg0, SessionID arg1)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		logger.debug("<<>> fromApp:" + arg0 + " : " + arg1);
		crack(arg0, arg1);
	}

	@Override
	public void onCreate(SessionID arg0) {
		logger.debug("onCreate:" + arg0);

	}

	@Override
	public void onLogon(SessionID arg0) {
		logger.debug("onLogon:" + arg0);
	}

	@Override
	public void onLogout(SessionID sessionID) {
		logger.debug("onLogout:" + sessionID);
		List<AbstractSubscriptionHandler> list = registry.getAndRemoveSessionSubscriptions(sessionID);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				pushWorker.removeSubscription(list.get(i));
			}
		}
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		try {
			String msgType = message.getHeader().getString(MsgType.FIELD);

			if (!MsgType.HEARTBEAT.equals(msgType))  {
				logger.debug("toAdmin: message = " + sessionId + " " + message);
			}
			if (isInitiatorMode && MsgType.LOGON.equals(msgType)) {
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
	public void toApp(Message arg0, SessionID arg1) throws DoNotSend {
		// logger.debug("toApp:" + arg1 + ":" + arg0);
	}

	public void onMessage(quickfix.fix50.QuoteRequest quoteRequest, SessionID sessionID)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

	}

	public void onMessage(quickfix.fix42.NewOrderSingle request, SessionID sessionID)  {
		boolean acked = false, confirmed = false, partialFilled = false, completeFilled = false;
		boolean messageProcessingException = false;
		StringBuilder sbTradeComment = new StringBuilder();
		TransactionVo transaction = null;
		String ttsOrderId = null;
		try {
			boolean isResent = false;
			try {
				isResent = request.getHeader().getBoolean(43);
			} catch ( quickfix.FieldNotFound e) {
				isResent = false;
			}
			
			quickfix.fix42.ExecutionReport report = new quickfix.fix42.ExecutionReport();
	    	double originalRequestSize = request.getOrderQty().getValue();
	    	String symbol = request.getSymbol().getValue();
	    	symbol=symbol.replaceFirst("/","");
	    	String clientOrderId = request.getClOrdID().getValue();

			report.set(request.getOrdType());
	    	report.set(request.getClOrdID());
			report.set(new OrigClOrdID(clientOrderId));

	    	report.set(request.getSide());
	    	report.set(new LastShares(0));
	    	report.set(request.getOrderQty());
	    	report.set(request.getSymbol());
	   		report.set(request.getCurrency());
	    	report.set(new ExecType(ExecType.NEW));  	
	    	report.set(new OrdStatus(OrdStatus.NEW));
			report.set(new LeavesQty(originalRequestSize));
	    	report.set(new CumQty(0));
	    	report.set(new LastPx(0));
	    	report.set(request.getPrice());
	    	report.set(new TransactTime());    	
	    	report.set(new ExecID("0"));
	    	report.set(new ExecTransType(ExecTransType.NEW));
	    		    	
	    	if(isResent)	{
	    		Session session = Session.lookupSession(sessionID);
	    		
	    		report.set(new ExecType(ExecType.CANCELED)); 
	        	report.set(new OrdStatus(OrdStatus.CANCELED));
	        	report.set(new OrderID(ChronologyUtil.getTimeStamp()));
		    	session.send(report);
		    	acked = false;
		    	return;
	    	}
	    	
			boolean ordType_IsFOK = request.getTimeInForce().getValue() == TimeInForce.IMMEDIATE_OR_CANCEL ? false : true;
			String clientTradeAction = request.getSide().getValue() == Side.BUY ? TradeAction.BUY : TradeAction.SELL;
						
			ExecutionReportInfo reportInt = null;
	    	if ( ( request.getOrdType().getValue() == OrdType.FOREX_LIMIT 
	    			|| request.getOrdType().getValue() == OrdType.LIMIT ) 
	    			&& symbol.indexOf(request.getCurrency().getValue()) == 0) {
				reportInt = controller.requestForHedge(
		    			clientOrderId, 
		    			symbol, 
		    			request.getCurrency().getValue(), 
		    			clientTradeAction, 
		    			Double.toString(originalRequestSize),  
		    			Double.toString(originalRequestSize),
		    			Double.toString(request.getPrice().getValue()), 
		    			ordType_IsFOK,
		    			true);
	    	}
			
			Session session = Session.lookupSession(sessionID);
	    	
			transaction = controller.registerExternalDeal(
	    			clientOrderId, 
	    			tradAirAccInTts,
	    			symbol, 
	    			request.getCurrency().getValue(), 
	    			request.getSide().getValue() == Side.BUY ? "B" : "S", 
	    			Double.toString(originalRequestSize),  
	    			"",
	    			Double.toString(request.getPrice().getValue()), 
	    			ordType_IsFOK,
	    			true);
			ttsOrderId = transaction.getTransRef();
	    	report.set(new OrderID(ttsOrderId));
	    	session.send(report);
	    	acked = true;
	    	
	    	sbTradeComment.append("Internal Reference: ").append(ttsOrderId).append(' ');
	    	sbTradeComment.append("ClientOrderId: ").append(clientOrderId).append(' ');
			sbTradeComment.append("OrdType: ").append(ordType_IsFOK ? "FOK" : "IOC").append(' ');
	    	sbTradeComment.append("LimitPrice: ").append(request.getPrice().getValue()).append(' ');
	    	sbTradeComment.append("OrgRequestSize: ").append(originalRequestSize).append(' ');

        	String settleDate = reportInt.getSettleDate();

	    	boolean tradeAccept = reportInt != null && TransStateType.INTERNAL___COVERED_BY_INTERNAL_LIQUIDITY.equals(reportInt.getStatus());
	    	if ( tradeAccept ) {		
	    		double filledAmt = Double.valueOf(reportInt.getSize());
	    		double filledRate = Double.valueOf(reportInt.getFinalPrice());
	    		double remindingAmt = originalRequestSize - filledAmt; 
	    		partialFilled = filledAmt < originalRequestSize;
	    		if ( partialFilled) {
		        	report.set(new ExecType(ExecType.FILL)); 
		        	report.set(new OrdStatus(OrdStatus.PARTIALLY_FILLED));
	    		} else {
		        	report.set(new ExecType(ExecType.FILL)); 
		        	report.set(new OrdStatus(OrdStatus.FILLED));
		        	completeFilled = true;
	    		}
		    	report.set(new LastShares(filledAmt));
				report.set(new LastPx(filledRate));
				report.set(new FutSettDate(settleDate));
	        	report.set(new ExecID(UUID.randomUUID().toString()));
	        	session.send(report);
	        	confirmed = true;
	        	if ( partialFilled ) {
		        	report.set(new ExecType(ExecType.CANCELED)); 
		        	report.set(new OrdStatus(OrdStatus.CANCELED));
			    	report.set(new LastShares(remindingAmt));
					report.set(new LastPx(0));
		        	report.removeField(64);
		        	report.set(new ExecID(UUID.randomUUID().toString()));
		        	session.send(report);
		        	confirmed = true;
	        	}
	        	transaction.getNearDateDetail().setCurrency1Amt(DoubleFormatter.convertToString(filledAmt, 2, RoundingMode.UNNECESSARY));
	        	transaction.getNearDateDetail().setTradeRate(DoubleFormatter.convertToString(filledRate, 5, RoundingMode.UNNECESSARY));
	        	if (partialFilled) {
		        	transaction.setStatus(TransStateType.TRADE_PARTIALLY_DONE);
	        	} else {
		        	transaction.setStatus(TransStateType.TRADE_DONE);
	        	}
	    	} else {
	        	report.set(new ExecType(ExecType.CANCELED)); 
	        	report.set(new OrdStatus(OrdStatus.CANCELED));	
	        	session.send(report);
	        	confirmed = true;
	        	transaction.getNearDateDetail().setCurrency1Amt("0.00");
	        	transaction.getNearDateDetail().setTradeRate("0.00000");
	        	transaction.setStatus(TransStateType.TRADE_CANCEL);
	    	}
	    	transaction.setNotionalCurrency(symbol.substring(0, 3));
			String providerTradeAction = TradeAction.BUY.equals(clientTradeAction) ? TradeAction.SELL : TradeAction.BUY;
			transaction.setTradeAction(clientTradeAction);
			transaction.setProviderTradeAction(providerTradeAction);
			transaction.getNearDateDetail().setValueDate(settleDate);
		} catch (FieldNotFound e) {
			messageProcessingException = true;
			logger.error("quickfix.FieldNotException on NewOrderSingle", e);
		} catch (RuntimeException e) {
			messageProcessingException = true;
			logger.error("RuntimeException on NewOrderSingle", e);
		} finally {
			if (messageProcessingException ) {
				String tradeCommentBefore = sbTradeComment.toString();
				StringBuilder errorSb = new StringBuilder();
				errorSb.append("External Order Processing Error!! ");
				errorSb.append("acked: ").append(acked).append(' ');
				errorSb.append("confirmed: ").append(confirmed).append(' ');
				errorSb.append("partiallyFilled: ").append(partialFilled).append(' ');
				errorSb.append("completelyFilled: ").append(completeFilled).append(' ');
				errorSb.append("orig. FIX request: ").append(request).append(' ');
				sbTradeComment = errorSb;
				sbTradeComment.append(tradeCommentBefore);
			}
			if ( !confirmed && (partialFilled || completeFilled) ) {
				sbTradeComment.append("Trade confirmed INTERNALLY only but NOT with Counterparty");
			}

			if (transaction != null ) {
				controller.recordCompletedExternalDeal(transaction, sbTradeComment.toString());
			}
		}
	}

	public void onMessage(quickfix.fix42.MarketDataRequest request, SessionID sessionID)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		String clientReqId = request.getMDReqID().getValue();

		if (SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST == request
				.getSubscriptionRequestType().getValue()) {
			AbstractSubscriptionHandler h = registry.findByRequestID(clientReqId, sessionID);
			if (h != null) {
				controller.registerListener(h.getRequest().getSymbol(), ((EspHandler) h));

				pushWorker.removeSubscription(h);
				registry.removeSubscription(clientReqId, sessionID);
			}
			return;
		}
		int mdEntryTypeCount = request.getNoMDEntryTypes().getValue();
		int relatedSymbolCount = request.getNoRelatedSym().getValue();

		quickfix.fix42.MarketDataRequest.NoMDEntryTypes noMDEntryTypes = new quickfix.fix42.MarketDataRequest.NoMDEntryTypes();

		QuoteSide quoteSide = null;
		boolean isBuy = false, isSell = false;
		for (int i = 1; i <= mdEntryTypeCount; ++i) {
			request.getGroup(i, noMDEntryTypes);
			if (MDEntryType.BID == noMDEntryTypes.getMDEntryType().getValue()) {
				isBuy = true;
			} else if (MDEntryType.OFFER == noMDEntryTypes.getMDEntryType().getValue()) {
				isSell = true;
			}
		}

		if (isBuy && isSell) {
			quoteSide = QuoteSide.BOTH;
		} else if (isBuy) {
			quoteSide = QuoteSide.BUY;
		} else if (isSell) {
			quoteSide = QuoteSide.SELL;
		}

		quickfix.fix42.MarketDataRequest.NoRelatedSym noRelatedSyms = new quickfix.fix42.MarketDataRequest.NoRelatedSym();

		for (int i = 1; i <= relatedSymbolCount; i++) {
			request.getGroup(i, noRelatedSyms);
			String symbol = noRelatedSyms.getSymbol().getValue();
			String tenor = TenorVo.NOTATION_SPOT;

			SubscriptionRequestVo sRequest = new SubscriptionRequestVo();
			sRequest.setClientReqId(clientReqId);
			sRequest.setQuoteSide(quoteSide);
			sRequest.setSymbol(symbol.replaceFirst("/", ""));
			sRequest.setStreamType(StreamType.ESP);
			sRequest.setTenor(tenor);
			if (request.isSetMarketDepth()) {
				sRequest.setTopOfBook(request.getMarketDepth().getValue() == 1);
			}
			AbstractSubscriptionHandler h = handlerFactory.getSubscriptionHandler(sRequest, sessionID, request,
					registry);
			registry.addSubscription(h, sessionID);
			pushWorker.addSubscription(h);
			controller.registerListener(sRequest.getSymbol(), ((EspHandler) h));
		}

	}

	public void onMessage(quickfix.fix44.NewOrderSingle request, SessionID sessionID)  {
		boolean acked = false, confirmed = false, partialFilled = false, completeFilled = false;
		boolean messageProcessingException = false;
		StringBuilder sbTradeComment = new StringBuilder();
		TransactionVo transaction = null;
		String ttsOrderId = null;
		try {
			boolean isResent = false;
			try {
				isResent = request.getHeader().getBoolean(43);
			} catch ( quickfix.FieldNotFound e) {
				isResent = false;
			}
			
			quickfix.fix42.ExecutionReport report = new quickfix.fix42.ExecutionReport();
	    	double originalRequestSize = request.getOrderQty().getValue();
	    	String symbol = request.getSymbol().getValue();
	    	symbol=symbol.replaceFirst("/","");
	    	String clientOrderId = request.getClOrdID().getValue();

			report.set(request.getOrdType());
	    	report.set(request.getClOrdID());
			report.set(new OrigClOrdID(clientOrderId));

	    	report.set(request.getSide());
	    	report.set(new LastShares(0));
	    	report.set(request.getOrderQty());
	    	report.set(request.getSymbol());
	   		report.set(request.getCurrency());
	    	report.set(new ExecType(ExecType.NEW));  	
	    	report.set(new OrdStatus(OrdStatus.NEW));
			report.set(new LeavesQty(originalRequestSize));
	    	report.set(new CumQty(0));
	    	report.set(new LastPx(0));
	    	report.set(request.getPrice());
	    	report.set(new TransactTime());    	
	    	report.set(new ExecID("0"));
	    	report.set(new ExecTransType(ExecTransType.NEW));
			boolean ordType_IsFOK = request.getTimeInForce().getValue() == TimeInForce.IMMEDIATE_OR_CANCEL ? false : true;
	    	Session session = Session.lookupSession(sessionID);

			if ( ! isResent ) {
				transaction = controller.registerExternalDeal(
		    			clientOrderId, 
		    			tradAirAccInTts,
		    			symbol, 
		    			request.getCurrency().getValue(), 
		    			request.getSide().getValue() == Side.BUY ? "B" : "S", 
		    			Double.toString(originalRequestSize),  
		    			"",
		    			Double.toString(request.getPrice().getValue()), 
		    			ordType_IsFOK,
		    			true);
				ttsOrderId = transaction.getTransRef();
		    	report.set(new OrderID(ttsOrderId));
		    	session.send(report);
		    	acked = true;
			} else {
	        	report.set(new ExecType(ExecType.CANCELED)); 
	        	report.set(new OrdStatus(OrdStatus.CANCELED));
	        	report.set(new OrderID(ChronologyUtil.getTimeStamp()));
		    	session.send(report);
		    	acked = false;
		    	return;
			}

	
	    	sbTradeComment.append("Internal Reference: ").append(ttsOrderId).append(' ');
	    	sbTradeComment.append("ClientOrderId: ").append(clientOrderId).append(' ');
			String clientTradeAction = request.getSide().getValue() == Side.BUY ? TradeAction.BUY : TradeAction.SELL;

	    	ExecutionReportInfo reportInt = null;
	    	if ( ( request.getOrdType().getValue() == OrdType.FOREX_LIMIT 
	    			|| request.getOrdType().getValue() == OrdType.LIMIT )
	    			&& symbol.indexOf(request.getCurrency().getValue()) == 0) {
				reportInt = controller.requestForHedge(
		    			clientOrderId, 
		    			symbol, 
		    			request.getCurrency().getValue(), 
		    			clientTradeAction, 
		    			Double.toString(originalRequestSize),  
		    			Double.toString(originalRequestSize),
		    			Double.toString(request.getPrice().getValue()), 
		    			ordType_IsFOK,
		    			true);
	    	}
	    	sbTradeComment.append("OrdType: ").append(ordType_IsFOK ? "FOK" : "IOC").append(' ');
	    	sbTradeComment.append("LimitPrice: ").append(request.getPrice().getValue()).append(' ');
	    	sbTradeComment.append("OrgRequestSize: ").append(originalRequestSize).append(' ');

        	String settleDate = reportInt.getSettleDate();

	    	boolean tradeAccept = reportInt != null && TransStateType.INTERNAL___COVERED_BY_INTERNAL_LIQUIDITY.equals(reportInt.getStatus());
	    	if ( tradeAccept ) {		
	    		double filledAmt = Double.valueOf(reportInt.getSize());
	    		double filledRate = Double.valueOf(reportInt.getFinalPrice());
	    		double remindingAmt = originalRequestSize - filledAmt; 
	    		partialFilled = filledAmt < originalRequestSize;
	    		if ( partialFilled) {
		        	report.set(new ExecType(ExecType.FILL)); 
		        	report.set(new OrdStatus(OrdStatus.PARTIALLY_FILLED));
	    		} else {
		        	report.set(new ExecType(ExecType.FILL)); 
		        	report.set(new OrdStatus(OrdStatus.FILLED));
		        	completeFilled = true;
	    		}
		    	report.set(new LastShares(filledAmt));
				report.set(new LastPx(filledRate));
				report.set(new FutSettDate(settleDate));
	        	report.set(new ExecID(UUID.randomUUID().toString()));
	        	session.send(report);
	        	confirmed = true;
	        	if ( partialFilled ) {
		        	report.set(new ExecType(ExecType.CANCELED)); 
		        	report.set(new OrdStatus(OrdStatus.CANCELED));
			    	report.set(new LastShares(remindingAmt));
					report.set(new LastPx(0));
		        	report.removeField(64);
		        	report.set(new ExecID(UUID.randomUUID().toString()));
		        	session.send(report);
		        	confirmed = true;
	        	}
	        	transaction.getNearDateDetail().setCurrency1Amt(DoubleFormatter.convertToString(filledAmt, 2, RoundingMode.UNNECESSARY));
	        	transaction.getNearDateDetail().setTradeRate(DoubleFormatter.convertToString(filledRate, 5, RoundingMode.UNNECESSARY));
	        	if (partialFilled) {
		        	transaction.setStatus(TransStateType.TRADE_PARTIALLY_DONE);
	        	} else {
		        	transaction.setStatus(TransStateType.TRADE_DONE);
	        	}
	    	} else {
	        	report.set(new ExecType(ExecType.CANCELED)); 
	        	report.set(new OrdStatus(OrdStatus.CANCELED));	
	        	session.send(report);
	        	confirmed = true;
	        	transaction.getNearDateDetail().setCurrency1Amt("0.00");
	        	transaction.getNearDateDetail().setTradeRate("0.00000");
	        	transaction.setStatus(TransStateType.TRADE_CANCEL);
	    	}
	    	transaction.setNotionalCurrency(symbol.substring(0, 3));
			String providerTradeAction = TradeAction.BUY.equals(clientTradeAction) ? TradeAction.SELL : TradeAction.BUY;
			transaction.setTradeAction(clientTradeAction);
			transaction.setProviderTradeAction(providerTradeAction);
			transaction.getNearDateDetail().setValueDate(settleDate);
		} catch (FieldNotFound e) {
			messageProcessingException = true;
			logger.error("quickfix.FieldNotException on NewOrderSingle", e);
		} catch (RuntimeException e) {
			messageProcessingException = true;
			logger.error("RuntimeException on NewOrderSingle", e);
		} finally {
			if (messageProcessingException ) {
				String tradeCommentBefore = sbTradeComment.toString();
				StringBuilder errorSb = new StringBuilder();
				errorSb.append("External Order Processing Error!! ");
				errorSb.append("acked: ").append(acked).append(' ');
				errorSb.append("confirmed: ").append(confirmed).append(' ');
				errorSb.append("partiallyFilled: ").append(partialFilled).append(' ');
				errorSb.append("completelyFilled: ").append(completeFilled).append(' ');
				errorSb.append("orig. FIX request: ").append(request).append(' ');
				sbTradeComment = errorSb;
				sbTradeComment.append(tradeCommentBefore);
			}
			if ( !confirmed && (partialFilled || completeFilled) ) {
				sbTradeComment.append("Trade confirmed INTERNALLY only but NOT with Counterparty");
			}

			if (transaction != null ) {
				controller.recordCompletedExternalDeal(transaction, sbTradeComment.toString());
			}
		}
	}

	public void onMessage(quickfix.fix44.MarketDataRequest request, SessionID sessionID)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		String clientReqId = request.getMDReqID().getValue();

		if (SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST == request
				.getSubscriptionRequestType().getValue()) {
			AbstractSubscriptionHandler h = registry.findByRequestID(clientReqId, sessionID);
			if (h != null) {
				controller.registerListener(h.getRequest().getSymbol(), ((EspHandler) h));

				pushWorker.removeSubscription(h);
				registry.removeSubscription(clientReqId, sessionID);
			}
			return;
		}
		int mdEntryTypeCount = request.getNoMDEntryTypes().getValue();
		int relatedSymbolCount = request.getNoRelatedSym().getValue();

		quickfix.fix42.MarketDataRequest.NoMDEntryTypes noMDEntryTypes = new quickfix.fix42.MarketDataRequest.NoMDEntryTypes();

		QuoteSide quoteSide = null;
		boolean isBuy = false, isSell = false;
		for (int i = 1; i <= mdEntryTypeCount; ++i) {
			request.getGroup(i, noMDEntryTypes);
			if (MDEntryType.BID == noMDEntryTypes.getMDEntryType().getValue()) {
				isBuy = true;
			} else if (MDEntryType.OFFER == noMDEntryTypes.getMDEntryType().getValue()) {
				isSell = true;
			}
		}

		if (isBuy && isSell) {
			quoteSide = QuoteSide.BOTH;
		} else if (isBuy) {
			quoteSide = QuoteSide.BUY;
		} else if (isSell) {
			quoteSide = QuoteSide.SELL;
		}

		quickfix.fix42.MarketDataRequest.NoRelatedSym noRelatedSyms = new quickfix.fix42.MarketDataRequest.NoRelatedSym();

		for (int i = 1; i <= relatedSymbolCount; i++) {
			request.getGroup(i, noRelatedSyms);
			String symbol = noRelatedSyms.getSymbol().getValue();
			String tenor = TenorVo.NOTATION_SPOT;

			SubscriptionRequestVo sRequest = new SubscriptionRequestVo();
			sRequest.setClientReqId(clientReqId);
			sRequest.setQuoteSide(quoteSide);
			sRequest.setSymbol(symbol.replaceFirst("/", ""));
			sRequest.setStreamType(StreamType.ESP);
			sRequest.setTenor(tenor);
			if (request.isSetMarketDepth()) {
				sRequest.setTopOfBook(request.getMarketDepth().getValue() == 1);
			}
			AbstractSubscriptionHandler h = handlerFactory.getSubscriptionHandler(sRequest, sessionID, request,
					registry);
			registry.addSubscription(h, sessionID);
			pushWorker.addSubscription(h);
			controller.registerListener(sRequest.getSymbol(), ((EspHandler) h));
		}

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
}
