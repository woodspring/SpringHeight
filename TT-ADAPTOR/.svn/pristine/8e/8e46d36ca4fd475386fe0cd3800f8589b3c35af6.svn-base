package com.tts.plugin.adapter.impl.base.app.tradeexec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.impl.base.app.AbstractSubscribingApp;
import com.tts.plugin.adapter.impl.base.vo.LatencyVo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;

public class DefaultFixTradeExecutionAppImpl extends AbstractSubscribingApp {
	private final static Logger logger = LoggerFactory.getLogger(DefaultFixTradeExecutionAppImpl.class);

	public static final String MY_NAME = "TRADE_ADAPTER";

	private final IResponseDialectHelper dialect;
	
	public DefaultFixTradeExecutionAppImpl(
			IMkQfixApp qfixApp,
			ISchedulingWorker worker,
			SessionInfo sessionInfo, 
			IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(
			qfixApp, 
			worker, 
			sessionInfo, 
			iPublishingEndpoint,
			iCertifiedPublishingEndpoint, 
			IFixIntegrationPluginSpi
		);
		this.dialect = IFixIntegrationPluginSpi.getResponseDialectHelper();
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.NO_CHANGE;
	}
	
	@Override
	public void onRequest(String topic, TtMsg ttMsg) {
		Transaction transactionMessage = null;
		LatencyVo latencyRecord = new LatencyVo();

		try {
			transactionMessage = Transaction.parseFrom(ttMsg.getParameters());
			String transId = transactionMessage.getTransId();
			logger.info(String.format("Received Trade from %s for transId<%s>: %s", topic, transId, TextFormat.shortDebugString(transactionMessage)));
			 new TradeWithNewRfsQuoteHandler(transactionMessage, getQfixApp(), dialect, latencyRecord, getCertifiedPublishingEndpoint());
			
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String[] getDesiredTopics() {
		return new String[] { IEventMessageTypeConstant.Market.TOPIC_TRADE_ALL };

	}
	
	@Override
	public void init() {
		
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
	public void stop() {
		// TODO Auto-generated method stub

	}

}
