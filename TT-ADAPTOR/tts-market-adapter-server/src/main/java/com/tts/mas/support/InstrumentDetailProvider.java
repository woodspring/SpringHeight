package com.tts.mas.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tts.entity.product.fx.InstrumentDetail;
import com.tts.plugin.adapter.support.IInstrumentDetailProperties;
import com.tts.plugin.adapter.support.IInstrumentDetailProvider;
import com.tts.service.sa.product.fx.IInstrumentDetailService;
import com.tts.util.AppContext;

public class InstrumentDetailProvider implements IInstrumentDetailProvider {
	
	private Map<String, IInstrumentDetailProperties> detailMap;
	
	public InstrumentDetailProvider() {
		Map<String, IInstrumentDetailProperties> _detailMap = new HashMap<String, IInstrumentDetailProperties>();
		IInstrumentDetailService instrumentDetailService = (IInstrumentDetailService) AppContext.getContext().getBean("instrumentDetailService");
		for(InstrumentDetail instr : instrumentDetailService.findAll()) {
			_detailMap.put(instr.getSymbol(), InstrumentDetailProperties.fromInstrumentDetail(instr));
		}
		this.detailMap = Collections.unmodifiableMap(_detailMap);
	}
	

	@Override
	public IInstrumentDetailProperties getInstrumentDetail(String symbol) {
		return detailMap.get(symbol);
	}

}
