package com.tts.ske;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.ske.qfx.impl.QuickfixEngineInitiatorContainer;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.ske.config.SkewRateEngineConfiguration;
import com.tts.ske.qfx.impl.QuickfixEngineAcceptorContainer;
import com.tts.ske.vo.LogControlVo;
import com.tts.util.AppConfig;
import com.tts.util.AppContext;

public class TtsSkewRateEngineMain {
	private final static Logger logger = LoggerFactory.getLogger(TtsSkewRateEngineMain.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	public static void main(String[] args) {
		String func = "TtsSkewRateEngineMain.main";
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();
			
			ctx.register(SkewRateEngineConfiguration.class);

			ctx.refresh();
			ctx.registerShutdownHook();
			
			String initiatorOrAcceptor = AppConfig.getValue("fix", "operatingMode");
			quickfix.Application qfixApplication = ctx.getBean(quickfix.Application.class);
			
			if ( initiatorOrAcceptor.equalsIgnoreCase("initiator")) {
				ArrayList<QuickfixEngineInitiatorContainer> startedInitiatorContainer = new ArrayList<>(2);
				int noOfFixInitiatorConfigFile = AppConfig.getIntegerValue("fix", "noOfFixInitiatorConfigFile", 1);

				for ( int i = 1; i <= noOfFixInitiatorConfigFile; i++ ) {
					InputStream is = Thread.currentThread().getContextClassLoader()
							.getResourceAsStream(AppConfig.getValue("fix", "initiator.config.file." + i));
				
					quickfix.SessionSettings qfixSessionSetting = new quickfix.SessionSettings(is);
					QuickfixEngineInitiatorContainer container = new QuickfixEngineInitiatorContainer(
							qfixSessionSetting,
							qfixApplication,
							new LogControlVo(false));
					container.logon();
					TimeUnit.SECONDS.sleep(5L);
					startedInitiatorContainer.add(container);
				}
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
			} else {
				String fixConfigFile = AppConfig.getValue("fix", "acceptor.config.file");
				quickfix.SessionSettings ss = new quickfix.SessionSettings(Thread.currentThread().getContextClassLoader().getResourceAsStream(fixConfigFile));
				
				QuickfixEngineAcceptorContainer qfixContainer = new QuickfixEngineAcceptorContainer(ss, qfixApplication, new LogControlVo(false));
				qfixContainer.start();
			}

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
