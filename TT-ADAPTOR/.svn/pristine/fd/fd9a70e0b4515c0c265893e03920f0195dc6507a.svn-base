package com.tts.fa;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fa.app.TtsGenericAdapterApp;
import com.tts.fa.config.FaConfiguration;
import com.tts.fa.qfx.impl.QuickfixEngineInitiatorContainer;
import com.tts.fa.vo.LogControlVo;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.util.AppConfig;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;


public class TtsGenericAdapterMain {
public final static String OPERATE_AS;

	private final static Logger logger = LoggerFactory.getLogger(TtsGenericAdapterMain.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

    static {
    	String operateAs = System.getProperty("operateAs");
    	if ( operateAs == null || operateAs.trim().length() == 0) {
    		operateAs = "CIBC";
    	} else {
    		operateAs = operateAs.toUpperCase();
    	}
    	OPERATE_AS = operateAs;
    }
	public static void main(String[] args) {
		String func = "TtsCibcAdapterMain.main";
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
			TtsGenericAdapterApp appBean = ctx.getBean(TtsGenericAdapterApp.class);

			for ( int i = 1; i <= noOfFixInitiatorConfigFile; i++ ) {
				Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource(AppConfig.getValue("fix", "initiator.config.file." + i)).toURI());
				Charset charset = StandardCharsets.UTF_8;

				String content = new String(Files.readAllBytes(path), charset);
				content = content.replaceAll("BANK2", OPERATE_AS);
				content = content.replaceAll("SocketConnectPort=9881", "SocketConnectPort=" + generatePort(OPERATE_AS));

				Path newPath = Files.write(Paths.get("tmp_FIX.cfg"), content.getBytes(charset));
				
				InputStream is = Files.newInputStream(newPath);
//				InputStream is = Thread.currentThread().getContextClassLoader()
//				.getResourceAsStream(AppConfig.getValue("fix", "initiator.config.file." + i));
			
				quickfix.SessionSettings qfixSessionSetting = new quickfix.SessionSettings(is);
				QuickfixEngineInitiatorContainer container = new QuickfixEngineInitiatorContainer(
						qfixSessionSetting,
						appBean,
						logControl);
				container.logon();
				Thread.sleep(ChronologyUtil.MILLIS_IN_SECOND * 5);
			}
					
			appBean.postInit();
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
	private static int generatePort(String lp) {
		int sum = 8000;
		for ( int i = 0; i < lp.length(); i++) {
			sum +=  (int) lp.charAt(i)  ;
		}
		return sum;
	}
}
