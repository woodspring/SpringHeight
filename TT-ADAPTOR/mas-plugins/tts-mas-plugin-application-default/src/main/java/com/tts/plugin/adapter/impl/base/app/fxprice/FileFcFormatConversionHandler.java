package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.LinkedList;

import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.plugin.adapter.impl.base.app.fxprice.converter.ActualDateBaseConverter;
import com.tts.plugin.adapter.impl.base.app.fxprice.converter.CSVForwardCurveConverter;
import com.tts.plugin.adapter.impl.base.app.fxprice.converter.IFcConverter;
import com.tts.plugin.adapter.impl.base.app.fxprice.converter.TenorBaseConverter;
import com.tts.plugin.adapter.support.vo.ContextVo;

public class FileFcFormatConversionHandler {

	private final IFcConverter tenorBaseConverter = new TenorBaseConverter();
	private final IFcConverter csvForwardCurveConverter = new CSVForwardCurveConverter();
	private final IFcConverter actualDateBaseConverter = new ActualDateBaseConverter();

	@SuppressWarnings("unchecked")
	public ForwardCurve.Builder doHandle(ForwardCurve.Builder fcBuilder, ContextVo sourceItem)
			throws Exception {
		long timestamp = System.currentTimeMillis();

		if ( sourceItem.getContextType().equals(String.class)) {
			fcBuilder.setUpdateTimestamp(timestamp);
			fcBuilder.setSymbol(sourceItem.getSymbol());
			fcBuilder.clearTenors();
			fcBuilder.clearLatency();
			
			Latency.Builder latencyBuilder = fcBuilder.getLatencyBuilder();
			latencyBuilder.setFaReceiveTimestamp(timestamp);
			
			
			LinkedList<String> symbolContext = (LinkedList<String>) sourceItem.getContext();
			for (String line: symbolContext) {
				getConverterType(line).convertToForwardCure(fcBuilder, line);
			}
		}
		
		return fcBuilder;
	}


	public IFcConverter getConverterType(String line) {
		String[] values = line.split(",");
		if (values.length < 4) {
			return tenorBaseConverter;
		} else {
			String tenor = values[0];
			if (tenor.length() > 9) {
				return csvForwardCurveConverter;
			} else {
				return actualDateBaseConverter;
			}
		}
	}
}
