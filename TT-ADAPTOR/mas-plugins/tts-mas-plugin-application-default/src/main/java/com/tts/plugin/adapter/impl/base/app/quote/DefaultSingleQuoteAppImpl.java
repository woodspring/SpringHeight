package com.tts.plugin.adapter.impl.base.app.quote;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.constant.Constants;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam;
import com.tts.message.eas.request.SubscriptionStruct.QuoteParam.QuoteDirection;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.impl.base.app.AbstractSubscribingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.vo.TenorVo;

public class DefaultSingleQuoteAppImpl extends AbstractSubscribingApp {
	private final static Logger logger = LoggerFactory.getLogger(DefaultSingleQuoteAppImpl.class);

	private final IPublishingEndpoint iPublishingEndpoint;
	private final IResponseDialectHelper dialect;

	public DefaultSingleQuoteAppImpl(IMkQfixApp qfixApp,
			ISchedulingWorker worker,
			SessionInfo sessionInfo, IPublishingEndpoint iPublishingEndpoint,
			ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint,
				iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
		this.dialect = IFixIntegrationPluginSpi.getResponseDialectHelper();
		this.iPublishingEndpoint = iPublishingEndpoint;
	}
	
	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.NO_CHANGE;
	}
	
	@Override
	public void onRequest(String topic, TtMsg ttMsg) {
		if ( topic.indexOf("REQUEST") > 0 ) {
			try {
				PriceSubscriptionRequest priceSubscriptionRequest = PriceSubscriptionRequest.parseFrom(ttMsg.getParameters());
				logger.debug(String.format("Received message from topic<%s>: %s", topic, TextFormat.shortDebugString(priceSubscriptionRequest)));

				QuoteParam quoteParam = priceSubscriptionRequest.getQuoteParam();
				String tenor = null;
				QuoteSide side = null;
				long expiryTime = -1L;
				
				if ( Constants.ProductType.FXSPOT.equals(quoteParam.getProduct() )) {
					tenor = TenorVo.NOTATION_SPOT;
				} else {
					String periodCode = quoteParam.getNearDateDetail().getPeriodCd();
					String periodValue = quoteParam.getNearDateDetail().getPeriodValue();
					if ( "-1".equals(periodValue) ) {
						tenor = periodCode;
					} else {
						tenor = periodValue + periodCode;
					}
				}
				
				if ( QuoteDirection.BOTH.equals(quoteParam.getQuoteDirection()) ) {
					side = QuoteSide.BOTH;
				} else if ( QuoteDirection.BUY.equals(quoteParam.getQuoteDirection()) ) { 
					side = QuoteSide.BUY;
				} else if (	QuoteDirection.SELL.equals(quoteParam.getQuoteDirection()) ) { 
					side = QuoteSide.SELL;
				}
					
					
				if (quoteParam.hasQuoteDuration() ) {
					expiryTime = quoteParam.getQuoteDuration();
				}
				
				String notionalCurrency;
				if ( quoteParam.hasNotionalCurrency() ) {
					notionalCurrency = quoteParam.getNotionalCurrency();
				} else {
					notionalCurrency = quoteParam.getCurrencyPair().substring(0, 3);
				}
				
				BigDecimal bd = new BigDecimal(quoteParam.getSize());
				getQfixApp().sendRfsRequest(
						bd.longValue(), 
						quoteParam.getCurrencyPair(),
						notionalCurrency,
						tenor,
						quoteParam.getNearDateDetail().getActualDate(), 
						side, 
						expiryTime, 
						new FixQuoteHandler(
								priceSubscriptionRequest.getRequestId(),
								priceSubscriptionRequest.getTopic(), 
								dialect, 
								iPublishingEndpoint), 
						AppType.QUOTEADAPTER);
				logger.debug("sent");
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		} else if (topic.indexOf("AMEND") > 0) {
			try {
				PriceSubscriptionRequest priceSubscriptionRequest = PriceSubscriptionRequest.parseFrom(ttMsg.getParameters());
				logger.debug(String.format("Received message from topic<%s>: %s", topic, TextFormat.shortDebugString(priceSubscriptionRequest)));
				String tenor = null;

				QuoteParam quoteParam = priceSubscriptionRequest.getQuoteParam();
				if ( Constants.ProductType.FXSPOT.equals(quoteParam.getProduct() )) {
					tenor = TenorVo.NOTATION_SPOT;
				} else {
					String periodCode = quoteParam.getNearDateDetail().getPeriodCd();
					String periodValue = quoteParam.getNearDateDetail().getPeriodValue();
					if ( "-1".equals(periodValue) ) {
						tenor = periodCode;
					} else {
						tenor = periodValue + periodCode;
					}
				}
				getQfixApp().cancelEspRequest(
						quoteParam.getCurrencyPair(), 
						tenor,
						quoteParam.getNearDateDetail().getActualDate(), 
						priceSubscriptionRequest.getRequestId(),
						AppType.QUOTEADAPTER);

			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public String[] getDesiredTopics() {
		return new String[] { IEventMessageTypeConstant.Market.TOPIC_QUOTE_ALL };
	}

	@Override
	public String getName() {
		return "QuoteAdapter";
	}
	
	@Override
	public void init() {
		
	}
	
	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}


}
