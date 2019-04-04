package com.tts.mde.support.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.message.system.RolloverStruct.RolloverNotification;
import com.tts.message.system.RolloverStruct.TradeDate;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.vo.TenorVo;

public class CachedFxCalendarBizService implements IFxCalendarBizServiceApi  {
	
	public final static Logger logger = LoggerFactory.getLogger(CachedFxCalendarBizService.class);
	
	private final IFxCalendarBizService fxCalendarBizService;
	
	private final ConcurrentHashMap<String, String> tradeDateMap = new ConcurrentHashMap<>();
	private final Map<String, ConcurrentHashMap<String, String>> vDateMap;

	public CachedFxCalendarBizService(String[] symbols) {
		this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
		
		Map<String, ConcurrentHashMap<String, String>> _vDateMap = new HashMap<String, ConcurrentHashMap<String, String>>();
		for ( String symbol: symbols) {
			_vDateMap.put(symbol, new ConcurrentHashMap<String, String>());
		}
		this.vDateMap = Collections.unmodifiableMap(_vDateMap);
	}
	
	@Override
	public String getForwardValueDate(String symbol, String tenor) {
		String valueDate = vDateMap.get(symbol).get(tenor);
				
		if ( valueDate == null ) {
			LocalDate tradeDate = ChronologyUtil.getLocalDateFromString(getCurrentBusinessDay(symbol));

			TenorVo tenorT = TenorVo.fromString(tenor);
			if ( tenorT == null ) {
				logger.debug("Unable to find valueDate for symbol, " + symbol + ", tenor, " + tenor);
			} else if (TenorVo.NOTATION_SPOT.equals(tenor)){
				LocalDate v = fxCalendarBizService.getSpotValueDate(symbol, tradeDate, PricingConventionConstants.INTERBANK);
				valueDate = ChronologyUtil.getDateString(v);
			} else {
				LocalDate v = fxCalendarBizService.getForwardValueDate(symbol, tradeDate, tenorT.getPeriodCd(), tenorT.getValue(), PricingConventionConstants.INTERBANK);
				valueDate = ChronologyUtil.getDateString(v);
			}
			vDateMap.get(symbol).put(tenor, valueDate);
		}
		return valueDate;
	}
	
	@Override
	public String getCurrentBusinessDay(String symbol) {
		String tradeDate = tradeDateMap.get(symbol);
		
		if ( tradeDate == null )  {
			tradeDate = ChronologyUtil.getDateString(fxCalendarBizService.getCurrentBusinessDay(symbol));
			//tradeDate = ChronologyUtil.getDateString(LocalDate.now());
			tradeDateMap.put(symbol, tradeDate);
		}
		return tradeDate;
	}

	@Override
	public void onRolloverEvent(RolloverNotification rolloverNotification) {
		for (TradeDate tradeDate: rolloverNotification.getNewTradeDateList() )  {
			String symbol = tradeDate.getCurrencyOrInstrument();
			String localTradeDate = tradeDate.getLocalDate();
			tradeDateMap.put(symbol, localTradeDate);
			Map<String, String> dateMap = vDateMap.get(symbol);
			if ( dateMap != null ) {
				LocalDate tradeDateLD = ChronologyUtil.getLocalDateFromString(localTradeDate);
				Map<String, String> newDateMap = new HashMap<>();
				Set<String> tenors = dateMap.keySet();
				for ( String tenor: tenors) {
					String valueDate = null;
					if (TenorVo.NOTATION_SPOT.equals(tenor)){
						LocalDate v = fxCalendarBizService.getSpotValueDate(symbol, tradeDateLD, PricingConventionConstants.INTERBANK);
						valueDate = ChronologyUtil.getDateString(v);
					} else {
						TenorVo tenorT = TenorVo.fromString(tenor);		
						LocalDate v = fxCalendarBizService.getForwardValueDate(symbol, tradeDateLD, tenorT.getPeriodCd(), tenorT.getValue(), PricingConventionConstants.INTERBANK);
						valueDate = ChronologyUtil.getDateString(v);
					}
					newDateMap.put(tenor, valueDate);
				}
				vDateMap.get(symbol).putAll(newDateMap);
			}
		}
	}
	

}
