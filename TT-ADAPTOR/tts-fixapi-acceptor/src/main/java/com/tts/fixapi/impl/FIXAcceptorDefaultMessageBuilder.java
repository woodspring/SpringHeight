package com.tts.fixapi.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fixapi.type.IFIXAcceptorMessageBuilder;

import quickfix.Message;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.ClOrdLinkID;
import quickfix.field.CumQty;
import quickfix.field.Currency;
import quickfix.field.CxlRejReason;
import quickfix.field.CxlRejResponseTo;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.SecondaryOrderID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Text;
import quickfix.field.TransactTime;


public class FIXAcceptorDefaultMessageBuilder implements IFIXAcceptorMessageBuilder {
	private static final Logger logger    = LoggerFactory.getLogger("FixAPILogger");
	private final SimpleDateFormat execId = new SimpleDateFormat("YYYYMMddHHmmssSS");
	
	@Override
	@MessageBuilder
	public Message buildExecutionReport(quickfix.fix44.ExecutionReport message, final Hashtable<String, Object> param) {
		logger.debug("@buildExecutionReport quickfix.fix44.ExecutionReport");
		quickfix.fix44.ExecutionReport exeReport = new quickfix.fix44.ExecutionReport();
		
		char ordStsCd    = String.valueOf(param.get("ordStatus")).charAt(0);
		char orderType   = String.valueOf(param.get("ordType")).charAt(0);
		char side        = String.valueOf(param.get("side")).charAt(0);
		double lastQty   = getLastQuantity(ordStsCd, Double.parseDouble(String.valueOf(param.get("orderQty"))));
		double cumQty    = getLastQuantity(ordStsCd, Double.parseDouble(String.valueOf(param.get("orderQty"))));
		double leavesQty = getLeavesQuantity(ordStsCd, cumQty, Double.parseDouble(String.valueOf(param.get("orderQty"))));
		
		exeReport.set(new AvgPx(Double.parseDouble(String.valueOf(param.get("price")))));							//	TAG6
		exeReport.set(new ClOrdID(String.valueOf(param.get("clOrdID"))));											//	TAG11
		exeReport.set(new OrigClOrdID(String.valueOf(param.get("clOrdID"))));										//	TAG41
		exeReport.set(new ExecID(execId.format(Calendar.getInstance(Locale.CANADA).getTime())));					//	TAG17
		exeReport.set(new ExecType(ordStsCd));																		//	TAG150
		
		exeReport.set(new OrdStatus(ordStsCd));																		//	TAG39
		exeReport.set(new OrderQty(Double.parseDouble(String.valueOf(param.get("orderQty")))));						//	TAG38
		exeReport.set(new LastQty(lastQty));																		//	TAG32
		exeReport.set(new CumQty(cumQty));																			//	TAG14
		exeReport.set(new OrderID(Long.toString(System.nanoTime())));												//	TAG37
		
		exeReport.set(new Currency(String.valueOf(param.get("currency"))));											//	TAG15
		exeReport.set(new Price(Double.parseDouble(String.valueOf(param.get("price")))));							//	TAG44
		exeReport.set(new LastPx(Double.parseDouble(String.valueOf(param.get("bankFillPrice")))));					//	TAG31
		exeReport.set(new OrdType(orderType));																		//	TAG40
		exeReport.set(new Side(side));																				//	TAG54
		
		exeReport.set(new Symbol(String.valueOf(param.get("symbol"))));												//	TAG55
		exeReport.set(new LeavesQty(leavesQty));																	//	TAG151
		
		String ordRefId =  String.valueOf(param.get("secondaryOrderID"));
		if((ordRefId != null) && (ordRefId.trim().length() > 0))
			exeReport.set(new SecondaryOrderID(ordRefId));															//	TAG198
		
		String transactTime = String.valueOf(param.get("transactTime"));
		if((transactTime != null) && (transactTime.trim().length() > 0) && (Long.parseLong(transactTime) > 0))
			exeReport.set(new TransactTime(new Date(Long.parseLong(transactTime))));								//	TAG60
		
		String text = String.valueOf(param.get("text"));
		if((text != null) && (text.trim().length() > 0))
			exeReport.set(new Text(text));																			//	TAG58
		
		String wlRequestId = String.valueOf(param.get("clOrdLinkID"));
		if((wlRequestId != null) && (wlRequestId.trim().length() > 0))
			exeReport.set(new ClOrdLinkID(wlRequestId));															//	TAG583
		
		String ordRejReason = (!param.containsKey("rejReason"))? null: String.valueOf(param.get("rejReason"));		//	TAG103
		if((ordRejReason != null) && (Integer.parseInt(ordRejReason) >= 0))	
			exeReport.set(new OrdRejReason(Integer.parseInt(ordRejReason)));
			
		return(exeReport);
	}

	@Override
	@MessageBuilder
	public Message buildExecutionReport(quickfix.fix50.ExecutionReport message, final Hashtable<String, Object> param) {
		logger.debug("@buildExecutionReport quickfix.fix50.ExecutionReport");
		quickfix.fix50.ExecutionReport exeReport = new quickfix.fix50.ExecutionReport();
		
		char ordStsCd    = String.valueOf(param.get("ordStatus")).charAt(0);
		char orderType   = String.valueOf(param.get("ordType")).charAt(0);
		char side        = String.valueOf(param.get("side")).charAt(0);
		double lastQty   = getLastQuantity(ordStsCd, Double.parseDouble(String.valueOf(param.get("orderQty"))));
		double cumQty    = getLastQuantity(ordStsCd, Double.parseDouble(String.valueOf(param.get("orderQty"))));
		double leavesQty = getLeavesQuantity(ordStsCd, cumQty, Double.parseDouble(String.valueOf(param.get("orderQty"))));
		
		exeReport.set(new AvgPx(Double.parseDouble(String.valueOf(param.get("price")))));							//	TAG6
		exeReport.set(new ClOrdID(String.valueOf(param.get("clOrdID"))));											//	TAG11
		exeReport.set(new OrigClOrdID(String.valueOf(param.get("clOrdID"))));										//	TAG41
		exeReport.set(new ExecID(execId.format(Calendar.getInstance(Locale.CANADA).getTime())));					//	TAG17
		exeReport.set(new ExecType(ordStsCd));																		//	TAG150
		
		exeReport.set(new OrdStatus(ordStsCd));																		//	TAG39
		exeReport.set(new OrderQty(Double.parseDouble(String.valueOf(param.get("orderQty")))));						//	TAG38
		exeReport.set(new LastQty(lastQty));																		//	TAG32
		exeReport.set(new CumQty(cumQty));																			//	TAG14
		exeReport.set(new OrderID(Long.toString(System.nanoTime())));												//	TAG37
		
		exeReport.set(new Currency(String.valueOf(param.get("currency"))));											//	TAG15
		exeReport.set(new Price(Double.parseDouble(String.valueOf(param.get("price")))));							//	TAG44
		exeReport.set(new LastPx(Double.parseDouble(String.valueOf(param.get("bankFillPrice")))));					//	TAG31
		exeReport.set(new OrdType(orderType));																		//	TAG40
		exeReport.set(new Side(side));																				//	TAG54
		
		exeReport.set(new Symbol(String.valueOf(param.get("symbol"))));												//	TAG55
		exeReport.set(new LeavesQty(leavesQty));																	//	TAG151
		
		String ordRefId =  String.valueOf(param.get("secondaryOrderID"));
		if((ordRefId != null) && (ordRefId.trim().length() > 0))
			exeReport.set(new SecondaryOrderID(ordRefId));															//	TAG198
		
		String transactTime = String.valueOf(param.get("transactTime"));
		if((transactTime != null) && (transactTime.trim().length() > 0) && (Long.parseLong(transactTime) > 0))
			exeReport.set(new TransactTime(new Date(Long.parseLong(transactTime))));								//	TAG60
		
		String text = String.valueOf(param.get("text"));
		if((text != null) && (text.trim().length() > 0))
			exeReport.set(new Text(text));																			//	TAG58
		
		String wlRequestId = String.valueOf(param.get("clOrdLinkID"));
		if((wlRequestId != null) && (wlRequestId.trim().length() > 0))
			exeReport.set(new ClOrdLinkID(wlRequestId));															//	TAG583
		
		String ordRejReason = (!param.containsKey("rejReason"))? null: String.valueOf(param.get("rejReason"));		//	TAG103
		if((ordRejReason != null) && (Integer.parseInt(ordRejReason) >= 0))	
			exeReport.set(new OrdRejReason(Integer.parseInt(ordRejReason)));
		
		return(exeReport);
	}
	
	@Override
	@MessageBuilder
	public Message buildOrderCancelReject(quickfix.fix44.OrderCancelReject message, Hashtable<String, Object> param) {
		logger.debug("@buildExecutionReport quickfix.fix44.OrderCancelReject");
		quickfix.fix44.OrderCancelReject ordCancelReject = new quickfix.fix44.OrderCancelReject();
		
		ordCancelReject.set(new OrderID(String.valueOf(param.get("clOrdID"))));										//  TAG37
		ordCancelReject.set(new ClOrdID(String.valueOf(param.get("clOrdID"))));										//	TAG11
		ordCancelReject.set(new OrigClOrdID(String.valueOf(param.get("origClOrdID"))));								//	TAG41
		ordCancelReject.set(new OrdStatus(String.valueOf(param.get("ordStatus")).charAt(0)));						//	TAG39
		ordCancelReject.set(new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST));							//	TAG434
		ordCancelReject.set(new CxlRejReason(CxlRejReason.OTHER));													//	TAG102
		
		String text = String.valueOf(param.get("text"));
		if((text != null) && (text.trim().length() > 0))
			ordCancelReject.set(new Text(text));																	//	TAG58
		
		String transactTime = String.valueOf(param.get("transactTime"));
		if((transactTime != null) && (transactTime.trim().length() > 0) && (Long.parseLong(transactTime) > 0))
			ordCancelReject.set(new TransactTime(new Date(Long.parseLong(transactTime))));							//	TAG60
		
		String wlRequestId = String.valueOf(param.get("clOrdLinkID"));
		if((wlRequestId != null) && (wlRequestId.trim().length() > 0))
			ordCancelReject.set(new ClOrdLinkID(wlRequestId));														//	TAG583
		
		return(ordCancelReject);
	}

	@Override
	@MessageBuilder
	public Message buildOrderCancelReject(quickfix.fix50.OrderCancelReject message, Hashtable<String, Object> param) {
		logger.debug("@buildExecutionReport quickfix.fix50.OrderCancelReject");
		quickfix.fix44.OrderCancelReject ordCancelReject = new quickfix.fix44.OrderCancelReject();
		
		ordCancelReject.set(new OrderID(String.valueOf(param.get("clOrdID"))));										//  TAG37
		ordCancelReject.set(new ClOrdID(String.valueOf(param.get("clOrdID"))));										//	TAG11
		ordCancelReject.set(new OrigClOrdID(String.valueOf(param.get("origClOrdID"))));								//	TAG41
		ordCancelReject.set(new OrdStatus(String.valueOf(param.get("ordStatus")).charAt(0)));						//	TAG39
		ordCancelReject.set(new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST));							//	TAG434
		ordCancelReject.set(new CxlRejReason(CxlRejReason.OTHER));													//	TAG102
		
		String text = String.valueOf(param.get("text"));
		if((text != null) && (text.trim().length() > 0))
			ordCancelReject.set(new Text(text));																	//	TAG58
		
		String transactTime = String.valueOf(param.get("transactTime"));
		if((transactTime != null) && (transactTime.trim().length() > 0) && (Long.parseLong(transactTime) > 0))
			ordCancelReject.set(new TransactTime(new Date(Long.parseLong(transactTime))));							//	TAG60
		
		String wlRequestId = String.valueOf(param.get("clOrdLinkID"));
		if((wlRequestId != null) && (wlRequestId.trim().length() > 0))
			ordCancelReject.set(new ClOrdLinkID(wlRequestId));														//	TAG583

		return(ordCancelReject);
	}
	
	
	private double getLastQuantity(char ordStsCode, double qty)	{
		double lastQty = qty;
		
		switch(ordStsCode)	{
			case OrdStatus.NEW:
			case OrdStatus.CANCELED:
			case OrdStatus.REJECTED:
				lastQty = 0;
				break;
		}
		
		return(lastQty);
	}
	
	private double getLeavesQuantity(char ordStsCode, double cumQty, double qty)	{
		double LeavesQty = qty - cumQty;
		
		switch(ordStsCode)	{
			case OrdStatus.CANCELED:
			case OrdStatus.REJECTED:
				LeavesQty = 0;
				break;
		}
		
		return(LeavesQty);
	}
}
