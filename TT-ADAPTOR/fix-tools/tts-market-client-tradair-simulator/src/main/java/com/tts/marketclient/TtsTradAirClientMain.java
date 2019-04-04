package com.tts.marketclient;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.marketclient.app.TtsTradAirClientApp;
import com.tts.marketclient.config.ClientAppConfiguration;
import com.tts.marketclient.qfx.impl.QuickfixEngineAcceptorContainer;
import com.tts.marketclient.qfx.impl.QuickfixEngineInitiatorContainer;
import com.tts.marketclient.vo.LogControlVo;
import com.tts.util.AppConfig;
import com.tts.util.AppContext;
import com.tts.web.EmbeddedJetty;


public class TtsTradAirClientMain {
	private final static Logger logger = LoggerFactory.getLogger(TtsTradAirClientMain.class);
	
	public static void main(String[] args) {
		String func = "TtsTradAirAdapterMain.main";
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();
			
			ctx.register(ClientAppConfiguration.class);

			ctx.refresh();
			ctx.registerShutdownHook();
			
			LogControlVo logControl = new LogControlVo( false );


			TtsTradAirClientApp appBean = ctx.getBean(TtsTradAirClientApp.class);

			String initiatorOrAcceptor = AppConfig.getValue("fix", "operatingMode");
			
			if ( initiatorOrAcceptor.equalsIgnoreCase("initiator")) {
				InputStream is = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream(AppConfig.getValue("fix", "initiator.config.file"));
			
				quickfix.SessionSettings qfixSessionSetting = new quickfix.SessionSettings(is);
				QuickfixEngineInitiatorContainer container = new QuickfixEngineInitiatorContainer(
						qfixSessionSetting,
						appBean,
						new LogControlVo(false));
				container.logon();
			} else {
				String fixConfigFile = AppConfig.getValue("fix", "acceptor.config.file");
				quickfix.SessionSettings ss = new quickfix.SessionSettings(Thread.currentThread().getContextClassLoader().getResourceAsStream(fixConfigFile));

				QuickfixEngineAcceptorContainer qfixContainer = new QuickfixEngineAcceptorContainer(ss, appBean, new LogControlVo(false));
				qfixContainer.start();
			}
			EmbeddedJetty webEngine = new EmbeddedJetty();
			//This call blocks in jetty
			webEngine.startJetty(8082, ctx);

		} catch (InterruptedException e) {
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ctx != null) {
				ctx.close();
			}
		}
	}
}
