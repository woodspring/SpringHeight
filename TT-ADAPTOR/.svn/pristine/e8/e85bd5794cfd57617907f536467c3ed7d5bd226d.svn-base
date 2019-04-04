package com.tts.ske.app.feature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.CommonStruct.BuySellActionCd;
import com.tts.message.common.CommonStruct.SideCd;
import com.tts.message.market.MarketMarkerStruct.AddMarketAdjustmentLiquidity;
import com.tts.message.market.MarketMarkerStruct.CancelMarketMakingRequest;
import com.tts.message.market.MarketMarkerStruct.MarketMakingStatus;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.message.market.MarketStruct.RawLiquidityEntry;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.biz.calendar.ICachedFxCalendarBizServiceApi;
import com.tts.ske.app.DataFlowController;
import com.tts.ske.app.price.subscription.IMdSubscriber;
import com.tts.ske.support.BankLiquidityStore;
import com.tts.ske.vo.BankLiquidityAdjustmentVo;
import com.tts.ske.vo.BankLiquidityAdjustmentVo.OwnerInfo;
import com.tts.ske.vo.SessionInfoVo;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.util.flag.IndicativeFlag;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;


public class InternalLiquidityHandler {
	private static final NumberVo ZERO = NumberVo.getInstance("0");
	private final static BankLiquidityAdjustmentVo NO_BANK_LIQUIDITY_ADJUSTMENT = BankLiquidityStore.NO_BANK_LIQUIDITY_ADJUSTMENT;
	private static final Logger logger = LoggerFactory.getLogger(DataFlowController.class);
	private static final long MUST_NOTIFY_THRESHOLD = 300; //miliiseconds

	private final String symbol;

	private final IMsgSender ctrlMsgSender;
	private final IMsgSender marketDataSender;
	private final ICachedFxCalendarBizServiceApi fxCalendarBizService;
	private final List<IMdSubscriber> listeners = new CopyOnWriteArrayList<>();
	private final BankLiquidityStore bankLiquidityStore;
	private final SessionInfoVo sessionInfo;
	private volatile long instrumentIndicativeFlag = IndicativeFlag.TRADABLE;
	private volatile BankLiquidityAdjustmentVo bidAdjustment = NO_BANK_LIQUIDITY_ADJUSTMENT;
    private volatile BankLiquidityAdjustmentVo askAdjustment = NO_BANK_LIQUIDITY_ADJUSTMENT;
    private volatile RawMarketBook.Builder myLastQuote = null;
	private volatile boolean bidActive;
	private volatile boolean askActive;
	private long lastNotify;

	public InternalLiquidityHandler(String symbol, IMsgSender marketDataSender, IMsgSender ctrlMsgSender, ICachedFxCalendarBizServiceApi fxCalendarBizService, SessionInfoVo sessionInfo) {
		BankLiquidityStore bankLiquidityStore = AppContext.getContext().getBean(BankLiquidityStore.class);
		this.bidAdjustment = bankLiquidityStore.getBidLiquidity(symbol);
		this.askAdjustment = bankLiquidityStore.getAskLiquidity(symbol);
		this.bankLiquidityStore = bankLiquidityStore;
		this.symbol = symbol;
		this.ctrlMsgSender = ctrlMsgSender;
		this.marketDataSender = marketDataSender;	
		this.sessionInfo = sessionInfo;
		this.fxCalendarBizService = fxCalendarBizService;
	}

	public void onNewMarketData(RawMarketBook mb) {
		boolean bidActive = false, askActive = false;

		if ( listeners.size() > 0 ) {
			RawMarketBook.Builder myLiquidityBook =  RawMarketBook.newBuilder();
			if (mb.getBidQuoteCount() == 0 || mb.getAskQuoteCount() == 0) {
				return ;
			}
			
			double tobBidRate = mb.getBidQuote(0).getRate();
			double tobAskRate = mb.getAskQuote(0).getRate();
			double bidLimitPrice = bidAdjustment.getLimitPrice();
			double askLimitPrice = askAdjustment.getLimitPrice();
			double askBoundaryPrice = tobAskRate < askLimitPrice ? askLimitPrice : tobBidRate;

			if (sessionInfo.getGlobalIndicativeFlag() == 0L
					&& instrumentIndicativeFlag == 0L
					&& askAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT && tobAskRate > askLimitPrice){
				int tickCount = mb.getAskQuoteCount();
				for (int i = 0; i < tickCount; i++) {
					RawLiquidityEntry.Builder myQuote = null;
					RawLiquidityEntry t = mb.getAskQuote(i);
					if (t.getSize() <= askAdjustment.getSize()) {
						myQuote = RawLiquidityEntry.newBuilder(t);
						double newValue = DoubleFormatter.roundDouble(t.getRate() + askAdjustment.getAdjustment(), 5, RoundingMode.CEILING);
						if (newValue <askBoundaryPrice) {
							newValue = askBoundaryPrice;
						}
						myQuote.setRate(newValue);
						myQuote.setSize(t.getSize());

						myLiquidityBook.addAskQuote(myQuote);
					}
				}
				askActive = true;
			} else {
				int tickCount = mb.getAskQuoteCount();
				for (int i = 0; i < tickCount; ) {
					RawLiquidityEntry.Builder myQuote = null;
					RawLiquidityEntry t = mb.getAskQuote(i);
					myQuote = RawLiquidityEntry.newBuilder(t);
					myQuote.setRate(t.getRate());
					myQuote.setSize(0);
					myLiquidityBook.addAskQuote(myQuote);
					break;
				}
			}
			
			double bidBoundaryPrice = tobAskRate > bidLimitPrice ? bidLimitPrice : tobAskRate;
			if ( myLiquidityBook.getAskQuoteCount() > 0  ) {
				double newTobAsk = myLiquidityBook.getAskQuote(0).getRate();
				if ( bidBoundaryPrice > newTobAsk ) {
					bidBoundaryPrice = newTobAsk;
				}
			}

			if (sessionInfo.getGlobalIndicativeFlag() == 0L 
					&& instrumentIndicativeFlag == 0L
					&& bidAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT && tobBidRate < bidLimitPrice) {
				int tickCount = mb.getBidQuoteCount();
				for (int i = 0; i < tickCount; i++) {
					RawLiquidityEntry.Builder myQuote = null;
					RawLiquidityEntry t = mb.getBidQuote(i);
					if (t.getSize() <= bidAdjustment.getSize()) {
						myQuote = RawLiquidityEntry.newBuilder(t);
						double newValue =  DoubleFormatter.roundDouble(t.getRate() + bidAdjustment.getAdjustment(), 5, RoundingMode.FLOOR);
						if (newValue >bidBoundaryPrice) {
							newValue = bidBoundaryPrice;
						}
						myQuote.setRate(newValue);
						myQuote.setSize(t.getSize());
						myLiquidityBook.addBidQuote(myQuote);
					}
				}
				bidActive = true;
			} else {
				int tickCount = mb.getBidQuoteCount();
				for (int i = 0; i < tickCount; ) {
					RawLiquidityEntry.Builder myQuote = null;
					RawLiquidityEntry t = mb.getBidQuote(i);
					myQuote = RawLiquidityEntry.newBuilder(t);
					myQuote.setRate(t.getRate());
					myQuote.setSize(0);
					myLiquidityBook.addBidQuote(myQuote);
					break;
				}
			}


			myLiquidityBook.setSymbol(symbol);
			myLiquidityBook.setAdapter("INTERNAL");
			myLiquidityBook.setQuoteId("INTERNAL-" + mb.getQuoteId());

			RawMarketBook myLiquidityBookBuilt = myLiquidityBook.build();
			for (IMdSubscriber listener: listeners) {
				listener.onNewMarketData(symbol, myLiquidityBookBuilt);
			}
			synchronized (this) {
				this.myLastQuote = myLiquidityBook;
			}
		}
		this.bidActive = bidActive;
		this.askActive = askActive;
		notifyTraderPeriodically(true);
	}

	public void notifyTraderPeriodically(boolean forceNotify) {
		long currTime = System.currentTimeMillis();
		if ( forceNotify || (currTime - this.lastNotify) > MUST_NOTIFY_THRESHOLD ) {
			if (bidAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT) {
				MarketMakingStatus.Builder _marketMakingStatus = MarketMakingStatus.newBuilder();
				_marketMakingStatus.setSymbol(symbol);
				_marketMakingStatus.setAdjustment(formatRate(Math.abs(bidAdjustment.getAdjustment())));
				_marketMakingStatus.setSize(formatSize(bidAdjustment.getSize()));
				_marketMakingStatus.setLimitPrice(formatRate(bidAdjustment.getLimitPrice()));
				_marketMakingStatus.setBuySellActionCd(BuySellActionCd.BUY);
				if ( bidActive ) {
					_marketMakingStatus.setActiveFlag(1);
				} else {
					_marketMakingStatus.setActiveFlag(0);
				}
				if ( bidAdjustment.getOwnerInfo() != null ) {
					_marketMakingStatus.setOwner(bidAdjustment.getOwnerInfo().getOwnerName());
					_marketMakingStatus.setOwnerIntCustId(bidAdjustment.getOwnerInfo().getOwnerIntCustId());
					_marketMakingStatus.setOwnerIntAcctId(bidAdjustment.getOwnerInfo().getOwnerIntAcctId());
				}
				MarketMakingStatus marketMakingStatus = _marketMakingStatus.build();
				TtMsg ttMsg = TtMsgEncoder.encode(marketMakingStatus);
				String topic = String.format(IEventMessageTypeConstant.MarketMaking.MARKET_MAKING_STATUS_TEMPLATE, symbol, BuySellActionCd.BUY.toString());
				marketDataSender.send(topic, ttMsg);
			}
			if (askAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT) {
				MarketMakingStatus.Builder _marketMakingStatus = MarketMakingStatus.newBuilder();
				_marketMakingStatus.setSymbol(symbol);
				_marketMakingStatus.setAdjustment(formatRate(Math.abs(askAdjustment.getAdjustment())));
				_marketMakingStatus.setSize(formatSize(askAdjustment.getSize()));
				_marketMakingStatus.setLimitPrice(formatRate(askAdjustment.getLimitPrice()));
				_marketMakingStatus.setBuySellActionCd(BuySellActionCd.SELL);
				if ( askActive ) {
					_marketMakingStatus.setActiveFlag(1);
				} else {
					_marketMakingStatus.setActiveFlag(0);
				}
				if ( askAdjustment.getOwnerInfo() != null ) {
					_marketMakingStatus.setOwner(askAdjustment.getOwnerInfo().getOwnerName());
					_marketMakingStatus.setOwnerIntCustId(askAdjustment.getOwnerInfo().getOwnerIntCustId());
					_marketMakingStatus.setOwnerIntAcctId(askAdjustment.getOwnerInfo().getOwnerIntAcctId());
				}
				MarketMakingStatus marketMakingStatus = _marketMakingStatus.build();
				TtMsg ttMsg = TtMsgEncoder.encode(marketMakingStatus);
				String topic = String.format(IEventMessageTypeConstant.MarketMaking.MARKET_MAKING_STATUS_TEMPLATE, symbol, BuySellActionCd.SELL.toString());
				marketDataSender.send(topic, ttMsg);
			}
			this.lastNotify = System.currentTimeMillis();
		}
	}
	
	public void registerListener(IMdSubscriber l) {
		listeners.add(l);
	}
	
	public void unregisterListener(IMdSubscriber l) {
		listeners.remove(l);
	}
	
	
	private void adjustPriceRequest(final SideCd side, final double bankWantedSize,
			final double delta, final double limitPrice, final OwnerInfo ownerInfo) {

		if (side == SideCd.BID) {
			BankLiquidityAdjustmentVo bidAdjustment = new BankLiquidityAdjustmentVo(bankWantedSize, delta, limitPrice, ownerInfo);
			logger.info(String.format("New position for %s on BID = ", symbol) + formatSize(bankWantedSize));
			this.bidAdjustment = bidAdjustment;
			this.bankLiquidityStore.updateBidLiquidity(symbol, bidAdjustment);
		}
		if (side == SideCd.ASK) {
			BankLiquidityAdjustmentVo askAdjustment = new BankLiquidityAdjustmentVo(bankWantedSize, delta, limitPrice, ownerInfo);
			logger.info(String.format("New position for %s on ASK = ", symbol) + formatSize(bankWantedSize));
			this.askAdjustment = askAdjustment;
			this.bankLiquidityStore.updateAskLiquidity(symbol, askAdjustment);
		}
	}

	
	private void sendBankLiquidityClearNotification(BuySellActionCd buySellActionCd, String symbol) {
		MarketMakingStatus.Builder _marketMakingStatus = MarketMakingStatus.newBuilder();
		_marketMakingStatus.setSymbol(symbol);
		_marketMakingStatus.setAdjustment(formatRate(0d));
		_marketMakingStatus.setSize(formatSize(0d));
		_marketMakingStatus.setLimitPrice(formatRate(0d));
		_marketMakingStatus.setBuySellActionCd(buySellActionCd);
		_marketMakingStatus.setActiveFlag(0);
		MarketMakingStatus marketMakingStatus = _marketMakingStatus.build();
		TtMsg ttMsg = TtMsgEncoder.encode(marketMakingStatus);
		String topic = String.format(IEventMessageTypeConstant.MarketMaking.MARKET_MAKING_STATUS_TEMPLATE, symbol, buySellActionCd.toString());
		ctrlMsgSender.send(topic, ttMsg);
	}

	public static String formatSize(double d) {
		DecimalFormat df = new DecimalFormat("0.00");
		return df.format(d);
	}

	public static String formatRate(double d) {
		DecimalFormat df = new DecimalFormat("0.00000");
		return df.format(d);
	}

	public void submitInternalLiquidity(AddMarketAdjustmentLiquidity adjustmentLqy) {
		OwnerInfo ownerInfo = null;

		if (adjustmentLqy.hasOwner() && adjustmentLqy.hasOwnerIntAcctId()
				&& adjustmentLqy.hasOwnerIntCustId()) {
			ownerInfo = new OwnerInfo(adjustmentLqy.getOwner(), adjustmentLqy.getOwnerIntCustId(),
					adjustmentLqy.getOwnerIntAcctId());
		}
		double adjustment = adjustmentLqy.getAdjustment();
		SideCd side = adjustmentLqy.getSide();
		if ( side == SideCd.ASK) {
			adjustment = -1.0 * adjustment;
		}
		adjustPriceRequest( side, adjustmentLqy.getSize(),
				adjustment, adjustmentLqy.getLimitPrice(), ownerInfo);
	}

	public void cancelInternalLiquidity(CancelMarketMakingRequest request) {
		BuySellActionCd buySellActionCd = request.getBuySellActionCd();
		String symbol = request.getSymbol();
		if ( buySellActionCd == BuySellActionCd.BUY) {
			this.bidAdjustment = NO_BANK_LIQUIDITY_ADJUSTMENT;
			this.bankLiquidityStore.updateBidLiquidity(symbol, NO_BANK_LIQUIDITY_ADJUSTMENT);

			
		} else if ( buySellActionCd == BuySellActionCd.SELL) {
			this.askAdjustment = NO_BANK_LIQUIDITY_ADJUSTMENT;
			this.bankLiquidityStore.updateAskLiquidity(symbol, NO_BANK_LIQUIDITY_ADJUSTMENT);
		} 
		sendBankLiquidityClearNotification(buySellActionCd, symbol);		
	}

	public synchronized ExecutionReportInfo requestForHedge(
			String clientOrderId, 
			String symbol, 
			String notionalCurrency, 
			String tradeAction, 
			String currency1Amt, 
			String currency2Amt,
			String limitPrice,
			boolean isFOK, 
			boolean isExternal) {
		RawMarketBook.Builder myLastQuote = this.myLastQuote;
		boolean execOnTerm = symbol.indexOf(notionalCurrency) > 2;
		NumberVo execRate = NumberVo.getInstance("-1.0");
		double ccy1ToBeFillAmt = 0.0d;
		String notionalRequestedAmtStr = execOnTerm ? currency2Amt : currency1Amt;
		double notionalFillAmt = 0.0d;
		List<RawLiquidityEntry.Builder> myQuotes = null;
		NumberVo clientLimitPrice = NumberVo.getInstance(limitPrice);
		RawLiquidityEntry.Builder largestQuote = null;
		
		boolean isBuy = TradeConstants.TradeAction.BUY.equals(tradeAction );
		RoundingMode roundingMode = isBuy ? RoundingMode.CEILING : RoundingMode.FLOOR;
		if ( myLastQuote != null ) {
			if ( isBuy ) {
				myQuotes = myLastQuote.getAskQuoteBuilderList();
			} else {
				myQuotes = myLastQuote.getBidQuoteBuilderList();
			}
			for ( RawLiquidityEntry.Builder myQuote : myQuotes) {
				BigDecimal ccy1RequestSize = new BigDecimal(notionalRequestedAmtStr) ;
				if ( execOnTerm) {
					ccy1RequestSize = new BigDecimal(notionalRequestedAmtStr).divide(BigDecimal.valueOf(myQuote.getRate()), 2, RoundingMode.HALF_UP);
				}
				if (  new BigDecimal(myQuote.getSize()).compareTo( ccy1RequestSize ) >= 0 ) {
					execRate = NumberVo.getInstance(DoubleFormatter.convertToString(myQuote.getRate(), 5, roundingMode));
					ccy1RequestSize.setScale(2);
					ccy1ToBeFillAmt = ccy1RequestSize.doubleValue();
					logger.info("Current Market rate for clientOrderId< " + clientOrderId + " >: " + execRate.getValue() + " Rate valid for size up to " + myQuote.getSize());
					break;
				}
				largestQuote = myQuote;
			}
			
			if ( execRate.isLess(ZERO) && largestQuote == null ) {
				logger.info("No Match Market rate (largest) for clientOrderId< " + clientOrderId + " >!!");
			}
		} else {
			logger.info("No available market rate or liquidity avialable!!");
		}
		
		//IOC only, not enough liquidity scenario handling, checking for partial fill opportunity
		if (execRate.isLess(ZERO) && !isFOK && largestQuote != null) { 
			NumberVo largestQuoteRate = NumberVo.getInstance(DoubleFormatter.convertToString(largestQuote.getRate(), 5, roundingMode));
			logger.info("Preparing partial fill for clientOrderId< " + clientOrderId + " >: " + largestQuoteRate.getValue() + " Rate valid for size up to " + largestQuote.getSize());

			if ( isBuy ) {
				if ( largestQuoteRate.isLessOrEqual(clientLimitPrice) && largestQuote.getSize() > 0 ) {
					execRate = largestQuoteRate;
					ccy1ToBeFillAmt = new BigDecimal(largestQuote.getSize()).setScale(2).doubleValue();
				}
			} else {
				if ( largestQuoteRate.isGreaterOrEqual( clientLimitPrice) && largestQuote.getSize() > 0 ) {
					execRate = largestQuoteRate;
					ccy1ToBeFillAmt = new BigDecimal(largestQuote.getSize()).setScale(2).doubleValue();
				}
			}
		}
		
		boolean accept = false;
		StringBuilder cmtBuilder = new StringBuilder();

		if ( execRate.isGreater(ZERO) && ccy1ToBeFillAmt > 0) {
			if ( isBuy ) {
				if ( (!isExternal || execRate.isLessOrEqual(clientLimitPrice)) &&  ccy1ToBeFillAmt <= this.askAdjustment.getSize() ) {
					accept = true;
					cmtBuilder.append("Covered by Internal Liquidity submitted by " );
					cmtBuilder.append(this.askAdjustment.getOwnerInfo().getOwnerName());
					cmtBuilder.append(" at ");
					cmtBuilder.append(this.askAdjustment.getOwnerInfo().getCreationTime());
					double newSize = this.askAdjustment.getSize() - ccy1ToBeFillAmt;
					BankLiquidityAdjustmentVo old = this.askAdjustment;
					if ( isFOK && !execOnTerm ) {
						notionalFillAmt = ccy1ToBeFillAmt;
					} else if ( isFOK ) {
						notionalFillAmt = new BigDecimal(notionalRequestedAmtStr).doubleValue();
					} else if ( !isFOK && !execOnTerm ) {
						notionalFillAmt = ccy1ToBeFillAmt;
					}
					if ( newSize <= 0.0 ) {
						this.askAdjustment = NO_BANK_LIQUIDITY_ADJUSTMENT;
						sendBankLiquidityClearNotification(BuySellActionCd.SELL, symbol);

					} else {
						this.askAdjustment = new BankLiquidityAdjustmentVo(newSize, old.getAdjustment(), old.getLimitPrice(), old.getOwnerInfo());
					}
					this.bankLiquidityStore.updateAskLiquidity(symbol, this.askAdjustment);
				}
			} else {
				if ( (!isExternal || execRate.isGreaterOrEqual(clientLimitPrice)) && ccy1ToBeFillAmt <= this.bidAdjustment.getSize() ) {
					accept = true;
					cmtBuilder.append("Covered by Internal Liquidity submitted by " );
					cmtBuilder.append(this.bidAdjustment.getOwnerInfo().getOwnerName());
					cmtBuilder.append(" at ");
					cmtBuilder.append(this.bidAdjustment.getOwnerInfo().getCreationTime());
					double newSize = this.bidAdjustment.getSize() - ccy1ToBeFillAmt;
					BankLiquidityAdjustmentVo old = this.bidAdjustment;
					if ( isFOK && !execOnTerm ) {
						notionalFillAmt = ccy1ToBeFillAmt;
					} else if ( isFOK ) {
						notionalFillAmt = new BigDecimal(notionalRequestedAmtStr).doubleValue();
					} else if ( !isFOK && !execOnTerm ) {
						notionalFillAmt = ccy1ToBeFillAmt;
					}
					if ( newSize <= 0.0 ) {
						this.bidAdjustment = NO_BANK_LIQUIDITY_ADJUSTMENT;
						sendBankLiquidityClearNotification(BuySellActionCd.BUY, symbol);

					} else {
						this.bidAdjustment = new BankLiquidityAdjustmentVo(newSize, old.getAdjustment(), old.getLimitPrice(), old.getOwnerInfo());
					}
					this.bankLiquidityStore.updateBidLiquidity(symbol, this.bidAdjustment);
				}
			}
			logger.info("ClientOrderId< " + clientOrderId + " >: mkRate:" + execRate.getValue() + " px: " + limitPrice + " accept:" + accept);
		}
		String execRateStr = execRate.getValue();

		ExecutionReportInfo.Builder tradeExecutionStatusInfo = ExecutionReportInfo.newBuilder();
		tradeExecutionStatusInfo.setRefId(UUID.randomUUID().toString());

		tradeExecutionStatusInfo.setTransId(clientOrderId);
		tradeExecutionStatusInfo.setStatus(accept ? TransStateType.INTERNAL___COVERED_BY_INTERNAL_LIQUIDITY : TransStateType.TRADE_REJECT);
		tradeExecutionStatusInfo.setFinalPrice(execRateStr);
		tradeExecutionStatusInfo.setSpotRate(execRateStr);
		tradeExecutionStatusInfo.setAllInPrice(execRateStr);
		tradeExecutionStatusInfo.setSymbol(symbol);
		tradeExecutionStatusInfo.setCurrency(notionalCurrency);
		tradeExecutionStatusInfo.setTradeAction(tradeAction);
		tradeExecutionStatusInfo.setSize(DoubleFormatter.convertToString(notionalFillAmt, 2, RoundingMode.UNNECESSARY));
		tradeExecutionStatusInfo.setOriginalSize(notionalRequestedAmtStr);
		tradeExecutionStatusInfo.setTransactTime(ChronologyUtil
					.getDateTimeSecString(LocalDateTime.now()));

		String spotValueDate =fxCalendarBizService.getForwardTenorValueDate(symbol, TenorVo.NOTATION_SPOT);
		tradeExecutionStatusInfo.setSettleDate(spotValueDate);


		tradeExecutionStatusInfo.setAdditionalInfo(cmtBuilder.toString());
		
		ExecutionReportInfo execReport = tradeExecutionStatusInfo.build();
		return execReport;
	
	}
	
	public void addIndicativeReason(IndicativeFlag.IndicativeReason reason) {
		this.instrumentIndicativeFlag = IndicativeFlag.setIndicative(instrumentIndicativeFlag, reason);
	}
	
	public void removeIndicativeReason(IndicativeFlag.IndicativeReason reason) {
		this.instrumentIndicativeFlag = IndicativeFlag.removeIndicative(instrumentIndicativeFlag, reason);
	}
	
}
