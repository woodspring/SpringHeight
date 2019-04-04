package com.tts.plugin.adapter.impl.ykb.dialect;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.message.trade.RestingOrderMessage.OrderParams;
import com.tts.plugin.adapter.impl.base.dialect.DefaultMarketRequestDialectHelper;
import com.tts.util.AppContext;

import quickfix.field.Account;
import quickfix.field.AggregatedBook;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.SecurityType;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;

public class YkbRequestDialectHelper extends DefaultMarketRequestDialectHelper {
	private static final char DEFAULT_TIME_IN_FORCE = TimeInForce.FILL_OR_KILL;

	private static final char DEFAULT_ORD_TYPE = OrdType.FOREX_LIMIT;

	private static final String DECIMAL_00 = ".00";
	
	public final static SecurityType SecurityType_SPOT = new SecurityType("FXSPOT"); 
	public final static SecurityType SecurityType_FWD = new SecurityType("FXFWD"); 
	public final static SecurityType SecurityType_SWAP = new SecurityType("FXSWAP"); 
	
	public final static SettlType SETTL_TYPE__ON_FOR_TOD = new SettlType("ON");
	public final static SettlType SettlType_SPOT = new SettlType("SP");
	
	private final String acctName;
	
	private final int tag264;
	private final String tag266;

	public YkbRequestDialectHelper() {
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		
		this.tag264 = (int) p.getProperty("spotadapter.tradair.tag264", 1L);
		this.tag266 = p.getProperty("spotadapter.tradair.tag266", "Y");
		this.acctName = p.getProperty("ACCT_NAME");
	}
	

	@Override
	public quickfix.Message buildEspRequestFix44(String symbol, String tenor, String settleDate, String requestId) {
		quickfix.fix44.MarketDataRequest marketDataRequest = new quickfix.fix44.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES),
				new MarketDepth(this.tag264));
		String sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
		marketDataRequest.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
		marketDataRequest.set(new AggregatedBook("Y".equals(this.tag266)));
		marketDataRequest.set(new NoRelatedSym(1));

		marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_BID);
		marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_OFFER);

		quickfix.fix44.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix44.MarketDataRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(sym));
		marketDataRequest.addGroup(noRelatedSym);
		return marketDataRequest;
	}

	@Override
	public quickfix.Message buildCancelEspRequestFix44(String symbol, String tenor, String settleDate, String requestId) {
		quickfix.fix44.MarketDataRequest marketDataRequest = new quickfix.fix44.MarketDataRequest(
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
		return marketDataRequest;	
	}

	@Override
	public String buildRequestId(long seq, String symbol, String tenor) {
		return Long.toString(seq);
	}


	@Override
	public quickfix.Message buildTradeExecRequestFix50(String product, String price,
			String amount, String currency, String symbol, String tenor,
			String settleDate, String priceFar, String amountFar, String tenorFar, String settleDateFar, 
			String clientOrderId, String quoteId,
			OrderParams orderParams,
			QuoteSide side, String transactTime, String comment) {

		return null;
	}

	@Override
	public quickfix.Message buildTradeExecRequestFix44(String product, String price,
			String amount, String currency, String symbol, String tenor,
			String settleDate, String priceFar, String amountFar, String tenorFar, String settleDateFar, 
			String clientOrderId, String quoteId,
			OrderParams orderParams,
			QuoteSide side, String transactTime, String comment) {
		String sym = symbol;
		char timeInForce = DEFAULT_TIME_IN_FORCE;
		char ordType = DEFAULT_ORD_TYPE;
		
		if ( orderParams != null ) {
			if ( orderParams.hasOrdType() ) {
				if ( orderParams.getOrdType() == OrderParams.OrdType.Market) {
					ordType = OrdType.FOREX_MARKET;
				} else if ( orderParams.getOrdType() == OrderParams.OrdType.Limit) {
					ordType = OrdType.FOREX_LIMIT;
				}
			}
			if ( orderParams.hasTimeInForce()) {
				if ( orderParams.getTimeInForce() == OrderParams.TimeInForce.Immediate_or_Cancel) {
					timeInForce = TimeInForce.IMMEDIATE_OR_CANCEL;
				} else {
					
				}
			}
		}
		
		if ( symbol.indexOf('/') < 0 ) {
			sym = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
		}
		String amt = amount;
		if ( amt.indexOf(".") < 0) {
			amt = amt + DECIMAL_00;
		}
		quickfix.fix44.NewOrderSingle message = new quickfix.fix44.NewOrderSingle();
		if ( this.acctName != null && !this.acctName.isEmpty() ) {
			message.set(new Account(this.acctName));
		}
		message.set(new ClOrdID(clientOrderId));
		message.set(new TransactTime(new java.util.Date(Long.parseLong(transactTime))));
		message.set(new Currency(currency));
		if ( side == QuoteSide.BUY ) {
			message.set(new Side(Side.BUY));
		} else if ( side == QuoteSide.SELL) {
			message.set(new Side(Side.SELL));
		}
		message.set(new Symbol(sym));
		message.set(new OrderQty(Double.parseDouble(amt)));
		message.set(new OrdType(ordType));
		
		if((price != null) && (price.trim().length() > 0))
			message.set(new Price(Double.parseDouble(price)));
		
		message.set(new TimeInForce(timeInForce));

		System.out.println(message);
		return message;
	}
	
	

}
