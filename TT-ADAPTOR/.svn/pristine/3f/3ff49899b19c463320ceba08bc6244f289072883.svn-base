package com.tts.mas.support;

import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.plugin.adapter.api.app.IApp;
import com.tts.plugin.adapter.support.IFxCalendarBizServiceApi;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.vo.TenorVo;

public class CachedFxCalendarBizServiceWrapper implements IFxCalendarBizServiceApi  {
	
	public final static Logger logger = LoggerFactory.getLogger(CachedFxCalendarBizServiceWrapper.class);
	
	private final IFxCalendarBizService fxCalendarBizService;
	
	private final SessionInfo sessionInfo;
	
	public CachedFxCalendarBizServiceWrapper() {
		this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
		this.sessionInfo = AppContext.getContext().getBean(SessionInfo.class);
	}
	
	@Override
	public String getForwardValueDate(String symbol, String tenor) {
		String valueDate = null;
		
		Map<String, IndividualInfoVo> sessionInstrumentInfo = this.sessionInfo.getMarketDataset().getMarketStructuresByType(IApp.PublishingFormatType.FxSpot.toString());

		if ( sessionInstrumentInfo.get(symbol) != null 
				&& sessionInstrumentInfo.get(symbol).getValueDateMap() != null
				&& sessionInstrumentInfo.get(symbol).getValueDateMap().get(tenor) != null ) {
			valueDate = sessionInstrumentInfo.get(symbol).getValueDateMap().get(tenor);
		}
		
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
		}
		return valueDate;
	}
	
	@Override
	public String getCurrentBusinessDay(String symbol) {
		String tradeDate = null;
		Map<String, IndividualInfoVo> sessionInstrumentInfo = this.sessionInfo.getMarketDataset().getMarketStructuresByType(IApp.PublishingFormatType.FxSpot.toString());
		if ( sessionInstrumentInfo.get(symbol) != null 
				&& sessionInstrumentInfo.get(symbol).getTradeDateString() != null ) {
			tradeDate = sessionInstrumentInfo.get(symbol).getTradeDateString();
		}
		
		if ( tradeDate == null )  {
			tradeDate = ChronologyUtil.getDateString(fxCalendarBizService.getCurrentBusinessDay(symbol));
		}
		return tradeDate;
	}
	

}
