package com.tts.fa;

import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fa.app.TtsTradAirAdapterApp;
import com.tts.fa.config.FaConfiguration;
import com.tts.fa.qfx.impl.QuickfixEngineInitiatorContainer;
import com.tts.fa.vo.LogControlVo;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.util.AppConfig;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;


public class TtsTradAirAdapterMain {
	private final static Logger logger = LoggerFactory.getLogger(TtsTradAirAdapterMain.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	public static void main(String[] args) {
		String func = "TtsTradAirAdapterMain.main";
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();
			
			ctx.register(FaConfiguration.class);

			ctx.refresh();
			ctx.registerShutdownHook();
			
			
			boolean mdLogEnabledDefault = false;
			try {
				mdLogEnabledDefault = Boolean.parseBoolean(System.getenv("MARKET_DATA_LOGGING_DYNAMIC_CONTROL_DEFAULT"));
			} catch (Exception e) {
				
			}
			LogControlVo logControl = new LogControlVo( mdLogEnabledDefault);

			int noOfFixInitiatorConfigFile = AppConfig.getIntegerValue("fix", "noOfFixInitiatorConfigFile", 1);
			TtsTradAirAdapterApp appBean = ctx.getBean(TtsTradAirAdapterApp.class);
			ArrayList<QuickfixEngineInitiatorContainer> startedInitiatorContainer = new ArrayList<>(2);

			for ( int i = 1; i <= noOfFixInitiatorConfigFile; i++ ) {
				InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(AppConfig.getValue("fix", "initiator.config.file." + i));
			
				quickfix.SessionSettings qfixSessionSetting = new quickfix.SessionSettings(is);
				QuickfixEngineInitiatorContainer container = new QuickfixEngineInitiatorContainer(
						qfixSessionSetting,
						appBean,
						logControl);
				container.logon();
				Thread.sleep(ChronologyUtil.MILLIS_IN_SECOND * 5);
				startedInitiatorContainer.add(container);

			}
					
			appBean.postInit();
			Runtime.getRuntime().addShutdownHook(new Thread()
		    {
			      public void run()
			      {
						logger.info("Shutting down, startedInitiatorContainer.count=" + startedInitiatorContainer.size() );

						if ( startedInitiatorContainer.size() > 0 ) {
							for ( QuickfixEngineInitiatorContainer c : startedInitiatorContainer ) {
								c.logout();
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						
						
			      }
		    });	
			while ( true) {
				Thread.sleep(Long.MAX_VALUE);
			}

		} catch (InterruptedException e) {
			
		} catch (Exception e) {
			monitorAgent.logError(func, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, "Main Exception " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (ctx != null) {
				ctx.close();
			}
		}
	}
}
