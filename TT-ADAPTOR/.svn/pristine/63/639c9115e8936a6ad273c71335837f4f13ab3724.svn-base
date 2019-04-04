package com.tts.mas.support;

import com.tts.entity.product.fx.InstrumentDetail;
import com.tts.plugin.adapter.support.IInstrumentDetailProperties;

public class InstrumentDetailProperties implements IInstrumentDetailProperties {
	private final InstrumentDetail instrumentDetail;
	
	public InstrumentDetailProperties(InstrumentDetail id) {
		this.instrumentDetail = id;
	}
	
	
	@Override
	public int getPointValue() {
		return instrumentDetail.getPointValue();
	}


	@Override
	public int getPrecision() {
		return instrumentDetail.getPrecision();
	}


	@Override
	public int getSwapPrecision() {
		return instrumentDetail.getSwapPrecision();
	}


	@Override
	public String getSymbol() {
		return instrumentDetail.getSymbol();
	}



	public static InstrumentDetailProperties fromInstrumentDetail(InstrumentDetail id) {
		return new InstrumentDetailProperties(id);
	}

}
