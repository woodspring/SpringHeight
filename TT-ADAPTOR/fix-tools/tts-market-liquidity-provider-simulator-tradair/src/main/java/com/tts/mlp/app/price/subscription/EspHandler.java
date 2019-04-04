package com.tts.mlp.app.price.subscription;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.vo.TenorVo;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.Currency;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.NoMDEntries;
import quickfix.field.QuoteCondition;
import quickfix.field.SecurityType;
import quickfix.field.Symbol;

public class EspHandler extends AbstractSubscriptionHandler {
	public final static SecurityType SecurityType_SPOT = new SecurityType("FXSPOT"); 
	public final static SecurityType SecurityType_FWD = new SecurityType("FXFWD"); 
	
	final IRandomMarketPriceProvider priceProvider;

	PriceSubscriptionRegistry priceSubscriptionRegistry;
	
	public EspHandler(
			SubscriptionRequestVo request, 
			IRandomMarketPriceProvider priceProvider, 
			SessionID sessionid, 
			quickfix.Message originalMessage,
			PriceSubscriptionRegistry registry) {
		super(request, sessionid, originalMessage);
		this.priceProvider = priceProvider;
		priceSubscriptionRegistry = registry;
	}
//
//	private MarketDataIncrementalRefresh[][] buildPriceDeltas(
//			SubscriptionRequestVo request, IPriceProvider priceProvider) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	private MarketDataSnapshotFullRefresh[] buildFullPrices(
//			SubscriptionRequestVo request, IPriceProvider priceProvider) {
//		Instrument[] instrumentPrices = priceProvider.getGeneratedPrices(request.getSymbol());
//		MarketDataSnapshotFullRefresh[] rte = new MarketDataSnapshotFullRefresh[instrumentPrices.length];
//		
//		int i = 0;
//		for ( Instrument instrumentPrice : instrumentPrices) {
//			quickfix.fix50.MarketDataSnapshotFullRefresh fRefresh = null;
//	
//			fRefresh = new quickfix.fix50.MarketDataSnapshotFullRefresh();
//			fRefresh.set(new MDReqID(request.getClientReqId()));
//			fRefresh.set(new Symbol(request.getSymbol()));
//			fRefresh.set(new SecurityType("FXSPOT"));
//			char[] entryTypes = null;
//			if ( request.getQuoteSide() == QuoteSide.BUY) {
//				entryTypes = new char[] { MDEntryType.BID };
//			} else if ( request.getQuoteSide() == QuoteSide.BUY) {
//				entryTypes = new char[] { MDEntryType.OFFER };
//			} else {
//				entryTypes = new char[] { MDEntryType.BID, MDEntryType.OFFER };
//			}
//			int count = 0;
//			for (char et : entryTypes)
//			{
//				for (com.tts.mlp.rate.provider.vo.Tick tick : instrumentPrice.getTicksByEntryType(et))
//				{
//					quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
//					noMDEntries.set(new MDEntryType(et));
//					noMDEntries.set(new MDEntryPx(tick.getPrice()));
//					noMDEntries.set(new MDEntrySize(tick.getQuantity()));
//					noMDEntries.set(new SettlType(SettlType.REGULAR));
//					noMDEntries.set(new MDEntryDate());
//					noMDEntries.set(new MDEntryPositionNo(tick.getLevel()));
//					noMDEntries.set(new QuoteCondition(QuoteCondition.OPEN_ACTIVE));
//					noMDEntries.set(new MDEntrySpotRate(tick.getPrice()));
//					fRefresh.addGroup(noMDEntries);
//					count++;
//				}
//			}
//			fRefresh.set(new NoMDEntries(count));
//	
//			rte[i++] = fRefresh;
//		}
//		return rte;
//	}
	

	@Override
	public Message push(long seq) {
		int mod = (int) (seq % 8);
		int sizeMod = 0;
		final SubscriptionRequestVo request = getRequest();
//		if ( ( seq % 7) == 0 ) {
		Instrument instrumentPrice = GlobalAppConfig.isRateFreezed() ? priceProvider.getCurrentPrice(request.getSymbol()) : priceProvider.getNextMarketPrice(request.getSymbol());
												//priceProvider.getNextMarketPrice(request.getSymbol());
		
		if ( GlobalAppConfig.isCrazyPriceStructure() ) {
			if ( seq % 1 == 0) {
				int n = (int) (seq % 9);
				sizeMod = (int) (n* Math.pow(-1.0, n));
			}
		}
		if ( instrumentPrice == null ) { return null; }
		
		quickfix.fix50.MarketDataSnapshotFullRefresh fRefresh = null;
		
		fRefresh = new quickfix.fix50.MarketDataSnapshotFullRefresh();
		String symbol = request.getSymbol();
		String sendOutsymbol = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);
		
		fRefresh.set(new MDReqID(request.getClientReqId()));
		fRefresh.set(new Symbol(sendOutsymbol));
		
		char[] entryTypes = null;
		if ( request.getQuoteSide() == QuoteSide.BUY) {
			entryTypes = new char[] { MDEntryType.BID };
		} else if ( request.getQuoteSide() == QuoteSide.BUY) {
			entryTypes = new char[] { MDEntryType.OFFER };
		} else {
			entryTypes = new char[] { MDEntryType.BID, MDEntryType.OFFER };
		}
		int count = 0;

		for (char et : entryTypes)
		{
			int c2 = 0;

			for (com.tts.mlp.rate.provider.vo.Tick tick : instrumentPrice.getTicksByEntryType(et))
			{
				quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
				noMDEntries.set(new MDEntryType(et));
				noMDEntries.set(new Currency(symbol.substring(0, 3)));

				noMDEntries.set(new MDEntryPx(tick.getPrice()));
				
				double size = tick.getQuantity() + sizeMod;
				
				if ( et ==  MDEntryType.BID && GlobalAppConfig.isDoubleBid() ) {
					size *= 2;
				} else if ( et == MDEntryType.OFFER && GlobalAppConfig.isDoubleOffer() ) {
					size *= 2;
				}
								
				noMDEntries.set(new MDEntrySize(size ));
								
				fRefresh.addGroup(noMDEntries);
				
				count++;
				c2++;
				//There is no point in sending a full depth book when is FWD ... so exit loop after one bid and offer has been added..
				if(
						(
								request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL) == false
								|| request.getTenor().equals(TenorVo.NOTATION_SPOT) == false
						)
						&& count >= 1) {
					break;
				}
				
			}
		}
		fRefresh.set(new NoMDEntries(count));
		getSession().send(fRefresh);
//		} else {
//			quickfix.fix50.MarketDataIncrementalRefresh[] deltaset = deltas[priceIdx];
//			
//			for ( int i = 0; i < deltaset.length; i++ ) {
//				getSession().send(message)
//			}
//		}
//		quickfix.fix50.Quote quote = quotes[(int) (seqQuoteId % quotes.length)];
//		quote.set(new QuoteID(Long.toString( quoteIdPrefix + seqQuoteId)));
//		getSession().send(quote);
		return fRefresh;
	}

	
	String getQuoteConditionForSymbol(String symbol) {
		if(priceSubscriptionRegistry != null && priceSubscriptionRegistry.getIndicativeStatus(symbol) == true) {
			return QuoteCondition.CLOSED_INACTIVE;
		} else {
			return QuoteCondition.OPEN_ACTIVE;
		}
	}

}
