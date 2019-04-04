package com.tts.plugin.adapter.impl.cibc;

import java.math.BigDecimal;
import java.math.MathContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.constant.Constants;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.impl.base.app.trade.TradeQuoteRepo;
import com.tts.plugin.adapter.impl.base.vo.TradeRequestWithQuoteVo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.constant.TransStateConstants.TransStateType;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;

public class CIBCTradeReportingAppImpl extends AbstractPublishingApp implements IFixListener {
	private static final Logger logger = LoggerFactory.getLogger(CIBCTradeReportingAppImpl.class);
	private static final String MY_NAME = "TRADE_REPORTING";

	private final TradeQuoteRepo tradeQuoteRepo;
	private final IResponseDialectHelper dialect;
	private final String reportPublishingTopic;
	
	public CIBCTradeReportingAppImpl(IMkQfixApp qfixApp,
			ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint,
				iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
		
		this.tradeQuoteRepo = AppContext.getContext().getBean(TradeQuoteRepo.class);

		this.dialect = IFixIntegrationPluginSpi.getResponseDialectHelper();
		this.reportPublishingTopic = IEventMessageTypeConstant.AutoCover.TRADE_STATUS_FROM_MR_EVENT;
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.NO_CHANGE;
	}
	
	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.TradeReport;
	}

	@Override
	public void atPublish(long masGlobalSeq) {

	}

	@Override
	public String getName() {
		return MY_NAME;
	}
	
	@Override
	public void init() {
		
	}
	
	@Override
	public void start() {
		getQfixApp().setExecutionReportListener(this);
	}

	@Override
	public void stop() {

	}

	@Override
	@SuppressWarnings("unused")
	public void onMessage(Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		ExecutionReportInfo.Builder executionReport = ExecutionReportInfo.newBuilder();

		if ( message instanceof quickfix.fix50.ExecutionReport) {
			quickfix.fix50.ExecutionReport report = (quickfix.fix50.ExecutionReport) message;
			dialect.convertAndUpdate(report, executionReport);
			String transId = executionReport.getTransId();
			TradeRequestWithQuoteVo tqr = null;
			QuoteVo quote = null;
			
			if (  TransStateType.TRADE_DONE.equals(executionReport.getStatus()) ) {
				tqr = tradeQuoteRepo.unassociateQuote(transId);
			} else {
				tqr = tradeQuoteRepo.find(transId);
			}
			String symbol = report.getSymbol().getValue();
			String notionalCurrency = report.getCurrency().getValue();
			
			String ccy2 = symbol.substring(3,6);
			String view = "offer";
			
			if ( TradeAction.BUY_AND_SELL.equals(executionReport.getTradeAction()) ) {
				view = "offer-bid";
			} else if ( TradeAction.SELL_AND_BUY.equals(executionReport.getTradeAction()) ) {
				view = "bid-offer";
			} 
			if ( TradeAction.SELL.equals(executionReport.getTradeAction()))  {
				view = "bid";
			} 
			
			if ( tqr != null ) {
				String product = tqr.getTransactionMessage().getProduct();

				quote = tqr.getQuote();

				if ( "offer".equals(view)) {
					executionReport.setAllInPrice(quote.getOfferPx());
					if ( quote.getOfferSpotRate() != null ) {
						executionReport.setSpotRate(quote.getOfferSpotRate());
					}
					if ( quote.getOfferPx() != null ) {
						executionReport.setAllInPrice(quote.getOfferPx());
					} 
					if ( quote.getOfferForwardPoints() != null ) {
						BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
						executionReport.setFwdPoints(bd.toPlainString());
					}
				} else if ( "bid".equals(view)) {
					executionReport.setAllInPrice(quote.getBidPx());
					if ( quote.getBidSpotRate() != null ) {
						executionReport.setSpotRate(quote.getBidSpotRate());
					}
					if ( quote.getBidPx() != null ) {
						executionReport.setAllInPrice(quote.getBidPx());
					} 
					if ( quote.getBidForwardPoints() != null ) {
						BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
						executionReport.setFwdPoints(bd.toPlainString());
					}
				} else if ( "offer-bid".equals(view)) { //BUY_AND_SELL
					executionReport.setAllInPrice(quote.getOfferPx());
					if ( quote.getOfferSpotRate() != null ) {
						executionReport.setSpotRate(quote.getOfferSpotRate());
					}
					if ( quote.getOfferPx() != null ) {
						executionReport.setAllInPrice(quote.getOfferPx());
					} 
					if ( quote.getOfferForwardPoints() != null ) {
						BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
						executionReport.setFwdPoints(bd.toPlainString());
					}
					if ( quote.getBidPx2() != null ) {
						executionReport.setAllInPrice2(quote.getBidPx2());
					} 
					if ( quote.getBidSpotRate() != null ) {
						executionReport.setSpotRate2(quote.getOfferSpotRate());
					} 
					if ( quote.getBidForwardPoints2() != null ) {
						BigDecimal bd = new BigDecimal(quote.getBidForwardPoints2(), MathContext.DECIMAL64);
						executionReport.setFwdPoints2(bd.toPlainString());
					}
				} else if (  "bid-offer".equals(view)) { //SELL_AND_BUY
					executionReport.setAllInPrice(quote.getBidPx());
					if ( quote.getBidSpotRate() != null ) {
						executionReport.setSpotRate(quote.getBidSpotRate());
					}
					if ( quote.getBidPx() != null ) {
						executionReport.setAllInPrice(quote.getBidPx());
					} 
					if ( quote.getBidForwardPoints() != null ) {
						BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
						executionReport.setFwdPoints(bd.toPlainString());
					}
					if ( quote.getOfferPx2() != null ) {
						executionReport.setAllInPrice2(quote.getOfferPx2());
					} 
					if ( quote.getBidSpotRate() != null ) {
						executionReport.setSpotRate2(quote.getBidSpotRate());
					} 
					if ( quote.getOfferForwardPoints2() != null ) {
						BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints2(), MathContext.DECIMAL64);
						executionReport.setFwdPoints2(bd.toPlainString());
					}
				}
				if (  TransStateType.TRADE_DONE.equals(executionReport.getStatus()) && !executionReport.hasAdditionalInfo()  ) {
					executionReport.setAdditionalInfo(quote.getQuoteId());
				}
				
				if ( Constants.ProductType.FXTIMEOPTION.equals(product)) {
					executionReport.setProduct(product);
				}
								
			}
		}
		
		if(message instanceof quickfix.fix44.ExecutionReport)	{
			quickfix.fix44.ExecutionReport report = (quickfix.fix44.ExecutionReport) message;
			dialect.convertAndUpdate(report, executionReport);
		}
		
		logger.debug(String.format("Sending trade report for %s, %s", executionReport.getTransId(), TextFormat.shortDebugString(executionReport)));
		getCertifiedPublishingEndpoint().publish(reportPublishingTopic, executionReport.build());
	}

	@Override
	public void onFixSessionLogoff() {
		
	}
	
	@Override
	public void onFixSessionLogon() {
		// TODO Auto-generated method stub
	}
}