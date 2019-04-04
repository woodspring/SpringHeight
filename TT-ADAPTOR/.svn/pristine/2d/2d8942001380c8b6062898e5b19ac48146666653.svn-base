package com.tts.plugin.adapter.impl.base.app.fxprice.converter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;


public class CSVForwardCurveConverter implements IFcConverter {
	private static final Logger log = LoggerFactory.getLogger(CSVForwardCurveConverter.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(log);
	
	@Override
	public ForwardCurve.Builder convertToForwardCure(ForwardCurve.Builder forwardCurveBuilder, String line) {
		String func = "CSVForwardCurveConverter.convertToForwardCure";
		
		if (line == null || line.isEmpty()) {
			return null;
		}

		String[] elements = line.split(",");
		if (elements.length < 4) {
			monitorAgent.logError(func, MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT,
					                    "Forward curve in bad format, it must contain peroid code, actual date & ask/bid value. Element length is " + elements.length);

			return null;
		}
		String symbol = elements[0];
		if (symbol.length() >= 7) {
			symbol = symbol.substring(1, 7);
		}
		String tenor = elements[0];
		int index = elements[0].indexOf("=>");
		if (elements[0].length() > 9) {
			tenor = tenor.substring(7, index);
		} else {
			monitorAgent.logError(func, MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT,
				               	        String.format("CSV File tenor withe wrong format [%S]", symbol) + ", " +
				               	        "Element Length is " + elements[0].length());

			return forwardCurveBuilder;
		}


		Tenor.Builder tenorBuilder = Tenor.newBuilder();
		tenorBuilder.setActualDate(elements[1]);
		tenorBuilder.setBidSwapPoints(elements[2]);
		tenorBuilder.setAskSwapPoints(elements[3]);
		tenorBuilder.setName(tenor);

		forwardCurveBuilder.addTenors(tenorBuilder);
		return forwardCurveBuilder;
	}

}
