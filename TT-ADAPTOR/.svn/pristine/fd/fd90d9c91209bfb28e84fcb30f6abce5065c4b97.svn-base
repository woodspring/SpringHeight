package com.tts.mlp.app.price.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.tts.mlp.app.ForwardCurveDataManager;
import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.vo.TenorVo;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.MDEntryDate;
import quickfix.field.MDEntryForwardPoints;
import quickfix.field.MDEntryID;
import quickfix.field.MDEntryPositionNo;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntrySpotRate;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.NoMDEntries;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.QuoteCondition;
import quickfix.field.SecurityType;
import quickfix.field.SettlDate;
import quickfix.field.SettlType;
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
		final SubscriptionRequestVo request = getRequest();
//		if ( ( seq % 7) == 0 ) {
		final Instrument instrumentPrice = GlobalAppConfig.isRateFreezed() || !request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL) ? priceProvider.getCurrentPrice(request.getSymbol()) : priceProvider.getNextMarketPrice(request.getSymbol());
		if ( instrumentPrice == null ) { return null; }
		
		quickfix.fix50.MarketDataSnapshotFullRefresh fRefresh = null;
		
		fRefresh = new quickfix.fix50.MarketDataSnapshotFullRefresh();
		fRefresh.set(new MDReqID(request.getClientReqId()));
		fRefresh.set(new Symbol(request.getSymbol()));
		if ( request.getOnBehaveOf() != null ) {
			fRefresh.getHeader().setField(new OnBehalfOfCompID(request.getOnBehaveOf()));
		}
		if(request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL)) {
			fRefresh.set(SecurityType_SPOT);
		} else {
			fRefresh.set(SecurityType_FWD);
		}
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
			for (com.tts.mlp.rate.provider.vo.Tick tick : instrumentPrice.getTicksByEntryType(et))
			{
				quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
				noMDEntries.set(new MDEntryType(et));
				noMDEntries.set(new MDEntryID(Long.toString(seq)));
				
				if(request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL) || request.getTenor().equals(TenorVo.NOTATION_SPOT)) {
					
					noMDEntries.set(new MDEntryPx(DoubleFormatter.roundDouble(tick.getPrice(), instrumentPrice.getPrecision(), RoundingMode.HALF_EVEN)));
					noMDEntries.set(new MDEntryForwardPoints(0.0));
					noMDEntries.set(new MDEntrySpotRate(tick.getPrice()));

				} else {
					noMDEntries.set(new MDEntrySpotRate(tick.getPrice()));
					double pts = ForwardCurveDataManager.GetFwdPoints(request.getSymbol(),request.getTenor(), et);
					
					BigDecimal ptsBd = new BigDecimal(pts).movePointLeft(instrumentPrice.getPointValue()).setScale(instrumentPrice.getPointValue()+2, BigDecimal.ROUND_HALF_UP);
					System.out.println(request.getSymbol() + " " + pts + " " + ptsBd.toPlainString());
					noMDEntries.set(new MDEntryForwardPoints(ptsBd.doubleValue()));
					noMDEntries.set(new MDEntryPx(new BigDecimal(tick.getPrice()).add(ptsBd).setScale(instrumentPrice.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue()));
				}
				if ( GlobalAppConfig.getOverrideSettleDate(request.getSymbol()) != null) {
					noMDEntries.set(new SettlDate( GlobalAppConfig.getOverrideSettleDate(request.getSymbol()) ));
				} else if ( request.getSettleDate() != null ) {
					noMDEntries.set(new SettlDate(request.getSettleDate()));
				}
				noMDEntries.set(new MDEntrySize(tick.getQuantity()));
				if(request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL) || request.getTenor().equals("SP")) {
					noMDEntries.set(new SettlType(SettlType.REGULAR));
				} else {
					noMDEntries.set(new SettlType("ON"));
				}
				noMDEntries.set(new MDEntryDate());
				noMDEntries.set(new MDEntryPositionNo(tick.getLevel()));
				noMDEntries.set(new QuoteCondition(getQuoteConditionForSymbol(request.getSymbol())));
				noMDEntries.set(new MDEntrySpotRate(tick.getPrice()));
				fRefresh.addGroup(noMDEntries);
				count++;
				
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
