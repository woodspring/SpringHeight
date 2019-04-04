package com.tts.mde;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.config.MdeConfiguration;
import com.tts.mde.controller.DataFlowController;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.util.AppContext;

public class MarketRateEngineMain {
	private final static Logger logger = LoggerFactory.getLogger(MarketRateEngineMain.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
		
	public static void main(String[] args) {
		
		DataFlowController dataFlowController = null;
		String func = "MarketRateEngineMain.main";
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			//KeyCheck.v();
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();
			
			ctx.register(MdeConfiguration.class);
			ctx.refresh();
			ctx.registerShutdownHook();
			

			dataFlowController = new DataFlowController();
			dataFlowController.init();
			
			while ( true) {
				Thread.sleep(Long.MAX_VALUE);
			}

		} catch (InterruptedException e) {
			
		} catch (Exception e) {
			monitorAgent.logError(func, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, "Main Exception " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if ( dataFlowController != null ) {
				try {
					dataFlowController.destroy();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (ctx != null) {
				ctx.close();
			}
		}
	}

}
