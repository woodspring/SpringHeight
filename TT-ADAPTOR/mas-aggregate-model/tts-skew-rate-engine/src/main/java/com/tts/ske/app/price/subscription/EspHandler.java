package com.tts.ske.app.price.subscription;

import com.tts.message.market.MarketStruct.RawLiquidityEntry;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.ske.vo.SubscriptionRequestVo;
import com.tts.ske.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.util.AppConfig;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.Currency;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.NoMDEntries;
import quickfix.field.SecurityType;
import quickfix.field.Symbol;

public class EspHandler extends AbstractSubscriptionHandler implements IMdSubscriber {
	public final static SecurityType SecurityType_SPOT = new SecurityType("FXSPOT");

	private final PriceSubscriptionRegistry priceSubscriptionRegistry;
	private final boolean timedInjection;
	private volatile RawMarketBook marketData = null;

	public EspHandler(SubscriptionRequestVo request, SessionID sessionid, quickfix.Message originalMessage,
			PriceSubscriptionRegistry registry) {
		super(request, sessionid, originalMessage);
		priceSubscriptionRegistry = registry;
		
		String enableSelfInjection = AppConfig.getValue("marketData", "enableSelfInjection");
		
		this.timedInjection = "TRUE".equalsIgnoreCase(enableSelfInjection);
	}

	@Override
	public Message push(long seq) {
		final SubscriptionRequestVo request = getRequest();

		quickfix.fix42.MarketDataSnapshotFullRefresh fRefresh = null;

		fRefresh = new quickfix.fix42.MarketDataSnapshotFullRefresh();
		String symbol = request.getSymbol();
		String sendOutsymbol = symbol.substring(0, 3) + '/' + symbol.substring(3, 6);

		fRefresh.set(new MDReqID(request.getClientReqId()));
		fRefresh.set(new Symbol(sendOutsymbol));
		int count = 0;

		if (marketData != null) {
			if (request.getQuoteSide() == null || request.getQuoteSide() == QuoteSide.SELL
					|| request.getQuoteSide() == QuoteSide.BOTH) {
				for (RawLiquidityEntry tick : marketData.getBidQuoteList()) {
					quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries = new quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries();
					noMDEntries.set(new MDEntryType(MDEntryType.BID));
					noMDEntries.set(new Currency(symbol.substring(0, 3)));
					noMDEntries.set(new MDEntryPx(tick.getRate()));
					double size = tick.getSize();
					noMDEntries.set(new MDEntrySize(size));
					if ( size == 0.0d) {
						continue;
					} 
					fRefresh.addGroup(noMDEntries);
					count++;

				}
			}
			if (request.getQuoteSide() == null || request.getQuoteSide() == QuoteSide.BUY
					|| request.getQuoteSide() == QuoteSide.BOTH) {
				for (RawLiquidityEntry tick : marketData.getAskQuoteList()) {
					quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries = new quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries();
					noMDEntries.set(new MDEntryType(MDEntryType.OFFER));
					noMDEntries.set(new Currency(symbol.substring(0, 3)));
					noMDEntries.set(new MDEntryPx(tick.getRate()));
					double size = tick.getSize();
					noMDEntries.set(new MDEntrySize(size));
					if ( size == 0.0d) {
						continue;
					} 
					fRefresh.addGroup(noMDEntries);
					count++;
				}
			}
		}
		fRefresh.set(new NoMDEntries(count));
		getSession().send(fRefresh);

		return fRefresh;
	}

	@Override
	public void onNewMarketData(String symbol, RawMarketBook mb) {
		this.marketData = mb;
		if ( !timedInjection ) {
			push(0);
		}
	}

}
