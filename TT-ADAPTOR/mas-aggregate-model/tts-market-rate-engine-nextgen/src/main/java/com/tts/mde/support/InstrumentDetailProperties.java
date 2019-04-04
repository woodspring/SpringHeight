package com.tts.mde.support;

import com.tts.entity.product.fx.InstrumentDetail;

public class InstrumentDetailProperties implements IInstrumentDetailProp {
	private final InstrumentDetail instrumentDetail;
	
	public InstrumentDetailProperties(InstrumentDetail id) {
		this.instrumentDetail = id;
	}
	
	public int getPointValue() {
		return instrumentDetail.getPointValue();
	}

	public int getPrecision() {
		return instrumentDetail.getPrecision();
	}

	public int getSwapPrecision() {
		return instrumentDetail.getSwapPrecision();
	}

	public String getSymbol() {
		return instrumentDetail.getSymbol();
	}

	public static InstrumentDetailProperties fromInstrumentDetail(InstrumentDetail id) {
		return new InstrumentDetailProperties(id);
	}

}
