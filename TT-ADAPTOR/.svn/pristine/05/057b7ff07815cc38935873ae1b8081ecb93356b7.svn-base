package com.tts.plugin.adapter.impl.cibc.dialect;

import java.util.Calendar;
import java.util.Date;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.message.constant.Constants;
import com.tts.message.trade.RestingOrderMessage.OrderParams;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.impl.base.dialect.DefaultMarketRequestDialectHelper;
import com.tts.util.AppContext;
import com.tts.vo.TenorVo;

import quickfix.Message;
import quickfix.StringField;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.ExpireTime;
import quickfix.field.HandlInst;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.NoRelatedSym;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.OrdStatusReqID;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrderQty2;
import quickfix.field.Price;
import quickfix.field.Price2;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.field.SecurityType;
import quickfix.field.SettlDate;
import quickfix.field.SettlDate2;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.StopPx;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.Text;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;

public class CibcRequestDialectHelper extends DefaultMarketRequestDialectHelper {
	
	public static final String DEFAULT_ACCOUNT_NAME = "DUMMY TOR";
	private static final String DECIMAL_00 = ".00";
	private final String acctName;
	private final OnBehalfOfCompID rfsOnBehalfOfCompID;
	private final OnBehalfOfCompID espOnBehalfOfCompID;
	private final OnBehalfOfCompID omsOnBehalfOfCompID;
	private final Account omsAccount;
	
	public final static SecurityType SecurityType_SPOT = new SecurityType("FXSPOT"); 
	public final static SecurityType SecurityType_FWD  = new SecurityType("FXFWD"); 
	public final static SecurityType SecurityType_SWAP = new SecurityType("FXSWAP"); 
	
	public final static SettlType SETTL_TYPE__ON_FOR_TOD  = new SettlType("ON");
	public final static SettlType SETTL_TYPE__SPOT        = new SettlType("SP");
	private static final SettlType SETTL_TYPE__TN_FOR_TOM = new SettlType("TN");
	
	public CibcRequestDialectHelper() {
		super();
		FixApplicationProperties p = AppContext.getContext().getBean("applicationProperties", FixApplicationProperties.class);
		String _acctName = p.getProperty("ACCT_NAME");
		if ( _acctName == null ) {
			_acctName = DEFAULT_ACCOUNT_NAME;
		}
		this.acctName = _acctName;
		
		String rfsOnBehaveOf = p.getProperty("REQUEST.RFS.ON_BEHALF_OF");
		
		if ( rfsOnBehaveOf != null ) {
			rfsOnBehalfOfCompID = new OnBehalfOfCompID(rfsOnBehaveOf);
		} else {
			rfsOnBehalfOfCompID = null;
		}
		String espOnBehaveOf = p.getProperty("REQUEST.ESP.ON_BEHALF_OF");
		if ( espOnBehaveOf != null ) {
			espOnBehalfOfCompID = new OnBehalfOfCompID(espOnBehaveOf);
		} else {
			espOnBehalfOfCompID = null;
		}
		
		String omsOnBehalfOf = p.getProperty("REQUEST.OMS.ON_BEHALF_OF", "");
		omsOnBehalfOf        = ((omsOnBehalfOf == null) || (omsOnBehalfOf.trim().isEmpty()))? p.getProperty("SESSION.DEFAULT.ON_BEHALF_OF", "")
																																			: omsOnBehalfOf;
		omsOnBehalfOfCompID  = ((omsOnBehalfOf != null) && (!omsOnBehalfOf.trim().isEmpty()))? new OnBehalfOfCompID(omsOnBehalfOf): null;
		
		String omsAcct       = p.getProperty("REQUEST.OMS.ACCOUNT", "");
		omsAcct              = ((omsAcct == null) || (omsAcct.trim().isEmpty()))? _acctName: omsAcct;
		omsAccount           = ((omsAcct != null) && (!omsAcct.trim().isEmpty()))? new Account(omsAcct): null;
	}
	
	public CibcRequestDialectHelper(String acctName, String onBehaveOf) {
		super();

		this.acctName = acctName;
		this.rfsOnBehalfOfCompID = new OnBehalfOfCompID(onBehaveOf);
		this.espOnBehalfOfCompID = new OnBehalfOfCompID(onBehaveOf);
		this.omsOnBehalfOfCompID =  new OnBehalfOfCompID(onBehaveOf);
		this.omsAccount = new Account(acctName);
	}

	@Override
	public Message buildCancelEspRequestFix50(String symbol, String tenor, String settleDate, String requestId) {
		Message m = super.buildCancelEspRequestFix50(symbol, tenor, settleDate, requestId);
		if ( espOnBehalfOfCompID != null  ) {
			m.getHeader().setField(espOnBehalfOfCompID);
		}
		return m;
	}

	@Override
	@SuppressWarnings("unused")
	public Message buildRfsRequestFix50(long size, String symbol, String notionalCurrency, String tenor, String settleDate, QuoteSide side, long expiryTime, String requestId, String product, long size2, String tenor2, String settleDate2) {
		quickfix.fix50.QuoteRequest quoteRequest = new quickfix.fix50.QuoteRequest(new QuoteReqID(requestId));
		quoteRequest.set(new NoRelatedSym(1));
		
		quickfix.fix50.QuoteRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.QuoteRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(symbol));
		
		String ccy1 = symbol.substring(0, 3);
		String ccy2 = symbol.substring(3, 6);

		noRelatedSym.set(new Currency(notionalCurrency));
		
		noRelatedSym.set(new OrderQty(size));
		
		if ( QuoteSide.BUY == side ) {
			noRelatedSym.set(new Side(Side.BUY));
		} else if ( QuoteSide.SELL == side ) {
			noRelatedSym.set(new Side(Side.SELL));
		} else  if ( QuoteSide.SWAP__BUY_AND_SELL == side ) {
			if ( symbol.indexOf(notionalCurrency) > 0 ) {
				noRelatedSym.set(new Side(Side.BUY));
			} else {
				noRelatedSym.set(new Side(Side.SELL));
			}
		} else if ( QuoteSide.SWAP__SELL_AND_BUY == side ) {
			if ( symbol.indexOf(notionalCurrency) > 0 ) {
				noRelatedSym.set(new Side(Side.SELL));
			} else {
				noRelatedSym.set(new Side(Side.BUY));
			}
		} else  {
			noRelatedSym.set(new Side('0'));
		}
		
		if ( Constants.ProductType.FXSWAP.equals(product)) {
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
			noRelatedSym.set(new OrderQty2(size2));
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
		noRelatedSym.setField(new StringField(6065, Long.toString(expiryTime)));
		quoteRequest.addGroup(noRelatedSym);
		quoteRequest.setField(new StringField(1, this.acctName));

		if ( rfsOnBehalfOfCompID != null  ) {
			quoteRequest.getHeader().setField(rfsOnBehalfOfCompID);
		}
		return quoteRequest;
	}
	
	@Override
	public Message buildEspRequestFix50(String symbol, String tenor,
			String settleDate, String requestId) {
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

		if ( TenorVo.NOTATION_SPOT.equals(tenor) ) {
			noRelatedSym.set(SecurityType_SPOT);
			noRelatedSym.set(SETTL_TYPE__SPOT);
		} else {
			noRelatedSym.set(SecurityType_FWD);
			if(tenor.equals(TenorVo.NOTATION_OVERNIGHT)) {
				noRelatedSym.set(new SettlType(SettlType.CASH));
			} else if(tenor.equals(TenorVo.NOTATION_TOMORROWNIGHT))   {
				noRelatedSym.set(new SettlType(SettlType.NEXT_DAY));
			} else if(tenor.equals(TenorVo.NOTATION_SPOTNIGHT))   {
				noRelatedSym.set(new SettlType(SettlType.FX_SPOT_NEXT_SETTLEMENT));
			} else if(tenor.equals(TenorVo.NOTATION_TODAY))   {
					noRelatedSym.set(SETTL_TYPE__ON_FOR_TOD);
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
		return marketDataRequest;
	}

	@Override
	public Message buildTradeExecRequestFix50(String product, String price, String amount,
			String currency, String symbol, String tenor, String _settleDate,
			String priceFar, String  amountFar, String  tenorFar, String  settleDateFar,
			String clientOrderId, String _quoteId, 
			OrderParams orderParams,
			QuoteSide side, String transactTime, String comment) {
		String quoteId = _quoteId.replace(IFixConstants.ESP_QUOTE_REF_ID_PREFIX, "");
		String settleType = null;
		String onBehaveOf = null;
		int delimiterPos = quoteId.indexOf(IFixConstants.DEFAULT_DELIMITER);
		String settleDate = null;
		String settleDate2 = settleDateFar;

		if ( delimiterPos > 0) {
			String[] a = quoteId.split(IFixConstants.DEFAULT_DELIMITER);
			quoteId = a[2];
			settleType = a[1];
			onBehaveOf = a[0];
		}
		
		if (_settleDate.indexOf("-") > 0  ) {
			String[] settleDates = _settleDate.split("-");
			settleDate = settleDates[0];
			settleDate2 = settleDates[1];
		} else {
			settleDate = _settleDate;
		}
		
		SecurityType securityType = null;
		
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
		if ( comment != null) {
			message.set(new Text(comment));
		}
		message.set(new QuoteID(quoteId));
		message.set(new TimeInForce(TimeInForce.FILL_OR_KILL));
		message.set(new Symbol(symbol));
		message.set(securityType);
		message.set(new Currency(currency));
		
		if ( side == QuoteSide.BUY ) {
			message.set(new Side(Side.BUY));
		} else if ( side == QuoteSide.SELL) {
			message.set(new Side(Side.SELL));
		} else if ( side == QuoteSide.SWAP__BUY_AND_SELL) {
			message.set(new Side(Side.SELL));
		} else if ( side == QuoteSide.SWAP__SELL_AND_BUY) {
			message.set(new Side(Side.BUY));
		} 
		
		if ( settleType != null && !settleType.trim().isEmpty() ) {
			message.set(new SettlType(settleType));
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
			message.set(new Price2(Double.parseDouble(priceFar)));
			if ( amountFar.indexOf(".") < 0) {
				amountFar = amountFar + DECIMAL_00;
			}
			message.set(new OrderQty2(Double.parseDouble(amountFar)));
			message.set(new OrdType(OrdType.FOREX_SWAP));
		}
		message.set(new Account(this.acctName));
		message.set(new Price(Double.parseDouble(price)));

		if ( transactTime != null ) {
			message.set(new TransactTime(new java.util.Date(Long.parseLong(transactTime))));
		} else {
			message.set(new TransactTime());
		}
		message.getHeader().setField(new OnBehalfOfCompID(onBehaveOf));
		System.out.println(message);
		return message;
	}
	
	
	@Override
	public Message buildOMSNewOrderSingleFix50(String clOrdID, double orderQty, char side, String ordType, 
			double price, char timeInForce, String currency, String symbol, long expireDate, long transactTime, String settlType, 
			String settlDate, String securityType, String possDupFlag) {
		
		quickfix.fix50.NewOrderSingle newOrderSingle =  new quickfix.fix50.NewOrderSingle();
		char orderType =  getOrderType(ordType);
		
		Calendar calTTime  = Calendar.getInstance();
		if(transactTime > 0)
			calTTime.setTimeInMillis(transactTime);
		
		if(omsOnBehalfOfCompID != null) 	{
			newOrderSingle.getHeader().setField(omsOnBehalfOfCompID);					//	115 - OnBehalfOfCompID
			clOrdID = (omsOnBehalfOfCompID.getValue() + clOrdID);		
		}
		
		if(omsAccount != null) 
			newOrderSingle.set(omsAccount);												//	1 - Account
		
		newOrderSingle.set(new ClOrdID(clOrdID));										//	11 - ClOrdID
		newOrderSingle.set(SettlType_SPOT);												//	63 - SettlType
		//newOrderSingle.set(new SettlDate(settlDate));									//	64 - SettlDate
		newOrderSingle.set(new Symbol(symbol));											//  55 - Symbol
		newOrderSingle.set(new SecurityType(securityType));								//	167 - SecurityType
		
		newOrderSingle.set(new Side(side));												//  54 - Side
		newOrderSingle.set(new OrdType(orderType));										//	40 - OrdType	
		newOrderSingle.set(new OrderQty(orderQty));										//	38 - OrderQty
		newOrderSingle.set(new Currency(currency));										//	15 - Currency
		newOrderSingle.set(new TimeInForce(timeInForce));								//	59 - TimeInForce
		newOrderSingle.set(new TransactTime(calTTime.getTime()));						//	60 - TransactTime
		
		if(orderType == OrdType.STOP)
			newOrderSingle.set(new StopPx(price));										//	99 - StopPx
		else
			newOrderSingle.set(new Price(price));										//	44 - Price
		
		if(expireDate > 0)
			newOrderSingle.set(new ExpireTime(new Date(expireDate)));					//	126 - ExpireTime
		
		return(newOrderSingle);
	}
	
	@Override
	public Message buildOMSOrderCancelFix50(String ttOrderId, char side, String symbol, String extOrderId, long transactTime) {
		
		quickfix.fix50.OrderCancelRequest orderCancel = new quickfix.fix50.OrderCancelRequest();
		
		if(omsOnBehalfOfCompID != null) 	{
			orderCancel.getHeader().setField(omsOnBehalfOfCompID);						//	115 - OnBehalfOfCompID
			ttOrderId = (omsOnBehalfOfCompID.getValue() + ttOrderId);
		}
		
		orderCancel.set(new ClOrdID(ttOrderId));						               	//	11 - ClOrdID
		//orderCancel.set(new OrigClOrdID(clOrdID));									//	41 - OrigClOrdID
		orderCancel.set(new OrderID(extOrderId));										//	37 - OrderID		
		orderCancel.set(new Symbol(symbol));											//  55 - Symbol
		orderCancel.setField(new StringField(63, "0"));									//	63 - SettlType
		//orderCancel.set(new SettlDate(settlDate));									//	64 - SettlDate		
		orderCancel.set(new TransactTime(Calendar.getInstance().getTime()));			//	60 - TransactTime
		
		return(orderCancel);
	}
	
	@Override
	public Message buildOMSOrderStatusFix50(String orderID, String clOrdID) {
		
		quickfix.fix50.OrderStatusRequest orderStatus = new quickfix.fix50.OrderStatusRequest();
		
		if(omsOnBehalfOfCompID != null) 	{
			orderStatus.getHeader().setField(omsOnBehalfOfCompID);
			orderID = (omsOnBehalfOfCompID.getValue() + orderID);		
		}
		
		if((clOrdID != null) && (!clOrdID.trim().isEmpty()))
			orderStatus.set(new OrderID(clOrdID));										//	37 - OrderID
		else
			orderStatus.set(new ClOrdID(orderID));										//	11 - ClOrdID
		
		orderStatus.set(new OrdStatusReqID(getUniqueOrdStatusReqID(orderID)));		    //	790 - OrdStatusReqID
		
		return(orderStatus);
	}
}
	