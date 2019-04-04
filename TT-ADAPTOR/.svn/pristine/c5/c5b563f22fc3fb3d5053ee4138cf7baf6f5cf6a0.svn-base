package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.tts.fix.support.IFixListener;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.fxprice.repo.Esp50Repo;
import com.tts.plugin.adapter.support.IInstrumentDetailProperties;
import com.tts.plugin.adapter.support.IInstrumentDetailProvider;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.util.AppContext;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.UnsupportedMessageType;
import quickfix.field.MDUpdateAction;
/**
 * @author andy
 *
 */
public class FixPriceUpdaterFwd implements IFixListener {
	private final static Logger logger = LoggerFactory.getLogger(FixPriceUpdaterFwd.class);

	private final Map<String, IndividualInfoVo> sessionMarketSetup;
	private final IndividualPriceStore<ForwardCurve.Builder> priceStore;
	private final ForwardCurve.Builder[] fwdcurves;
	private final Fix50SnapshotUpdateFunction fix50SnapshotUpdateFunction;
	
	public FixPriceUpdaterFwd( 
			IFixIntegrationPluginSpi plugin, 
			IndividualPriceStore<ForwardCurve.Builder> priceStore, 
			ForwardCurve.Builder[] fwdcurves, 
			Map<String, IndividualInfoVo> sessionMarketSetup, 
			IEspRepo<?> espRepo) {
		this.priceStore = priceStore;
		this.fwdcurves = fwdcurves;
		this.sessionMarketSetup = sessionMarketSetup;
		this.fix50SnapshotUpdateFunction = new Fix50SnapshotUpdateFunction(plugin.getResponseDialectHelper(), sessionMarketSetup, espRepo);
	}

	@Override
	public void onMessage(quickfix.Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		if ( message instanceof quickfix.fix50.MarketDataSnapshotFullRefresh ) {
			ForwardCurve.Builder fcBuilder = priceStore.updateLatest(fix50SnapshotUpdateFunction, message);
			
			//IndividualInfoVo individualInfo = sessionMarketSetup.get(fcBuilder.getSymbol());
			int entry = 0;
			for(ForwardCurve.Builder fc : fwdcurves) {
				if(fc != null && fc.getSymbol() != null && fc.getSymbol().equals(fcBuilder.getSymbol()) && fc != fcBuilder) {
					fwdcurves[entry] = fcBuilder;
					if(fc.getSymbol().equals("EURUSD"))
						logger.info("Assigned "+fc.getSymbol()+" at entry "+entry+" currently has "+fcBuilder.getTenorsCount()+" tenors "+fc+" "+fcBuilder);
					break;
				}
				entry++;
			}
			
		} else 	if ( message instanceof quickfix.fix50.MarketDataIncrementalRefresh ) {
			
			quickfix.fix50.MarketDataIncrementalRefresh response = (quickfix.fix50.MarketDataIncrementalRefresh) message;
			int noMDEntries = response.getNoMDEntries().getValue();

			for ( int i =1 ; i <= noMDEntries; i++ ) {
				quickfix.fix50.MarketDataIncrementalRefresh.NoMDEntries noMDEntry = new quickfix.fix50.MarketDataIncrementalRefresh.NoMDEntries();
				response.getGroup(i, noMDEntry);
				
				if ( MDUpdateAction.DELETE == noMDEntry.getMDUpdateAction().getValue()) {
					logger.debug("Incoming data for " + priceStore.getLatest().getSymbol() + " is MDUpdateAction.DELETE. Flag as no market data.");

					priceStore.updateNextSlot(new UpdateFullBookWithNoMarketData(sessionMarketSetup), null);
				}
			}

			
		} 

		
	}
	
	private static class Fix50SnapshotUpdateFunction implements BiFunction<Message.Builder, Object, Message.Builder> {
		private final IResponseDialectHelper dialect;
		private final Map<String, IndividualInfoVo> sessionMarketSetup;
		private final IEspRepo<?> espRepo;

		private Fix50SnapshotUpdateFunction(final IResponseDialectHelper dialect,
				final Map<String, IndividualInfoVo> sessionMarketSetup, final IEspRepo<?> espRepo) {
			this.dialect = dialect;
			this.sessionMarketSetup = sessionMarketSetup;
			this.espRepo = espRepo;
		}

		@Override
		public Message.Builder apply(Message.Builder t, Object u) {
			ForwardCurve.Builder fcBuilder = (ForwardCurve.Builder) t;
			quickfix.fix50.MarketDataSnapshotFullRefresh response = (quickfix.fix50.MarketDataSnapshotFullRefresh) u;
			@SuppressWarnings("unused")
			String requestId;
			try {
				requestId = response.getMDReqID().getValue();
			} catch (Exception e) {
				return fcBuilder;
			}
			String symbol = fcBuilder.getSymbol();
			IndividualInfoVo individualInfo = sessionMarketSetup.get(symbol);
			try {
				FullBook.Builder fb = FullBook.newBuilder();
				dialect.convertAndUpdate(response, fb);
				fcBuilder.setLatency(fb.getLatencyBuilder());
				long indicativeFlag = fb.getIndicativeFlag();

				fcBuilder.setIndicativeFlag(indicativeFlag);

				long indicativeFlagConfig = individualInfo.getIndicativeFlag();
				if (IndicativeFlag.isContains(indicativeFlagConfig, IndicativeReason.MA_NoData)) {
					individualInfo.removeIndicativeReason(IndicativeReason.MA_NoData);
				}
				if (fb.getAskTicksCount() > 0 || fb.getBidTicksCount() > 0) {

					Tenor.Builder tenorFound = null;
					if (fb.getTenorsCount() > 0) {
						tenorFound = Tenor.newBuilder(fb.getTenors(0));

						if (tenorFound.getName().equals(TenorVo.NOTATION_OVERNIGHT)) {
							tenorFound.setName(TenorVo.NOTATION_TODAY);
						}
						// tenorFound.setActualDate(elements[1]);
						tenorFound.setQuoteRefId(fb.getQuoteRefId());
						((Esp50Repo) espRepo).registerPrice(fb.getSymbol(), tenorFound.getName(), fb, response);

						fcBuilder.clearTenors();
						fcBuilder.addTenors(tenorFound);
					}
				}
			} catch (FieldNotFound e) {
				e.printStackTrace();
			}
			return fcBuilder;
		}

		@SuppressWarnings("unused")
		public Message.Builder old_apply(Message.Builder t, Object u) {
			ForwardCurve.Builder fcBuilder = (ForwardCurve.Builder) t;
			quickfix.fix50.MarketDataSnapshotFullRefresh response = (quickfix.fix50.MarketDataSnapshotFullRefresh) u;
			String requestId;
			try {
				requestId = response.getMDReqID().getValue();
			} catch (Exception e) {
				return fcBuilder;
			}
			String symbol = fcBuilder.getSymbol();
			IndividualInfoVo individualInfo = sessionMarketSetup.get(symbol);
			try {
				FullBook.Builder fb = FullBook.newBuilder();
				dialect.convertAndUpdate(response, fb);
				fcBuilder.setLatency(fb.getLatencyBuilder());
				long indicativeFlag = fb.getIndicativeFlag();
				// if ( IndicativeFlag.isContains(indicativeFlag,
				// IndicativeReason.MA_NoData)) {
				// indicativeFlag =
				// IndicativeFlag.removeIndicative(indicativeFlag,
				// IndicativeReason.MA_NoData);
				// }
				fcBuilder.setIndicativeFlag(indicativeFlag);

				long indicativeFlagConfig = individualInfo.getIndicativeFlag();
				if (IndicativeFlag.isContains(indicativeFlagConfig, IndicativeReason.MA_NoData)) {
					individualInfo.removeIndicativeReason(IndicativeReason.MA_NoData);
				}
				if (fb.getAskTicksCount() > 0 || fb.getBidTicksCount() > 0) {
					// Which Tenor has been updated...
					String[] parts = requestId.split("\\.");
					if (parts.length > 1) {
						Tenor.Builder tenorFound = null;
						Tenor.Builder tenorFoundTN = null;
						for (Tenor.Builder tnr : fcBuilder.getTenorsBuilderList()) {
							if (tnr.getName().equals(parts[1])) {
								tenorFound = tnr;
								if (parts[1].equals("ON") == false || (parts[1].equals("ON") == true
										&& tenorFound != null && tenorFoundTN != null))
									break;
							}
							if (tnr.getName().equals("TN")) {
								tenorFoundTN = tnr;
							}
							if (tenorFound != null && tenorFoundTN != null) {
								break;
							}
						}

						if (tenorFound == null) {
							tenorFound = Tenor.newBuilder();
							tenorFound.setName(parts[1]);
							if (symbol.equals("EURUSD"))
								logger.info("Added TENOR " + symbol + "." + parts[1]);
							fcBuilder.addTenors(tenorFound);
						}

						// tenorFound.setActualDate(elements[1]);
						tenorFound.setQuoteRefId(fb.getQuoteRefId());
						((Esp50Repo) espRepo).registerPrice(fb.getSymbol(), tenorFound.getName(), fb, response);

						if (fb.getBidTicksCount() > 0) {
							if (fb.getBidTicks(0).getRate() != null && fb.getBidTicks(0).getRate().length() > 0) {
								BigDecimal outright = new BigDecimal(fb.getBidTicks(0).getRate());
								BigDecimal spot = new BigDecimal(fb.getBidTicks(0).getSpotRate());
								BigDecimal points = outright.subtract(spot);

								IInstrumentDetailProperties instr = null;
								if (instr != null) {
									points = points.movePointRight(instr.getPointValue());
								}
								if (parts[1].equals("ON") && tenorFoundTN != null
										&& tenorFoundTN.getAskSwapPoints() != null
										&& tenorFoundTN.getAskSwapPoints().length() > 0) {
									points = points.add(new BigDecimal(tenorFoundTN.getAskSwapPoints())).negate();
									tenorFound.setAskSwapPoints(points.toPlainString());
									// if(symbol.equals("GBPUSD")) {
									// logger.info("ask raw =
									// "+points.toPlainString());
									// }
								} else if (parts[1].equals("TN")) {
									tenorFound.setAskSwapPoints(points.negate().toPlainString());
								} else {
									tenorFound.setBidSwapPoints(points.toPlainString());
								}
							}
						}
						if (fb.getAskTicksCount() > 0) {
							if (fb.getAskTicks(0).getRate() != null && fb.getAskTicks(0).getRate().length() > 0) {
								BigDecimal outright = new BigDecimal(fb.getAskTicks(0).getRate());
								BigDecimal spot = new BigDecimal(fb.getAskTicks(0).getSpotRate());
								BigDecimal points = outright.subtract(spot);
								IInstrumentDetailProperties instr = null;

								if (instr != null) {
									points = points.movePointRight(instr.getPointValue());
								}

								if (parts[1].equals("ON") && tenorFoundTN != null
										&& tenorFoundTN.getBidSwapPoints() != null
										&& tenorFoundTN.getAskSwapPoints().length() > 0) {
									points = points.add(new BigDecimal(tenorFoundTN.getBidSwapPoints())).negate();
									tenorFound.setBidSwapPoints(points.toPlainString());
									// if(symbol.equals("GBPUSD")) {
									// logger.info("bid raw =
									// "+points.toPlainString());
									// }
								} else if (parts[1].equals("TN")) {
									tenorFound.setBidSwapPoints(points.negate().toPlainString());
								} else {
									tenorFound.setAskSwapPoints(points.toPlainString());
								}
							}
						}
					}
				}
			} catch (FieldNotFound e) {
				e.printStackTrace();
			}
			return fcBuilder;
		}

	}

	@Override
	public void onFixSessionLogoff() {
		for (int i = 0; i < IndividualPriceStore.INDIVIDUAL_STORE_QUEUE_SIZE; i++ ) {
			logger.debug("Flag " + priceStore.getLatest().getSymbol() + " as no market data because of FIX session logoff");
			priceStore.updateNextSlot(new UpdateForwardCurveWithNoMarketData(sessionMarketSetup), new Object());
		}
		sessionMarketSetup.get(priceStore.getLatest().getSymbol()).setLastRefresh(0L);
		sessionMarketSetup.get(priceStore.getLatest().getSymbol()).setLastRequest(-1L);
	}
	
	@Override
	public void onFixSessionLogon() {
		// TODO Auto-generated method stub
	}
}
