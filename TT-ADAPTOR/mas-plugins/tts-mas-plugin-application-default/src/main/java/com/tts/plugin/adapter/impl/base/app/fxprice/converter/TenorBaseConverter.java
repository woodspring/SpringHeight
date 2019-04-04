package com.tts.plugin.adapter.impl.base.app.fxprice.converter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;



public class TenorBaseConverter implements IFcConverter {
	private static final Logger log = LoggerFactory.getLogger(TenorBaseConverter.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(log);

	@Override
	public ForwardCurve.Builder convertToForwardCure(ForwardCurve.Builder fcBuilder, String line) {
		String func = "TenorBaseConverter.convertToForwardCure";
		
		if (line == null || line.isEmpty()) {
			return null;
		}

		String[] elements = line.split(",");
		if (elements.length < 3) {
			monitorAgent.logError(func, MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT,
					                    "Tenor Based CSV File with wrong format. Element Length is " + elements.length);
			return fcBuilder;
		}

		String symbol = elements[0];
		String tenorName = elements[0];
		if (symbol.length() >= 7) {
			symbol = symbol.substring(1, 7);
		}
		int index = tenorName.indexOf("=>");
		if (index > 7) {
			tenorName = tenorName.substring(7, index);
		}

		// building the tenor
		Tenor.Builder tenorBuilder = Tenor.newBuilder();
		tenorBuilder.setName(tenorName.trim());

		tenorBuilder.setBidSwapPoints(elements[1]);
		tenorBuilder.setAskSwapPoints(elements[2]);

		// adding tenor to ForwardCurve
		fcBuilder.addTenors(tenorBuilder);
		

		return fcBuilder;
	}

	

}
