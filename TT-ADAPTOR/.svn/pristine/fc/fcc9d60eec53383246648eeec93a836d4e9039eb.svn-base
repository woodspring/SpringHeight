package com.tts.mlp.app;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.app.price.push.PricePushWorker;
import com.tts.mlp.app.price.subscription.AbstractSubscriptionHandler;
import com.tts.mlp.app.price.subscription.PriceSubscriptionHandlerFactory;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.StreamType;
import com.tts.util.AppConfig;
import com.tts.util.Constants;
import com.tts.vo.TenorVo;

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
import quickfix.StringField;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.HandlInst;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.MDEntryType;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.QuoteCancelType;
import quickfix.field.QuoteID;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Text;
import quickfix.fix50.QuoteRequest;

public class QfixApplication extends MessageCracker implements Application {
	private final static Logger logger = LoggerFactory.getLogger(QfixApplication.class);
	
	private final PriceSubscriptionHandlerFactory handlerFactory
									= new PriceSubscriptionHandlerFactory();
	
	private final PriceSubscriptionRegistry registry;
	
	private final PricePushWorker pushWorker = new PricePushWorker();
		
	private final static long TIME_TO_LIVE;
	
	static {
		int injectInterval = AppConfig.getIntegerValue(
				Constants.APP_SECTION, 
				Constants.MARKET_INJECT_INTERVAL,
				PricePushWorker.DEFAULT_INJECT_INTERVAL);
		TIME_TO_LIVE = injectInterval * 2;
		
	}
	
	public QfixApplication(PriceSubscriptionRegistry registry) {
		super();
		pushWorker.start();
		this.registry = registry;
	}
	

	@Override
	public void fromAdmin(Message arg0, SessionID arg1) throws FieldNotFound,
			IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.debug("fromAdmin:" + arg1 + ":" + arg0);
	}

	@Override
	public void fromApp(Message arg0, SessionID arg1) throws FieldNotFound,
			IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
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
    	String clientReqId = quoteRequest.getQuoteReqID().getValue();
    	int numOfRelatedSymbols = quoteRequest.getNoRelatedSym().getValue();

		for (int i = 1; i <= numOfRelatedSymbols; i++)
		{
			quickfix.fix50.QuoteRequest.NoRelatedSym noRelatedSyms = new quickfix.fix50.QuoteRequest.NoRelatedSym();
			quoteRequest.getGroup(i, noRelatedSyms);
			String symbol = noRelatedSyms.getSymbol().getValue();
			String notionalCurrency = null;
			String settleDate = null, settleDate2 = null;
			double qty = noRelatedSyms.getOrderQty().getValue();
			
			if ( noRelatedSyms.isSetSettlDate() ) {
				settleDate = noRelatedSyms.getSettlDate().getValue();
			}
			if ( noRelatedSyms.isSetSettlDate2() ) {
				settleDate2 = noRelatedSyms.getSettlDate2().getValue();
			}
			if (  noRelatedSyms.isSetCurrency() ) {
				notionalCurrency = noRelatedSyms.getCurrency().getValue();
			}
			String tenor = null;
			
			if ( noRelatedSyms.isSetSettlType() ) {
				tenor = noRelatedSyms.getSettlType().getValue();
			}
			QuoteSide side = QuoteSide.BOTH;
			if ( noRelatedSyms.getSide() != null ) {
				if ( Side.BUY == noRelatedSyms.getSide().getObject().charValue() ) {
					side = QuoteSide.BUY;
				} else if ( Side.SELL == noRelatedSyms.getSide().getObject().charValue() ) {
					side = QuoteSide.SELL;
				}
			}
			
			StringField expiry = new StringField(6065);
			long expiryTime =  Long.parseLong(noRelatedSyms.getField(expiry).getValue());
			
			SubscriptionRequestVo sRequest = new SubscriptionRequestVo();
			sRequest.setClientReqId(clientReqId);
			sRequest.setQuoteSide(side);
			sRequest.setNotionalCurrency(notionalCurrency);
			sRequest.setSize((long) qty);
			sRequest.setSymbol(symbol);
			sRequest.setStreamType(StreamType.RFS);
			sRequest.setTenor(tenor);
			sRequest.setExpiryTime(expiryTime);
			sRequest.setSettleDate(settleDate);
			sRequest.setSettleDateFar(settleDate2);
			sRequest.setSecurityType(noRelatedSyms.getSecurityType().getValue());
			
			if ( noRelatedSyms.isSetOrderQty2()) {
				double qty2 = noRelatedSyms.getOrderQty2().getValue();
				sRequest.setSizeFar((long) qty2); 
			}
			
			if (			quoteRequest.getHeader().isSetField(115) ) {
				sRequest.setOnBehaveOf(quoteRequest.getHeader().getString(115));
			}
			
			
			AbstractSubscriptionHandler h = handlerFactory.getSubscriptionHandler(sRequest, sessionID, quoteRequest,registry);
			registry.addSubscription(h, sessionID);
			pushWorker.addSubscription(h);
			
		}	
    }
    public void onMessage(quickfix.fix50.NewOrderSingle request, SessionID sessionID) throws FieldNotFound  {
		logger.debug("Received NewOrderSingle:" + request.toString());
		long receivedTime = System.currentTimeMillis();
		String quoteId = request.getQuoteID().getValue();
		
		long sentTime = -1;
		if ( quoteId.indexOf("!") > 0 ) {
			sentTime = Long.parseLong( quoteId.substring(quoteId.indexOf("!")  +1));
		} else {
			sentTime = Long.parseLong(quoteId);
		}
		boolean tooOld = GlobalAppConfig.isOrderTimeCheckEnabled() ? (receivedTime - sentTime ) > TIME_TO_LIVE : false;
		boolean foundQuoteSubscription = false;
		AbstractSubscriptionHandler h = null;
    	String clientSubscriptionId = findSubscriptionId(request.getQuoteID());

	    if ( clientSubscriptionId != null ) {
			h = registry.findBySubscriptionId(clientSubscriptionId);
			pushWorker.removeSubscription(h);
			foundQuoteSubscription = true;
    	}
		logger.debug("NewOrderSingle.clientSubscriptionId:" + clientSubscriptionId + ". SubscriptionHandler is null? " + (h == null));

    	quickfix.fix50.ExecutionReport report = new quickfix.fix50.ExecutionReport();
    	report.set(request.getSecurityType());
    	report.set(new OrdType(OrdType.PREVIOUSLY_QUOTED));
    	report.set(request.getClOrdID());
    	report.set(new OrderID(Long.toString(System.nanoTime())));
    	

    	report.set(new OrdStatus(OrdStatus.NEW));	
    	
    	
    	String symbol = request.getSymbol().getValue();
    	report.set(request.getSymbol());
    	
    	if ( request.isSetSettlType() && !request.getSettlType().getValue().trim().isEmpty() ) {
    		report.set(request.getSettlType());
    	}
    	if ( request.isSetSettlDate() ) {
    		report.set(request.getSettlDate());
    	}
    	if ( request.isSetCurrency()) {
    		report.set(request.getCurrency());
    	}
    	if ( request.isSetSide()) { 
    		report.set(request.getSide());
    	}
    	if ( request.isSetTransactTime() ) {
    		report.set(request.getTransactTime());
    	}
    	report.set(request.getPrice());
    	report.set(request.getOrderQty());
    	if (request.isSetOrdType() && OrdType.FOREX_SWAP == request.getOrdType().getValue()) {
        	report.setString(192, request.getOrderQty2().getValue() + "");
        	report.setString(193, request.getSettlDate2().getValue() + "");
        	report.setString(640, "0");
        	report.setString(6058, "0");
        	report.setString(6059, "0");
        	report.setDouble(9093, request.getOrderQty2().getValue());
        	report.setString(9094, "0");
    	}
    	if ( tooOld || "USDDDK".equals(symbol)) {
    		report.set(new ExecID(Long.toString(System.nanoTime())));
        	report.set(new ExecType(ExecType.REJECTED));
        	report.set(new OrdStatus(OrdStatus.REJECTED));
        	report.set(request.getAccount());
        	report.set(request.getTimeInForce());
        	report.set(new LastPx(0.00000000));
        	report.set(new LastQty(0));
        	report.set(new LeavesQty(0));
        	report.set(new CumQty(0));
        	report.set(new AvgPx(0.00000000));

        	if ( tooOld ) {
        		report.set(new Text("slateRate"));	
        	} else {
        		report.set(new Text("MOCK REJECTION"));
        	}
        	
        	report.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC));
        	Session session = Session.lookupSession(sessionID);
        	session.send(report);
        	return;
    	}
    	

    	report.set(new LastPx(request.getPrice().getValue()));
    	report.set(new LastQty(request.getOrderQty().getValue()));
    	report.set(new LeavesQty(0));
    	report.set(new CumQty(request.getOrderQty().getValue()));

    	report.set(new Text("TRADE IN-PROGRESS"));
    	Session session = Session.lookupSession(sessionID);
    	session.send(report);
    	
    	if (request.isSetOrdType() && OrdType.FOREX_SWAP == request.getOrdType().getValue()) {
        	report.set(new AvgPx(request.getPrice2().getValue()));

        	report.setString(640, request.getPrice2().getValue() + "");
        	report.setString(6058, request.getOrderQty().getValue() + "");
        	report.setString(6059, "0");
        	report.setDouble(9093, request.getPrice2().getValue());
        	report.setString(9094, request.getOrderQty().getValue() + "");
    	} else {
        	report.set(new AvgPx(request.getPrice().getValue()));
    	}
    	report.set(new Text("TRADE COMPLETED"));
    	report.set(new ExecType(ExecType.TRADE));
    	report.set(new OrdStatus(OrdStatus.FILLED));
    	session.send(report);
    	
    	if ( foundQuoteSubscription ) {
	    	quickfix.fix50.QuoteRequest quoteRequest = (QuoteRequest) h.getOriginalMessage();
	    	quickfix.fix50.QuoteCancel quoteCancel = new quickfix.fix50.QuoteCancel();
	    	quoteCancel.set(quoteRequest.getQuoteReqID());
	    	quoteCancel.set(new QuoteID("*"));
	    	quoteCancel.set(new QuoteCancelType(QuoteCancelType.CANCEL_QUOTE_SPECIFIED_IN_QUOTEID ));
	    	session.send(quoteCancel);
	    	
			registry.removeSubscription(quoteRequest.getQuoteReqID().getValue(), sessionID);
    	}

    }
    
    private String findSubscriptionId(QuoteID quoteID) {
		String quoteIdValue = quoteID.getValue();
		int idx = quoteIdValue.indexOf("!");
		if ( idx < 0 ) return null;
		return quoteIdValue.substring(0, idx);
	}


	public void onMessage(quickfix.fix50.MarketDataRequest request, SessionID sessionID) 
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

		quickfix.fix50.MarketDataRequest.NoMDEntryTypes noMDEntryTypes 
				= new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		
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

		quickfix.fix50.MarketDataRequest.NoRelatedSym noRelatedSyms = new quickfix.fix50.MarketDataRequest.NoRelatedSym();

		for (int i = 1; i <= relatedSymbolCount; i++)
		{
			request.getGroup(i, noRelatedSyms);
			String symbol = noRelatedSyms.getSymbol().getValue();
			String securityType = noRelatedSyms.getSecurityType().getValue();
			String tenor = null;
			if ( securityType.equals("FXFWD") ) {
				if(noRelatedSyms.getSettlType() != null) {
					String settlType = noRelatedSyms.getSettlType().getValue().toUpperCase();
					if(settlType.equals(SettlType.CASH)) {
						tenor = TenorVo.NOTATION_OVERNIGHT;
					} else if(settlType.equals(SettlType.NEXT_DAY)) {
						tenor = TenorVo.NOTATION_TOMORROWNIGHT;
					} else if(settlType.equals(SettlType.FX_SPOT_NEXT_SETTLEMENT)) {
						tenor = TenorVo.NOTATION_SPOTNIGHT;
					} else if (settlType.equals("TOD") ) {
						tenor = TenorVo.NOTATION_TODAY;
					} else {
						String firstChar = settlType.substring(0, 1);
						tenor = settlType.substring(1)+firstChar;
					}
					
				} else if(noRelatedSyms.getSettlDate() != null) {
					tenor = noRelatedSyms.getSettlDate().getValue();
				} else {
					tenor = "???";
				}
				
			} else {
				tenor = "SPOT";
			}

			SubscriptionRequestVo sRequest = new SubscriptionRequestVo();
			sRequest.setClientReqId(clientReqId);
			sRequest.setQuoteSide(quoteSide);
			sRequest.setSymbol(symbol);
			sRequest.setStreamType(StreamType.ESP);
			sRequest.setTenor(tenor);
			if ( noRelatedSyms.isSetSettlDate() ) {
				sRequest.setSettleDate(noRelatedSyms.getSettlDate().getValue());
				if ( sRequest.getSettleDate() != null && sRequest.getTenor() != null ) {
					ForwardCurveDataManager.registerSymbolTenorDate(symbol, tenor, noRelatedSyms.getSettlDate().getValue());
				}
			}
			
			if (			request.getHeader().isSetField(115) ) {
				sRequest.setOnBehaveOf(request.getHeader().getString(115));
			}
			
			AbstractSubscriptionHandler h = handlerFactory.getSubscriptionHandler(sRequest, sessionID, request, registry);
			registry.addSubscription(h, sessionID);
			pushWorker.addSubscription(h);
		}

    }

}
