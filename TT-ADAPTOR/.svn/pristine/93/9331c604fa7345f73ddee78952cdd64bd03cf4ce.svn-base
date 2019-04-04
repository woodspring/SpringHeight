package com.tts.plugin.adapter.impl.base.app.fxprice.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;


public class ActualDateBaseConverter implements IFcConverter {
	private static final Logger log = LoggerFactory.getLogger(ActualDateBaseConverter.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(log);
	
	@Override
	public ForwardCurve.Builder convertToForwardCure(ForwardCurve.Builder forwardCurveBuilder, String line) {
		String func = "ActualDateBaseConverter.convertToForwardCure";
		if (line == null || line.isEmpty()) {
			return forwardCurveBuilder;
		}

		String[] elements = line.split(",");
		if (elements.length < 4) {
			monitorAgent.logError(func, MonitorConstant.FCADT.ERROR_FORWARD_BAD_FORMAT,
					                    "Actual Date Base CSV File withe wrong format. Element length is " + elements.length);
			return null;
		}
		String symbol = elements[0];
		if (symbol.length() >= 7) {
			symbol = symbol.substring(1, 7);
		}


		Tenor.Builder tenorBuilder = Tenor.newBuilder();
		tenorBuilder.setActualDate(elements[1]);
		tenorBuilder.setBidSwapPoints(elements[2]);
		tenorBuilder.setAskSwapPoints(elements[3]);

		forwardCurveBuilder.addTenors(tenorBuilder);
		

		return forwardCurveBuilder;
	}

}
