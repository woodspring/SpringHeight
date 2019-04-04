package com.tts.plugin.adapter.impl.base.app.tradeexec;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.CommonStruct.BuySellActionCd;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.RestingOrderMessage.OrderParams;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp.CoveringResult;
import com.tts.plugin.adapter.feature.ITraderAdjustmentDependentApp;
import com.tts.plugin.adapter.impl.base.app.AbstractSubscribingApp;
import com.tts.plugin.adapter.impl.base.app.fxprice.IEspPriceRepoDependent;
import com.tts.plugin.adapter.impl.base.vo.LatencyVo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IEspRepo.FullBookSrcWrapper;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.constant.TransStateConstants.TransStateType;

public class DirectTradeExecWithMKTPriceAppImpl extends AbstractSubscribingApp implements IEspPriceRepoDependent, ITraderAdjustmentDependentApp {
	private final static Logger logger = LoggerFactory.getLogger(DirectTradeExecWithMKTPriceAppImpl.class);
	private final static String reportPublishingTopic = IEventMessageTypeConstant.AutoCover.TRADE_STATUS_FROM_MR_EVENT;

	public static final String MY_NAME = "MKTPRICE_TRADE_ADAPTER";
	private final IMkQfixApp qfixApp;
	private IEspRepo<?> espRepo;
	private ITraderAdjustmentApp traderAdjustmentApp;
	
	public DirectTradeExecWithMKTPriceAppImpl(
			IMkQfixApp qfixApp,
			ISchedulingWorker worker,
			SessionInfo sessionInfo, 
			IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi
			) {
		super(
			qfixApp, 
			worker, 
			sessionInfo, 
			iPublishingEndpoint,
			iCertifiedPublishingEndpoint, 
			IFixIntegrationPluginSpi
		);
		
		this.qfixApp = qfixApp;
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.NO_CHANGE;
	}
	
	@Override
	public void onRequest(String topic, TtMsg ttMsg) {
		Transaction transactionMessage = null;
		String mktOrdPrice = "";
		LatencyVo latencyRecord = new LatencyVo();
		CoveringResult coveringResult = null;
		try {
			transactionMessage = Transaction.parseFrom(ttMsg.getParameters());
			String transId     = transactionMessage.getTransId();
			logger.info(String.format("Received Trade(MKT) from %s for transId <%s>: %s", topic, transId, TextFormat.shortDebugString(transactionMessage)));
			
			
			String amount = getMarketTradeDataInfo(transactionMessage, "AMT");
			String side   = getMarketTradeDataInfo(transactionMessage, "SIDE");
			
			long currentTime = System.currentTimeMillis();
			String priceTime = "";
			OrderParams op = null;
			
			if ( transactionMessage.hasOrderParams() ) {
				op = transactionMessage.getOrderParams();
			}
			if (traderAdjustmentApp != null ) {
				double coverAmount = new BigDecimal(amount).doubleValue();
				coveringResult = traderAdjustmentApp.coverAdjustment(
						transactionMessage.getSymbol(), 
						side.equalsIgnoreCase("B") ? BuySellActionCd.BUY : BuySellActionCd.SELL, 
						coverAmount);
				if ( coverAmount == coveringResult.getCoveredAmount()) {
					ExecutionReportInfo.Builder executionReport = ExecutionReportInfo.newBuilder();

					String symbol = transactionMessage.getSymbol();
					String currency = transactionMessage.getNotionalCurrency();

					executionReport.setTransId(transId);
					executionReport.setSymbol(symbol);
					executionReport.setCurrency(currency);

					executionReport.setStatus(TransStateType.INTERNAL___COVERED_BY_INTERNAL_LIQUIDITY);
					StringBuilder sb = new StringBuilder("Covered by Internal Liquidity ");
					if ( coveringResult.getProvider() != null ) {
						sb.append("submitted by " );
						sb.append(coveringResult.getProvider().getOwnerName());
						sb.append(" at ");
						sb.append(coveringResult.getProvider().getCreationTime());
					}

					String comment = sb.toString();
					executionReport.setAdditionalInfo(comment);
							
					logger.info("Booked trade<" + transId + ">. against INTERNAL LIQUIDITY. " + comment);
					logger.debug(String.format("Sending trade report for %s, %s", executionReport.getTransId(), TextFormat.shortDebugString(executionReport)));
					getCertifiedPublishingEndpoint().publish(reportPublishingTopic, executionReport.build());
					return;
				}
			}
			if ( transactionMessage.hasOrderParams() 
					&& transactionMessage.getOrderParams().hasTargetPrice()
					&& ( transactionMessage.getOrderParams().getOrdType() == OrderParams.OrdType.Limit
						|| transactionMessage.getOrderParams().getOrdType() == OrderParams.OrdType.Market) ) {
				if ( transactionMessage.hasQuoteRefId() && transactionMessage.getOrderParams().getQuoteRefId().length() > 1) {
					priceTime = transactionMessage.getOrderParams().getQuoteRefId();
				} else 
					priceTime = transactionMessage.getOrderParams().getQuoteRefId();
				mktOrdPrice = transactionMessage.getOrderParams().getTargetPrice();
			} else {
				FullBookSrcWrapper<?> marketData = espRepo.findLatestPriceBySymbol(transactionMessage.getSymbol());
				if((marketData != null) && (side.equalsIgnoreCase("B")) && (marketData.getFullBook().getAskTicksCount() > 0))	{
					mktOrdPrice = marketData.getFullBook().getAskTicks(0).getRate();
					priceTime = transactionMessage.getOrderParams().getQuoteRefId();
				}
				if((marketData != null) && (side.equalsIgnoreCase("S")) && (marketData.getFullBook().getBidTicksCount() > 0))	{
					mktOrdPrice = marketData.getFullBook().getBidTicks(0).getRate();
					priceTime = transactionMessage.getOrderParams().getQuoteRefId();
	
				}
			}
			String msg = (currentTime + " MktTime(LQVP):" + priceTime );
			
			System.out.println(msg);
			logger.info("price latency message: " + msg);
			
			if((mktOrdPrice == null) || (mktOrdPrice.trim() == ""))	{
				// TODO Action when price is EMPTY				
			}
						
			if ( qfixApp.isLoggedOn(AppType.FIXTRADEADAPTER)) {
				logger.info(String.format("Executing Trade(MKT) from %s for transId <%s>: %s @ %s", topic, transId, TextFormat.shortDebugString(transactionMessage), mktOrdPrice));
				qfixApp.sendTradeExecRequest("", mktOrdPrice, amount, 
						transactionMessage.getNotionalCurrency(), 
						transactionMessage.getSymbol(),
						"", 
						"", 
						op,
						(side.equalsIgnoreCase("B")? QuoteSide.BUY: QuoteSide.SELL), 
						transactionMessage.getTransId(), 
						"", 
						AppType.FIXTRADEADAPTER, 
						Long.toString(System.currentTimeMillis()),
						qfixApp.getExecutionReportListener(),
						"");
			} else {
				rejectTrade(transactionMessage, "FIX Ordering Session not Online");	
			}
			latencyRecord.setEndTimeMillis();
			logger.debug(String.format("Trade(MKT) Order from %s sent..., %s", topic,  latencyRecord.toString()));
		} 
		catch(InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		finally {
			latencyRecord = null;
		}
	}
	
	private void rejectTrade(Transaction transactionMessage, String reason) {
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
		getCertifiedPublishingEndpoint().publish(reportPublishingTopic, executionReport.build());
		
	}
	
	@Override
	public String[] getDesiredTopics() {
		return new String[] { IEventMessageTypeConstant.Market.TOPIC_TRADE_ALL };

	}

	@Override
	public String getName() {
		return MY_NAME;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void init() {
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo = espRepo;		
	}
	
	private String getMarketTradeDataInfo(Transaction transactionMessage, String dataName)	{
		String dataValue = "";
		String currency1 = transactionMessage.getSymbol().substring(0, 3);
		String currency2 = transactionMessage.getSymbol().substring(3);
		
		String nationalCurrency = transactionMessage.getNotionalCurrency();
		if(dataName.equalsIgnoreCase("AMT"))	{
			dataValue = (nationalCurrency.equalsIgnoreCase(currency1))? transactionMessage.getNearDateDetail().getCurrency1Amt(): transactionMessage.getNearDateDetail().getCurrency2Amt();
		}
		
		if(dataName.equalsIgnoreCase("SIDE"))	{
			if(nationalCurrency.equalsIgnoreCase(currency1))	{
				dataValue = transactionMessage.getTradeAction();
			}
			if(nationalCurrency.equals(currency2))	{
				dataValue = (transactionMessage.getTradeAction().equalsIgnoreCase("B"))? "S": "B";
			}
		}
		
		return(dataValue);
	}

	@Override
	public void setTraderAdjustmentApp(ITraderAdjustmentApp traderAdjustmentApp) {
		this.traderAdjustmentApp = traderAdjustmentApp;		
	}
	

}
