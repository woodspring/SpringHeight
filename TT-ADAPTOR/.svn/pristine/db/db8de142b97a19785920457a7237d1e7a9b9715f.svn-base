package com.tts.plugin.adapter.impl.ykb.dialect;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.RestingOrderMessage.RestingOrder.Builder;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.impl.base.dialect.DefaultResponseDialectHelper;
import com.tts.util.chronology.ChronoConvUtil;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;

import quickfix.FieldNotFound;
import quickfix.field.MDEntryType;
import quickfix.field.OrdStatus;
import quickfix.field.QuoteCondition;
import quickfix.field.Side;


public class YkbResponseDialectHelper extends DefaultResponseDialectHelper {
	
	private final long addSmallDollarRung;
	private final long addLargeDollarRung;
	private final int tagForOrderRef;
	private final boolean isVWAPenabled;
	
	public YkbResponseDialectHelper(FixApplicationProperties p) { 		
		this.isVWAPenabled = p.getProperty("spotadapter.vwap", false);
		this.addSmallDollarRung = p.getProperty("spotadapter.tradair.add_small_dollar_rung", 500000);
		this.addLargeDollarRung = p.getProperty("spotadapter.tradair.add_large_dollar_rung", 20000000);
		this.tagForOrderRef = (int) p.getProperty("spotadapter.tradair.tradair_ref_from_tag", 17L);
	}
	
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(YkbResponseDialectHelper.class);
	@Override
	public QuoteVo convert(quickfix.fix50.Quote response) throws FieldNotFound {
		return null;
	}

	@Override
	public String convertAndUpdate(quickfix.fix44.MarketDataSnapshotFullRefresh response,
			FullBook.Builder fullBookBuilder) throws FieldNotFound {
		String quoteId = null;
		long timestamp = System.currentTimeMillis();
		String sentTime = response.getHeader().getString(52);
		fullBookBuilder.setQuoteRefId(sentTime);
		String symbol = response.getSymbol().getValue();
		
		if ( symbol != null ) {
			symbol = symbol.replaceFirst("/", "");
		}
		// building the marketData
		fullBookBuilder.setSymbol(symbol);
		fullBookBuilder.setUpdateTimestamp(timestamp);
		
		// building the latency
		Latency.Builder latencyBuilder = fullBookBuilder.getLatencyBuilder();
		latencyBuilder.clear();
		latencyBuilder.setFaReceiveTimestamp(timestamp);
		
		fullBookBuilder.clearAskTicks();
		fullBookBuilder.clearBidTicks();

		
		quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
		int noMDEntries = response.getNoMDEntries().getValue();

		int bidTickLevel = 1, offerTickLevel = 1;
		int tradableRung = 0, indicativeRung = 0;
		long indicativeFlag = IndicativeFlag.TRADABLE;

		for (int i = 1; i <= noMDEntries; i++) {
			noMDEntry = new quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries();
			response.getGroup(i, noMDEntry);
			long size = new Double(noMDEntry.getMDEntrySize().getValue()).longValue();

			if ( noMDEntry.isSetQuoteCondition() ) {
				if ( QuoteCondition.CLOSED_INACTIVE.equals(noMDEntry.getQuoteCondition().getValue() )) {
					indicativeRung++;
					continue;
				}
			}
			
			if ( ! noMDEntry.isSetMDEntryPx() ) {
				continue;
			}
			
			tradableRung++;
			// building the tick
			Tick.Builder tickBuilder = Tick.newBuilder();
			tickBuilder.setRate(String.valueOf(noMDEntry.getMDEntryPx().getValue()));
			tickBuilder.setSpotRate(String.valueOf(noMDEntry.getMDEntryPx().getValue()));
			tickBuilder.setSize(size);
			// adding tick to marketData
			switch (noMDEntry.getMDEntryType().getValue()) {
				case MDEntryType.BID:
					tickBuilder.setLevel(bidTickLevel++);
					fullBookBuilder.addBidTicks(tickBuilder);
					break;

				case MDEntryType.OFFER:
					tickBuilder.setLevel(offerTickLevel++);
					fullBookBuilder.addAskTicks(tickBuilder);
					break;

				default:
					break;
			}
		}
		if ( tradableRung == 0 && indicativeRung > 0) {
			indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_Indicative_Data);
		}
		
		fullBookBuilder.setIndicativeFlag(indicativeFlag);
		
		//validateFullBook(fullBookBuilder);
		
		return quoteId;
	}



	@Override
	public void convertAndUpdate(quickfix.fix44.ExecutionReport report,
			ExecutionReportInfo.Builder tradeExecutionStatusInfo) throws FieldNotFound {
		String orderId = "";
		String execId = "";
		String tag8000value = "";
		String cmt = null;
		
		if(report.isSetOrderID())
			orderId = report.getOrderID().getValue();
		if ( report.isSetExecID() ) {
			execId = report.getExecID().getValue();
		}
		if (report.isSetField(8000)) {
			tag8000value = report.getString(8000);
		}
		
		
		String clientOrderId = report.getClOrdID().getValue();
		String status = "";
		
		char orderStatus = report.getOrdStatus().getValue();
		if ( orderStatus == OrdStatus.NEW) {
			status = TransStateType.TRADE_PENDING;
		} else if ( orderStatus == OrdStatus.CANCELED) {
			status = TransStateType.TRADE_CANCEL;
		} else if ( orderStatus == OrdStatus.REJECTED) {
			status = TransStateType.TRADE_REJECT;
		} else if ( orderStatus == OrdStatus.PARTIALLY_FILLED) {
				status = TransStateType.TRADE_PARTIALLY_DONE; 
		} else if ( orderStatus == OrdStatus.FILLED) {
			status = TransStateType.TRADE_DONE;
		} 
				
		String tradeAction = null;
		char side = report.getSide().getValue();
		String symbol = report.getSymbol().getValue().replaceFirst("/", "");

		if(orderStatus == OrdStatus.FILLED || orderStatus == OrdStatus.PARTIALLY_FILLED)	{
			String currency1 = symbol.substring(0, 3);
			String currency2 = symbol.substring(3);
			String notionalCurrency = report.getCurrency().getValue();
			
			if(notionalCurrency.equalsIgnoreCase(currency1))	{
				tradeAction = (side == Side.BUY)? TradeConstants.TradeAction.BUY: TradeConstants.TradeAction.SELL;
			}
			if(notionalCurrency.equals(currency2))	{
				tradeAction = (side == Side.BUY)? TradeConstants.TradeAction.SELL: TradeConstants.TradeAction.BUY;
			}	
			
			StringBuilder sb = new StringBuilder();
			sb.append(17).append('=').append(execId).append(' ');
			sb.append(37).append('=').append(orderId).append(' ');
			sb.append(8000).append('=').append(tag8000value);
			cmt = sb.toString();
		}
		else	{		
			if(side == Side.BUY) { 
				tradeAction = TradeConstants.TradeAction.BUY;
			} 
			else if (side == Side.SELL)  { 
				tradeAction = TradeConstants.TradeAction.SELL;
			}
			if ( report.isSetText() ) {
				cmt = report.getText().getValue();
			}
		}
		
		String finalPrice = "0";
		String orderPrice = "0";
		if(report.isSetLastPx()) {
			finalPrice = convertRateToString(report.getLastPx().getValue());
		} 
		if ( report.isSetPrice()) {
			orderPrice = convertRateToString(report.getPrice().getValue());
			cmt = cmt + " ordPrice=" + orderPrice;
		} 	
		
		if ( tagForOrderRef == quickfix.field.OrderID.FIELD ) {
			tradeExecutionStatusInfo.setRefId(orderId);
		} else if ( tagForOrderRef == quickfix.field.ExecID.FIELD ) {
			tradeExecutionStatusInfo.setRefId(execId);
		} else if ( tagForOrderRef == 8000 ) {
			tradeExecutionStatusInfo.setRefId(tag8000value);
		} 
		
		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(status);
		tradeExecutionStatusInfo.setFinalPrice(finalPrice);
		tradeExecutionStatusInfo.setSpotRate(finalPrice);
		tradeExecutionStatusInfo.setAllInPrice(finalPrice);
		tradeExecutionStatusInfo.setSymbol(symbol);
		if ( report.isSetCurrency() ) {
			tradeExecutionStatusInfo.setCurrency(report.getCurrency().getValue());
		}
		if ( tradeAction != null ) {
			tradeExecutionStatusInfo.setTradeAction(tradeAction);
		}
		
		double filledQty, requestQty;
		requestQty = report.getOrderQty().getValue();
		if ( report.isSetLastQty()) {
			filledQty = report.getLastQty().getValue();
		} else {
			filledQty = report.getOrderQty().getValue();
		}
		if ( Double.compare(filledQty, requestQty) != 0 ) {
			tradeExecutionStatusInfo.setSize(convertSizeToString(filledQty));
			tradeExecutionStatusInfo.setOriginalSize(convertSizeToString(requestQty));
		} else {
			tradeExecutionStatusInfo.setSize(convertSizeToString(filledQty));
			tradeExecutionStatusInfo.setOriginalSize(convertSizeToString(filledQty));
		}
		
		if ( report.isSetOrdRejReason() ) {
			cmt = report.getOrdRejReason().toString();
		} 
		if ( report.isSetTransactTime()) {
			tradeExecutionStatusInfo.setTransactTime(
					ChronologyUtil.getDateTimeSecString(ChronoConvUtil.convertDateTime(report.getTransactTime().getValue()))
				);
		}
		
		if ( report.isSetSettlDate() ) {
			tradeExecutionStatusInfo.setSettleDate( report.getSettlDate().getValue() );
		}
		
		if ( cmt != null ) {
			tradeExecutionStatusInfo.setAdditionalInfo(cmt);
		}
		
	}
	
	
	private void validateFullBook(FullBook.Builder fullBookBuilder) {
		int askTickCount = fullBookBuilder.getAskTicksCount();
		int bidTickCount = fullBookBuilder.getBidTicksCount();
		Tick.Builder minAskTick = null;
		Tick.Builder maxAskTick = null;
		Tick.Builder minBidTick = null;
		Tick.Builder maxBidTick = null;

		long minAskTickSize = Long.MAX_VALUE;
		long maxAskTickSize = Long.MIN_VALUE;
		long minBidTickSize = Long.MAX_VALUE;
		long maxBidTickSize = Long.MIN_VALUE;
		
		if ( askTickCount == 0 || bidTickCount == 0 ) {
			fullBookBuilder.clearAskTicks();
			fullBookBuilder.clearBidTicks();
			return;
		}
		
		HashSet<Long> askTickSizes = new HashSet<Long>();
		for ( int i = 0; i < askTickCount; i ++ ) {
			Tick.Builder t = fullBookBuilder.getAskTicksBuilder(i);
			askTickSizes.add(t.getSize());
			if ( t.getSize() < minAskTickSize) {
				minAskTick = t;
				minAskTickSize = t.getSize();
			}
			if ( t.getSize() > maxAskTickSize ) {
				maxAskTick = t;
				maxAskTickSize = t.getSize();
			}
		}
		HashSet<Long> bidTickSizes = new HashSet<Long>();
		for ( int i = 0; i < bidTickCount; i ++ ) {
			Tick.Builder t = fullBookBuilder.getBidTicksBuilder(i);
			bidTickSizes.add(t.getSize());
			if ( t.getSize() < minBidTickSize) {
				minBidTick = t;
				minBidTickSize = t.getSize();
			}
			if ( t.getSize() > maxBidTickSize ) {
				maxBidTick = t;
				maxBidTickSize = t.getSize();
			}
		}
		
		if ( askTickCount == 1 && bidTickCount == 1) {
			if ( fullBookBuilder.getAskTicks(0).getSize() > fullBookBuilder.getBidTicks(0).getSize()) {
				askTickSizes.remove(fullBookBuilder.getAskTicks(0).getSize());
				fullBookBuilder.getAskTicksBuilder(0).setSize(fullBookBuilder.getBidTicks(0).getSize());
				askTickSizes.add(fullBookBuilder.getBidTicks(0).getSize());
			} else if ( fullBookBuilder.getAskTicks(0).getSize() < fullBookBuilder.getBidTicks(0).getSize()) {
				bidTickSizes.remove(fullBookBuilder.getBidTicks(0).getSize());
				fullBookBuilder.getBidTicksBuilder(0).setSize(fullBookBuilder.getAskTicks(0).getSize());
				bidTickSizes.add(fullBookBuilder.getAskTicks(0).getSize());
			}
		}
		
		if ( addLargeDollarRung > 0L && !askTickSizes.contains(addLargeDollarRung)) {
			Tick.Builder newAskTick = Tick.newBuilder(maxAskTick.build());
			newAskTick.setSize(addLargeDollarRung);
			fullBookBuilder.addAskTicks( newAskTick);
			Tick.Builder newBidTick = Tick.newBuilder(maxBidTick.build());
			newBidTick.setSize(addLargeDollarRung);
			fullBookBuilder.addBidTicks(newBidTick);
		}
		
		if ( addSmallDollarRung > 0L && !askTickSizes.contains(addSmallDollarRung)) {
			Tick.Builder newAskTick = Tick.newBuilder(minAskTick.build());
			newAskTick.setSize(addSmallDollarRung);
			fullBookBuilder.addAskTicks(0, newAskTick);
			Tick.Builder newBidTick = Tick.newBuilder(minBidTick.build());
			newBidTick.setSize(addSmallDollarRung);
			fullBookBuilder.addBidTicks(0, newBidTick);
		}
	}
	
	
	
	@Override
	public void postValidate(FullBook.Builder fullBookBuilder, quickfix.Message src) {
		if ( this.isVWAPenabled 
				&& src instanceof quickfix.fix44.MarketDataSnapshotFullRefresh) {
			int askTickCount = fullBookBuilder.getAskTicksCount();
			int maxLevel = 0;
			HashSet<Long> askTickSizes = new HashSet<Long>(askTickCount);
			for ( int i = 0; i < askTickCount; i ++ ) {
				Tick.Builder t = fullBookBuilder.getAskTicksBuilder(i);
				askTickSizes.add(t.getSize());
				if ( t.getLevel() > maxLevel ) {
					maxLevel = t.getLevel();
				}
			}
			quickfix.fix44.MarketDataSnapshotFullRefresh refresh = (quickfix.fix44.MarketDataSnapshotFullRefresh) src;
 			try {
				int noMDEntries = refresh.getNoMDEntries().getValue();
				quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = null;
				double worstAsk = -1.0;
				double worstBid = -1.0;
				double bestAsk = -1.0;
				double bestBid = -1.0;
				for (int i = 1; i <= noMDEntries; i++) {
					noMDEntry = new quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries();
					refresh.getGroup(i, noMDEntry);

					switch (noMDEntry.getMDEntryType().getValue()) {
						case MDEntryType.BID:
							double dB = noMDEntry.getMDEntryPx().getValue();
							if (worstBid < 0 || dB < worstBid) {
								worstBid = dB;
							}
							if (bestBid < 0 || dB > bestBid) {
								bestBid = dB;
							}
							break;

						case MDEntryType.OFFER:
							double dO = noMDEntry.getMDEntryPx().getValue();
							if (worstAsk < 0 || dO > worstAsk) {
								worstAsk = dO;
							}
							if (bestAsk < 0 || dO < bestAsk) {
								bestAsk = dO;
							}
							break;

						default:
							break;
					}
				}
				if ( worstAsk > 0 && bestBid > 0
						&& addLargeDollarRung > 0L && !askTickSizes.contains(addLargeDollarRung) ) {
					int newLevel = maxLevel +1;
					
					Tick.Builder newAskTick = Tick.newBuilder();
					newAskTick.setSize(addLargeDollarRung);
					newAskTick.setRate(String.valueOf(worstAsk));
					newAskTick.setLevel(newLevel);
					fullBookBuilder.addAskTicks( newAskTick);
					Tick.Builder newBidTick = Tick.newBuilder();
					newBidTick.setSize(addLargeDollarRung);
					newBidTick.setRate(String.valueOf(worstBid));
					newBidTick.setLevel(newLevel);
					fullBookBuilder.addBidTicks(newBidTick);
				}
				if ( bestAsk > 0 && bestBid > 0
						&& addSmallDollarRung > 0L && !askTickSizes.contains(addSmallDollarRung) ) {
					Tick.Builder newAskTick = Tick.newBuilder();
					newAskTick.setLevel(0);
					newAskTick.setSize(addSmallDollarRung);
					newAskTick.setRate(String.valueOf(bestAsk));
					fullBookBuilder.addAskTicks( newAskTick);
					Tick.Builder newBidTick = Tick.newBuilder();
					newBidTick.setLevel(0);
					newBidTick.setSize(addSmallDollarRung);
					newBidTick.setRate(String.valueOf(bestBid));
					fullBookBuilder.addBidTicks(newBidTick);
				}
			} catch (FieldNotFound e) {

			}

			

		} else {
			validateFullBook(fullBookBuilder);
		}
	}

	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix44.ExecutionReport msgExeReport, Builder roeBuilder)
			throws FieldNotFound {
		return null;
	}

	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix50.ExecutionReport msgExeReport, Builder roeBuilder)
			throws FieldNotFound {
		return null;
	}

	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix44.OrderCancelReject rejectCancel, Builder roeBuilder)
			throws FieldNotFound {
		return null;
	}

	@Override
	public String convertRestOrderResponseAndUpdate(quickfix.fix50.OrderCancelReject rejectCancel, Builder roeBuilder)
			throws FieldNotFound {
		return null;
	}



}
