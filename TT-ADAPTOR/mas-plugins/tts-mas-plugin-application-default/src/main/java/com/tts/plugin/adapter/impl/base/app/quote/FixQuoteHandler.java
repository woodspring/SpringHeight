package com.tts.plugin.adapter.impl.base.app.quote;

import java.math.BigDecimal;
import java.math.MathContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.IFixListener;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.plugin.adapter.api.IFixConstants;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.QuoteStackRepo;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.util.AppContext;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.NumberVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;

public class FixQuoteHandler implements IFixListener {
	private final static Logger logger = LoggerFactory.getLogger(FixQuoteHandler.class);

	private final String quoteReqId;
	private final String outboundTopic; 
	private final IResponseDialectHelper dialect;
	private final IPublishingEndpoint iPublishingEndpoint;
	private final QuoteStackRepo quoteStackRepo;

	public FixQuoteHandler(
			String quoteReqId,
			String topic, 
			IResponseDialectHelper dialect, 
			IPublishingEndpoint iPublishingEndpoint) {
		this.quoteReqId = quoteReqId;
		this.outboundTopic = topic;
		this.dialect = dialect;
		this.iPublishingEndpoint = iPublishingEndpoint;
		this.quoteStackRepo = AppContext.getContext().getBean(QuoteStackRepo.class);
		
		logger.debug(String.format("New FixQuoteHandler init for outbound topic<%s>", topic));

	}

	@Override
	public void onMessage(Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		long receivedTimestamp = System.currentTimeMillis();
		
		QuoteVo quote = null;
		if ( message instanceof quickfix.fix50.Quote) { 
			quote = dialect.convert((quickfix.fix50.Quote) message);
		} else if ( message instanceof quickfix.fix50.QuoteCancel) {
			quickfix.fix50.QuoteCancel quoteCancel = (quickfix.fix50.QuoteCancel) message;
			FullBook.Builder fbBuilder = FullBook.newBuilder();
			fbBuilder.setIndicativeFlag(IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE, IndicativeReason.MA_NoData));
			fbBuilder.setQuoteRefId(quoteReqId + IFixConstants.DEFAULT_DELIMITER + quoteCancel.getQuoteID());
			Latency.Builder latency = Latency.newBuilder();
			fbBuilder.setLatency(latency);
			
			latency.setFaReceiveTimestamp(receivedTimestamp);
			latency.setFaSendTimestamp(System.currentTimeMillis());
			
			iPublishingEndpoint.publish(outboundTopic, fbBuilder.build());
		}
		
		if ( quote != null ) {
			FullBook.Builder fbBuilder = FullBook.newBuilder();
			fbBuilder.setSymbol(quote.getSymbol());
			fbBuilder.setIndicativeFlag(IndicativeFlag.TRADABLE);
			fbBuilder.setQuoteRefId(quote.getQuoteId());
			
			if ( quote.getBidPx() != null ) {
				Tick.Builder bidTick = Tick.newBuilder();
				bidTick.setRate(quote.getBidPx());
				bidTick.setSize(NumberVo.getInstance(quote.getBidSize()).getLongValueFloored());
				bidTick.setSpotRate(quote.getBidSpotRate());
				fbBuilder.addBidTicks(bidTick);
			}
			if ( quote.getOfferPx() != null ) {
				Tick.Builder askTick = Tick.newBuilder();
				askTick.setRate(quote.getOfferPx());
				askTick.setSize(NumberVo.getInstance(quote.getOfferSize()).getLongValueFloored());
				askTick.setSpotRate(quote.getOfferSpotRate());
				fbBuilder.addAskTicks(askTick);
			}
			
			if ( quote.getBidForwardPoints() != null || quote.getOfferForwardPoints() != null ) {
				Tenor.Builder tenor = Tenor.newBuilder();
				if ( quote.getBidForwardPoints() != null ) {
					BigDecimal bd = new BigDecimal(quote.getBidForwardPoints(), MathContext.DECIMAL64);
					tenor.setBidSwapPoints(bd.toPlainString());
				}
				if ( quote.getOfferForwardPoints() != null ) {
					BigDecimal bd = new BigDecimal(quote.getOfferForwardPoints(), MathContext.DECIMAL64);
					tenor.setAskSwapPoints(bd.toPlainString());
				}
				tenor.setActualDate(quote.getSettleDate());
				fbBuilder.addTenors(tenor);
			}
			Latency.Builder latency = Latency.newBuilder();
			fbBuilder.setLatency(latency);
			
			latency.setFaReceiveTimestamp(receivedTimestamp);
			latency.setFaSendTimestamp(System.currentTimeMillis());
			
			quoteStackRepo.addQuote(quoteReqId, quote);

			iPublishingEndpoint.publish(outboundTopic, fbBuilder.build());
		}
		
		
		
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
