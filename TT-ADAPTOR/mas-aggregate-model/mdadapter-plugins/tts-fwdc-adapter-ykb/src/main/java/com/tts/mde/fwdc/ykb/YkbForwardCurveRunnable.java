package com.tts.mde.fwdc.ykb;

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
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.util.AppContext;
import com.tts.vo.NumberVo;
import com.tts.vo.TenorVo;
import com.tts.ws.plugin.api.entity.SwapPointEntity;
import com.tts.ws.plugin.ykb.impl.YkbWebServices;

public class YkbForwardCurveRunnable implements Runnable {
	private static final ZoneId refZone = ZoneId.of(com.tts.service.biz.calendar.FxCalendarServiceConfig.DEFAULT_TIMEZONE_NAME);
	private final static Logger logger = LoggerFactory.getLogger(YkbForwardCurveRunnable.class);
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

	public YkbForwardCurveRunnable(List<String> interestedSymbol, ISymbolIdMapper symbolIdMapper, ForwardCurve.Builder[] fwdCurves) {
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
		if ( _debugSymbols == null ) {
			this.debugSymbols = Collections.emptyList();
		} else {
			this.debugSymbols = _debugSymbols;
		}
		logger.debug(this.debugSymbols + " are being set to be debug");
		
		boolean _showTPlus0Tenor =true;
		String settingShowTPlus0Tenor = System.getProperty("getSwapPoints.showTPlus0Tenor");
		try {
			if ( settingShowTPlus0Tenor != null) {
				_showTPlus0Tenor = Boolean.parseBoolean(settingShowTPlus0Tenor);
			}
		} catch (Exception e) {
			_showTPlus0Tenor = true;
		}
		boolean _showONTNTenor =true;
		String settingShowONTNTenor = System.getProperty("getSwapPoints.showONTNTenor");
		try {
			if ( settingShowONTNTenor != null) {
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
		HashMap<String, List<Tenor.Builder>> result = new HashMap<String, List<Tenor.Builder>> ();
		HashMap<String, Tenor.Builder> tenorONset = new HashMap<String, Tenor.Builder> ();
		HashMap<String, Tenor.Builder> tenorTNset = new HashMap<String, Tenor.Builder> ();

		YkbWebServices webService = new YkbWebServices();
		webService.setSecured();
		
		if ( interestedSymbol.size() == 0) {
			logger.warn("No ccyPair swap points data interested. nothing in MarketDataSet!?! skip calling getSwapPoints");
			return;
		}
		if (running.compareAndSet(false, true)) {
			try {
				long startTime = System.currentTimeMillis();
				List<SwapPointEntity> spList = webService.getSwapPoints((String) null);
				callNumber++;
				long receivedTime = System.currentTimeMillis();
				if ( spList != null ) {
					StringBuilder sb = new StringBuilder("getSwapPoints.updateDate: ");
					for ( SwapPointEntity sp: spList) {
						TenorVo tenor = null;
						String symbol = sp.getSymbol();
						
						// validate
						try {
							String tenorNm = sp.getTenor();
							if ( TenorVo.NOTATION_SPOTWEEK.equals(tenorNm)) {
								tenorNm = "1W";
							}
							tenor = TenorVo.fromString(tenorNm);
						} catch (NumberFormatException e) {
							logger.warn("Skip processing SwapPointEntity. Unable to handle tenor, " + sp.getTenor());
							continue;
						}
						List<Tenor.Builder> tenorList = result.get(symbol);
						if ( tenorList == null ) {
							tenorList = new ArrayList<Tenor.Builder>();
							result.put(symbol, tenorList);
						}
						if ( ((callNumber % 8) == 0)
								&& (TenorVo.NOTATION_OVERNIGHT.equals(tenor.getTenorNm()) || TenorVo.NOTATION_TOMORROWNIGHT.equals(tenor.getTenorNm()) ) ) {
							sb.append(symbol).append(tenor.getTenorNm()).append(':').append(sp.getLocalDate()).append(' ');
						}
						Tenor.Builder t = Tenor.newBuilder();
						t.setAskSwapPoints(sp.getAskValue().toPlainString()); 
						t.setBidSwapPoints(sp.getBidValue().toPlainString());
						t.setName(tenor.toString());
						
						tenorList.add(t);
						if (TenorVo.NOTATION_OVERNIGHT.equals(tenor.getTenorNm()) ) {
							tenorONset.put(symbol, t);
						}
						if (TenorVo.NOTATION_TOMORROWNIGHT.equals(tenor.getTenorNm()) ) {
							tenorTNset.put(symbol, Tenor.newBuilder(t.build()));
						}
					}
					Set<Entry<String, List<Tenor.Builder>>> entrySet = result.entrySet();
					
					LocalDate currentDate = LocalDate.now(refZone);
					DayOfWeek d = currentDate.getDayOfWeek();
					if ( d == DayOfWeek.SATURDAY ) {
						currentDate = currentDate.plusDays(2);
					} else if ( d == DayOfWeek.SUNDAY ) {
						currentDate = currentDate.plusDays(1);
					}
					
					boolean isUSDBusinessDay = fxCalendarBizService.isBusinessDay("USD", currentDate);
					for (Entry<String, List<Tenor.Builder>> e : entrySet) {
						String symbol = e.getKey();
						if ( interestedSymbol.contains(symbol)) {
							int spotLag = fxCalendarBizService.getSpotLagInDefaultPriceConvention(symbol);
							int pos = symbolIdMapper.map(symbol);
							ForwardCurve.Builder fc = fwdCurves[pos];
							fc.clearTenors();
							if ( !symbol.equals(fc.getSymbol())) {
								logger.warn("error in lookup corelated forwardcurve, " + symbol);
							}
							Tenor.Builder tenorON = tenorONset.get(fc.getSymbol());
							Tenor.Builder tenorTN = tenorTNset.get(fc.getSymbol());
							List<Tenor.Builder> tenors = e.getValue();
							for (int i = 0; i < tenors.size(); i++ ) {
								Tenor.Builder t = tenors.get(i);

								if (  TenorVo.NOTATION_OVERNIGHT.equals(t.getName())) {
									if ( showONTNTenor ) {
										fc.addTenors(t);
									}
								} else if (  TenorVo.NOTATION_TOMORROWNIGHT.equals(t.getName())) {
									if ( tenorON != null) { //
										t.setAskSwapPoints(ONE.multiply(t.getAskSwapPoints()).minus(tenorON.getAskSwapPoints()).getValue());
										t.setBidSwapPoints(ONE.multiply(t.getBidSwapPoints()).minus(tenorON.getBidSwapPoints()).getValue());
									}	
									if ( showONTNTenor ) {
										fc.addTenors(t);
									}
								} else  {
									// For other Forward Tenors - depending on T+1 or T+2 instrument will need to remove ON or TN Harmoni Points
									if ( spotLag == 1 ) {
										// If T+1 Instrument, remove Harmoni ON points
										if ( tenorON != null) { //
											t.setAskSwapPoints(ONE.multiply(t.getAskSwapPoints()).minus(tenorON.getAskSwapPoints()).getValue());
											t.setBidSwapPoints(ONE.multiply(t.getBidSwapPoints()).minus(tenorON.getBidSwapPoints()).getValue());
										}
									} else {
										// If T+2 Instrument, remove Harmoni TN points
										if (tenorTN != null) {
											t.setAskSwapPoints(ONE.multiply(t.getAskSwapPoints()).minus(tenorTN.getAskSwapPoints()).getValue());
											t.setBidSwapPoints(ONE.multiply(t.getBidSwapPoints()).minus(tenorTN.getBidSwapPoints()).getValue());
										}
									}	
									fc.addTenors(t);
								}
							}
							if ( showTplus0Tenor ) {
								boolean gotEnoughData = true;
								if ( isUSDBusinessDay ) {
									Tenor.Builder t = Tenor.newBuilder();
									if ( spotLag == 2 ) {
										//Tenor.Builder tenorTN = tenorTNset.get(symbol);
										if ( tenorTN != null) {
											t.setBidSwapPoints(NumberVo.getInstance(tenorTN.getAskSwapPoints()).multiply(-1).getValue());
											t.setAskSwapPoints(NumberVo.getInstance(tenorTN.getBidSwapPoints()).multiply(-1).getValue());
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
							if ( ((callNumber % 8) == 0) && debugSymbols.contains(fc.getSymbol())) {
								logger.debug("interpreted data from getSwapPoints: " + TextFormat.shortDebugString(fc));
							}
						}
					}
					long endTime = System.currentTimeMillis();
					logger.debug("Completed updating forwardcurve by webservice. Took:" + (endTime - startTime)+ ", webservice call took:" + (receivedTime -startTime) );
					if ( (callNumber % 8) == 0) {
						logger.debug(sb.toString());
					}
				}
			} finally {
				running.set(false);
			}
		} else {
			logger.warn("Previous webservice call still running. skip running...");
		}
	}

}
