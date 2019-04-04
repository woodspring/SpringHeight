package com.tts.mde.support.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tts.entity.product.fx.InstrumentDetail;
import com.tts.mde.support.IInstrumentDetailProp;
import com.tts.mde.support.IInstrumentDetailProvider;
import com.tts.mde.support.InstrumentDetailProperties;
import com.tts.service.sa.product.fx.IInstrumentDetailService;
import com.tts.util.AppContext;

public class InstrumentDetailProvider implements IInstrumentDetailProvider {
	
	private Map<String, IInstrumentDetailProp> detailMap;
	
	public InstrumentDetailProvider() {
		Map<String, IInstrumentDetailProp> _detailMap = new HashMap<>();
		IInstrumentDetailService instrumentDetailService = (IInstrumentDetailService) AppContext.getContext().getBean("instrumentDetailService");
		for(InstrumentDetail instr : instrumentDetailService.findAll()) {
			_detailMap.put(instr.getSymbol(), InstrumentDetailProperties.fromInstrumentDetail(instr));
		}
		this.detailMap = Collections.unmodifiableMap(_detailMap);
	}
	

	@Override
	public IInstrumentDetailProp getInstrumentDetail(String symbol) {
		return detailMap.get(symbol);
	}

}
