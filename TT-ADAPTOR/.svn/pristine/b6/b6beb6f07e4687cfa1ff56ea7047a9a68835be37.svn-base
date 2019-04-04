package com.tts.mde.fwdc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.mlp.data.provider.MarketDataFetcher;
import com.tts.mlp.data.provider.vo.InstrumentSwapPointsVo;
import com.tts.mlp.data.provider.vo.SwapPointEntityVo;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.util.AppContext;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;

public class ForwardCurveRunnable implements Runnable {
	private static final ZoneId refZone = ZoneId
			.of(com.tts.service.biz.calendar.FxCalendarServiceConfig.DEFAULT_TIMEZONE_NAME);
	private final static Logger logger = LoggerFactory.getLogger(ForwardCurveRunnable.class);
	private final IFxCalendarBizService fxCalendarBizService;
	private final List<String> interestedSymbol;
	private final List<String> debugSymbols;
	private final ISymbolIdMapper symbolIdMapper;
	private final ForwardCurve.Builder[] fwdCurves;
	private final static NumberVo ONE = NumberVo.getInstance("1.0000");
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final boolean showTplus0Tenor;
	private final boolean showONTNTenor;
	private int callNumber = 0;

	public ForwardCurveRunnable(List<String> interestedSymbol, ISymbolIdMapper symbolIdMapper,
			ForwardCurve.Builder[] fwdCurves) {
		super();
		this.interestedSymbol = interestedSymbol;
		this.symbolIdMapper = symbolIdMapper;
		this.fwdCurves = fwdCurves;

		List<String> _debugSymbols = null;
		try {
			String setting = System.getProperty("getSwapPoints.debugSymbols");
			String[] symbols = setting.split(",");
			_debugSymbols = Arrays.asList(symbols);
		} catch (Exception e) {

		}
		if (_debugSymbols == null) {
			this.debugSymbols = Collections.emptyList();
		} else {
			this.debugSymbols = _debugSymbols;
		}
		logger.debug(this.debugSymbols + " are being set to be debug");

		boolean _showTPlus0Tenor = true;
		String settingShowTPlus0Tenor = System.getProperty("getSwapPoints.showTPlus0Tenor");
		try {
			if (settingShowTPlus0Tenor != null) {
				_showTPlus0Tenor = Boolean.parseBoolean(settingShowTPlus0Tenor);
			}
		} catch (Exception e) {
			_showTPlus0Tenor = true;
		}
		boolean _showONTNTenor = true;
		String settingShowONTNTenor = System.getProperty("getSwapPoints.showONTNTenor");
		try {
			if (settingShowONTNTenor != null) {
				_showONTNTenor = Boolean.parseBoolean(settingShowONTNTenor);
			}
		} catch (Exception e) {
			_showONTNTenor = true;
		}
		this.showONTNTenor = _showONTNTenor;
		this.showTplus0Tenor = _showTPlus0Tenor;
		this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
	}

	@Override
	public void run() {
		HashMap<String, List<Tenor.Builder>> result = new HashMap<String, List<Tenor.Builder>>();
		HashMap<String, Tenor.Builder> tenorONset = new HashMap<String, Tenor.Builder>();
		HashMap<String, Tenor.Builder> tenorTNset = new HashMap<String, Tenor.Builder>();

		logger.debug("Creating instance of MarketDataFetcher");
		MarketDataFetcher fetcher = new MarketDataFetcher();
		logger.debug("Created instance of MarketDataFetcher. ready to fetch data for " + interestedSymbol.toString());

		if (interestedSymbol.size() == 0) {
			logger.warn(
					"No ccyPair swap points data interested. nothing in MarketDataSet!?! skip calling getSwapPoints");
			return;
		}
		if (running.compareAndSet(false, true)) {
			try {
				long startTime = System.currentTimeMillis();
				callNumber++;
				StringBuilder sb = new StringBuilder("getSwapPoints. ");

				for (String symbol : interestedSymbol) {
					InstrumentSwapPointsVo swapPointData = null;
					try {
						swapPointData = fetcher.getSwapPoints(symbol);
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (swapPointData != null) {
						List<SwapPointEntityVo> spList = swapPointData.getFwdPointsEntries();

						for (SwapPointEntityVo sp : spList) {
							TenorVo tenor = null;

							// validate
							try {
								String tenorNm = sp.getTenorNm();
								if (TenorVo.NOTATION_SPOTWEEK.equals(tenorNm)) {
									tenorNm = "1W";
								}
								tenor = TenorVo.fromString(tenorNm);
							} catch (NumberFormatException e) {
								logger.warn(
										"Skip processing SwapPointEntity. Unable to handle tenor, " + sp.getTenorNm());
								continue;
							}
							List<Tenor.Builder> tenorList = result.get(symbol);
							if (tenorList == null) {
								tenorList = new ArrayList<Tenor.Builder>();
								result.put(symbol, tenorList);
							}

							Tenor.Builder t = Tenor.newBuilder();
							t.setAskSwapPoints(BigDecimal.valueOf(sp.getAskSwapPoint()).toPlainString());
							t.setBidSwapPoints(BigDecimal.valueOf(sp.getBidSwapPoint()).toPlainString());
							t.setBidSpotRefRate(BigDecimal.valueOf(sp.getBidSpotRefRate()).toPlainString());
							t.setAskSpotRefRate(BigDecimal.valueOf(sp.getAskSpotRefRate()).toPlainString());
							t.setName(tenor.toString());

							tenorList.add(t);
							if (TenorVo.NOTATION_OVERNIGHT.equals(tenor.getTenorNm())) {
								tenorONset.put(symbol, t);
							}
							if (TenorVo.NOTATION_TOMORROWNIGHT.equals(tenor.getTenorNm())) {
								tenorTNset.put(symbol, Tenor.newBuilder(t.build()));
							}
						}
					}
				}
				long receivedTime = System.currentTimeMillis();

				Set<Entry<String, List<Tenor.Builder>>> entrySet = result.entrySet();

				LocalDate currentDate = LocalDate.now(refZone);
				DayOfWeek d = currentDate.getDayOfWeek();
				if (d == DayOfWeek.SATURDAY) {
					currentDate = currentDate.plusDays(2);
				} else if (d == DayOfWeek.SUNDAY) {
					currentDate = currentDate.plusDays(1);
				}

				boolean isUSDBusinessDay = fxCalendarBizService.isBusinessDay("USD", currentDate);
				for (Entry<String, List<Tenor.Builder>> e : entrySet) {
					if (interestedSymbol.contains(e.getKey())) {
						int spotLag = fxCalendarBizService.getSpotLagInDefaultPriceConvention(e.getKey());
						int pos = symbolIdMapper.map(e.getKey());
						ForwardCurve.Builder fc = fwdCurves[pos];
						fc.clearTenors();
						if (!e.getKey().equals(fc.getSymbol())) {
							logger.warn("error in lookup corelated forwardcurve, " + e.getKey());
						}
						Tenor.Builder tenorON = tenorONset.get(fc.getSymbol());
						Tenor.Builder tenorTN = tenorTNset.get(fc.getSymbol());
						List<Tenor.Builder> tenors = e.getValue();
						for (int i = 0; i < tenors.size(); i++) {
							Tenor.Builder t = tenors.get(i);

							fc.addTenors(t);
						}
						if ( showTplus0Tenor ) {
							boolean gotEnoughData = true;
							if ( isUSDBusinessDay ) {
								Tenor.Builder t = Tenor.newBuilder();
								if ( spotLag == 2 ) {
									//Tenor.Builder tenorTN = tenorTNset.get(symbol);
									if ( tenorTN != null) {
										t.setBidSwapPoints(NumberVo.getInstance(tenorTN.getAskSwapPoints()).multiply(-1).minus(tenorON.getAskSwapPoints()).getValue());
										t.setAskSwapPoints(NumberVo.getInstance(tenorTN.getBidSwapPoints()).multiply(-1).minus(tenorON.getBidSwapPoints()).getValue());
									} else {
										gotEnoughData = false;
									}
								} else {
									if ( tenorON != null) {
										t.setBidSwapPoints(NumberVo.getInstance(tenorON.getAskSwapPoints()).multiply(-1).getValue());
										t.setAskSwapPoints(NumberVo.getInstance(tenorON.getBidSwapPoints()).multiply(-1).getValue());
									} else {
										gotEnoughData = false;
									}
								}
								t.setBidSpotRefRate(tenorTN.getBidSpotRefRate().toString());
								t.setAskSpotRefRate(tenorON.getAskSpotRefRate().toString());
								if ( gotEnoughData ) {
									t.setName(TenorVo.Tenor_Tplus0.getTenorNm());
									fc.addTenors(0, t);
								}

							} else {
								Tenor.Builder t = Tenor.newBuilder();
								if ( tenorON != null) {
									t.setBidSwapPoints(NumberVo.getInstance(tenorON.getAskSwapPoints()).multiply(-1).getValue());
									t.setAskSwapPoints(NumberVo.getInstance(tenorON.getBidSwapPoints()).multiply(-1).getValue());
									t.setName(TenorVo.Tenor_Tplus0.getTenorNm());
									fc.addTenors(0, t);
								}
							}
							
							
						}
						fc.getLatencyBuilder().setFaReceiveTimestamp(receivedTime);
						if (((callNumber % 8) == 0) && debugSymbols.contains(fc.getSymbol())) {
							logger.debug("interpreted data from getSwapPoints: " + TextFormat.shortDebugString(fc));
						}
					}
				}
				long endTime = System.currentTimeMillis();
				logger.debug("Completed updating forwardcurve by webservice. Took:" + (endTime - startTime)
						+ ", webservice call took:" + (receivedTime - startTime));
				if ((callNumber % 8) == 0) {
					logger.debug(sb.toString());
				}
			} finally {
				running.set(false);
			}
		} else {
			logger.warn("Previous webservice call still running. skip running...");
		}
	}

}
