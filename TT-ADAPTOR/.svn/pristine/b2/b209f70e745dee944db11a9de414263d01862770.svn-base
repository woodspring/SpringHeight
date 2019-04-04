package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.math.RoundingMode;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.IFixListener;
import com.tts.message.eas.rate.RateConverter;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.fxprice.RefreshRequesterSpot.OnMDCancelCallback;
import com.tts.plugin.adapter.impl.base.app.fxprice.repo.Esp44Repo;
import com.tts.plugin.adapter.impl.base.app.fxprice.repo.Esp50Repo;
import com.tts.plugin.adapter.impl.base.util.VWAPUtil;
import com.tts.plugin.adapter.support.IInstrumentDetailProperties;
import com.tts.plugin.adapter.support.IInstrumentDetailProvider;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.util.AppContext;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.TenorVo;
import com.tts.vo.TickBookVo;
import com.tts.vo.TickVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.UnsupportedMessageType;
import quickfix.field.MDUpdateAction;

public class FixPriceUpdater implements IFixListener {
	private final static Logger logger = LoggerFactory.getLogger(FixPriceUpdater.class);

	private final Map<String, IndividualInfoVo> sessionMarketSetup;
	private final IndividualPriceStore<FullBook.Builder> priceStore;
	private final Fix50Fix44SnapshotUpdateFunction fix50fix44SnapshotUpdateFunction;
	private final OnMDCancelCallback onMDCancelCallback;

	public FixPriceUpdater( 
			IFixIntegrationPluginSpi plugin, 
			IndividualPriceStore<FullBook.Builder> priceStore, 
			Map<String, IndividualInfoVo> sessionMarketSetup, 
			IEspRepo<?> espRepo, 
			OnMDCancelCallback onMDCancelCallback) {
		IInstrumentDetailProvider instrumentDetailProvider = AppContext.getContext().getBean(IInstrumentDetailProvider.class);

		FixApplicationProperties p = AppContext.getContext().getBean(FixApplicationProperties.class);
		boolean isVWAPenabled = p.getProperty("spotadapter.vwap", false);
		this.priceStore = priceStore;
		this.sessionMarketSetup = sessionMarketSetup;
		this.onMDCancelCallback = onMDCancelCallback;
		this.fix50fix44SnapshotUpdateFunction = new Fix50Fix44SnapshotUpdateFunction(
				isVWAPenabled, 
				plugin.getResponseDialectHelper(), 
				sessionMarketSetup, 
				espRepo,
				instrumentDetailProvider
		);

	}

	@Override
	public void onMessage(quickfix.Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		if ( message instanceof quickfix.fix50.MarketDataSnapshotFullRefresh ) {
			priceStore.updateNextSlot(fix50fix44SnapshotUpdateFunction, message);
		} else 	if ( message instanceof quickfix.fix50.MarketDataIncrementalRefresh ) {
			
			quickfix.fix50.MarketDataIncrementalRefresh response = (quickfix.fix50.MarketDataIncrementalRefresh) message;
			int noMDEntries = response.getNoMDEntries().getValue();

			for ( int i =1; i <= noMDEntries; i++ ) {
				quickfix.fix50.MarketDataIncrementalRefresh.NoMDEntries noMDEntry = new quickfix.fix50.MarketDataIncrementalRefresh.NoMDEntries();
				response.getGroup(i, noMDEntry);
				
				if ( MDUpdateAction.DELETE == noMDEntry.getMDUpdateAction().getValue()) {
					logger.debug("Incoming data for " + priceStore.getLatest().getSymbol() + " is MDUpdateAction.DELETE. Flag as no market data.");
					FullBook.Builder fbBuilder = priceStore.updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), new Object());
					sessionMarketSetup.get(fbBuilder.getSymbol()).setLastRefresh(-1L);
				}
			}

		}else if ( message instanceof quickfix.fix50.MarketDataRequestReject ) {
			FullBook.Builder fbBuilder = priceStore.updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), new Object());
			sessionMarketSetup.get(fbBuilder.getSymbol()).setLastRefresh(-1L);
			onMDCancelCallback.apply(null);
			
		} else 	if ( message instanceof quickfix.fix44.MarketDataSnapshotFullRefresh ) {
			priceStore.updateNextSlot(fix50fix44SnapshotUpdateFunction, message);
		} else 	if ( message instanceof quickfix.fix44.MarketDataIncrementalRefresh ) {
			
			quickfix.fix44.MarketDataIncrementalRefresh response = (quickfix.fix44.MarketDataIncrementalRefresh) message;
			int noMDEntries = response.getNoMDEntries().getValue();

			for ( int i =1 ; i <= noMDEntries; i++ ) {
				quickfix.fix44.MarketDataIncrementalRefresh.NoMDEntries noMDEntry = new quickfix.fix44.MarketDataIncrementalRefresh.NoMDEntries();
				response.getGroup(i, noMDEntry);
				
				if ( MDUpdateAction.DELETE == noMDEntry.getMDUpdateAction().getValue()) {
					logger.debug("Incoming data for " + priceStore.getLatest().getSymbol() + " is MDUpdateAction.DELETE. Flag as no market data.");

					FullBook.Builder fbBuilder = priceStore.updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), new Object());
					sessionMarketSetup.get(fbBuilder.getSymbol()).setLastRefresh(-1L);

				}
			}

			
		} else if ( message instanceof quickfix.fix44.MarketDataRequestReject ) {
			FullBook.Builder fbBuilder = priceStore.updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), new Object());
			sessionMarketSetup.get(fbBuilder.getSymbol()).setLastRefresh(-1L);
			onMDCancelCallback.apply(null);
		}

		
	}
	
	private static class Fix50Fix44SnapshotUpdateFunction implements BiFunction<Message.Builder, Object, Message.Builder> {
		private final IResponseDialectHelper dialect;
		private final Map<String, IndividualInfoVo> sessionMarketSetup;
		private final IEspRepo<?> espRepo;
		private final IInstrumentDetailProvider instrumentDetailProvider;
		private final boolean isVWAPenabled;
		
		private Fix50Fix44SnapshotUpdateFunction(
				final boolean isVWAPenabled,
				final IResponseDialectHelper dialect,
				final Map<String, IndividualInfoVo> sessionMarketSetup, 
				final IEspRepo<?> espRepo,
				IInstrumentDetailProvider instrumentDetailProvider) {
			this.isVWAPenabled = isVWAPenabled;
			this.dialect = dialect;
			this.sessionMarketSetup = sessionMarketSetup;
			this.espRepo = espRepo;
			this.instrumentDetailProvider = instrumentDetailProvider; 
		}

		@Override
		public Message.Builder apply(Message.Builder t, Object u) {
			FullBook.Builder fbBuilder = (FullBook.Builder ) t;
			quickfix.fix50.MarketDataSnapshotFullRefresh fix50resp = null;
			quickfix.fix44.MarketDataSnapshotFullRefresh fix44resp = null;
			
			if ( u instanceof quickfix.fix50.MarketDataSnapshotFullRefresh ) {
				fix50resp = (quickfix.fix50.MarketDataSnapshotFullRefresh) u;
			} else if ( u instanceof quickfix.fix44.MarketDataSnapshotFullRefresh ) {
				fix44resp = (quickfix.fix44.MarketDataSnapshotFullRefresh) u;
			}
 
			String symbol = fbBuilder.getSymbol();
			IndividualInfoVo individualInfo = sessionMarketSetup.get(symbol);
			try {
				if ( fix50resp != null) {
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.convertAndUpdate starting");
					}
					dialect.convertAndUpdate(fix50resp, fbBuilder);
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.convertAndUpdate completed");
					}
					if ( isVWAPenabled ) {
						if ( individualInfo.isDebug() ) {
							logger.debug(symbol + " vwap starting");
						}
						IInstrumentDetailProperties id = instrumentDetailProvider.getInstrumentDetail(symbol);
						int pp = id.getPrecision();
						TickBookVo tickBook = RateConverter.convertFullBookToTickBook(fbBuilder);
						TickBookVo vwapBook = VWAPUtil.calculateVWAP(tickBook, individualInfo.getLiquidities());
						fbBuilder.clearAskTicks();
						fbBuilder.clearBidTicks();
						for ( int i = 1; i <= vwapBook.getAskTicks().size(); i++) {
							TickVo ask = vwapBook.getAskTicks().get(i-1);
							Tick.Builder tt = Tick.newBuilder();
							tt.setLevel(i);
							tt.setRate(ask.getRate().toPrecisionString(RoundingMode.CEILING, pp));
							tt.setSize(ask.getSize());
							fbBuilder.addAskTicks(tt);
						}
						for ( int i = 1; i <= vwapBook.getBidTicks().size(); i++) {
							TickVo bid = vwapBook.getBidTicks().get(i-1);
							Tick.Builder tt = Tick.newBuilder();
							tt.setLevel(i);
							tt.setRate(bid.getRate().toPrecisionString(RoundingMode.FLOOR, pp));
							tt.setSize(bid.getSize());
							fbBuilder.addBidTicks(tt);
						}
						if ( individualInfo.isDebug() ) {
							logger.debug(symbol + " vwap completed");
						}
					}
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.postValidate starting");
					}
					dialect.postValidate(fbBuilder, fix50resp);
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.postValidate completed");
					}
					((Esp50Repo) espRepo).registerPrice(symbol, TenorVo.NOTATION_SPOT, fbBuilder, fix50resp);

				} else if ( fix44resp != null) {
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.convertAndUpdate starting");
					}
					dialect.convertAndUpdate(fix44resp, fbBuilder);
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.convertAndUpdate completed");
					}
					if ( isVWAPenabled ) {
						if ( individualInfo.isDebug() ) {
							logger.debug(symbol + " vwap starting");
						}
						IInstrumentDetailProperties id = instrumentDetailProvider.getInstrumentDetail(symbol);
						int pp = id.getPrecision();

						TickBookVo tickBook = RateConverter.convertFullBookToTickBook(fbBuilder);

						TickBookVo vwapBook = VWAPUtil.calculateVWAP(tickBook, individualInfo.getLiquidities());

						fbBuilder.clearAskTicks();
						fbBuilder.clearBidTicks();
						for ( int i = 1; i <= vwapBook.getAskTicks().size(); i++) {
							TickVo ask = vwapBook.getAskTicks().get(i-1);
							Tick.Builder tt = Tick.newBuilder();
							tt.setLevel(i);
							tt.setRate(ask.getRate().toPrecisionString(RoundingMode.CEILING, pp));
							tt.setSize(ask.getSize());
							fbBuilder.addAskTicks(tt);
						}
						for ( int i = 1; i <= vwapBook.getBidTicks().size(); i++) {
							TickVo bid = vwapBook.getBidTicks().get(i-1);
							Tick.Builder tt = Tick.newBuilder();
							tt.setLevel(i);
							tt.setRate(bid.getRate().toPrecisionString(RoundingMode.FLOOR, pp));
							tt.setSize(bid.getSize());
							fbBuilder.addBidTicks(tt);
						}
						if ( individualInfo.isDebug() ) {
							logger.debug(symbol + " vwap completed");
						}
					}
					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.postValidate starting");
					}
					dialect.postValidate(fbBuilder, fix44resp);

					if ( individualInfo.isDebug() ) {
						logger.debug(symbol + " dialect.postValidate completed");
					}
					((Esp44Repo) espRepo).registerPrice(symbol, TenorVo.NOTATION_SPOT, fbBuilder, fix44resp);

				}

				long indicativeFlag = fbBuilder.getIndicativeFlag();
				if ( IndicativeFlag.isContains(indicativeFlag, IndicativeReason.MA_NoData)) {
					indicativeFlag = IndicativeFlag.removeIndicative(indicativeFlag, IndicativeReason.MA_NoData);
					fbBuilder.setIndicativeFlag(indicativeFlag);
					logger.debug("Removing NO_MD_DATA flag in fbBuilder, " + fbBuilder.getIndicativeFlag() );
				}
				long indicativeFlagConfig = individualInfo.getIndicativeFlag();
				if ( IndicativeFlag.isContains(indicativeFlagConfig, IndicativeReason.MA_NoData)) {
					individualInfo.removeIndicativeReason(IndicativeReason.MA_NoData);
					logger.debug("Removing NO_MD_DATA flag in config, " + symbol + " "  + individualInfo.getIndicativeFlag());
				}
				long currentTimeMillis = System.currentTimeMillis();
				fbBuilder.getLatencyBuilder().setFaReceiveTimestamp(currentTimeMillis);
				individualInfo.setLastRefresh(currentTimeMillis);
			} catch (FieldNotFound e) {
				e.printStackTrace();
			}
			return fbBuilder;
		}
		
	}

	@Override
	public void onFixSessionLogoff() {
		
		for (int i = 0; i < IndividualPriceStore.INDIVIDUAL_STORE_QUEUE_SIZE; i++ ) {
			logger.debug("Flag " + priceStore.getLatest().getSymbol() + " as no market data because of FIX session logoff");
			priceStore.updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), new Object());
		}
		onMDCancelCallback.apply(null);

		sessionMarketSetup.get(priceStore.getLatest().getSymbol()).setLastRefresh(0L);
		sessionMarketSetup.get(priceStore.getLatest().getSymbol()).setLastRequest(-1L);
	}
	
	@Override
	public void onFixSessionLogon() {
		// TODO Auto-generated method stub
	}
}
