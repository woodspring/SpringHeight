package com.tts.plugin.adapter.impl.base.dialect;

import java.math.BigDecimal;
import java.math.MathContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.RestingOrderMessage.RestingOrder;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.api.dialect.vo.RejectReasonVo;
import com.tts.util.chronology.ChronoConvUtil;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.RestingOrderConstants.OrderStateCd;
import com.tts.util.constant.RestingOrderConstants.OrderType;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqRejReason;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.QuoteCondition;
import quickfix.field.Side;


public class DefaultResponseDialectHelper implements IResponseDialectHelper {
	public static final Logger logger = LoggerFactory.getLogger(DefaultResponseDialectHelper.class);
	
	@Override
	public RejectReasonVo convert( quickfix.fix50.MarketDataRequestReject rejectMessage) throws quickfix.FieldNotFound {
		RejectReasonVo rejectReason = new RejectReasonVo();
		rejectReason.setClientRequestId(rejectMessage.getMDReqID().getValue());
		if ( MDReqRejReason.DUPLICATE_MDREQID == rejectMessage.getMDReqRejReason().getValue()) {
			rejectReason.setRejectReason(REJECT_REASON__DUP_ID);
		} else if ( MDReqRejReason.INSUFFICIENT_PERMISSIONS == rejectMessage.getMDReqRejReason().getValue()) {
			rejectReason.setRejectReason(REJECT_REASON__PERMISSION);
		}
		
		rejectReason.setRejectDetail(rejectMessage.getText().getValue());
		return rejectReason;
	}
	
	@Override
	public QuoteVo convert(quickfix.fix50.Quote response) throws FieldNotFound {
		QuoteVo quote = new QuoteVo();
		quote.setQuoteReqId( response.getQuoteReqID().getValue());		
		quote.setQuoteId(response.getQuoteID().getValue());
		quote.setSymbol( response.getSymbol().getValue());
		
		if ( response.isSetCurrency()) {
			quote.setCurrency(response.getCurrency().getValue());
		}
		
		if ( response.isSetSettlDate()) {
			quote.setSettleDate(response.getSettlDate().getValue());
		}

		if (response.isSetBidSize() ) {
			quote.setBidSize(response.getBidSize().getValue());
		}
		if (response.isSetOfferSize()) {
			quote.setOfferSize(response.getOfferSize().getValue());
		}
		
		if (response.isSetOfferPx()) {
			quote.setOfferPx(response.getString(133));
		}

		if (response.isSetBidPx()) {
			quote.setBidPx(response.getString(132));
		}
		
		if ( response.isSetOfferForwardPoints()) {
			quote.setOfferForwardPoints(response.getString(191));
		}
		if ( response.isSetBidForwardPoints()) {
			quote.setBidForwardPoints(response.getString(189));
		}
		
		if ( response.isSetOfferSpotRate()) {
			quote.setOfferSpotRate(response.getOfferSpotRate().getValue());
		}
		if ( response.isSetBidSpotRate()) {
			quote.setBidSpotRate(response.getBidSpotRate().getValue());
		}
		
		if ( response.isSetMinBidSize()) {
			quote.setMinBidSize(response.getMinBidSize().getValue());
		}
		if ( response.isSetMinOfferSize()) {
			quote.setMinOfferSize(response.getMinOfferSize().getValue());
		}
		if ( response.isSetOfferForwardPoints()) {
			quote.setOfferForwardPoints(response.getOfferForwardPoints().getValue());
		}
		if ( response.isSetBidForwardPoints()) {
			quote.setBidForwardPoints(response.getBidForwardPoints().getValue());
		}
			
		if ( response.isSetTransactTime() ) {
			quote.setTransactTime(Long.toString(response.getTransactTime().getValue().getTime()));
		}
		return quote;
	}
	
	@Override
	public String convertAndUpdate(quickfix.fix50.Quote response, FullBook.Builder fullBookBuilder) throws FieldNotFound {
		
		long timestamp = System.currentTimeMillis();
		
		@SuppressWarnings("unused")
		String requestId = response.getQuoteReqID().getValue();		
		String quoteId = response.getQuoteID().getValue();
		String symbol = response.getSymbol().getValue();

		// building the marketData
		fullBookBuilder.setSymbol(symbol);
		fullBookBuilder.setUpdateTimestamp(timestamp);

		// building the latency
		Latency.Builder latencyBuilder = fullBookBuilder.getLatencyBuilder();
		latencyBuilder.clear();
		latencyBuilder.setFaReceiveTimestamp(timestamp);

		long size = -1;
		if (!response.isSetBidSize() && !response.isSetOfferSize()) {

		} else if (!response.isSetBidSize()) {
			size = (long) response.getOfferSize().getValue();
		} else {
			size = (long) response.getBidSize().getValue();
		}
		
		
		// building ask ticks
		if (response.isSetOfferPx()) {
			boolean foundAsk = false;	
			for (int i = 0; i < fullBookBuilder.getAskTicksCount(); i ++) {
				Tick.Builder tickBuilder = fullBookBuilder.getAskTicksBuilder(i);
				if (tickBuilder.hasSize() && tickBuilder.getSize() == size) {
					tickBuilder.setRate(String.valueOf(response.getOfferPx().getValue()));
					foundAsk = true;
				}
			}			
		
			if (!foundAsk) {
				Tick.Builder tickBuilder = Tick.newBuilder();
				tickBuilder.setRate(String.valueOf(response.getOfferPx().getValue()));
				tickBuilder.setSize(size);	
				fullBookBuilder.addAskTicks(tickBuilder);
			}
		}
		
		// building bid ticks
		if (response.isSetBidPx()) {
			boolean foundBid = false;	
			for (int i = 0; i < fullBookBuilder.getBidTicksCount(); i ++) {
				Tick.Builder tickBuilder = fullBookBuilder.getBidTicksBuilder(i);
				if (tickBuilder.hasSize() && tickBuilder.getSize() == size) {
					tickBuilder.setRate(String.valueOf(response.getBidPx().getValue()));
					foundBid = true;
				}
			}		
			
			if (!foundBid) {
				Tick.Builder tickBuilder = Tick.newBuilder();
				tickBuilder.setRate(String.valueOf(response.getBidPx().getValue()));
				tickBuilder.setSize(size);	
				fullBookBuilder.addBidTicks(tickBuilder);
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
		
		if(orderStatus == OrdStatus.FILLED)	{
			String symbol = report.getSymbol().getValue().replaceFirst("/", "");
			String currency1 = symbol.substring(0, 3);
			String currency2 = symbol.substring(3, 6);
			String nationalCurrency = report.getCurrency().getValue();
			System.out.println(symbol + " " +  currency1 + " " + currency2 + " " + side  );
			if(nationalCurrency.equalsIgnoreCase(currency1))	{
				tradeAction = (side == Side.BUY)? TradeConstants.TradeAction.BUY: TradeConstants.TradeAction.SELL;
			}
			if(nationalCurrency.equals(currency2))	{
				tradeAction = (side == Side.BUY)? TradeConstants.TradeAction.SELL: TradeConstants.TradeAction.BUY;
			}			
		}
		else	{
			if(side == Side.BUY) { 
				tradeAction = TradeConstants.TradeAction.BUY;
			} else if(side == Side.SELL)  { 
				tradeAction = TradeConstants.TradeAction.SELL;
			}
		}
		
		String finalPrice = convertRateToString(report.getPrice().getValue());
		tradeExecutionStatusInfo.setRefId(orderId);
		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(report.getSymbol().getValue());
		if ( report.isSetCurrency() ) {
			tradeExecutionStatusInfo.setCurrency(report.getCurrency().getValue());
		}
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
	public String convertAndUpdate(quickfix.fix50.MarketDataSnapshotFullRefresh response, FullBook.Builder fullBookBuilder) 
			throws FieldNotFound {
		String quoteId = null;
		String bidFwdPoints = null, askFwdPoints = null;
		long timestamp = System.currentTimeMillis();
		String symbol = response.getSymbol().getValue();
		String settleType = null;
		String settleDate = null;
		
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
		
		
		if (  settleDate != null) {
			Tenor.Builder tenor = Tenor.newBuilder();
			
			if ( settleType != null && TenorVo.NOTATION_OVERNIGHT.equals(settleType)) {
				tenor.setName(TenorVo.NOTATION_TODAY);
			} else {
				tenor.setName(TenorVo.NOTATION_SPOT);
			}
			tenor.setActualDate(settleDate);
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
			fullBookBuilder.setQuoteRefId(IFixConstants.ESP_QUOTE_REF_ID_PREFIX + settleType + IFixConstants.DEFAULT_DELIMITER + quoteId);
		} else {
			fullBookBuilder.setQuoteRefId(IFixConstants.ESP_QUOTE_REF_ID_PREFIX + quoteId);
		}
		
		return quoteId;
	}

	@Override
	public QuoteVo convert(quickfix.fix50.MarketDataSnapshotFullRefresh response, long targetSize) throws FieldNotFound {
		QuoteVo q = null;
		String symbol = response.getSymbol().getValue();

		quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
		int noMDEntries = response.getNoMDEntries().getValue();


		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();

			if ( size >= targetSize ) {
				if ( q == null ) {
					q = new QuoteVo();
					q.setSymbol(symbol);
					q.setQuoteId(noMDEntry.getMDEntryID().getValue());
					q.setTransactTime(Long.toString(System.currentTimeMillis()));
				}
				String quoteId = null;
				switch (noMDEntry.getMDEntryType().getValue()) {
					case MDEntryType.BID:
						if ( q.getBidPx() == null && noMDEntry.getQuoteCondition().getValue().equals(QuoteCondition.OPEN_ACTIVE) ) {
							q.setBidPx(noMDEntry.getMDEntryPx().getValue());
							if ( noMDEntry.isSetMDEntrySpotRate()) {
								q.setBidSpotRate(noMDEntry.getMDEntrySpotRate().getValue());
							}
							q.setBidSize(size);
							q.setMinBidSize(targetSize);
							if ( noMDEntry.isSetMDEntryForwardPoints()) {
								q.setBidForwardPoints(noMDEntry.getMDEntryForwardPoints().getValue());
							}
							if ( noMDEntry.isSetMDEntryID()) {
								quoteId = noMDEntry.getMDEntryID().getValue();
							}
						}
						break;
					case MDEntryType.OFFER:
						if ( q.getOfferPx() == null && noMDEntry.getQuoteCondition().getValue().equals(QuoteCondition.OPEN_ACTIVE) ) {
							q.setOfferPx(noMDEntry.getMDEntryPx().getValue());
							if ( noMDEntry.isSetMDEntrySpotRate()) {
								q.setOfferSpotRate(noMDEntry.getMDEntrySpotRate().getValue());
							}
							q.setOfferSize(size);
							q.setMinOfferSize(targetSize);
							if ( noMDEntry.isSetMDEntryForwardPoints()) {
								q.setOfferForwardPoints(noMDEntry.getMDEntryForwardPoints().getValue());
							}
							if ( noMDEntry.isSetMDEntryID()) {
								quoteId = noMDEntry.getMDEntryID().getValue();
							}
						}
						break;
				}
				q.setQuoteId(quoteId);
			}
			
			if ( q != null && q.getBidPx() != null && q.getOfferPx() != null ) {
				break;
			}
		}
		
		if ( q != null &&  q.getBidPx() == null && q.getOfferPx() == null ) {
			return null;
		}
		return q;
	}

	@Override
	public QuoteVo convert(quickfix.fix44.Quote arg0) throws FieldNotFound {
		return null;
	}

	@Override
	public RejectReasonVo convert(quickfix.fix44.MarketDataRequestReject rejectMessage) throws FieldNotFound {
		RejectReasonVo rejectReason = new RejectReasonVo();
		rejectReason.setClientRequestId(rejectMessage.getMDReqID().getValue());
		if ( MDReqRejReason.DUPLICATE_MDREQID == rejectMessage.getMDReqRejReason().getValue()) {
			rejectReason.setRejectReason(REJECT_REASON__DUP_ID);
		} else if ( MDReqRejReason.INSUFFICIENT_PERMISSIONS == rejectMessage.getMDReqRejReason().getValue()) {
			rejectReason.setRejectReason(REJECT_REASON__PERMISSION);
		}
		
		rejectReason.setRejectDetail(rejectMessage.getText().getValue());
		return rejectReason;
	}

	@Override
	public QuoteVo convert(quickfix.fix44.MarketDataSnapshotFullRefresh response, long targetSize) throws FieldNotFound {
		QuoteVo q = null;
		String symbol = response.getSymbol().getValue();

		quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
		int noMDEntries = response.getNoMDEntries().getValue();


		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();

			if ( size == targetSize ) {
				if ( q == null ) {
					q = new QuoteVo();
					q.setSymbol(symbol);
					q.setQuoteId(noMDEntry.getMDEntryID().getValue());
					q.setTransactTime(Long.toString(System.currentTimeMillis()));
				}
				
				switch (noMDEntry.getMDEntryType().getValue()) {
					case MDEntryType.BID:
						q.setBidPx(noMDEntry.getMDEntryPx().getValue());
						if ( noMDEntry.isSetMDEntrySpotRate()) {
							q.setBidSpotRate(noMDEntry.getMDEntrySpotRate().getValue());
						}
						q.setBidSize(size);
						q.setMinBidSize(targetSize);
						if ( noMDEntry.isSetMDEntryForwardPoints()) {
							q.setBidForwardPoints(noMDEntry.getMDEntryForwardPoints().getValue());
						}
						break;
					case MDEntryType.OFFER:
						q.setOfferPx(noMDEntry.getMDEntryPx().getValue());
						if ( noMDEntry.isSetMDEntrySpotRate()) {
							q.setOfferSpotRate(noMDEntry.getMDEntrySpotRate().getValue());
						}
						q.setOfferSize(size);
						q.setMinOfferSize(targetSize);
						if ( noMDEntry.isSetMDEntryForwardPoints()) {
							q.setOfferForwardPoints(noMDEntry.getMDEntryForwardPoints().getValue());
						}
						break;
				}
			}
		}
		
		return q;
	}

	@Override
	public String convertAndUpdate(quickfix.fix44.MarketDataSnapshotFullRefresh response, FullBook.Builder fullBookBuilder) throws FieldNotFound {
		String quoteId = null;
		long timestamp = System.currentTimeMillis();
		String symbol = response.getSymbol().getValue();

		
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


		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();

			// building the tick
			Tick.Builder tickBuilder = Tick.newBuilder();
			tickBuilder.setLevel(noMDEntry.getMDEntryPositionNo().getValue());
			tickBuilder.setRate(String.valueOf(noMDEntry.getMDEntryPx().getValue()));
			tickBuilder.setSpotRate(String.valueOf(noMDEntry.getMDEntrySpotRate().getValue()));
			tickBuilder.setSize(size);
			
			long indicativeFlag = fullBookBuilder.getIndicativeFlag();
			
			if(noMDEntry.getQuoteCondition().getValue().equals(QuoteCondition.OPEN_ACTIVE) == false) {
				indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_Indicative_Data);
			} else {
				indicativeFlag = IndicativeFlag.removeIndicative(indicativeFlag, IndicativeReason.MA_Indicative_Data);
			}

			fullBookBuilder.setIndicativeFlag(indicativeFlag);

			
			quoteId = noMDEntry.getMDEntryID().getValue();
			// adding tick to marketData
			switch (noMDEntry.getMDEntryType().getValue()) {
				case MDEntryType.BID:
					fullBookBuilder.addBidTicks(tickBuilder);
					break;

				case MDEntryType.OFFER:
					fullBookBuilder.addAskTicks(tickBuilder);
					break;

				default:
					break;
			}
		}
		fullBookBuilder.setQuoteRefId(IFixConstants.ESP_QUOTE_REF_ID_PREFIX + quoteId);

		return quoteId;
	}

	@Override
	public String convertAndUpdate(quickfix.fix44.Quote response, FullBook.Builder fullBookBuilder) throws FieldNotFound {
		
		long timestamp = System.currentTimeMillis();
		
		@SuppressWarnings("unused")
		String requestId = response.getQuoteReqID().getValue();		
		String quoteId = response.getQuoteID().getValue();
		String symbol = response.getSymbol().getValue();

		// building the marketData
		fullBookBuilder.setSymbol(symbol);
		fullBookBuilder.setUpdateTimestamp(timestamp);

		// building the latency
		Latency.Builder latencyBuilder = fullBookBuilder.getLatencyBuilder();
		latencyBuilder.clear();
		latencyBuilder.setFaReceiveTimestamp(timestamp);

		long size = -1;
		if (!response.isSetBidSize() && !response.isSetOfferSize()) {

		} else if (!response.isSetBidSize()) {
			size = (long) response.getOfferSize().getValue();
		} else {
			size = (long) response.getBidSize().getValue();
		}
		
		
		// building ask ticks
		if (response.isSetOfferPx()) {
			boolean foundAsk = false;	
			for (int i = 0; i < fullBookBuilder.getAskTicksCount(); i ++) {
				Tick.Builder tickBuilder = fullBookBuilder.getAskTicksBuilder(i);
				if (tickBuilder.hasSize() && tickBuilder.getSize() == size) {
					tickBuilder.setRate(String.valueOf(response.getOfferPx().getValue()));
					foundAsk = true;
				}
			}			
		
			if (!foundAsk) {
				Tick.Builder tickBuilder = Tick.newBuilder();
				tickBuilder.setRate(String.valueOf(response.getOfferPx().getValue()));
				tickBuilder.setSize(size);	
				fullBookBuilder.addAskTicks(tickBuilder);
			}
		}
		
		// building bid ticks
		if (response.isSetBidPx()) {
			boolean foundBid = false;	
			for (int i = 0; i < fullBookBuilder.getBidTicksCount(); i ++) {
				Tick.Builder tickBuilder = fullBookBuilder.getBidTicksBuilder(i);
				if (tickBuilder.hasSize() && tickBuilder.getSize() == size) {
					tickBuilder.setRate(String.valueOf(response.getBidPx().getValue()));
					foundBid = true;
				}
			}		
			
			if (!foundBid) {
				Tick.Builder tickBuilder = Tick.newBuilder();
				tickBuilder.setRate(String.valueOf(response.getBidPx().getValue()));
				tickBuilder.setSize(size);	
				fullBookBuilder.addBidTicks(tickBuilder);
			}
		}
		return quoteId;	
	}

	@Override
	public void convertAndUpdate(quickfix.fix44.ExecutionReport report,
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
		
		if(orderStatus == OrdStatus.FILLED)	{
			String symbol = report.getSymbol().getValue().replaceFirst("/", "");
			String currency1 = symbol.substring(0, 3);
			String currency2 = symbol.substring(3);
			String nationalCurrency = report.getCurrency().getValue();
			
			if(nationalCurrency.equalsIgnoreCase(currency1))	{
				tradeAction = (side == Side.BUY)? TradeConstants.TradeAction.BUY: TradeConstants.TradeAction.SELL;
			}
			if(nationalCurrency.equals(currency2))	{
				tradeAction = (side == Side.BUY)? TradeConstants.TradeAction.SELL: TradeConstants.TradeAction.BUY;
			}			
		}
		else	{
			if(side == Side.BUY) { 
				tradeAction = TradeConstants.TradeAction.BUY;
			} else if(side == Side.SELL)  { 
				tradeAction = TradeConstants.TradeAction.SELL;
			}
		}
		
		String finalPrice = convertRateToString(report.getPrice().getValue());
		tradeExecutionStatusInfo.setRefId(orderId);
		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(report.getSymbol().getValue());
		if ( report.isSetCurrency() ) {
			tradeExecutionStatusInfo.setCurrency(report.getCurrency().getValue());
		}
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
		
		if ( report.isSetSettlDate() ) {
			tradeExecutionStatusInfo.setSettleDate( report.getSettlDate().getValue() );
		}
		
	}

	public static String convertRateToString(double d) {
		BigDecimal bd = new BigDecimal(d, MathContext.DECIMAL64);
		return bd.stripTrailingZeros().toPlainString();
	}
	
	public static String convertSizeToString(double d) {
		return  String.format("%.2f",d );
	}

	@Override
	public void postValidate(FullBook.Builder fbBuilder, quickfix.Message src) {
		// DO NOTHING
	}

	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix44.ExecutionReport msgExeReport, RestingOrder.Builder roeBuilder) 
																			throws quickfix.FieldNotFound { 
		
		String symbol   = msgExeReport.getSymbol().getValue();
		String currency = msgExeReport.getCurrency().getValue();
		logger.info("@ExecutionReport OrdId: " +  msgExeReport.getClOrdID().getValue() + ", Symbol: " + symbol);
		
		roeBuilder.setOrderId(Long.parseLong(msgExeReport.getClOrdID().getValue()));
		roeBuilder.setSize(String.valueOf(msgExeReport.getOrderQty().getValue()));
		roeBuilder.setSymbol(symbol);
		roeBuilder.setNotionalCurrency(currency);
		roeBuilder.setMarketTargetRate(String.valueOf(msgExeReport.getPrice().getValue()));
		
		if(msgExeReport.isSetField(31))
			roeBuilder.setMarketFillRate(String.valueOf(msgExeReport.getLastPx().getValue()));
		roeBuilder.setOrderType(getOrderType(msgExeReport.getOrdType().getValue()));
		roeBuilder.setTradeAction(getTradeAction(msgExeReport.getSide().getValue(), symbol, currency));
		
		char ordStatus = msgExeReport.getOrdStatus().getValue();
		char execType  = msgExeReport.getExecType().getValue();
		roeBuilder.setStatus(getOrderStatus(ordStatus, execType));
		
		String text = "";
		if(msgExeReport.isSetField(58))	{
			text = msgExeReport.getText().getValue();
			text = ((text == null) || (text.trim().length() <= 0))? "": text;
		}
		roeBuilder.setStatusMessage(text);
		
		if(msgExeReport.isSetField(198))
			roeBuilder.setExternalOrderId(msgExeReport.getSecondaryOrderID().getValue());
		if(msgExeReport.isSetField(103))
			roeBuilder.setRejectReason(msgExeReport.getOrdRejReason().getValue());
		
		logger.info("@ExecutionReport OrdId: " +  msgExeReport.getClOrdID().getValue() + ", Symbol: " + symbol + " COMPLETED...");
		return(msgExeReport.getClOrdID().getValue());
	}
	
	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix50.ExecutionReport msgExeReport, RestingOrder.Builder roeBuilder) 
																			throws quickfix.FieldNotFound {
		
		String symbol   = msgExeReport.getSymbol().getValue();
		String currency = msgExeReport.getCurrency().getValue();
		logger.info("@ExecutionReport OrdId: " +  msgExeReport.getClOrdID().getValue() + ", Symbol: " + symbol);
		
		roeBuilder.setOrderId(Long.parseLong(msgExeReport.getClOrdID().getValue()));
		roeBuilder.setSize(String.valueOf(msgExeReport.getOrderQty().getValue()));
		roeBuilder.setSymbol(symbol);
		roeBuilder.setNotionalCurrency(currency);
		roeBuilder.setMarketTargetRate(String.valueOf(msgExeReport.getPrice().getValue()));
		
		if(msgExeReport.isSetField(31))
			roeBuilder.setMarketFillRate(String.valueOf(msgExeReport.getLastPx().getValue()));
		roeBuilder.setOrderType(getOrderType(msgExeReport.getOrdType().getValue()));
		roeBuilder.setTradeAction(getTradeAction(msgExeReport.getSide().getValue(), symbol, currency));
		
		char ordStatus = msgExeReport.getOrdStatus().getValue();
		char execType  = msgExeReport.getExecType().getValue();
		roeBuilder.setStatus(getOrderStatus(ordStatus, execType));
		
		String text = "";
		if(msgExeReport.isSetField(58))	{
			text = msgExeReport.getText().getValue();
			text = ((text == null) || (text.trim().length() <= 0))? "": text;
		}
		roeBuilder.setStatusMessage(text);
		
		if(msgExeReport.isSetField(198))
			roeBuilder.setExternalOrderId(msgExeReport.getSecondaryOrderID().getValue());
		if(msgExeReport.isSetField(103))
			roeBuilder.setRejectReason(msgExeReport.getOrdRejReason().getValue());
		
		logger.info("@ExecutionReport OrdId: " +  msgExeReport.getClOrdID().getValue() + ", Symbol: " + symbol + " COMPLETED...");
		return(msgExeReport.getClOrdID().getValue());
	}
	
	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix44.OrderCancelReject rejectCancel, RestingOrder.Builder roeBuilder) 
																				throws quickfix.FieldNotFound {
		logger.info("@OrderCancelReject OrdId: " +  rejectCancel.getClOrdID().getValue() + ", Symbol: ");
		roeBuilder.setOrderId(Long.parseLong(rejectCancel.getClOrdID().getValue()));
		
		char ordStatus = rejectCancel.getOrdStatus().getValue();
		roeBuilder.setStatus(getOrderStatus(ordStatus, ' '));
		roeBuilder.setStatusMessage(rejectCancel.getText().getValue());
		roeBuilder.setExternalOrderId(rejectCancel.getOrigClOrdID().getValue());
		
		return(rejectCancel.getClOrdID().getValue());
	}
	
	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix50.OrderCancelReject rejectCancel, RestingOrder.Builder roeBuilder) 
																				throws quickfix.FieldNotFound {	
		logger.info("@OrderCancelReject OrdId: " +  rejectCancel.getClOrdID().getValue() + ", Symbol: ");
		roeBuilder.setOrderId(Long.parseLong(rejectCancel.getClOrdID().getValue()));
		
		char ordStatus = rejectCancel.getOrdStatus().getValue();
		roeBuilder.setStatus(getOrderStatus(ordStatus, ' '));
		roeBuilder.setStatusMessage(rejectCancel.getText().getValue());
		roeBuilder.setExternalOrderId(rejectCancel.getOrigClOrdID().getValue());
		
		return(rejectCancel.getClOrdID().getValue());
	}
	
	public String getOrderType(char orderType)	{
		String orderTypeValue = OrderType.LIMIT;
		
		if(OrdType.LIMIT == orderType)
			orderTypeValue = OrderType.LIMIT;
		
		if(OrdType.STOP == orderType)
			orderTypeValue = OrderType.STOP;
		
		return(orderTypeValue);
	}
	
	public String getTradeAction(char side, String symbol, String currency)	{
		String ordAction = TradeAction.BUY;
		String CCY1    = symbol.substring(0, 3);
		String CCY2    = symbol.substring(3);
		String NCCY    = currency;
		
		if(NCCY.equalsIgnoreCase(CCY1))	{
			ordAction = (Side.BUY == side)? TradeAction.BUY: TradeAction.SELL;
		}
		if(NCCY.equals(CCY2))	{
			ordAction = (Side.BUY == side)? TradeAction.SELL: TradeAction.BUY;
		}
		
		return(ordAction);
	}
	
	public String getOrderStatus(char status, char 	execType)	{
		String orderStatus = "";
		
		switch(status)	{
			case OrdStatus.NEW:
				orderStatus = OrderStateCd.ACTIVE;
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
			default:
				logger.warn("<<<   @@@getOrderStatus() PARTIALLY_FILLED/UN_HNADLED Order Status.   >>>");
				logger.warn("OrdStatus: " + String.valueOf(status));
				break;
		}
		
		return(orderStatus);
	}
}
