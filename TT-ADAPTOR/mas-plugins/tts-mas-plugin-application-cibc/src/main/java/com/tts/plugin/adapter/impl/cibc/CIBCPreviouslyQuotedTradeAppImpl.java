package com.tts.plugin.adapter.impl.cibc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.impl.base.app.AbstractSubscribingApp;
import com.tts.plugin.adapter.impl.base.app.fxprice.IEspPriceRepoDependent;
import com.tts.plugin.adapter.impl.base.app.tradeexec.TradeWithExistingEspQuoteIdHandler;
import com.tts.plugin.adapter.impl.base.vo.LatencyVo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IMasGlobolSequenceProvider;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;

public class CIBCPreviouslyQuotedTradeAppImpl extends AbstractSubscribingApp implements IEspPriceRepoDependent {
	private final static Logger logger = LoggerFactory.getLogger(CIBCPreviouslyQuotedTradeAppImpl.class);

	public static final String MY_NAME = "CIBC_TRADE_ADAPTER";

	private boolean espPriceCheck;
	private final IResponseDialectHelper dialect;
	private final IMasGlobolSequenceProvider sequenceProvider;
	private IEspRepo<?> espRepo;
	
	public CIBCPreviouslyQuotedTradeAppImpl(
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
		this.dialect = IFixIntegrationPluginSpi.getResponseDialectHelper();
		this.sequenceProvider = AppContext.getContext().getBean(IMasGlobolSequenceProvider.class);
		
		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		this.espPriceCheck = p.getProperty("ORDER.PreviouslyQuote.Esp.priceCheck", true);
		logger.info("ESP Price validation is set to " + this.espPriceCheck);
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
			if ( (transactionMessage.getOrderParams().hasQuoteRefId() && transactionMessage.getOrderParams().getQuoteRefId().startsWith(IFixConstants.ESP_QUOTE_REF_ID_PREFIX)) ) {
				new TradeWithExistingEspQuoteIdHandler(transactionMessage, getQfixApp(), espRepo, sequenceProvider, dialect, latencyRecord, getCertifiedPublishingEndpoint(), espPriceCheck);
			} else {
				new TradeWithCibcRfsQuoteHandler(transactionMessage, getQfixApp(), dialect, latencyRecord, getCertifiedPublishingEndpoint());
			}
			
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
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

}
