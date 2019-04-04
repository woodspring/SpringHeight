package com.tts.plugin.adapter.impl.base.app.tradeexec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.constant.Constants.ProductType;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.message.trade.TradeMessage.TransactionDetail;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.QuoteStackRepo;
import com.tts.plugin.adapter.impl.base.app.trade.TradeQuoteRepo;
import com.tts.plugin.adapter.impl.base.vo.LatencyVo;
import com.tts.plugin.adapter.impl.base.vo.TradeRequestWithQuoteVo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;

public class TradeWithNewRfsQuoteHandler implements IFixListener {
	private final static Logger logger = LoggerFactory.getLogger(TradeWithNewRfsQuoteHandler.class);
	private final  IMkQfixApp qfixApp;
	private final Transaction transactionMessage;
	private final IResponseDialectHelper dialect;
	private final QuoteStackRepo quoteStackRepo;
	private final TradeQuoteRepo tradeQuoteRepo;
	private final ICertifiedPublishingEndpoint certifiedPublishingEndpoint;
	@SuppressWarnings("unused")
	private final boolean isCcy2;
	
	private volatile boolean  sent = false;
	private volatile int quoteCount = 0;
	private QuoteSide side;
	
	public TradeWithNewRfsQuoteHandler(Transaction transactionMessage, IMkQfixApp qfixApp, IResponseDialectHelper dialect, LatencyVo latencyRecord, ICertifiedPublishingEndpoint certifiedPublishingEndpoint) {
		this.qfixApp = qfixApp;
		this.transactionMessage = transactionMessage;
		this.dialect = dialect;
		this.certifiedPublishingEndpoint = certifiedPublishingEndpoint;
		TransactionDetail transactionDetail = transactionMessage.getNearDateDetail();
//		IFxCalendarBizService fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
//		String tradeDate = transactionDetail.getTradeDate();
//		String valueDate = transactionDetail.getValueDate();
		
		String tradeDirection = transactionMessage.getTradeAction(); 
		if ( TradeConstants.TradeAction.BUY.equals(tradeDirection)) {
			tradeDirection = TradeConstants.TradeAction.BUY;
		} else if ( TradeConstants.TradeAction.SELL.equals(tradeDirection)) {
			tradeDirection = TradeConstants.TradeAction.SELL;
		}
		
		//fxCalendarBizService.
		QuoteSide side = null;
				
		
		if ( TradeConstants.TradeAction.BUY.equals(tradeDirection)) {
			side = QuoteSide.BUY;
		} else if ( TradeConstants.TradeAction.SELL.equals(tradeDirection)) {
			side = QuoteSide.SELL;
		} else {
			side = QuoteSide.BOTH;
		}
		
		this.tradeQuoteRepo = AppContext.getContext().getBean(TradeQuoteRepo.class);
		this.quoteStackRepo = AppContext.getContext().getBean(QuoteStackRepo.class);
		this.isCcy2 = transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) >= 2;
		this.side = side;
		
		if ( !transactionMessage.hasQuoteRefId()) {
			logger.debug("Created Trade handler for " + transactionMessage.getTransId() + ". Sending quote...");

			qfixApp.sendRfsRequest(
					NumberVo.getInstance(transactionDetail.getCurrency1Amt()).getLongValueFloored(), 
					transactionMessage.getSymbol(), 
					transactionMessage.getSymbol().substring(0 ,3),
					TenorVo.NOTATION_SPOT, 
					transactionDetail.getValueDate(), 
					side, 
					60L, 
					this, AppType.BANKFIXTRADEADAPTER);
		} else {
			logger.debug("Created Trade handler for " + transactionMessage.getTransId() + ". Execute Order with quote...");

			String finalTradeDirection = determineTradeDirection();
			QuoteVo quote = doLookUpQuote(transactionMessage.getOrderParams().getQuoteRefId());
			if ( quote == null ) {
				logger.error("QuoteRefId not found: " + quoteStackRepo.dump());
				
				rejectTrade();
				
			} else {
				logger.debug(transactionMessage.getTransId() + " " + transactionMessage.getOrderParams().getQuoteRefId() + " quote=" + quote + " q1=" + quote.getOrigQuote1() + " q2=" + quote.getOrigQuote2() );

				execTradeWithQuote(finalTradeDirection, quote);
			}
			
		}
	}

	private void rejectTrade() {
		ExecutionReportInfo.Builder executionReport = ExecutionReportInfo.newBuilder();

		String transId = transactionMessage.getTransId();

		String symbol = transactionMessage.getSymbol();
		String currency = transactionMessage.getNotionalCurrency();

		executionReport.setTransId(transId);
		executionReport.setSymbol(symbol);
		executionReport.setCurrency(currency);

		executionReport.setStatus(TransStateType.TRADE_REJECT);
		executionReport.setAdditionalInfo("price/quote not found");
				
		
		logger.warn("Rejecting trade<" + transId + ">. Not able to find price/quote. Possible latency.");
		logger.debug(String.format("Sending trade report for %s, %s", executionReport.getTransId(), TextFormat.shortDebugString(executionReport)));
		certifiedPublishingEndpoint.publish(IEventMessageTypeConstant.AutoCover.TRADE_STATUS_FROM_MR_EVENT, executionReport.build());
		
	}

	private QuoteVo doLookUpQuote(String quoteRefId) {
		int delimiterIdx = quoteRefId.indexOf(IFixConstants.DEFAULT_DELIMITER);
		String quoteReqId = quoteRefId.substring(0, delimiterIdx);
		String quoteId = quoteRefId.substring(
				delimiterIdx + IFixConstants.DEFAULT_DELIMITER.length(), 
				quoteRefId.length());
		logger.debug(transactionMessage.getTransId() + " doLookUpQuote: quoteRefId=" + quoteRefId  + "; quoteReqId=" + quoteReqId + "; quoteId=" + quoteId);

		return quoteStackRepo.findQuote(quoteReqId, quoteId);
	}

	@Override
	public void onMessage(Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		String tradeDirection = determineTradeDirection();
		if (!sent &&  message instanceof quickfix.fix50.Quote) {
			logger.debug("Trade handler for " + transactionMessage.getTransId() + " received quote...");
			quoteCount++;
			
			if (quoteCount < 2 ) return;
			QuoteVo quote = dialect.convert((quickfix.fix50.Quote) message);
			execTradeWithQuote(tradeDirection, quote);
			
		}
		
	}

	private String determineTradeDirection() {
		String tradeDirection = transactionMessage.getTradeAction(); 
		if ( TradeConstants.TradeAction.BUY.equals(tradeDirection)) { 
			tradeDirection = TradeConstants.TradeAction.BUY;
		} else if ( TradeConstants.TradeAction.SELL.equals(tradeDirection)) {
			tradeDirection = TradeConstants.TradeAction.SELL;
		}
		return tradeDirection;
	}

	private void execTradeWithQuote(String tradeDirection, QuoteVo quote) {
		TransactionDetail transactionDetail = transactionMessage.getNearDateDetail();
		String quoteId = quote.getQuoteId(), price = null, tradeNotionalCurrency = null, amount = null;
		String priceFar = null, amountFar = null, settleDateFar = null;
		tradeNotionalCurrency = transactionMessage.getNotionalCurrency();
		boolean execOnTerm = transactionMessage.getSymbol().indexOf(tradeNotionalCurrency) > 1;


		if ( TradeAction.BUY.equals(tradeDirection)) {
			price = quote.getOfferPx();

		} else if ( TradeAction.SELL.equals(tradeDirection)) {
			price = quote.getBidPx();
		} else if ( TradeAction.BUY_AND_SELL.equals(tradeDirection)) {
			price = quote.getOfferPx();
			priceFar = quote.getBidPx2();
			settleDateFar = quote.getSettleDate2() == null ? transactionMessage.getFarDateDetail().getValueDate() : quote.getSettleDate2();

		} else if ( TradeAction.SELL_AND_BUY.equals(tradeDirection)) {
			price = quote.getBidPx();
			priceFar = quote.getOfferPx();
			settleDateFar = quote.getSettleDate2() == null ? transactionMessage.getFarDateDetail().getValueDate() : quote.getSettleDate2();
		}
		String settleDate = quote.getSettleDate() == null ? transactionDetail.getValueDate() : quote.getSettleDate();

		if ( settleDate.length() == 16) {
			if ( side == QuoteSide.BUY) {
				settleDate = settleDate.substring(8);
			} else {
				settleDate = settleDate.substring(0, 8);
			}
		}
		logger.debug(transactionMessage.getTransId() + " origSettleDate=" + quote.getSettleDate() + " side=" + side + " settleDate=" + settleDate);

		if ( ProductType.FXSWAP.equals(transactionMessage.getProduct()) ) {
			if ( execOnTerm ) {
				amount = transactionMessage.getNearDateDetail().getCurrency2Amt();
				amountFar = transactionMessage.getFarDateDetail().getCurrency2Amt();
			} else {
				amount = transactionMessage.getNearDateDetail().getCurrency1Amt();
				amountFar = transactionMessage.getFarDateDetail().getCurrency1Amt();
			}
			if ( TradeConstants.TradeAction.BUY_AND_SELL.equals(tradeDirection)) {
				side = QuoteSide.SWAP__BUY_AND_SELL;
			} else if ( TradeConstants.TradeAction.SELL_AND_BUY.equals(tradeDirection)) {
				side = QuoteSide.SWAP__SELL_AND_BUY;
			} 
		} else if ( execOnTerm ) {
			amount = transactionMessage.getNearDateDetail().getCurrency2Amt();

			if ( TradeConstants.TradeAction.BUY.equals(tradeDirection)) {
				side = QuoteSide.SELL;
			} else if ( TradeConstants.TradeAction.SELL.equals(tradeDirection)) {
				side = QuoteSide.BUY;
			} 

		} else {
			amount = transactionMessage.getNearDateDetail().getCurrency1Amt();
		}
		

		String comment = null;
		if ( ProductType.FXTIMEOPTION.equals(transactionMessage.getProduct()) ) {
			String nearDate = transactionMessage.getTimeOptStartDate();
			String farDate = transactionMessage.getTimeOptEndDate();

			logger.debug(transactionMessage.getTransId() + " settleDate=" + settleDate + " q1=" + quote.getOrigQuote1() + " q2=" + quote.getOrigQuote2());
			if ( quote.getOrigQuote1() != null && quote.getOrigQuote2() != null) {
				//send the unused quote (QuoteID) as comment
				if (settleDate.equals(quote.getOrigQuote2().getSettleDate()) ) {
					comment =  quote.getOrigQuote1().getQuoteId();
					quoteId = quote.getOrigQuote2().getQuoteId();
					quote = quote.getOrigQuote2();
				} else 	if (settleDate.equals(quote.getOrigQuote1().getSettleDate()) ) {
					comment =  quote.getOrigQuote2().getQuoteId();
					quoteId = quote.getOrigQuote1().getQuoteId();
					quote = quote.getOrigQuote1();
				} 
 
				if ( comment.indexOf("%%%") > 0) {
					String[] s = comment.split("%%%");
					comment = s[ s.length -1 ];
				}

			}
			if ( !settleDate.contains(nearDate)) {
				settleDate = settleDate + "-" + nearDate;
			} else if ( !settleDate.contains(farDate)) {
				settleDate = settleDate + "-" + farDate;
			}
		}
		tradeQuoteRepo.associateQuote(transactionMessage.getTransId(), new TradeRequestWithQuoteVo(transactionMessage, quote));

		String product = transactionMessage.getProduct();
		
		if ( quote.getProduct() != null ) {
			product = quote.getProduct();
		}
		sent = qfixApp.sendTradeExecRequest(
			product,
			price, 
			amount,
			tradeNotionalCurrency,
			transactionMessage.getSymbol(),
			TenorVo.NOTATION_SPOT,
			settleDate,
			priceFar,
			amountFar,
			null, //tenorFar
			settleDateFar,
			null,
			side,
			transactionMessage.getTransId(),
			quoteId,
			AppType.FIXTRADEADAPTER, 
			quote.getTransactTime(),
			qfixApp.getExecutionReportListener(),
			"",
			comment);
		logger.debug("Sent Order for transId<" + transactionMessage.getTransId() + ">");

	}

	@Override
	public void onFixSessionLogoff() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onFixSessionLogon() {
		// TODO Auto-generated method stub
	}
}
