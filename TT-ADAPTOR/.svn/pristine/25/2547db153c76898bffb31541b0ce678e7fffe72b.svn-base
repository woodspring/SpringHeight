package com.tts.plugin.adapter.impl.base.dialect;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.trade.RestingOrderMessage.OrderParams;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.dialect.IMkRequestDialectHelper;
import com.tts.util.AppUtils;
import com.tts.util.constant.RestingOrderConstants.OrderType;
import com.tts.vo.TenorVo;

import quickfix.Message;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.ExpireTime;
import quickfix.field.HandlInst;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdStatusReqID;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.OrigSendingTime;
import quickfix.field.Price;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.field.SecondaryClOrdID;
import quickfix.field.SecurityType;
import quickfix.field.SettlDate;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.OrderCancelRequest;


public class DefaultMarketRequestDialectHelper implements IMkRequestDialectHelper {
	public final static Logger logger = LoggerFactory.getLogger(DefaultMarketRequestDialectHelper.class);
	
	public final static quickfix.fix50.MarketDataRequest.NoMDEntryTypes FIX50_MarketDataRequest_SIDE_BID;
	public final static quickfix.fix50.MarketDataRequest.NoMDEntryTypes FIX50_MarketDataRequest_SIDE_OFFER;
	
	public final static quickfix.fix44.MarketDataRequest.NoMDEntryTypes FIX44_MarketDataRequest_SIDE_BID;
	public final static quickfix.fix44.MarketDataRequest.NoMDEntryTypes FIX44_MarketDataRequest_SIDE_OFFER;
	
	public final static SecurityType SecurityType_SPOT   = new SecurityType("FXSPOT"); 
	public final static SecurityType SecurityType_FWD    = new SecurityType("FXFWD"); 
	public final static SecurityType SecurityType_SWAP   = new SecurityType("FXSWAP"); 
	public final static SettlType SettlType_SPOT         = new SettlType(SettlType.REGULAR);
	public final static SettlType SETTL_TYPE__ON_FOR_TOD = new SettlType("ON");
		
	static {

		quickfix.fix50.MarketDataRequest.NoMDEntryTypes noMDEntryType = null;
		noMDEntryType = new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType.set(new MDEntryType( MDEntryType.BID));
		FIX50_MarketDataRequest_SIDE_BID = noMDEntryType;
		noMDEntryType = new quickfix.fix50.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType.set(new MDEntryType( MDEntryType.OFFER));
		FIX50_MarketDataRequest_SIDE_OFFER = noMDEntryType;


		quickfix.fix44.MarketDataRequest.NoMDEntryTypes noMDEntryType44 = null;
		noMDEntryType44 = new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType( MDEntryType.BID));
		FIX44_MarketDataRequest_SIDE_BID = noMDEntryType44;
		noMDEntryType44 = new quickfix.fix44.MarketDataRequest.NoMDEntryTypes();
		noMDEntryType44.set(new MDEntryType( MDEntryType.OFFER));
		FIX44_MarketDataRequest_SIDE_OFFER = noMDEntryType44;

	}
	

	@Override
	public Message buildRfsRequestFix50(long size, String symbol, String notionalCurrency, String tenor, String settleDate, QuoteSide side, long expiryTime, String requestId, String product, long size2, String tenor2, String settleDate2) {
		quickfix.fix50.QuoteRequest quoteRequest = new quickfix.fix50.QuoteRequest(new QuoteReqID(requestId));
		quoteRequest.set(new NoRelatedSym(1));
		
		quickfix.fix50.QuoteRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.QuoteRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(symbol));
		noRelatedSym.set(new Currency(notionalCurrency));
		noRelatedSym.set(new OrderQty(size));
		
		if  ( side == QuoteSide.BOTH) {
			noRelatedSym.set(new Side('0'));
		} else if ( side == QuoteSide.BUY ) {
			noRelatedSym.set(new Side(Side.BUY));
		} else if ( side == QuoteSide.SELL) {
			noRelatedSym.set(new Side(Side.SELL));
		}
		if ( !TenorVo.NOTATION_SPOT.equals(tenor) ) {
			noRelatedSym.set(new SettlDate(settleDate));
		}
		quoteRequest.addGroup(noRelatedSym);
		return quoteRequest;
	}
	
	
	@Override
	public Message buildEspRequestFix50(String symbol, String tenor,
			String settleDate, String requestId) {
		quickfix.fix50.MarketDataRequest marketDataRequest = new quickfix.fix50.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES),
				new MarketDepth(0));
		
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
			noRelatedSym.set(SettlType_SPOT);
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
			String currency, String symbol, String tenor, String settleDate,
			String priceFar, String  amountFar, String  tenorFar, String  settleDateFar,
			String clientOrderId, String _quoteId, 
			OrderParams orderParams,
			QuoteSide side, String transactTime, String comment) {
		String quoteId = _quoteId;
		if ( quoteId.startsWith(IFixConstants.ESP_QUOTE_REF_ID_PREFIX)) {
			quoteId = quoteId.substring(IFixConstants.ESP_QUOTE_REF_ID_PREFIX.length());
		}
		
		quickfix.fix50.NewOrderSingle message = new quickfix.fix50.NewOrderSingle();
		message.set(new OrdType(OrdType.PREVIOUSLY_QUOTED));
		message.set(new ClOrdID(clientOrderId));
		message.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC));
		message.set(new QuoteID(quoteId));
		message.set(new TimeInForce(TimeInForce.FILL_OR_KILL));
		message.set(new Symbol(symbol));
		message.set(new Currency(currency));
		if ( side == QuoteSide.BUY ) {
			message.set(new Side(Side.BUY));
		} else if ( side == QuoteSide.SELL) {
			message.set(new Side(Side.SELL));
		}
		message.set(new OrderQty(Double.parseDouble(amount)));
		message.set(new SettlDate(settleDate));
		message.set(new Account("SETTLEACCT"));
		message.set(new Price(Double.parseDouble(price)));
		
		return message;
	}


	@Override
	public Message buildCancelEspRequestFix50(String symbol, String tenor,
			String settleDate, String requestId) {
		quickfix.fix50.MarketDataRequest marketDataRequest = new quickfix.fix50.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST),
				new MarketDepth(1));
		marketDataRequest.set(new NoRelatedSym(1));

		marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_BID);
		marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_OFFER);

		quickfix.fix50.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.MarketDataRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(symbol));
		if ( !TenorVo.NOTATION_SPOT.equals(tenor) && settleDate != null ) {
			noRelatedSym.set(new SettlDate(settleDate));
		}
		marketDataRequest.addGroup(noRelatedSym);
		return marketDataRequest;
	}

	@Override
	public Message buildCancelEspRequestFix44(String symbol, String tenor,
			String settleDate, String requestId) {
		quickfix.fix44.MarketDataRequest marketDataRequest = new quickfix.fix44.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST),
				new MarketDepth(1));
		marketDataRequest.set(new NoRelatedSym(1));

		marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_BID);
		marketDataRequest.addGroup(FIX50_MarketDataRequest_SIDE_OFFER);

		quickfix.fix50.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix50.MarketDataRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(symbol));

		marketDataRequest.addGroup(noRelatedSym);
		return marketDataRequest;	
	}


	@Override
	public Message buildEspRequestFix44(String symbol, String tenor,
			String settleDate, String requestId) {
		quickfix.fix44.MarketDataRequest marketDataRequest = new quickfix.fix44.MarketDataRequest(
				new MDReqID(requestId),
				new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES),
				new MarketDepth(0));
		
		marketDataRequest.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
		marketDataRequest.set(new NoRelatedSym(1));

		marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_BID);
		marketDataRequest.addGroup(FIX44_MarketDataRequest_SIDE_OFFER);

		quickfix.fix44.MarketDataRequest.NoRelatedSym noRelatedSym = new quickfix.fix44.MarketDataRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(symbol));
		marketDataRequest.addGroup(noRelatedSym);
		return marketDataRequest;
	}


	@Override
	public Message buildRfsRequestFix44(long size, String symbol, String notionalCurrency, String tenor, String settleDate, QuoteSide side, long expiryTime, String requestId, String product, long size2, String tenor2, String settleDate2) {
		return null;
	}


	@Override
	public Message buildTradeExecRequestFix44(String product, String price, String amount,
			String currency, String symbol, String tenor, String settleDate,
			String priceFar, String  amountFar, String  tenorFar, String  settleDateFar,
			String clientOrderId, String _quoteId, 
			OrderParams orderParams,
			QuoteSide side, String transactTime, String comment) {
		return null;
	}


	@Override
	public String buildRequestId(long seq, String symbol, String tenor) {
		if ( tenor == null || tenor.isEmpty() ) {
			return String.format("%s.%s", symbol ,  seq);
		}
		return String.format("%s.%s.%s", symbol , tenor , seq);
	}


	@Override
	public Message buildOMSNewOrderSingleFix44(String clOrdID, double orderQty, char side, String ordType, 
			double price, char timeInForce, String currency, String symbol, long expireDate, long transactTime, String settlType, 
			String settlDate, String securityType, String possDupFlag) {
		
		quickfix.fix44.NewOrderSingle newOrderSingle =  new quickfix.fix44.NewOrderSingle();
		Calendar calTTime = Calendar.getInstance();
		if(transactTime > 0)
			calTTime.setTimeInMillis(transactTime);
		
		newOrderSingle.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		newOrderSingle.set(new OrderQty(orderQty));											//	38 - OrderQty
		newOrderSingle.set(new Side(side));													//  54 - Side
		newOrderSingle.set(new OrdType(getOrderType(ordType)));								//	40 - OrdType
		
		newOrderSingle.set(new Price(price));												//	44 - Price
		newOrderSingle.set(new TimeInForce(timeInForce));									//	59 - TimeInForce
		newOrderSingle.set(new Currency(currency));											//	15 - Currency
		newOrderSingle.set(new Symbol(symbol));												//  55 - Symbol
		newOrderSingle.set(new TransactTime(calTTime.getTime()));							//	60 - TransactTime
		if(expireDate > 0)
			newOrderSingle.set(new ExpireTime(new Date(expireDate))); 						//	432 - ExpireDate
		
		if((possDupFlag != null) && (possDupFlag.equals("Y")))	{
			newOrderSingle.setBoolean(43, true);											//	43 - PossDupFlag
			newOrderSingle.setField(new OrigSendingTime(Calendar.getInstance().getTime()));	//	122 - OrigSendingTime
		}
		
		return(newOrderSingle);
	}

	@Override
	public Message buildOMSNewOrderSingleFix50(String clOrdID, double orderQty, char side, String ordType, 
			double price, char timeInForce, String currency, String symbol, long expireDate, long transactTime, String settlType, 
			String settlDate, String securityType, String possDupFlag) {
		
		quickfix.fix50.NewOrderSingle newOrderSingle =  new quickfix.fix50.NewOrderSingle();
		Calendar calTTime = Calendar.getInstance();
		if(transactTime > 0)
			calTTime.setTimeInMillis(transactTime);
		
		newOrderSingle.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		newOrderSingle.set(new OrderQty(orderQty));											//	38 - OrderQty
		newOrderSingle.set(new Side(side));													//  54 - Side
		newOrderSingle.set(new OrdType(getOrderType(ordType)));								//	40 - OrdType
		
		newOrderSingle.set(new Price(price));												//	44 - Price
		newOrderSingle.set(new TimeInForce(timeInForce));									//	59 - TimeInForce
		newOrderSingle.set(new Currency(currency));											//	15 - Currency
		newOrderSingle.set(new Symbol(symbol));												//  55 - Symbol
		newOrderSingle.set(new TransactTime(calTTime.getTime()));							//	60 - TransactTime
		if(expireDate > 0)
			newOrderSingle.set(new ExpireTime(new Date(expireDate)));						//	432 - ExpireDate
		
		if((possDupFlag != null) && (possDupFlag.equals("Y")))	{
			newOrderSingle.setBoolean(43, true);											//	43 - PossDupFlag
			newOrderSingle.setField(new OrigSendingTime(Calendar.getInstance().getTime()));	//	122 - OrigSendingTime
		}
		
		return(newOrderSingle);
	}

	@Override
	public Message buildOMSOrderCancelFix44(String clOrdID, char side, String symbol, String origClOrdID, long transactTime) {
		
		quickfix.fix44.OrderCancelRequest orderCancel = new OrderCancelRequest();
		
		orderCancel.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		orderCancel.set(new Side(side));												//  54 - Side
		orderCancel.set(new Symbol(symbol));											//  55 - Symbol
		orderCancel.set(new OrigClOrdID(origClOrdID));									//	41 - OrigClOrdID	
		orderCancel.set(new TransactTime(Calendar.getInstance().getTime()));			//	60 - TransactTime
		
		return(orderCancel);
	}

	@Override
	public Message buildOMSOrderCancelFix50(String clOrdID, char side, String symbol, String origClOrdID, long transactTime) {
		
		quickfix.fix50.OrderCancelRequest orderCancel = new quickfix.fix50.OrderCancelRequest();
		
		orderCancel.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		orderCancel.set(new Side(side));												//  54 - Side
		orderCancel.set(new Symbol(symbol));											//  55 - Symbol
		orderCancel.set(new OrigClOrdID(origClOrdID));									//	41 - OrigClOrdID	
		orderCancel.set(new TransactTime(Calendar.getInstance().getTime()));			//	60 - TransactTime
		
		return(orderCancel);
	}
	
	@Override
	public Message buildOMSOrderStatusFix44(String orderID, String clOrdID) {
		
		quickfix.fix44.OrderStatusRequest orderStatus = new quickfix.fix44.OrderStatusRequest();
		
		orderStatus.set(new OrderID(orderID));											//	37 - OrderID
		orderStatus.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		orderStatus.set(new OrdStatusReqID(getUniqueOrdStatusReqID(orderID)));		    //	790 - OrdStatusReqID
		
		return(orderStatus);
	}


	@Override
	public Message buildOMSOrderStatusFix50(String orderID, String clOrdID) {
		
		quickfix.fix50.OrderStatusRequest orderStatus = new quickfix.fix50.OrderStatusRequest();
		
		orderStatus.set(new OrderID(orderID));											//	37 - OrderID
		orderStatus.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		orderStatus.set(new OrdStatusReqID(getUniqueOrdStatusReqID(orderID)));		    //	790 - OrdStatusReqID
		
		return(orderStatus);
	}


	
	@Override
	public Message buildOrderHeatBandMessageFix44(String clOrdID, String origClOrdID, int temperature) {
		
		quickfix.fix44.OrderStatusRequest orderStatus = new quickfix.fix44.OrderStatusRequest();
		
		orderStatus.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		orderStatus.set(new SecondaryClOrdID(origClOrdID));								//	526 - SecondaryClOrdID
		orderStatus.set(new OrdStatusReqID(String.valueOf(temperature)));				//  790 - OrdStatusReqID 
		
		return(orderStatus);
	}

	@Override
	public Message buildOrderHeatBandMessageFix50(String clOrdID, String origClOrdID, int temperature) {
		
		quickfix.fix50.OrderStatusRequest orderStatus = new quickfix.fix50.OrderStatusRequest();
		
		orderStatus.set(new ClOrdID(clOrdID));											//	11 - ClOrdID
		orderStatus.set(new SecondaryClOrdID(origClOrdID));								//	526 - SecondaryClOrdID
		orderStatus.set(new OrdStatusReqID(String.valueOf(temperature)));				//  790 - OrdStatusReqID 
		
		return(orderStatus);
	}
	
	public char getOrderType(String orderType)	{
		char orderTypeValue = OrdType.MARKET;
		
		if(OrderType.LIMIT.equals(orderType))
			orderTypeValue = OrdType.LIMIT;
		
		if(OrderType.STOP.equals(orderType))
			orderTypeValue = OrdType.STOP;
		
		return(orderTypeValue);
	}
	
	public String getUniqueClOrdID(String clOrdID)	{
		long uuid =  AppUtils.createUniqueId();
		if(uuid < 0)	
			uuid *= -1;
		
		String uClOrdID = ((clOrdID == null) || (clOrdID.trim().isEmpty()))? "": (clOrdID + "-");
		return(uClOrdID + String.valueOf(uuid));
	}
	
	public String getUniqueOrdStatusReqID(String orderID)	{
		return("STATREQ-" + orderID);
	}
}