package com.tts.mlp.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.app.price.push.PricePushWorker;
import com.tts.mlp.app.price.subscription.AbstractSubscriptionHandler;
import com.tts.mlp.app.price.subscription.PriceSubscriptionHandlerFactory;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.StreamType;
import com.tts.util.AppContext;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.MDEntryType;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.QuoteID;
import quickfix.field.SettlDate;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Text;
import quickfix.field.TimeInForce;
import quickfix.fix44.ExecutionReport;

public class QfixApplication extends MessageCracker implements Application {
	private final static Logger logger = LoggerFactory.getLogger(QfixApplication.class);
	
	private final PriceSubscriptionHandlerFactory handlerFactory
									= new PriceSubscriptionHandlerFactory();
	
	private final PriceSubscriptionRegistry registry;
	
	private final PricePushWorker pushWorker = new PricePushWorker(); 
	private final SimpleDateFormat execId    = new SimpleDateFormat("YYYYMMddHHmmssSS");
	
	private final IRandomMarketPriceProvider priceProvider;
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

	public QfixApplication(PriceSubscriptionRegistry registry) {
		super();
		pushWorker.start();
		this.registry = registry;
		priceProvider = AppContext.getContext().getBean(IRandomMarketPriceProvider.class);

	}
	

	@Override
	public void fromAdmin(Message arg0, SessionID arg1) throws FieldNotFound,
			IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.debug("fromAdmin:" + arg1 + ":" + arg0);
	}

	@Override
	public void fromApp(Message arg0, SessionID arg1) throws FieldNotFound,
			IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
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
		if ( list != null ) {
			for ( int i = 0; i < list.size(); i++ ) {
				pushWorker.removeSubscription(list.get(i));
			}
		}
	}

	@Override
	public void toAdmin(Message arg0, SessionID arg1) {
		logger.debug("toAdmin: message = " + arg1+" "+arg0);
	}

	@Override
	public void toApp(Message arg0, SessionID arg1) throws DoNotSend {
		//logger.debug("toApp:" + arg1 + ":" + arg0);
	}

    public void onMessage(quickfix.fix50.QuoteRequest quoteRequest, SessionID sessionID) 
    		throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
    	
    }
    
    public void onMessage(quickfix.fix44.NewOrderSingle request, SessionID sessionID) throws FieldNotFound  {
    	String symbol = request.getSymbol().getValue().replace("/", "");
    	String notional = request.getCurrency().getValue();
		logger.info("Received NewOrderSingle:" + request.toString());

    	boolean isTradingDoNotReply = GlobalAppConfig.isTradeDoNotReplyForSymbol(symbol);
    	System.out.println(symbol + " " + isTradingDoNotReply); 
    	
    	if ( isTradingDoNotReply ) {
    		logger.info("isTradingDoNotReply = true, not replying");
    		return;
    	}
    	
    	boolean isTradingAllow = !GlobalAppConfig.isTradeRejectForSymbol(symbol);
    	System.out.println(symbol + " " + isTradingAllow); 
    	if ( !isTradingAllow ) {
    		logger.info("isTradingAllow = false, rejecting");
    	}
    	
    	boolean isTradingCancel = GlobalAppConfig.isTradeCancelForSymbol(symbol);
    	System.out.println(symbol + " isTradingCancel automatically? " + isTradingCancel); 

    	if ( isTradingCancel ) {
    		quickfix.fix44.ExecutionReport report = new quickfix.fix44.ExecutionReport();
    		report.set(request.getOrdType());
        	report.set(request.getClOrdID());
        	report.set(new OrigClOrdID(request.getClOrdID().getValue()));
        	report.set(new OrderID(Long.toString(System.nanoTime())));
        	
        	if ( request.isSetSide()) { 
        		report.set(request.getSide());
        	}
        	report.set(new LastQty(0));
        	report.set(request.getOrderQty());
        	report.set(request.getSymbol());
        	
        	if ( request.isSetCurrency()) {
        		report.set(request.getCurrency());
        	}
        	report.set(new ExecType(ExecType.CANCELED));  	
        	report.set(new OrdStatus(OrdStatus.CANCELED));
        	report.set(new LeavesQty(0));
        	report.set(new CumQty(0));
        	report.set(new LastPx(0));
        	report.set(new AvgPx(0));
        	report.set(request.getPrice());
        	report.setString(20, "0");
        	
        	report.set(new ExecID("0"));

        	
        	if(request.isSetSettlType()) {
        		report.set(request.getSettlType());
        	}
        	if(request.isSetSettlDate()) {
        		report.set(request.getSettlDate());
        	}
        	if(request.isSetTransactTime()) {
        		report.set(request.getTransactTime());
        	}
          	report.set(new Text("TRADE CANCELLED"));
        	Session session = Session.lookupSession(sessionID);
        	session.send(report);
    		return;
    	}
		quickfix.fix44.ExecutionReport report = new quickfix.fix44.ExecutionReport();
		report.set(request.getOrdType());
    	report.set(request.getClOrdID());
    	report.set(new OrigClOrdID(request.getClOrdID().getValue()));
    	report.set(new OrderID(Long.toString(System.nanoTime())));
    	
    	if ( request.isSetSide()) { 
    		report.set(request.getSide());
    	}
    	report.set(new LastQty(0));
    	report.set(request.getOrderQty());
    	report.set(request.getSymbol());
    	
    	if ( request.isSetCurrency()) {
    		report.set(request.getCurrency());
    	}
    	report.set(new ExecType(ExecType.NEW));  	
    	report.set(new OrdStatus(OrdStatus.NEW));
    	report.set(new LeavesQty((request.getOrderQty().getValue())));
    	report.set(new CumQty(0));
    	report.set(new LastPx(0));
    	report.set(new AvgPx(0));
    	report.set(request.getPrice());
    	report.setString(20, "0");
    	
    	
    	
    	if(request.isSetSettlType()) {
    		report.set(request.getSettlType());
    	}
    	
    	if ( GlobalAppConfig.getOverrideSettleDate( request.getSymbol().getValue().replace("/", "") ) != null ) {
    		report.set(new SettlDate(GlobalAppConfig.getOverrideSettleDate( request.getSymbol().getValue().replace("/", "") )));
    	} else if (request.isSetSettlDate()) {
    		report.set(request.getSettlDate());
    	}
    	if(request.isSetTransactTime()) {
    		report.set(request.getTransactTime());
    	}
    	
    	report.set(new ExecID("0"));
    	report.set(new Text("NEW TRADE IN-PROGRESS"));
    	Session session = Session.lookupSession(sessionID);

    	if ( isTradingAllow ) {
	    	session.send(report);
    	}
    	    	
    	boolean isTradingPartial = GlobalAppConfig.isTradePartialFillForSymbol(symbol);

    	report.set(new LastQty(request.getOrderQty().getValue()));
    	if ( !isTradingAllow ) {
        	report.set(new ExecType(ExecType.REJECTED));  	
        	report.set(new OrdStatus(OrdStatus.REJECTED));
        	report.set(new CumQty(0));
        	report.set(new Text("TRADE REJECTED"));
        	report.set(new OrderID(""));
	    	session.send(report);
	    	return;
    	} 
    	
    	if ( isTradingPartial && request.getTimeInForce().getValue() == TimeInForce.IMMEDIATE_OR_CANCEL ) {
    		int partialNo = 1;
    		double orderQ = request.getOrderQty().getValue();
    		double firstFill = Math.round(0.1d * orderQ);;
    		double leaveQ = orderQ - firstFill;
    		double fillPrice =  findFillPrice(priceProvider, symbol, notional, request.getSide(), firstFill );
    		
	    	report.set(new ExecID(UUID.randomUUID().toString()));
	    	report.setString( 8000, genTag8000());
        	report.set(new CumQty(firstFill));
        	report.set(new LastQty(firstFill));
        	report.set(new OrderQty(orderQ));
        	report.set(new LeavesQty(leaveQ));
        	report.set(new OrdStatus(OrdStatus.PARTIALLY_FILLED));
        	report.set(new AvgPx(fillPrice));
        	report.set(new LastPx(fillPrice));
        	report.set(request.getPrice());
        	report.set(new Text("PARTIAL FILL #" + partialNo++ + " - FILLED"));

	    	session.send(report);

	    	String clientTradeId = request.getClOrdID().getValue();
	    	int lastDigit = Integer.parseInt(clientTradeId.substring(clientTradeId.length() -2 )) % 10;
	    	
	    	if ( lastDigit < 5) {
	    		
	    		double fillPrice2 =  findFillPrice(priceProvider, symbol, notional, request.getSide(), leaveQ );
	    		if ( lastDigit < 2) {
	        		double secondFill = Math.round(0.2d * orderQ);;
	        		leaveQ = leaveQ - secondFill;
	    	    	report.set(new ExecID(UUID.randomUUID().toString()));
	    	    	report.setString( 8000, genTag8000());
	            	report.set(new CumQty(firstFill + secondFill));
	            	report.set(new LastQty(secondFill));
	            	report.set(new OrderQty(orderQ));
	            	report.set(new LeavesQty(leaveQ));
	            	report.set(new OrdStatus(OrdStatus.PARTIALLY_FILLED));
	            	report.set(new AvgPx(fillPrice2));
	            	report.set(new LastPx(fillPrice2));
	            	report.set(request.getPrice());
	            	report.set(new Text("PARTIAL FILL #" + partialNo++ + " - FILLED"));

	    	    	session.send(report);
	    		}
	    		
		    	report.set(new ExecID(UUID.randomUUID().toString()));
		    	report.setString( 8000, genTag8000());
	        	report.set(new CumQty(orderQ));
	        	report.set(new LastQty(leaveQ));
	        	report.set(new OrderQty(orderQ));
	        	report.set(new LeavesQty(0));
	        	report.set(new OrdStatus(OrdStatus.FILLED));
	        	report.set(new AvgPx(fillPrice2));
	        	report.set(new LastPx(fillPrice2));
	        	report.set(request.getPrice());
	        	report.set(new Text("PARTIAL FILL #" + partialNo++ + " - ALL FILLED"));

		    	session.send(report);
	    	} else {
		    	report.set(new ExecID(execId.format(Calendar.getInstance(Locale.CANADA).getTime())));
		    	report.setString( 8000, genTag8000());
	        	report.set(new CumQty(0));
	        	report.set(new LastQty(0));
	        	report.set(new OrderQty(orderQ));
	        	report.set(new LeavesQty(leaveQ));
	        	report.set(new OrdStatus(OrdStatus.CANCELED));
	        	report.set(new AvgPx(0));
	        	report.set(new LastPx(0));
	        	report.set(request.getPrice());
	        	report.set(new Text("PARTIAL FILL #2 - CANCELLED"));

		    	session.send(report);
	    	}

    	} else {
    		double orderQ = request.getOrderQty().getValue();

    		double fillPrice =  findFillPrice(priceProvider, symbol, notional, request.getSide(), orderQ );
    		
    		if ( GlobalAppConfig.isFillPriceMustBeDiffThanOrdPrice() && fillPrice == request.getPrice().getValue()) {
    			fillPrice = fillPrice - 0.00002;
    		}
	    	report.set(new ExecType(ExecType.FILL));  	
	    	report.set(new OrdStatus(OrdStatus.FILLED));
        	report.set(new CumQty(orderQ));
        	report.set(new LastQty(orderQ));
        	report.set(new OrderQty(orderQ));
        	report.set(new LeavesQty(0));
        	report.set(new AvgPx(fillPrice));
        	report.set(new LastPx(fillPrice));
        	report.set(request.getPrice());
	    	report.set(new Text("TRADE COMPLETED"));
	    	report.set(new LeavesQty(0));
	    	report.set(new ExecID(UUID.randomUUID().toString()));
	    	report.setString( 8000, genTag8000());
	    	
	    	if ( "EURUSD".equals(symbol) || "EUR/USD".equals(symbol)) {
	    		delaySend(8, sessionID, report);
	    	}
	    	session.send(report);
    	}	

    }
    
    private void delaySend(long numberOfSeconds, SessionID sessionID, ExecutionReport report) {
    	scheduledExecutorService.schedule(new SendQuickfixMessageCallable(report, sessionID), numberOfSeconds, TimeUnit.SECONDS);
		
	}


	private static double  findFillPrice(
    		IRandomMarketPriceProvider priceProvider, 
    		String symbol, 
    		String notional, 
    		Side _side,
			double qty) {
		com.tts.mlp.rate.provider.vo.Instrument instrumentPrice = priceProvider.getCurrentPrice(symbol) ;
		char et, side;
		side = _side.getValue();
		if ( (side == Side.BUY && symbol.indexOf(notional) == 0)
				|| (side == Side.SELL && symbol.indexOf(notional) > 2)) {
			et = MDEntryType.OFFER;
		} else {
			et = MDEntryType.BID;
		}
	
		double worstPrice = -1.0d;
		for (com.tts.mlp.rate.provider.vo.Tick tick : instrumentPrice.getTicksByEntryType(et)) {
			if ( tick.getPrice() > worstPrice) {
				worstPrice = tick.getPrice();
			}
			if ( tick.getQuantity() > qty) {
				return tick.getPrice();
			}
		}

		return worstPrice;
	}


	private String genTag8000() {
    	return "t8000" + System.currentTimeMillis();
    }
    
    private String findSubscriptionId(QuoteID quoteID) {
		String quoteIdValue = quoteID.getValue();
		int idx = quoteIdValue.indexOf("!");
		if ( idx < 0 ) return null;
		return quoteIdValue.substring(0, idx);
	}


	public void onMessage(quickfix.fix44.MarketDataRequest request, SessionID sessionID) 
    		throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		String clientReqId = request.getMDReqID().getValue();

		if ( SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST == request.getSubscriptionRequestType().getValue()) {
			AbstractSubscriptionHandler h = registry.findByRequestID(clientReqId, sessionID);
			if (h != null ) {
			pushWorker.removeSubscription(h);
			registry.removeSubscription(clientReqId, sessionID);
			}
			return ;
		}
		int mdEntryTypeCount = request.getNoMDEntryTypes().getValue();
		int relatedSymbolCount = request.getNoRelatedSym().getValue();

		quickfix.fix44.MarketDataRequest.NoMDEntryTypes noMDEntryTypes 
				= new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();
		
		QuoteSide quoteSide = null;
		boolean isBuy = false, isSell = false;
		for (int i = 1; i <= mdEntryTypeCount; ++i)
		{
			request.getGroup(i, noMDEntryTypes);
			if ( MDEntryType.BID == noMDEntryTypes.getMDEntryType().getValue() ) {
				isBuy = true;
			} else if ( MDEntryType.OFFER == noMDEntryTypes.getMDEntryType().getValue() ) {
				isSell = true;
			}
			
		}
		
		if ( isBuy && isSell ) {
			quoteSide = QuoteSide.BOTH;
		} else if ( isBuy ) {
			quoteSide = QuoteSide.BUY;
		} else if ( isSell ) {
			quoteSide = QuoteSide.SELL;
		}

		quickfix.fix44.MarketDataRequest.NoRelatedSym noRelatedSyms = new quickfix.fix44.MarketDataRequest.NoRelatedSym();

		for (int i = 1; i <= relatedSymbolCount; i++)
		{
			request.getGroup(i, noRelatedSyms);
			String symbol = noRelatedSyms.getSymbol().getValue();
			
			String securityType = "FXSPOT";
			String tenor =  "SPOT";

			SubscriptionRequestVo sRequest = new SubscriptionRequestVo();
			sRequest.setClientReqId(clientReqId);
			sRequest.setQuoteSide(quoteSide);
			sRequest.setSymbol(symbol.replaceFirst("/", ""));
			sRequest.setStreamType(StreamType.ESP);
			sRequest.setTenor(tenor);
			if ( request.isSetMarketDepth()) {
				sRequest.setTopOfBook(request.getMarketDepth().getValue() == 1);
			}
			AbstractSubscriptionHandler h = handlerFactory.getSubscriptionHandler(sRequest, sessionID, request, registry);
			registry.addSubscription(h, sessionID);
			pushWorker.addSubscription(h);
		}

    }
	
	public static class SendQuickfixMessageCallable implements Callable<quickfix.Message> {
		
		private final quickfix.Message message;
		private final SessionID sessionID;

		
		
		public SendQuickfixMessageCallable(quickfix.Message message, SessionID sessionID) {
			super();
			this.message = message;
			this.sessionID = sessionID;
		}



		@Override
		public quickfix.Message call() throws Exception {
			Session.lookupSession(sessionID).send(message);
			return message;
		}
    		
    	
	}

}
