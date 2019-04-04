package com.tts.plugin.adapter.impl.base.app.interest;

import java.util.LinkedList;

import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.InterestRateCurveStruct.InterestRateCurve;
import com.tts.message.market.InterestRateCurveStruct.InterestRateTenor;
import com.tts.plugin.adapter.support.vo.ContextVo;

public class FileIrFormatConversionHandler {

	private static final String CSV_COLUMN_SEPERATOR = ",";
	

	@SuppressWarnings("unchecked")
	public InterestRateCurve.Builder doHandle(InterestRateCurve.Builder irBuilder, ContextVo sourceItem)
			throws Exception {
		long timestamp = System.currentTimeMillis();

		if ( sourceItem.getContextType().equals(String.class)) {
			irBuilder.setUpdateTimestamp(timestamp);
			irBuilder.setSymbol(sourceItem.getSymbol());
			irBuilder.clearTenors();
			irBuilder.clearLatency();
			Latency.Builder latencyBuilder = irBuilder.getLatencyBuilder();
			latencyBuilder.setFaReceiveTimestamp(timestamp);
			
			LinkedList<String> symbolContext = (LinkedList<String>) sourceItem.getContext();
			for (String line: symbolContext) {
				String[] elements = line.split(CSV_COLUMN_SEPERATOR);
				String tenor = elements[2];
				String bidInterestRate = elements[3];
				String askInterestRate = elements[4];
				InterestRateTenor.Builder tb = InterestRateTenor.newBuilder();

				tb.setAskPercentage(askInterestRate);
				tb.setBidPercentage(bidInterestRate);
				tb.setInvalidate(false);
				tb.setName(tenor);
				irBuilder.addTenors(tb);
			}
		}
		
		return irBuilder;
	}


}
