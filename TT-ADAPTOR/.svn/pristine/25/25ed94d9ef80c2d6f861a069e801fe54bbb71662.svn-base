package com.tts.plugin.adapter.impl.cibc.dialect;

import java.math.BigDecimal;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.message.constant.Constants;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.RestingOrderMessage.RestingOrder.Builder;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.impl.base.dialect.DefaultResponseDialectHelper;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronoConvUtil;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.RestingOrderConstants.OrderStateCd;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.field.ExecType;
import quickfix.field.MDEntryType;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.QuoteCondition;
import quickfix.field.Side;
import quickfix.fix50.MarketDataSnapshotFullRefresh;


public class CibcResponseDialectHelper extends DefaultResponseDialectHelper {
	private final OnBehalfOfCompID omsOnBehalfOfCompID;
	
	public CibcResponseDialectHelper() {
		super();
		FixApplicationProperties p = AppContext.getContext().getBean("applicationProperties", FixApplicationProperties.class);
		
		String omsOnBehalfOf = p.getProperty("REQUEST.OMS.ON_BEHALF_OF", "");
		omsOnBehalfOf        = ((omsOnBehalfOf == null) || (omsOnBehalfOf.trim().isEmpty()))? p.getProperty("SESSION.DEFAULT.ON_BEHALF_OF", "")
																																			: omsOnBehalfOf;
		omsOnBehalfOfCompID  = ((omsOnBehalfOf != null) && (!omsOnBehalfOf.trim().isEmpty()))? new OnBehalfOfCompID(omsOnBehalfOf): null;
	}
	
	@Override
	public QuoteVo convert(quickfix.fix50.Quote response) throws FieldNotFound {
		QuoteVo quote = super.convert(response);
		
		if ( response.isSetMinBidSize()) {
			quote.setMinBidSize(response.getMinBidSize().getValue());
		}
		if ( response.isSetMinOfferSize()) {
			quote.setMinOfferSize(response.getMinOfferSize().getValue());
		}
		if ( response.isSetOfferForwardPoints()) {
			quote.setOfferForwardPoints(response.getString(191));
		}
		if ( response.isSetBidForwardPoints()) {
			quote.setBidForwardPoints(response.getString(189));
		}
		
		if ( response.isSetOfferForwardPoints2()) {
			quote.setOfferForwardPoints2(response.getString(643));
		}
		if ( response.isSetBidForwardPoints2()) {
			quote.setBidForwardPoints2(response.getString(642));
		}
		
		if ( response.isSetOfferSpotRate()) {
			quote.setOfferSpotRate(response.getOfferSpotRate().getValue());
		}
		if ( response.isSetBidSpotRate()) {
			quote.setBidSpotRate(response.getBidSpotRate().getValue());
		}
		
		if ( response.isSetTransactTime() ) {
			quote.setTransactTime(Long.toString(response.getTransactTime().getValue().getTime()));
		}
		
		if ( response.isSetSettlDate2()) {
			quote.setSettleDate2(response.getSettlDate2().getValue());
		}
		
		if ( response.isSetField(6050)) {
			quote.setBidPx2(response.getString(6050));
		}
		if ( response.isSetField(6051)) {
			quote.setOfferPx2(response.getString(6051));
		}
		if ( response.isSetField(6052)) {
			quote.setBidSize2(response.getDouble(6052));
		}
		if ( response.isSetField(6053)) {
			quote.setOfferSize2(response.getDouble(6053));
		}
		String settleType = " ";
		String onBehalfOf = null;
		if ( response.isSetSettlType()) {
			settleType = response.getSettlType().getValue();
		}
		if (response.getHeader().isSetField(115)) {
			onBehalfOf = response.getHeader().getString(115);
		}
		
		if ( response.isSetSecurityType()) {
			String responseSecurityType = response.getSecurityType().getValue();
			if( "FXSPOT".equals(responseSecurityType)) {
				quote.setProduct(Constants.ProductType.FXSPOT);
			} else if( "FXFWD".equals(responseSecurityType)) {
				quote.setProduct(Constants.ProductType.FXFORWARDS);
			} else if ( ("FXSWAP".equals(responseSecurityType))) {
				quote.setProduct(Constants.ProductType.FXSWAP);
			}
		}

		quote.setQuoteId(onBehalfOf + IFixConstants.DEFAULT_DELIMITER + settleType + IFixConstants.DEFAULT_DELIMITER + quote.getQuoteId() );


		return quote;
	}
	
	@Override
	public String convertAndUpdate(MarketDataSnapshotFullRefresh response,
			FullBook.Builder fullBookBuilder) throws FieldNotFound {
		String quoteId = null;
		String bidFwdPoints = null, askFwdPoints = null;
		long timestamp = System.currentTimeMillis();
		String symbol = response.getSymbol().getValue();
		String settleType = null;
		String settleDate = null;
		boolean oneDollarRungPresent = false;
		
		// building the marketData
		fullBookBuilder.setSymbol(symbol);
		fullBookBuilder.setUpdateTimestamp(timestamp);
		
		// building the latency
		Latency.Builder latencyBuilder = fullBookBuilder.getLatencyBuilder();
		latencyBuilder.clear();
		latencyBuilder.setFaReceiveTimestamp(timestamp);
		
		fullBookBuilder.clearAskTicks();
		fullBookBuilder.clearBidTicks();

		
		quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
		int noMDEntries = response.getNoMDEntries().getValue();
		String onBehalfOfName = null;
		
		if ( response.getHeader().isSetField(115)) {
			onBehalfOfName = response.getHeader().getString(115);
		}
		
		int bidTickLevel = 1, offerTickLevel = 1;
		int tradableRung = 0, indicativeRung = 0;
		long indicativeFlag = IndicativeFlag.TRADABLE;

		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();
			quoteId = noMDEntry.getMDEntryID().getValue();

			if ( QuoteCondition.CLOSED_INACTIVE.equals(noMDEntry.getQuoteCondition().getValue() )) {
				indicativeRung++;
				continue;
			}
			
			if ( ! noMDEntry.isSetMDEntryPx() ) {
				continue;
			}
			
			tradableRung++;
			// building the tick
			Tick.Builder tickBuilder = Tick.newBuilder();
			tickBuilder.setRate(String.valueOf(noMDEntry.getMDEntryPx().getValue()));
			tickBuilder.setSpotRate(String.valueOf(noMDEntry.getMDEntrySpotRate().getValue()));
			quoteId = noMDEntry.getMDEntryID().getValue();
			
			settleType = noMDEntry.getSettlType().getValue();
			
			if ( noMDEntry.isSetSettlDate()) {
				settleDate = noMDEntry.getSettlDate().getValue();
			}
			if ( size <= 0) {
				indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_Indicative_Data);
			}
			tickBuilder.setSize(size);
			
			if ( size == 1L ) {
				oneDollarRungPresent = true;
			}
			// adding tick to marketData
			switch (noMDEntry.getMDEntryType().getValue()) {
				case MDEntryType.BID:
					tickBuilder.setLevel(bidTickLevel++);
					if (noMDEntry.isSetMDEntryForwardPoints() ) {
						bidFwdPoints = new BigDecimal(noMDEntry.getMDEntryForwardPoints().getValue()).toPlainString();
					}
					fullBookBuilder.addBidTicks(tickBuilder);
					break;

				case MDEntryType.OFFER:
					tickBuilder.setLevel(offerTickLevel++);
					if (noMDEntry.isSetMDEntryForwardPoints() ) {
						askFwdPoints = new BigDecimal(noMDEntry.getMDEntryForwardPoints().getValue()).toPlainString();
					}
					fullBookBuilder.addAskTicks(tickBuilder);
					break;

				default:
					break;
			}
		}
		if ( tradableRung == 0 && indicativeRung > 0) {
			indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_Indicative_Data);
		}
		
		
		if (  settleDate != null || settleType != null ) {
			Tenor.Builder tenor = Tenor.newBuilder();
			
			if ( settleType != null && TenorVo.NOTATION_OVERNIGHT.equals(settleType)) {
				tenor.setName(TenorVo.NOTATION_TODAY);
			} else {
				tenor.setName(TenorVo.NOTATION_SPOT);
			}
			if ( settleDate != null ) {
				tenor.setActualDate(settleDate);
			}
			if ( askFwdPoints != null ) {
				tenor.setAskSwapPoints(askFwdPoints);
			}
			if ( bidFwdPoints != null ) {
				tenor.setBidSwapPoints(bidFwdPoints);
			}
			fullBookBuilder.clearTenors();
			fullBookBuilder.addTenors(tenor);
		}
		fullBookBuilder.setIndicativeFlag(indicativeFlag);

		if ( settleType != null ) {
			fullBookBuilder.setQuoteRefId(IFixConstants.ESP_QUOTE_REF_ID_PREFIX + onBehalfOfName + IFixConstants.DEFAULT_DELIMITER  + settleType + IFixConstants.DEFAULT_DELIMITER + quoteId);
		} else {
			fullBookBuilder.setQuoteRefId(IFixConstants.ESP_QUOTE_REF_ID_PREFIX + onBehalfOfName + IFixConstants.DEFAULT_DELIMITER + " " + IFixConstants.DEFAULT_DELIMITER + quoteId);
		}
		
		int bidTicksCount = fullBookBuilder.getBidTicksCount();
		int askTicksCount = fullBookBuilder.getAskTicksCount();
		if ( !oneDollarRungPresent && askTicksCount > 0  && bidTicksCount > 0) {
			Tick.Builder oneDollarAsk = Tick.newBuilder(fullBookBuilder.getAskTicks(0));
			oneDollarAsk.setSize(1);
			Tick.Builder oneDollarBid = Tick.newBuilder(fullBookBuilder.getBidTicks(0));
			oneDollarBid.setSize(1);
			fullBookBuilder.addAskTicks(0, oneDollarAsk);
			fullBookBuilder.addBidTicks(0, oneDollarBid);
			for (int i = 0; i < askTicksCount; i++ ) {
				fullBookBuilder.getAskTicksBuilder(i).setLevel(i+1);
			}
			for (int i = 0; i < bidTicksCount; i++ ) {
				fullBookBuilder.getBidTicksBuilder(i).setLevel(i+1);
			}
		}
		return quoteId;
	}
	
	@Override
	public void convertAndUpdate(
			quickfix.fix50.ExecutionReport report,
			ExecutionReportInfo.Builder tradeExecutionStatusInfo) throws FieldNotFound {
		String orderId = report.getOrderID().getValue();
		String clientOrderId = report.getClOrdID().getValue();
		String status = null;
		
		char orderStatus = report.getOrdStatus().getValue();
		if ( orderStatus == OrdStatus.NEW) {
			status = TransStateType.TRADE_PENDING;
		} else if ( orderStatus == OrdStatus.CANCELED) {
			status = TransStateType.TRADE_CANCEL;
		} else if ( orderStatus == OrdStatus.REJECTED) {
			status = TransStateType.TRADE_REJECT;
		} else if ( orderStatus == OrdStatus.FILLED) {
			status = TransStateType.TRADE_DONE;
		} 
		
		String tradeAction = null;
		char side = report.getSide().getValue();
		
		String ccyPair = report.getSymbol().getValue();
		String notional = ccyPair.substring(0, 3);
		if ( report.isSetCurrency() ) {
			notional = report.getCurrency().getValue();
		}
		tradeExecutionStatusInfo.setCurrency(notional);
		
		
		if ( report.isSetSettlDate() ) {
			tradeExecutionStatusInfo.setSettleDate( report.getSettlDate().getValue() );
		}
		
		String product = com.tts.message.constant.Constants.ProductType.FXSPOT;

		if ( report.isSetSecurityType() ) {
			if ( "FXFWD".equals(report.getSecurityType().getValue())) {
				product = com.tts.message.constant.Constants.ProductType.FXFORWARDS;
			} else if ( "FXSWAP".equals(report.getSecurityType().getValue())) {
				product = com.tts.message.constant.Constants.ProductType.FXSWAP;
			}
		}
		tradeExecutionStatusInfo.setProduct(product);

		boolean isCcy2notional = ccyPair.indexOf(notional) > 2;


		
		if ( "FXSWAP".equals(report.getSecurityType().getValue())) {
			if ( !isCcy2notional) {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.SELL_AND_BUY;
				} else {
					tradeAction = TradeConstants.TradeAction.BUY_AND_SELL;
				}
			} else {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.BUY_AND_SELL;
				} else {
					tradeAction = TradeConstants.TradeAction.SELL_AND_BUY;
				}
			}

		} else {
			if ( !isCcy2notional) {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.BUY;
				} else if(side == Side.SELL)  { 
					tradeAction = TradeConstants.TradeAction.SELL;
				}
			} else {
				if (side == Side.BUY) { 
					tradeAction = TradeConstants.TradeAction.SELL;
				} else if(side == Side.SELL)  { 
					tradeAction = TradeConstants.TradeAction.BUY;
				}
			}
		}
		
		String finalPrice = convertRateToString(report.getPrice().getValue());
		if (com.tts.message.constant.Constants.ProductType.FXSWAP.equals(product) ) {
			if ( report.isSetField(640)) {
				String finalPrice2 = (report.getString(640));
				tradeExecutionStatusInfo.setFinalPrice2(finalPrice2);
			}
			
			String size2 = convertSizeToString(report.getOrderQty2().getValue());
			tradeExecutionStatusInfo.setSize2(size2);

			
		}
		if ( report.isSetSettlDate2() ) {
			String settleDate2 =  report.getSettlDate2().getValue();
			tradeExecutionStatusInfo.setSettleDate2(settleDate2);

		}
		
		tradeExecutionStatusInfo.setRefId(orderId);
		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(ccyPair);

		tradeExecutionStatusInfo.setTradeAction(tradeAction);
		tradeExecutionStatusInfo.setSize(convertSizeToString(report.getOrderQty().getValue()));
		

		if ( report.isSetText() ) {
			tradeExecutionStatusInfo.setAdditionalInfo(report.getText().getValue());
		}
		if ( report.isSetTransactTime()) {
			tradeExecutionStatusInfo.setTransactTime(
					ChronologyUtil.getDateTimeSecString(ChronoConvUtil.convertDateTime(report.getTransactTime().getValue()))
				);
		}
	}
	
	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix50.ExecutionReport msgExeReport, Builder roeBuilder)
																					throws FieldNotFound {
		boolean ordStsResponse = false;
		boolean processMsg     = false;
		String symbol   	   = "";
		String currency 	   = "";
		String clOrdID  	   = "";
		String text            = "";
		
		
		
		/**
		 *	Check If ExecutionReport is Response to OrderStatusRequest
		 */
		if(msgExeReport.isSetField(790))	{
			ordStsResponse      = true;
			String ordStsReqId  = msgExeReport.getOrdStatusReqID().getValue();
			String stsReqPrefix = "STATREQ-";
			if(omsOnBehalfOfCompID != null)
				stsReqPrefix = stsReqPrefix + omsOnBehalfOfCompID.getValue();
			
			clOrdID = ordStsReqId.replaceAll(stsReqPrefix, "");
		}
		
		/**
		 * 	Received "Unknown Order" in Response for Order Status Request.
		 * 	OrdStatus = REJECTED
		 */
		if((ordStsResponse) && (msgExeReport.isSetField(150)))	{
			ExecType execType = msgExeReport.getExecType();
			char ordStatus    = msgExeReport.getOrdStatus().getValue();
			processMsg        = ((ExecType.ORDER_STATUS == execType.getValue()) && (OrdStatus.REJECTED == ordStatus));
			
			if(msgExeReport.isSetField(58))
				text = msgExeReport.getText().getValue();
			
			logger.info("ExecType: " +  String.valueOf(execType.getValue()) + " OrdStatus: " + String.valueOf(ordStatus) 
			                         + " processMsg: " + String.valueOf(processMsg) + " text: " + text);
			
			/**
			 *	Unknown/Rejected Order. No Further Processing Required.
			 */
			if(processMsg)	{
				roeBuilder.setOrderId(Long.parseLong(clOrdID));
				roeBuilder.setStatusMessage(text);
				
				String orderStatus = getOrderStatus(ordStatus, execType.getValue());
				roeBuilder.setStatus(orderStatus);
				if(msgExeReport.isSetField(37))
					roeBuilder.setExternalOrderId(msgExeReport.getOrderID().getValue());
				
				logger.info("@ExecutionReport(OrderStatusRequest) OrdId: " +  clOrdID + "(" + clOrdID + "), Text: " + text + " COMPLETED...");
				return(clOrdID);
			}
		}
		
		
		
		symbol   = msgExeReport.getSymbol().getValue();
		currency = msgExeReport.getCurrency().getValue();
		clOrdID  = msgExeReport.getClOrdID().getValue();
				
		if(omsOnBehalfOfCompID != null)
			clOrdID = clOrdID.replaceAll(omsOnBehalfOfCompID.getValue(), "");
		logger.info("@ExecutionReport OrdId: " +  clOrdID + "(" + msgExeReport.getClOrdID().getValue() + "), Symbol: " + symbol);
		
		char ordType = msgExeReport.getOrdType().getValue();
		double price = ((ordType == OrdType.STOP) && (!msgExeReport.isSetField(44)))? msgExeReport.getStopPx().getValue()
																					: msgExeReport.getPrice().getValue();
				
		roeBuilder.setSize(String.valueOf(msgExeReport.getOrderQty().getValue()));
		roeBuilder.setSymbol(symbol);
		roeBuilder.setNotionalCurrency(currency);
		roeBuilder.setMarketTargetRate(String.valueOf(price));
		
		if(msgExeReport.isSetField(31))
			roeBuilder.setMarketFillRate(String.valueOf(msgExeReport.getLastPx().getValue()));
		roeBuilder.setOrderType(getOrderType(msgExeReport.getOrdType().getValue()));
		roeBuilder.setTradeAction(getTradeAction(msgExeReport.getSide().getValue(), symbol, currency));
		
		
		
		/**
		 *	If the CANCEL is Initiated by CIBC, On WL Side it is treated as REJECTED.
		 *	All WL Initiated Cancel Request is Prefixed with 'CXL' 
		 */
		char ordStatus = msgExeReport.getOrdStatus().getValue();
		char execType  = msgExeReport.getExecType().getValue();
		
		String orderStatus = getOrderStatus(ordStatus, execType);
		if((orderStatus.equals(OrderStateCd.CANCELLED)) && (clOrdID.indexOf("CXL") < 0))
			orderStatus = OrderStateCd.REJECTED;
		
		
		clOrdID = clOrdID.replaceAll("CXL", "");
		roeBuilder.setOrderId(Long.parseLong(clOrdID));
		roeBuilder.setStatus(orderStatus);
				
		
		if(msgExeReport.isSetField(58))	{
			text = msgExeReport.getText().getValue();
			text = ((text == null) || (text.trim().length() <= 0))? "": text;
		}
		roeBuilder.setStatusMessage(text);
		
		if(msgExeReport.isSetField(37))
			roeBuilder.setExternalOrderId(msgExeReport.getOrderID().getValue());
		
		if(msgExeReport.isSetField(103))
			roeBuilder.setRejectReason(msgExeReport.getOrdRejReason().getValue());
		
		logger.info("@ExecutionReport OrdId: " +  clOrdID + "(" + msgExeReport.getClOrdID().getValue() + "), Symbol: " + symbol + " COMPLETED...");
		return(clOrdID);
	}

	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix50.OrderCancelReject rejectCancel, Builder roeBuilder)
																				throws FieldNotFound {
		String clOrdID     = rejectCancel.getClOrdID().getValue();
		String message     = "";
		
		if(omsOnBehalfOfCompID != null)
			clOrdID = clOrdID.replaceAll(omsOnBehalfOfCompID.getValue(), "");
		logger.info("@OrderCancelReject OrdId: " +  clOrdID + ", Symbol: ");
		
		clOrdID = clOrdID.replaceAll("CXL", "");
		roeBuilder.setOrderId(Long.parseLong(clOrdID));
		
		char ordStatus = rejectCancel.getOrdStatus().getValue();
		roeBuilder.setStatus(getOrderStatus(ordStatus, ' '));
		roeBuilder.setExternalOrderId(rejectCancel.getOrderID().getValue());
		
		if(rejectCancel.isSetField(102))
			message += "CANCEL REJECTED. REASON: " + rejectCancel.getCxlRejReason().getValue();
		if(rejectCancel.isSetField(58))	
			message += " TEXT: " + rejectCancel.getText().getValue();
		roeBuilder.setStatusMessage(message);
		
		return(clOrdID);
	}
	
	
	public String getOrderStatus(char status, char 	execType)	{
		String orderStatus = "";
		
		switch(status)	{
			case OrdStatus.NEW:
				orderStatus = (execType == 'O')? OrderStateCd.RESUMED: OrderStateCd.ACTIVE;
				break;
			case OrdStatus.FILLED:
				orderStatus = OrderStateCd.DONE;
				break;	
			case OrdStatus.REJECTED:
				orderStatus = OrderStateCd.REJECTED;
				break;
			case OrdStatus.CANCELED:
				orderStatus = OrderStateCd.CANCELLED;
				break;
			case OrdStatus.EXPIRED:
				orderStatus = OrderStateCd.EXPIRED;
				break;
			case 'M':
				orderStatus = OrderStateCd.PENDING_SUSPEND;
				break;
			case OrdStatus.SUSPENDED:
				orderStatus = OrderStateCd.SUSPENDED;
				break;
			case 'N':
				orderStatus = OrderStateCd.PENDING_RESUME;
				break;
			case OrdStatus.PENDING_CANCEL:
				orderStatus = OrderStateCd.PENDING_CANCEL;
				break;
			case OrdStatus.PARTIALLY_FILLED:
			default:
				logger.warn("<<<   FUN getOrderStatus() PARTIALLY_FILLED/UN_HNADLED Order Status.   >>>");
				logger.warn("OrdStatus: " + String.valueOf(status));
				break;
		}
		
		return(orderStatus);
	}
}
