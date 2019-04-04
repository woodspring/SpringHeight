package com.tts.plugin.adapter.impl.base.app.tradeexec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.constant.Constants;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.message.trade.TradeMessage.TransactionDetail;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.impl.base.app.trade.TradeQuoteRepo;
import com.tts.plugin.adapter.impl.base.vo.LatencyVo;
import com.tts.plugin.adapter.impl.base.vo.TradeRequestWithQuoteVo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IMasGlobolSequenceProvider;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IEspRepo.FullBookSrcWrapper;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.constant.TradeConstants;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;

public class TradeWithExistingEspQuoteIdHandler  {
	private static final String REJECT_REASON__PRICE_QUOTE_NOT_FOUND = "price/quote not found";
	private static final String REJECT_REASON__QUOTEID_NOT_MATCH = "QuoteId not match";
	private static final String REJECT_REASON__PRICE_NOT_MATCH = "Price not match";
	private static final String LOGMSG__UNABLE_TO_FIND_S_QUOTE_WITH_ID_S_REQUEST_SEQEUNCE_S_CURRENT_SEQUENCE_S_EXISTING_QUOTE_REF_IDS_S = "Unable to find %s quote with id<%s>. Request Seqeunce: %s. Current Sequence: %s. Existing QuoteRefIds: %s";
	private static final Logger logger = LoggerFactory.getLogger(TradeWithExistingEspQuoteIdHandler.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	private final  IMkQfixApp qfixApp;
	private final Transaction transactionMessage;
	private final TradeQuoteRepo tradeQuoteRepo;
	private final IResponseDialectHelper dialect;
	private final LatencyVo latencyRecord;
	private final ICertifiedPublishingEndpoint certifiedPublishingEndpoint;
	private final boolean isCcy2;

	@SuppressWarnings("unused")
	private volatile boolean sent;
	private QuoteSide side;
	
	public TradeWithExistingEspQuoteIdHandler(
			Transaction transactionMessage, 
			IMkQfixApp qfixApp, 
			IEspRepo<?> espRepo, 
			IMasGlobolSequenceProvider sequenceProvider,
			IResponseDialectHelper dialect, 
			LatencyVo latencyRecord, 
			ICertifiedPublishingEndpoint certifiedPublishingEndpoint,
			boolean quotePriceCheck) {
		this.qfixApp = qfixApp;
		this.transactionMessage = transactionMessage;
		this.isCcy2 = transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) >= 2;

		@SuppressWarnings("unused")
		TransactionDetail transactionDetail = transactionMessage.getNearDateDetail();
		
		String tradeDirection = transactionMessage.getTradeAction(); 
		if ( TradeConstants.TradeAction.BUY.equals(tradeDirection)) {
			tradeDirection = TradeConstants.TradeAction.BUY;
		} else if ( TradeConstants.TradeAction.SELL.equals(tradeDirection)) {
			tradeDirection = TradeConstants.TradeAction.SELL;
		}
		
		QuoteSide side = null;
		
		if ( (TradeConstants.TradeAction.BUY.equals(tradeDirection) && !this.isCcy2 )
				|| (TradeConstants.TradeAction.SELL.equals(tradeDirection) && this.isCcy2)  ) {
			side = QuoteSide.BUY;
		} else if ( ( TradeConstants.TradeAction.SELL.equals(tradeDirection) && !this.isCcy2 )
				|| (TradeConstants.TradeAction.BUY.equals(tradeDirection) && this.isCcy2) ) {
			side = QuoteSide.SELL;
		} else {
			side = QuoteSide.BOTH;
		}
		
		this.tradeQuoteRepo = AppContext.getContext().getBean(TradeQuoteRepo.class);
		this.side = side;
		this.dialect = dialect;
		this.latencyRecord = latencyRecord;
		this.certifiedPublishingEndpoint = certifiedPublishingEndpoint;
		logger.debug("Created Trade handler for " + transactionMessage.getTransId() + ". Execute market order with quote...");

		String finalTradeDirection = determineTradeDirection();
		QuoteVoWrapper quoteWrapper = null;
		try {
			quoteWrapper = doLookUpQuote(espRepo, sequenceProvider);
		} catch (Exception e) {
			
		}
		if ( quoteWrapper == null || quoteWrapper.getQuote() == null ) {
			rejectTrade(REJECT_REASON__PRICE_QUOTE_NOT_FOUND);
		} else if ( !this.transactionMessage.getOrderParams().getQuoteRefId().contains(quoteWrapper.getQuote().getQuoteId()) ) {
			logger.error("QuoteID not match, " + this.transactionMessage.getOrderParams().getQuoteRefId() + " " + quoteWrapper.getQuote().getQuoteId());
			rejectTrade(REJECT_REASON__QUOTEID_NOT_MATCH);
		} else if ( quotePriceCheck && !validatePrice(quoteWrapper.getQuote() , quoteWrapper.getFullBookBid(), quoteWrapper.getFullBookAsk())) {
			rejectTrade(REJECT_REASON__PRICE_NOT_MATCH);
			String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logError("MktOrderValidation", topic, MonitorConstant.FXADT.ERROR_MESSAGE_REJECTED, 
					"Trade Rejected due to unable to match ESP price, id = " + this.transactionMessage.getTransId() );
		} else {
			quoteWrapper.getQuote() .setQuoteId( this.transactionMessage.getOrderParams().getQuoteRefId());
			execTradeWithQuote(finalTradeDirection, quoteWrapper.getQuote()  );
		}
	}

	private boolean validatePrice(QuoteVo quote, String fullBookBid, String fullBookAsk) {
		return fullBookBid != null && fullBookAsk != null
				&& fullBookAsk.equals(quote.getOfferPx()) && fullBookBid.equals(quote.getBidPx());
	}

	private void rejectTrade(String reason) {
		ExecutionReportInfo.Builder executionReport = ExecutionReportInfo.newBuilder();

		String transId = transactionMessage.getTransId();

		String symbol = transactionMessage.getSymbol();
		String currency = transactionMessage.getNotionalCurrency();

		executionReport.setTransId(transId);
		executionReport.setSymbol(symbol);
		executionReport.setCurrency(currency);

		executionReport.setStatus(TransStateType.TRADE_REJECT);
		executionReport.setAdditionalInfo(reason);
				
		
		logger.warn("Rejecting trade<" + transId + ">. " + reason);
		logger.debug(String.format("Sending trade report for %s, %s", executionReport.getTransId(), TextFormat.shortDebugString(executionReport)));
		certifiedPublishingEndpoint.publish(IEventMessageTypeConstant.AutoCover.TRADE_STATUS_FROM_MR_EVENT, executionReport.build());
		
	}

	private QuoteVoWrapper doLookUpQuote(
			IEspRepo<?> espRepo, IMasGlobolSequenceProvider sequenceProvider ) {
		String selectedAskTickRate = null, selectedBidTickRate = null;

		FullBookSrcWrapper<?> p = null;
		String symbol = transactionMessage.getSymbol();

		if ( Constants.ProductType.FXSPOT.equals(transactionMessage.getProduct()) ){
			p = espRepo.findPriceBySymbolTenorQuoteId(
					transactionMessage.getSymbol(), 
					TenorVo.NOTATION_SPOT, 
					transactionMessage.getOrderParams().getQuoteRefId());
		} else {
			p = espRepo.findPriceBySymbolTenorQuoteId(
					transactionMessage.getSymbol(), 
					TenorVo.NOTATION_TODAY, 
					transactionMessage.getOrderParams().getQuoteRefId());
		}
		
		if ( p == null ) {
			FullBookSrcWrapper<?>[] allRepoFb = null;
			if ( Constants.ProductType.FXFORWARDS.equals(transactionMessage.getProduct()) ){
				allRepoFb = espRepo.findPriceBySymbolTenor(symbol, TenorVo.NOTATION_TODAY);
			} else {
				allRepoFb = espRepo.findPriceBySymbolTenor(symbol, TenorVo.NOTATION_SPOT);
			}
			StringBuilder sb = new StringBuilder();
			for ( FullBookSrcWrapper<?> fbW : allRepoFb) {
				if ( fbW.getFullBook() != null && fbW.getFullBook().getQuoteRefId().equals(transactionMessage.getOrderParams().getQuoteRefId())) {
					p = fbW;
					break;
				}
				if ( fbW.getFullBook() != null && fbW.getFullBook().hasSequence()  ) {
					sb.append(fbW.getFullBook().getSequence()).append(',');	
				} 
				if ( fbW.getFullBook() != null ) {
					sb.append(fbW.getFullBook().getQuoteRefId()).append(' ');
				}
			}
			if ( p == null ) {
				logger.warn(String.format(LOGMSG__UNABLE_TO_FIND_S_QUOTE_WITH_ID_S_REQUEST_SEQEUNCE_S_CURRENT_SEQUENCE_S_EXISTING_QUOTE_REF_IDS_S, 
						symbol, 
						transactionMessage.getOrderParams().getQuoteRefId(), 
						transactionMessage.getSequence(),
						sequenceProvider.getCurrentSequence(),
						sb.toString()));
			}
		}
		String tradeDirection = determineTradeDirection();
		QuoteVo q = null;

		if ( p != null ) {
			boolean tradeOnCcy2 = transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) > 2;
			NumberVo tradeSize = null, ccy1TradeSize = null;
			long ccy1TradeSizeLong = -1L;
			if ( tradeOnCcy2) {
				tradeSize = NumberVo.getInstance(transactionMessage.getNearDateDetail().getCurrency2Amt());
			} else {
				tradeSize = NumberVo.getInstance(transactionMessage.getNearDateDetail().getCurrency1Amt());
				ccy1TradeSize = tradeSize;
				ccy1TradeSizeLong = ccy1TradeSize.getLongValueCeiled();
			}
			
			logger.debug("Orig FIX message: " + p.getSource());
			FullBook.Builder fb = p.getFullBook();
			int numOfRung = fb.getAskTicksCount();
			Tick selectedAskTick = null, selectedBidTick = null;
			
			for (int i = 0; i < numOfRung; i++ ) {
				long size = fb.getAskTicks(i).getSize();
				//NumberVo sizeVo = NumberVo.getInstance(size + ".00");
				if (tradeOnCcy2) {
					if ( TradeAction.BUY.equals(tradeDirection)) {
						ccy1TradeSize = tradeSize.divide(fb.getAskTicks(i).getRate());
						ccy1TradeSizeLong = ccy1TradeSize.getLongValueCeiled();
					} else {
						ccy1TradeSize = tradeSize.divide(fb.getBidTicks(i).getRate());
						ccy1TradeSizeLong = ccy1TradeSize.getLongValueCeiled();
					}
				}
				if ( ccy1TradeSizeLong <= size) {
					selectedAskTick = fb.getAskTicks(i);
					selectedBidTick = fb.getBidTicks(i);
					break;
				}
			}
			
			if ( selectedAskTick != null && selectedBidTick != null ) {
				try {
					String msg = String.format("trade<%s>, ccy1size<%d> matches rate with <%s, %s> rungSize=%s, fb=%s, source=%s",
								transactionMessage.getTransId(), 
								ccy1TradeSizeLong, 
								selectedBidTick.getRate(), selectedAskTick.getRate(), 
								selectedAskTick.getSize(), 
								TextFormat.shortDebugString(fb),
								p.getSource());
					logger.debug(msg);
					Object qfixMessage = p.getSource();
					if ( qfixMessage instanceof quickfix.fix50.MarketDataSnapshotFullRefresh) {
						q = dialect.convert((quickfix.fix50.MarketDataSnapshotFullRefresh) qfixMessage, selectedAskTick.getSize());
					} else if ( qfixMessage instanceof quickfix.fix44.MarketDataSnapshotFullRefresh) {
						q = dialect.convert((quickfix.fix44.MarketDataSnapshotFullRefresh) qfixMessage, selectedAskTick.getSize());
					}
					
					//q.setQuoteId(transactionMessage.getQuoteRefId());
					selectedBidTickRate = selectedBidTick.getRate();
					selectedAskTickRate = selectedAskTick.getRate();
				} catch (FieldNotFound e) {

				}
			} 

		}
		return new QuoteVoWrapper(q, selectedBidTickRate, selectedAskTickRate);
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
		String quoteId = quote.getQuoteId(), price = null, currency = null, amount = null;
		 
		if ( TradeAction.BUY.equals(tradeDirection)) {
			price = quote.getOfferPx();
		} else if ( TradeAction.SELL.equals(tradeDirection)) {
			price = quote.getBidPx();
		}

		currency = transactionMessage.getNotionalCurrency();
		if ( transactionMessage.getSymbol().indexOf(transactionMessage.getNotionalCurrency()) > 1 ) {
			amount = transactionMessage.getNearDateDetail().getCurrency2Amt();
		} else {
			amount = transactionMessage.getNearDateDetail().getCurrency1Amt();
		}
		
		tradeQuoteRepo.associateQuote(transactionMessage.getTransId(), new TradeRequestWithQuoteVo(transactionMessage,quote));

		String tenor = null;
		
		if ( Constants.ProductType.FXSPOT.equals(transactionMessage.getProduct()) ){
			tenor = TenorVo.NOTATION_SPOT;
		} else {
			tenor = TenorVo.NOTATION_TODAY;
		}
		
		
		
		sent = qfixApp.sendTradeExecRequest(
			transactionMessage.getProduct(),
			price, 
			amount,
			currency,
			transactionMessage.getSymbol(),
			tenor,
			quote.getSettleDate() == null ? transactionDetail.getValueDate() : quote.getSettleDate(),
			null,
			side,
			transactionMessage.getTransId(),
			quoteId,
			AppType.FIXTRADEADAPTER, 
			quote.getTransactTime(), 
			qfixApp.getExecutionReportListener(),
			"");
		latencyRecord.setEndTimeMillis();
		logger.debug(String.format("Sent Order for transId<%s>, %s", transactionMessage.getTransId(), latencyRecord.toString()));
		

	}

	public static class QuoteVoWrapper {
		private final QuoteVo quote;
		private final String fullBookBid;
		private final String fullBookAsk;
		
		public QuoteVoWrapper(QuoteVo quote, String fullBookBid, String fullBookAsk) {
			super();
			this.quote = quote;
			this.fullBookBid = fullBookBid;
			this.fullBookAsk = fullBookAsk;
		}
		
		public QuoteVo getQuote() {
			return quote;
		}
		public String getFullBookBid() {
			return fullBookBid;
		}
		public String getFullBookAsk() {
			return fullBookAsk;
		}
	}
}
