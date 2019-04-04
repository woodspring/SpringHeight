package com.tts.mlp.app.price.subscription;

import java.util.List;

import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.mlp.rate.provider.vo.Tick;
import com.tts.util.chronology.ChronologyUtil;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.BidPx;
import quickfix.field.BidSize;
import quickfix.field.BidSpotRate;
import quickfix.field.OfferPx;
import quickfix.field.OfferSize;
import quickfix.field.OfferSpotRate;
import quickfix.field.OrderQty;
import quickfix.field.QuoteCancelType;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.field.QuoteResponseLevel;
import quickfix.field.SecurityType;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;

public class RfsHandler extends AbstractSubscriptionHandler {
	
	private final String clientReqId;
	private final long quoteIdPrefix;
	private final long endTime;
	private final IRandomMarketPriceProvider priceProvider;
	private final SubscriptionRequestVo request; 
	
	public RfsHandler(SubscriptionRequestVo request, IRandomMarketPriceProvider priceProvider, SessionID sessionid, quickfix.Message originalMessage) {
		super(request, sessionid, originalMessage);
		String quoteId = request.getClientReqId().replaceAll("\\D+","");
		this.quoteIdPrefix = Long.parseLong(quoteId) << 15;
		this.priceProvider = priceProvider;
		this.request = request;
		this.clientReqId = request.getClientReqId();
		
		boolean sizeNotOk = validateQuoteSize(request.getSymbol(), request.getSize());
		if ( !sizeNotOk ) {
			this.endTime = 1;
		} else if ( request.getExpiryTime() > 0 ) {
			this.endTime = System.currentTimeMillis() + request.getExpiryTime() * ChronologyUtil.MILLIS_IN_SECOND;
		} else {
			this.endTime = -1;
		}
		System.out.println("endTime = " + this.endTime);
	}
	

	private boolean validateQuoteSize(String symbol, long size) {
		Instrument instrument = priceProvider.getCurrentPrice(symbol);
		List<Tick> ticks = instrument.getBidTicks();
		long maxRung = -1L;
		for ( Tick tick: ticks) {
			if ( tick.getQuantity().longValue() > maxRung) {
				maxRung = tick.getQuantity().longValue();
			}
		}
		if ( size > maxRung) {
			return false;
		}
		return true;
	}


	@Override
	public Message push(long seqQuoteId) {
		if ( endTime > 0 && System.currentTimeMillis() > endTime ) {
			quickfix.fix50.QuoteCancel quoteCancel = new quickfix.fix50.QuoteCancel();
			quoteCancel.set(new QuoteReqID(clientReqId));
			quoteCancel.set(new QuoteID(getIdentity() + "!" + seqQuoteId));
			quoteCancel.set(new QuoteCancelType(QuoteCancelType.CANCEL_QUOTE_SPECIFIED_IN_QUOTEID));
			getSession().send(quoteCancel);
			return quoteCancel; 
		}
		Instrument instrument = priceProvider.getNextMarketPrice(request.getSymbol());
		if ( instrument.getAskTicks().size() == 1 && instrument.getAskTicks().get(0).getQuantity() == 0.0d) {
			quickfix.fix50.QuoteCancel quoteCancel = new quickfix.fix50.QuoteCancel();
			quoteCancel.set(new QuoteReqID(clientReqId));
			quoteCancel.set(new QuoteID(getIdentity() + "!" + seqQuoteId));
			quoteCancel.set(new QuoteCancelType(QuoteCancelType.CANCEL_QUOTE_SPECIFIED_IN_QUOTEID));
			getSession().send(quoteCancel);
			return quoteCancel; 
		}
		quickfix.fix50.Quote quote = buildPrices(request, instrument);
		if ( quote != null ) {
			quote.set(new QuoteID(getIdentity() + "!" + seqQuoteId));
			quote.set(new TransactTime());
			getSession().send(quote);
		}
		return quote;
	}

	private quickfix.fix50.Quote buildPrices(SubscriptionRequestVo request, IRandomMarketPriceProvider priceProvider ) {
		Instrument instrument = priceProvider.getNextMarketPrice(request.getSymbol());
		return buildPrices(request, instrument);
	}
	
	private quickfix.fix50.Quote buildPrices(SubscriptionRequestVo request, Instrument instrument  ) {
		
		if ( instrument == null ) { return null; }
			int tickIdx = findTickIndex(instrument, request.getSize());
	
			quickfix.fix50.Quote quote = new quickfix.fix50.Quote();
			quote.set(new QuoteReqID(request.getClientReqId()));
	
			quote.set(new Symbol(request.getSymbol()));
			quote.set(new OrderQty(request.getSize()));
			quote.set(new SecurityType("FXSPOT"));	
	
			quote.set(new QuoteResponseLevel(0));
			
			if ( request.getQuoteSide() == QuoteSide.BUY) {
				quote.set(new Side(Side.BUY));
			} else if ( request.getQuoteSide() == QuoteSide.SELL) {
				quote.set(new Side(Side.SELL));
			} else {
				quote.set(new Side('0'));
			}
				
			//if ( request.getQuoteSide() == QuoteSide.BUY || request.getQuoteSide() == QuoteSide.BOTH) {
				Tick offerTick = instrument.getAskTicks().get(tickIdx);
				quote.set(new OfferPx(offerTick.getPrice()));
				quote.set(new OfferSpotRate(offerTick.getPrice()));
				quote.set(new OfferSize(request.getSize()));

			//} 
			//if ( request.getQuoteSide() == QuoteSide.SELL  || request.getQuoteSide() == QuoteSide.BOTH) {
				Tick bidTick = instrument.getBidTicks().get(tickIdx);
				quote.set(new BidPx(bidTick.getPrice()));
				quote.set(new BidSpotRate(bidTick.getPrice()));
				quote.set(new BidSize(request.getSize()));

			//}
	
		
		
		return quote;
	}
		
	private int findTickIndex(Instrument instrument, long size) {
		List<Tick> ticks = instrument.getAskTicks();
		
		for ( int i = 0; i < ticks.size(); i++) {
			Tick tick = ticks.get(i);
			if ( tick.getQuantity() >= size   ) {
				return i;
			}
		}
		return 0;
	}

}
