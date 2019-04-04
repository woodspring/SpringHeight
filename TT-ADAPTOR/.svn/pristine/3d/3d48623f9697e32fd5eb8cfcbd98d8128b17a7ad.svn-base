package com.tts.mde.ems;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.mde.algo.IMDPriceAndExceAlgo;
import com.tts.mde.algo.IMDPriceAndExceAlgo.BuySellActionCd;
import com.tts.mde.data.TransacationService;
import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.IMrSubscriptionHandler;
import com.tts.mde.spot.impl.MrSubscriptionHandlerManager;
import com.tts.mde.spot.impl.VwapByPriceAggressive;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IInstrumentDetailProvider;
import com.tts.mde.support.impl.SessionInfoVo;
import com.tts.mde.vo.IMDProvider;
import com.tts.mde.vo.ITradingParty;
import com.tts.mde.vo.LiquidityProviderVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.TradeMessage.BankTransaction;
import com.tts.message.trade.TradeMessage.BankTransactionSummary;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.message.trade.TradeMessage.TransactionSummary;
import com.tts.message.trade.TradeRoutingContextStruct.TransactionRoutingOrder;
import com.tts.message.ui.client.constant.ClientResponseType;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.biz.transactions.vo.TransactionVo;
import com.tts.service.sa.transaction.TransWrapper;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TransStateConstants;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.vo.CounterPartyVo;
import com.tts.vo.currency.MoneyVo;

public class ExecutionManagementService {
	private static final Logger logger = LoggerFactory.getLogger(ExecutionManagementService.class);

	private final ConcurrentHashMap<String, Transaction> outstanding = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ExecutionContextVo> outstandingContext = new ConcurrentHashMap<>();

	private final TransacationService transactionService;
	private final IFxCalendarBizServiceApi fxCalendarBizService;
	private final IMsgSender ctrlMsgSender;
	private final IMsgSender aceReportSender;
	private final IInstrumentDetailProvider instrumentDetailProvider;
	private final SessionInfoVo sessionInfo;

	public ExecutionManagementService(IFxCalendarBizServiceApi fxCalendarBizService, SessionInfoVo sessionInfo,
			IInstrumentDetailProvider instrumentDetailProvider) {
		IMsgSenderFactory msgSenderFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		IMsgSender aceReportSender = msgSenderFactory.getMsgSender(false, false, true);
		aceReportSender.init();

		IMsgSender ctrlMsgSender = msgSenderFactory.getMsgSender(false, false, false);

		ctrlMsgSender.init();
		
		this.sessionInfo = sessionInfo;
		this.instrumentDetailProvider = instrumentDetailProvider;
		this.ctrlMsgSender = ctrlMsgSender;
		this.aceReportSender = aceReportSender;
		this.fxCalendarBizService = fxCalendarBizService;

		this.transactionService = AppContext.getContext().getBean(TransacationService.class);
	}
	
	public void handleExecutionResult(ExecutionReportInfo executionReportInfo) {
		logger.info("ExecutionReport: " + TextFormat.shortDebugString(executionReportInfo));
		String transId = executionReportInfo.getTransId();
		ExecutionContextVo context = outstandingContext.get(transId);
		if (context != null) {
			handleExecutionResult(executionReportInfo, context);
			return;
		}
		Transaction tranaction = outstanding.get(transId);
		com.tts.message.trade.TradeMessage.Transaction.Builder update = tranaction.toBuilder();

		String status = executionReportInfo.getStatus();
		update.setStatus(status);

		if (executionReportInfo.hasProvider() && executionReportInfo.getProvider().length() > 0) {
			LiquidityProviderVo c = (LiquidityProviderVo) sessionInfo.getMDproviderBySourceNm(executionReportInfo.getProvider());
			update.setProviderAccountId(Long.toString(c.getFxSPOTCounterParty().getAccountId())).setProviderAccountNm(c.getFxSPOTCounterParty().getAccountNm())
					.setProviderAccountType("LP").setProviderCustomerId(Long.toString(c.getFxSPOTCounterParty().getCustomerId()))
					.setProviderCustomerNm(c.getFxSPOTCounterParty().getCustomerNm());
		}
		String ccy2Amt = null;
		if (TransStateType.TRADE_PARTIALLY_DONE.equals(status) || TransStateType.TRADE_DONE.equals(status)) {
			ccy2Amt = new MoneyVo(update.getNearDateDetail().getCurrency1Amt(),
					Currency.getInstance(executionReportInfo.getSymbol().substring(3, 6)))
							.multiply(executionReportInfo.getFinalPrice()).getValue();
			update.getNearDateDetailBuilder().setAllInCoreRate(executionReportInfo.getAllInPrice());
			update.getNearDateDetailBuilder().setSpotRate(executionReportInfo.getSpotRate());
			update.getNearDateDetailBuilder().setTradeRate(executionReportInfo.getFinalPrice());
			update.getNearDateDetailBuilder().setCurrency2Amt(ccy2Amt);
		}

		update.setTimeInForceCode("FOK");
		update.setOrderTypeCode("LIMIT");
		update.setStatusMessage(executionReportInfo.getAdditionalInfo());
		updateTransactions(Arrays.asList(new Transaction[] { update.build() }));
		if (TransStateConstants.TransStateType.TRADE_DONE.equals(update.getStatus())) {
			TransactionRoutingOrder.Builder trOrderBuilder = TransactionRoutingOrder.newBuilder();
			trOrderBuilder.setNotPersisted(true);
			trOrderBuilder.setTransaction(update);
			trOrderBuilder.setTradingSessionId(sessionInfo.getTradingSessionId());
			TtMsg ttMsg = TtMsgEncoder.encode(trOrderBuilder.build());
			ctrlMsgSender.send(IEventMessageTypeConstant.ClientTrader.TRADE_ROUTING_EVENT, ttMsg);
		}
	}

	private void handleExecutionResult(ExecutionReportInfo executionReportInfo, ExecutionContextVo context) {
		TransactionWithLPWrapperVo w = context.findTransaction(executionReportInfo.getTransId());
		TransactionVo update = w.getTransaction();

		String status = executionReportInfo.getStatus();
		update.setStatus(status);

		if (executionReportInfo.hasProvider() && executionReportInfo.getProvider().length() > 0) {
			CounterPartyVo c = w.getCounterparty();
			update.setProviderAccountId(Long.toString(c.getAccountId()));
			update.setProviderAccountNm(c.getAccountNm());
			update.setProviderAccountType("LP");
			update.setProviderCustomerId(Long.toString(c.getCustomerId()));
			update.setProviderCustomerNm(c.getCustomerNm());
		}
		String ccy2Amt = null;
		if (TransStateType.TRADE_PARTIALLY_DONE.equals(status) || TransStateType.TRADE_DONE.equals(status)) {
			ccy2Amt = new MoneyVo(update.getNearDateDetail().getCurrency1Amt(),
					Currency.getInstance(executionReportInfo.getSymbol().substring(3, 6)))
							.multiply(executionReportInfo.getFinalPrice()).getValue();
			update.getNearDateDetail().setAllInCoreRate(executionReportInfo.getAllInPrice());
			update.getNearDateDetail().setSpotRate(executionReportInfo.getSpotRate());
			update.getNearDateDetail().setTradeRate(executionReportInfo.getFinalPrice());
			update.getNearDateDetail().setCurrency2Amt(ccy2Amt);
		}

		update.setTimeInForceCode("FOK");
		update.setOrderTypeCode("LIMIT");
		update.setStatusMessage(executionReportInfo.getAdditionalInfo());
		updateTransactions(Arrays.asList(new Transaction[] { update.toMessage() }));
		if (TransStateConstants.TransStateType.TRADE_DONE.equals(update.getStatus())) {
			TransactionRoutingOrder.Builder trOrderBuilder = TransactionRoutingOrder.newBuilder();
			trOrderBuilder.setNotPersisted(true);
			trOrderBuilder.setTransaction(update.toMessage());
			trOrderBuilder.setTradingSessionId(sessionInfo.getTradingSessionId());
			TtMsg ttMsg = TtMsgEncoder.encode(trOrderBuilder.build());
			ctrlMsgSender.send(IEventMessageTypeConstant.ClientTrader.TRADE_ROUTING_EVENT, ttMsg);
		}

		boolean allCompleted = true;
		BigDecimal filledAmt = BigDecimal.valueOf(0.0d);

		for (TransactionWithLPWrapperVo wt : context.getAllTransactions()) {
			boolean completed = false;
			boolean ternNotional = wt.getTransaction().getSymbol()
					.indexOf(wt.getTransaction().getNotionalCurrency()) > 0;

			if (TransStateConstants.TransStateType.TRADE_DONE.equals(wt.getTransaction().getStatus())
					|| TransStateConstants.TransStateType.TRADE_CANCEL.equals(wt.getTransaction().getStatus())
					|| TransStateConstants.TransStateType.TRADE_REJECT.equals(wt.getTransaction().getStatus())) {
				completed = true;
				if (TransStateConstants.TransStateType.TRADE_DONE.equals(wt.getTransaction().getStatus())) {
					if (ternNotional) {
						filledAmt = filledAmt
								.add(new BigDecimal(wt.getTransaction().getNearDateDetail().getCurrency2Amt()));
					} else {
						filledAmt = filledAmt
								.add(new BigDecimal(wt.getTransaction().getNearDateDetail().getCurrency1Amt()));
					}
				}
			}
			allCompleted = allCompleted && completed;
		}

		if (allCompleted && !context.isUseOrgTransaction()) {

			if (filledAmt.compareTo(
					new BigDecimal(context.getCorrespondingTransaction().getNearDateDetail().getCurrency1Amt())) == 0) {
				context.getCorrespondingTransaction().setStatus(TransStateConstants.TransStateType.TRADE_COMPLETED);
			} else {
				context.getCorrespondingTransaction()
						.setStatus(TransStateConstants.TransStateType.TRADE_PARTIALLY_CANCELLED);
			}
			updateTransactions(Arrays.asList(new Transaction[] { context.getCorrespondingTransaction().toMessage() }));
			broadcastTransaction(context.getCorrespondingTransaction().toMessage(), true, false);

			if ("autocover".equals(context.getCorrespondingTransaction().getRequestType())) {
				ExecutionReportInfo.Builder tradeExecutionStatusInfo = ExecutionReportInfo.newBuilder();
				tradeExecutionStatusInfo.setRefId(UUID.randomUUID().toString());

				tradeExecutionStatusInfo.setTransId(context.getCorrespondingTransaction().getTransId());
				tradeExecutionStatusInfo.setStatus(TransStateConstants.TransStateType.TRADE_DONE);
				tradeExecutionStatusInfo.setFinalPrice("0.00");// TODO
				tradeExecutionStatusInfo.setSpotRate("0.00");// TODO
				tradeExecutionStatusInfo.setAllInPrice("0.00");// TODO
				tradeExecutionStatusInfo.setSymbol(context.getCorrespondingTransaction().getSymbol());
				tradeExecutionStatusInfo.setCurrency(context.getCorrespondingTransaction().getNotionalCurrency());
				tradeExecutionStatusInfo.setTradeAction(context.getCorrespondingTransaction().getTradeAction());
				tradeExecutionStatusInfo
						.setSize(context.getCorrespondingTransaction().getNearDateDetail().getCurrency1Amt());
				tradeExecutionStatusInfo
						.setOriginalSize(context.getCorrespondingTransaction().getNearDateDetail().getCurrency1Amt());
				tradeExecutionStatusInfo.setTransactTime(ChronologyUtil.getDateTimeSecString(LocalDateTime.now()));
				tradeExecutionStatusInfo.setAdditionalInfo("");
				tradeExecutionStatusInfo.setProvider("AGG");
				ExecutionReportInfo execReport = tradeExecutionStatusInfo.build();
				TtMsg ttMsg = TtMsgEncoder.encode(execReport);
				aceReportSender.send(IEventMessageTypeConstant.AutoCover.TRADE_STATUS_FROM_MR_EVENT, ttMsg);

			}
		}
	}

	public void handleExecution(Transaction transaction, MrSubscriptionHandlerManager mrSubscriptionHandlerManager) {
		boolean isBuy = TradeConstants.TradeAction.BUY.equals(transaction.getTradeAction());
		LiquidityProviderVo providerParty = findCounterParty(transaction, null);
		if (!providerParty.isInternalTradingParty()) {
			String specificLP = transaction.getOrderParams().getSpecificLP(0);
			LiquidityProviderVo c = providerParty;
			com.tts.message.trade.TradeMessage.Transaction update = transaction.toBuilder()
					.setStatus(TransStateConstants.TransStateType.TRADE_PENDING)
					.setProviderAccountId(Long.toString(c.getFxSPOTCounterParty().getAccountId())).setProviderAccountNm(c.getFxSPOTCounterParty().getAccountNm())
					.setProviderAccountType("LP").setProviderCustomerId(Long.toString(c.getFxSPOTCounterParty().getCustomerId()))
					.setProviderCustomerNm(c.getFxSPOTCounterParty().getCustomerNm()).build();
			broadcastTransaction(update, true, false);

			String handlerId = transaction.hasQuoteRefId() ? transaction.getQuoteRefId()
					: transaction.getOrderParams().getQuoteRefId();
			IMrSubscriptionHandler h = mrSubscriptionHandlerManager.findHandlerById(handlerId);

			if (h == null) {
				cancelTransaction(transaction,  "unable to find matching quote /AggHandler not found");
			} else {
				ILiquidityPool lp = h.getLiquidityPool();
				RawLiquidityVo[] myQuotes = null;
				if (isBuy) {
					myQuotes = lp.getAskLqy(false);
				} else {
					myQuotes = lp.getBidLqy(false);
				}
				if (myQuotes != null && myQuotes.length > 0) {
					doSpecificLpFokEx(transaction, specificLP, isBuy, myQuotes, c);
				}

			}
		} else {

			
			String handlerId = transaction.getOrderParams().hasQuoteRefId()
					? transaction.getOrderParams().getQuoteRefId() : transaction.getQuoteRefId();
			IMrSubscriptionHandler h = mrSubscriptionHandlerManager.findHandlerById(handlerId);

			doVmapIocEx(transaction, isBuy, h);
		}
	}

	private void updateTransactions(List<Transaction> transactions) {
		int i = 0;
		for (Transaction transaction : transactions) {
			broadcastTransaction(transaction, i == 0, false);
			if (TransStateConstants.TransStateType.TRADE_CANCEL.equals(transaction.getStatus())
					|| TransStateConstants.TransStateType.TRADE_REJECT.equals(transaction.getStatus())) {
				transactionService.updateTrade(Long.parseLong(transaction.getTransId()), transaction.getStatus(), true,
						transaction.getStatusMessage(), null, null, null, null, null, null, false, 1L);
			} else {
				transactionService.updateTrade(Long.parseLong(transaction.getTransId()), transaction.getStatus(), false,
						transaction.getStatusMessage(), transaction.getNearDateDetail().getSpotRate(), "0.0",
						transaction.getNearDateDetail().getTradeRate(),
						transaction.getNearDateDetail().getCurrency1Amt(),
						transaction.getNearDateDetail().getCurrency2Amt(), transaction.getTransRef(), false, 1L);
			}
			i++;
		}
	}

	private void cancelTransaction(Transaction transaction, String cmt) {
		com.tts.message.trade.TradeMessage.Transaction.Builder cancelupdate = transaction.toBuilder()
				.setStatus(TransStateConstants.TransStateType.TRADE_CANCEL).setStatusMessage(cmt);

		broadcastTransaction(cancelupdate.build(), true, false);
		transactionService.updateTrade(Long.parseLong(cancelupdate.getTransId()),
				TransStateConstants.TransStateType.TRADE_CANCEL, true, null, null, null, null, null, null, null, false,
				1L);
	}

	private void doVmapIocEx(Transaction transaction, boolean isBuy, IMrSubscriptionHandler originatingHandler) {
		int precison = instrumentDetailProvider.getInstrumentDetail(transaction.getSymbol()).getPrecision();
		long quantity = new BigDecimal(transaction.getNearDateDetail().getCurrency1Amt()).longValue();
		double quantityDD = new BigDecimal(transaction.getNearDateDetail().getCurrency1Amt()).doubleValue();

		double limitPrice = -1.0;
		if (transaction.hasOrderParams() && transaction.getOrderParams().hasTargetPrice()) {
			limitPrice = new BigDecimal(transaction.getOrderParams().getTargetPrice()).doubleValue();
		}
		ILiquidityPool lp = originatingHandler.getLiquidityPool();
		RawLiquidityVo[] myQuotes = null;
		ArrayList<RawLiquidityVo> quotes = null;
		com.tts.mde.vo.AggPxVo currentVwapPrice = null;
		IMDPriceAndExceAlgo aggAlgo = originatingHandler.getMdeAggAlgo();

		if (isBuy) {
			myQuotes = lp.getAskLqy(false);
			quotes = new ArrayList<>(Arrays.asList(myQuotes));
			VwapByPriceAggressive.sortTickByPrice(quotes, !isBuy);
			currentVwapPrice = aggAlgo.getVwapPrice(quotes, BuySellActionCd.BUY, quantity, precison, sessionInfo.getMarketMode());

		} else {
			myQuotes = lp.getBidLqy(false);
			quotes = new ArrayList<>(Arrays.asList(myQuotes));
			VwapByPriceAggressive.sortTickByPrice(quotes, !isBuy);
			currentVwapPrice = aggAlgo.getVwapPrice(quotes, BuySellActionCd.SELL, quantity, precison, sessionInfo.getMarketMode());
		}

		if (currentVwapPrice == null) {
			cancelTransaction(transaction,  "unable to find matching quote");
			return;
		}
		System.out.println(currentVwapPrice.shortDebugString());
		ArrayList<WeightedExecutionRateVo> plannedExecutionList = new ArrayList<>();
		for (int i = 0; i < currentVwapPrice.getOrgQuotes().length; i++) {
			if (currentVwapPrice.getOrgQuotes()[i] != null) {
				plannedExecutionList.add(new WeightedExecutionRateVo(currentVwapPrice.getOrgQuotes()[i],
						currentVwapPrice.getWeights()[i]));
			}
		}
		if (plannedExecutionList.size() == 1) {
			if (plannedExecutionList.get(0).getAmt() < quantityDD) {
				RawLiquidityVo quote = plannedExecutionList.get(0).getRate();
				plannedExecutionList.clear();
				plannedExecutionList.add(new WeightedExecutionRateVo(quote, quantityDD));
			}
		}

		Collections.sort(plannedExecutionList);
		if (!isBuy) {
			Collections.reverse(plannedExecutionList);
		}

		ExecutionContextVo context = new ExecutionContextVo(false, TransactionVo.fromMessage(transaction),
				currentVwapPrice, plannedExecutionList);
		executeContext(context, isBuy);
	}

	private void executeContext(ExecutionContextVo context, boolean isBuy) {
		double plannedVwapPrice = 0.0d;
		double executedAmt = 0.0d;
		double quantity = new BigDecimal(context.getCorrespondingTransaction().getNearDateDetail().getCurrency1Amt())
				.doubleValue();
		double limitPrice = -1.0;
		TransactionVo transaction = (TransactionVo) context.getCorrespondingTransaction();
		if (transaction.getOrderParams().getTargetPrice() != null) {
			limitPrice = new BigDecimal(transaction.getOrderParams().getTargetPrice()).doubleValue();
		}
		int filledCount = 0;
		List<WeightedExecutionRateVo> plannedExecutionList = context.plannedExecutionList;
		for (int i = 0; i < plannedExecutionList.size(); i++) {
			RawLiquidityVo orgQuote = plannedExecutionList.get(i).getRate();
			double weight = plannedExecutionList.get(i).getAmt();
			plannedVwapPrice = (plannedVwapPrice * executedAmt + orgQuote.getRate() * weight) / (executedAmt + weight);

			if ("IOC".equals(context.getCorrespondingTransaction().getOrderParams().getAlgoOrdType())
					&& (((isBuy && plannedVwapPrice > limitPrice)) || (!isBuy && plannedVwapPrice < limitPrice))) {
				System.out.println("VWAP LIMIT PRICE HIT");
				if (filledCount == 0) {
					cancelTransaction(transaction.toMessage(),
							"market rate moved. order price: " + limitPrice + " current vwap price: "
									+ DoubleFormatter.convertToString(context.getVwapRateAtExecution().getVwapPrice(),
											5, isBuy ? RoundingMode.CEILING : RoundingMode.FLOOR));
					return;
				} else {
					TransactionVo cancelAmtTransaction = transaction.deepClone();
					cancelAmtTransaction.setParentTransId(Long.parseLong(transaction.getTransId()));
					cancelAmtTransaction.setStatus(TransStateConstants.TransStateType.TRADE_CANCEL);
					double remindingAmt = quantity - executedAmt;
					cancelAmtTransaction.getNearDateDetail().setCurrency1Amt(new MoneyVo("" + remindingAmt,
							Currency.getInstance(cancelAmtTransaction.getSymbol().substring(0, 3))).getValue());
					TransWrapper w = transactionService.submitTradeFull(cancelAmtTransaction,
							sessionInfo.getTradingSessionId(), 1L, Long.parseLong(transaction.getTransId()), null,
							"market rate moved. order stopped", sessionInfo.getInternalTradingParties().get(0).getFxSPOTCounterParty(),
							null, "VirtualBankTrader", 1L);
					cancelAmtTransaction.setTransId("" + w.getTrade().getPk());
					cancelAmtTransaction.setProviderAccountId("0");
					cancelAmtTransaction.setProviderAccountNm("BANK AGG");
					cancelAmtTransaction.setProviderAccountType("LP");
					cancelAmtTransaction.setProviderCustomerId("0");
					cancelAmtTransaction.setProviderCustomerNm("BANK");
					broadcastTransaction(cancelAmtTransaction.toMessage(), false, false);
				}
				break;
			}
			filledCount++;
			executedAmt += weight;
			ITradingParty c = (ITradingParty) sessionInfo.getMDproviderBySourceNm(orgQuote.getLiquidityProviderSrc());
			TransactionVo executingTransaction = context.isUseOrgTransaction() ? transaction : transaction.deepClone();
			int precison = instrumentDetailProvider.getInstrumentDetail(executingTransaction.getSymbol())
					.getPrecision();

			if (!context.isUseOrgTransaction()) {
				executingTransaction.setTransId(null);
			}
			executingTransaction.setParentTransId(Long.parseLong(transaction.getTransId()));
			executingTransaction.setStatus(TransStateConstants.TransStateType.TRADE_PENDING);
			executingTransaction.setProviderAccountId(Long.toString(c.getFxSPOTCounterParty().getAccountId()));
			executingTransaction.setProviderAccountNm(c.getFxSPOTCounterParty().getAccountNm());
			executingTransaction.setProviderAccountType("LP");
			executingTransaction.setProviderCustomerId(Long.toString(c.getFxSPOTCounterParty().getCustomerId()));
			executingTransaction.setProviderCustomerNm(c.getFxSPOTCounterParty().getCustomerNm());
			int dec = "JPY".equals(executingTransaction.getSymbol().substring(0, 3)) ? 0 : 2;
			executingTransaction.getNearDateDetail()
					.setCurrency1Amt(DoubleFormatter.convertToString(weight, dec, RoundingMode.HALF_EVEN));
			executingTransaction.getOrderParams()
					.setSpecificLP(Arrays.asList(new String[] { orgQuote.getLiquidityProviderSrc() }));

			TransWrapper w = null;
			if (!context.isUseOrgTransaction()) {
				w = transactionService.submitTradeFull(executingTransaction, sessionInfo.getTradingSessionId(), 1L,
						Long.parseLong(transaction.getTransId()), null, "", c.getFxSPOTCounterParty(), null, "",
						transaction.getUserId() > 0 ? transaction.getUserId() : 1L);
//				w = transactionService.submitTradePartI(executingTransaction, sessionInfo.getTradingSessionId(), 1L,
//						Long.parseLong(transaction.getTransId()), null, "", c.getCounterPartyVo(), null, "",
//						transaction.getUserId() > 0 ? transaction.getUserId() : 1L);
				executingTransaction.setTransId("" + w.getTrade().getPk());
			}
			System.out.println(TextFormat.shortDebugString(executingTransaction.toMessage()));
			context.addTransaction(new TransactionWithLPWrapperVo(w, executingTransaction, c.getFxSPOTCounterParty(),
					Long.parseLong(context.getCorrespondingTransaction().getTransId())));
			outstandingContext.put(executingTransaction.getTransId(), context);
			sendToFA(executingTransaction.toMessage(), orgQuote, precison);
			broadcastTransaction(executingTransaction.toMessage(), false, false);
		}
//		if (!context.isUseOrgTransaction()) {
//			for (TransactionWithLPWrapperVo hc : context.getAllTransactions()) {
//				transactionService.submitTradePartII(hc.getTransWrapper(), hc.getTransaction(),
//						sessionInfo.getTradingSessionId(), 1L, hc.getParentId(), null, "", hc.getCounterparty(),
//						hc.getTransaction().getUserId() > 0 ? transaction.getUserId() : 1L, null);
//			}
//		}
	}

	private void doSpecificLpFokEx(Transaction transaction, String specificLP, boolean isBuy, RawLiquidityVo[] myQuotes,
			IMDProvider c) {
		double ccy1size = new BigDecimal(transaction.getNearDateDetail().getCurrency1Amt()).doubleValue();
		double limitPrice = transaction.getNearDateDetail().hasTradeRate()
				? new BigDecimal(transaction.getNearDateDetail().getTradeRate()).doubleValue()
				: new BigDecimal(transaction.getOrderParams().getTargetPrice()).doubleValue();
		RawLiquidityVo exQuote = null;
		double currRate = -0.1d;

		for (int i = 0; i < myQuotes.length; i++) {
			RawLiquidityVo quote = myQuotes[i];
			if (isBuy) {
				if (specificLP.equals(quote.getLiquidityProviderSrc())) {
					if (ccy1size <= quote.getSize()) {
						if (quote.getRate() <= limitPrice) {
							System.out.println("FULL:" + quote.getRate() + " " + quote.getSize());
							exQuote = quote;
						} else {
							currRate = quote.getRate();
						}
						break;
					}
				}
			} else {
				if (specificLP.equals(quote.getLiquidityProviderSrc())) {
					if (ccy1size <= quote.getSize()) {
						if (quote.getRate() >= limitPrice) {
							System.out.println("FULL:" + quote.getRate() + " " + quote.getSize());
							exQuote = quote;
						} else {
							currRate = quote.getRate();
						}
						break;
					}
				}
			}
		}

		if (exQuote != null) {
			// int precison =
			// instrumentDetailProvider.getInstrumentDetail(transaction.getSymbol()).getPrecision();
			//
			// Transaction.Builder pendingTransaction = transaction.toBuilder();
			// pendingTransaction.getNearDateDetailBuilder().setTradeRate(DoubleFormatter.convertToString(exQuote.getRate(),
			// precison, isBuy ? RoundingMode.CEILING : RoundingMode.FLOOR));
			// pendingTransaction.getNearDateDetailBuilder().setSpotRate(DoubleFormatter.convertToString(exQuote.getSpotRate(),
			// precison, isBuy ? RoundingMode.CEILING : RoundingMode.FLOOR));
			// pendingTransaction.getNearDateDetailBuilder().setForwardPoints(DoubleFormatter.convertToString(exQuote.getForwardPts(),
			// precison, isBuy ? RoundingMode.CEILING : RoundingMode.FLOOR));
			// Transaction _pendingTransaction = pendingTransaction.build();
			// outstanding.put(transaction.getTransId(), _pendingTransaction);
			// sendToFA(_pendingTransaction, exQuote, precison);
			executeContext(
					new ExecutionContextVo(true, TransactionVo.fromMessage(transaction),
							new com.tts.mde.vo.AggPxVo(exQuote.getRate(), new long[] { (long) ccy1size }, myQuotes),
							Arrays.asList(
									new WeightedExecutionRateVo[] { new WeightedExecutionRateVo(exQuote, ccy1size) })),
					isBuy);
		} else {
			logger.debug("quote not found");
		}
	}

	private void sendToFA(Transaction transaction, RawLiquidityVo exQuote, int precision) {
		Transaction.Builder pendingTransaction = transaction.toBuilder();
		pendingTransaction.getNearDateDetailBuilder()
				.setTradeRate(DoubleFormatter.convertToString(exQuote.getRate(), precision, RoundingMode.UNNECESSARY));
		pendingTransaction.getOrderParamsBuilder().setQuoteRefId(exQuote.getQuoteId());

		Transaction pendingTransactionBuilt = pendingTransaction.build();
		TtMsg ttMsg = TtMsgEncoder.encode(pendingTransactionBuilt);
		ctrlMsgSender.send("TTS.TRAN.FX.MR.TRANINFO.FA." + exQuote.getProviderAdapter(), ttMsg);
		logger.debug("send to FA: TTS.TRAN.FX.MR.TRANINFO.FA." + exQuote.getProviderAdapter() + ": "
				+ TextFormat.shortDebugString(pendingTransactionBuilt));
	}

	private void doSpecificLpIocEx(Transaction transaction, String specificLP, boolean isBuy, RawLiquidityVo[] myQuotes,
			ITradingParty c) {
		double ccy1size = new BigDecimal(transaction.getNearDateDetail().getCurrency1Amt()).doubleValue();
		double limitPrice = new BigDecimal(transaction.getNearDateDetail().getTradeRate()).doubleValue();
		RawLiquidityVo exQuote = null;
		RawLiquidityVo altQuote = null;

		double currRate = -0.1d;

		for (int i = 0; i < myQuotes.length; i++) {
			RawLiquidityVo quote = myQuotes[i];
			if (isBuy) {
				if (specificLP.equals(quote.getLiquidityProviderSrc())) {
					if (ccy1size <= quote.getSize()) {
						if (quote.getRate() <= limitPrice) {
							System.out.println("FULL:" + quote.getRate() + " " + quote.getSize());
							exQuote = quote;
						} else {
							currRate = quote.getRate();
						}
						break;
					} else {
						altQuote = quote;
					}
				}
			} else {
				if (specificLP.equals(quote.getLiquidityProviderSrc())) {
					if (ccy1size <= quote.getSize()) {
						if (quote.getRate() >= limitPrice) {
							System.out.println("FULL:" + quote.getRate() + " " + quote.getSize());
							exQuote = quote;
						} else {
							currRate = quote.getRate();
						}
						break;
					} else {
						altQuote = quote;
					}
				}
			}
		}
		if (exQuote != null) {
			String execRate = "" + exQuote.getRate();
			System.out.println("FULL:" + execRate + " " + exQuote.getSize());
			com.tts.message.trade.TradeMessage.Transaction.Builder update = transaction.toBuilder()
					.setStatus(TransStateConstants.TransStateType.TRADE_DONE)
					.setProviderAccountId(Long.toString(c.getFxSPOTCounterParty().getAccountId())).setProviderAccountNm(c.getFxSPOTCounterParty().getAccountNm())
					.setProviderAccountType("LP").setProviderCustomerId(Long.toString(c.getFxSPOTCounterParty().getCustomerId()))
					.setProviderCustomerNm(c.getFxSPOTCounterParty().getCustomerNm());
			String ccy2Amt = new MoneyVo(update.getNearDateDetail().getCurrency1Amt(),
					Currency.getInstance(transaction.getSymbol().substring(3, 6))).multiply(execRate).getValue();
			update.getNearDateDetailBuilder().setAllInCoreRate(execRate);
			update.getNearDateDetailBuilder().setSpotRate(execRate);
			update.getNearDateDetailBuilder().setTradeRate(execRate);
			update.getNearDateDetailBuilder().setCurrency2Amt(ccy2Amt);
			update.setTimeInForceCode("IOC");
			update.setOrderTypeCode("LIMIT");
			update.setStatusMessage("Order rate: " + limitPrice);
			updateTransactions(Arrays.asList(new Transaction[] { update.build() }));
		} else {
			if (altQuote != null) {
				if (isBuy) {
					if (altQuote.getRate() <= limitPrice) {

					}
				} else {
					if (altQuote.getRate() >= limitPrice) {

					}
				}
			} else {
				cancelTransaction(transaction,  "market rate moved. current rate:" + currRate);
			}
		}
	}
	private LiquidityProviderVo findCounterParty(Transaction t, String srcNm) {
		if ( srcNm != null) {
			IMDProvider p = sessionInfo.getMDproviderBySourceNm(srcNm);
			if ( p != null ) {
				return (LiquidityProviderVo) p;
			}
		}
		
		for ( LiquidityProviderVo counterParty : sessionInfo.getInternalTradingParties() ) {
			if ( t.getProviderAccountId().equals("" + counterParty.getFxSPOTCounterParty().getAccountId())
					&& t.getProviderCustomerId().equals("" + counterParty.getFxSPOTCounterParty().getCustomerId())) {
				if ( srcNm != null && counterParty.getSourceNm().equals(srcNm) ) { 
					return counterParty;
				}
					
				if ( srcNm == null ) {
					return counterParty;
				}
			}
		}
		for ( IMDProvider mdProvider : sessionInfo.getMDprovidersExternal() ) {
			if ( mdProvider instanceof LiquidityProviderVo ) {
				LiquidityProviderVo counterParty = (LiquidityProviderVo) mdProvider;
				if ( t.getProviderAccountId().equals("" + counterParty.getFxSPOTCounterParty().getAccountId())
						&& t.getProviderCustomerId().equals("" + counterParty.getFxSPOTCounterParty().getCustomerId())) {
					if ( srcNm != null && counterParty.getSourceNm().equals(srcNm) ) { 
						return counterParty;
					}
						
					if ( srcNm == null ) {
						return counterParty;
					}
				}
			}
		}
	
		return null;
	}
	
	private void broadcastTransaction(Transaction transaction, boolean isUpdate, boolean isClientTrader) {
		try {
			logger.info("Send broadcast transaction to BT." + TextFormat.shortDebugString(transaction));

			String msgType = ClientResponseType.ADD_TRANSACTION_ENTRY;
			if (isUpdate)
				msgType = ClientResponseType.UPDATE_TRANSACTION_ENTRY;

			TransactionSummary.Builder transSummBuilder = TransactionSummary.newBuilder();
			transSummBuilder.addTransactions(transaction);

			BankTransactionSummary.Builder bankTransSummBuilder = BankTransactionSummary.newBuilder();
			// bankTransSummBuilder.setUserSessionInfo(userSession.getUserSessionInfo());
			BankTransaction.Builder bTransBuilder = BankTransaction.newBuilder();
			bTransBuilder.setTransaction(transaction);
			bankTransSummBuilder.addTransaction(bTransBuilder.build());

			TtMsg ttMsg = TtMsgEncoder.encode(msgType, bankTransSummBuilder.build());

			ctrlMsgSender.send(IEventMessageTypeConstant.BankTrader.TRADE_EVENT, ttMsg);

			if (isClientTrader) {
				logger.info("Send to " + IEventMessageTypeConstant.ClientTrader.TRADE_EVENT);
				ttMsg = TtMsgEncoder.encode(msgType, transSummBuilder.build());
				ctrlMsgSender.send(IEventMessageTypeConstant.ClientTrader.TRADE_EVENT, ttMsg);
			}
		} catch (Exception ex) {
			logger.error("Unable to broadcast transaction", ex);
		} finally {
			// safeClose(msgSender);
		}
	}

	private static class TransactionWithLPWrapperVo {
		private final TransactionVo transaction;
		private final TransWrapper transWrapper;
		private final CounterPartyVo counterparty;
		private final long parentId;

		private TransactionWithLPWrapperVo(TransWrapper w, TransactionVo newTransaction, CounterPartyVo c,
				long parentId) {
			super();
			this.transWrapper = w;
			this.transaction = newTransaction;
			this.counterparty = c;
			this.parentId = parentId;
		}

		public TransWrapper getTransWrapper() {
			return transWrapper;
		}

		public TransactionVo getTransaction() {
			return transaction;
		}

		public CounterPartyVo getCounterparty() {
			return counterparty;
		}

		public long getParentId() {
			return parentId;
		}

	}

	private static class ExecutionContextVo {
		private final boolean useOrgTransaction; // correspondingTransaction ==
													// relatedTransactions.get(0)
		private final com.tts.mde.vo.AggPxVo vwapRateAtExecution;
		private final List<WeightedExecutionRateVo> plannedExecutionList;
		private final TransactionVo correspondingTransaction;
		private final List<TransactionWithLPWrapperVo> relatedTransactions = new ArrayList<>();

		private ExecutionContextVo(boolean useOrgTransaction, TransactionVo correspondingTransaction,
				com.tts.mde.vo.AggPxVo vwapRateAtExecution, List<WeightedExecutionRateVo> plannedExecution) {
			super();
			this.vwapRateAtExecution = vwapRateAtExecution;
			this.plannedExecutionList = plannedExecution;
			this.correspondingTransaction = correspondingTransaction;
			this.useOrgTransaction = useOrgTransaction;
		}

		public void addTransaction(TransactionWithLPWrapperVo relatedTransaction) {
			relatedTransactions.add(relatedTransaction);
		}

		public TransactionWithLPWrapperVo findTransaction(String transId) {
			for (TransactionWithLPWrapperVo t : relatedTransactions) {
				if (transId.equals(t.getTransaction().getTransId())) {
					return t;
				}
			}
			return null;
		}

		public List<TransactionWithLPWrapperVo> getAllTransactions() {
			return relatedTransactions;
		}

		public com.tts.mde.vo.AggPxVo getVwapRateAtExecution() {
			return vwapRateAtExecution;
		}

		public List<WeightedExecutionRateVo> getPlannedExecutionList() {
			return plannedExecutionList;
		}

		public TransactionVo getCorrespondingTransaction() {
			return correspondingTransaction;
		}

		public boolean isUseOrgTransaction() {
			return useOrgTransaction;
		}

	}

	private static class WeightedExecutionRateVo implements Comparable<WeightedExecutionRateVo> {
		private final RawLiquidityVo rate;
		private final double amt;

		private WeightedExecutionRateVo(RawLiquidityVo rate, double amt) {
			super();
			this.rate = rate;
			this.amt = amt;
		}

		public RawLiquidityVo getRate() {
			return rate;
		}

		public double getAmt() {
			return amt;
		}

		@Override
		public int compareTo(WeightedExecutionRateVo o) {
			return Double.compare(this.getRate().getRate(), o.getRate().getRate());
		}

	}

}
