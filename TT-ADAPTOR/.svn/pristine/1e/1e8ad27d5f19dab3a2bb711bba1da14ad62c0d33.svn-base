package com.tts.plugin.adapter.impl.base.app.tradereport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.impl.base.app.trade.TradeQuoteRepo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;

public class DefaultTradeReportingAppImpl extends AbstractPublishingApp implements IFixListener {
	private static final Logger logger = LoggerFactory.getLogger(DefaultTradeReportingAppImpl.class);
	private static final String MY_NAME = "TRADE_REPORTING";

	@SuppressWarnings("unused")
	private final TradeQuoteRepo tradeQuoteRepo;
	private final IResponseDialectHelper dialect;
	private final String reportPublishingTopic;
	
	public DefaultTradeReportingAppImpl(IMkQfixApp qfixApp,
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
	public void onMessage(Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		ExecutionReportInfo.Builder executionReport = ExecutionReportInfo.newBuilder();

		if ( message instanceof quickfix.fix50.ExecutionReport) {
			quickfix.fix50.ExecutionReport report = (quickfix.fix50.ExecutionReport) message;
			dialect.convertAndUpdate(report, executionReport);
		} else 	if(message instanceof quickfix.fix44.ExecutionReport)	{
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