package com.tts.mlp;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.app.ForwardCurveDataManager;
import com.tts.mlp.app.QfixApplication;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.mlp.config.MLPConfiguration;
import com.tts.mlp.qfix.QuickfixEngineContainer;
import com.tts.util.AppContext;
import com.tts.util.AppJFigLocator;
import com.tts.util.AppUtils;
import com.tts.web.EmbeddedJetty;

public class MarketLiquidityProviderSimMain {
	static final Logger log = LoggerFactory.getLogger(MarketLiquidityProviderSimMain.class);
	public static void main(String[] args) throws Exception {
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			System.setProperty(AppJFigLocator.CONFIG_DIRECTORY_PROPERTY, "env-resources/simulator/");
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();

			ctx.register(MLPConfiguration.class);
			ctx.refresh();
			ctx.registerShutdownHook();
			
			String envName = AppUtils.getActiveEnvironment();
			Properties envProps = new Properties();
			String envConfigFilepath = String.format("env-resources/simulator/adapterConfig_%s.properties", envName);
			System.out.println(envConfigFilepath);
			InputStream envIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(envConfigFilepath);
			envProps.load(envIs);
			envIs.close();
			
			//Load forward curve data from file and start file watcher to see changes
			File fcDataFile = null;
			try {
				String fcFileName = String.format("app-resources/fc_%s.csv", envName);
				fcDataFile = new File(Thread.currentThread().getContextClassLoader().getResource(fcFileName).toURI());
			} catch (Exception e1) {
				fcDataFile = new File(Thread.currentThread().getContextClassLoader().getResource("app-resources/fc.csv").toURI());
			}
			ForwardCurveDataManager.start(fcDataFile.getAbsolutePath());

					
			String fixConfigFilepath = envProps.getProperty("adapter.fix.config");
			System.out.println(fixConfigFilepath);
			InputStream fixIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(fixConfigFilepath);
			
			QfixApplication app = new QfixApplication(ctx.getBean(PriceSubscriptionRegistry.class));
			QuickfixEngineContainer container = new QuickfixEngineContainer(fixIs, app);
			fixIs.close();
			container.start();
			
			String[] beanNames = ctx.getBeanDefinitionNames();
	        Arrays.sort(beanNames);
	        log.info("List all spring beans :");
	        for (String beanName : beanNames) {
	        	log.info("\t"+beanName);
	        }
	        
			log.info("for example USE < http://localhost:8080/set?symbol=EURUSD&indicative=true > to set EURUSD to indicative");
			
			EmbeddedJetty webEngine = new EmbeddedJetty();
			//This call blocks in jetty
			webEngine.startJetty(8080, ctx);
			
//			
//			while ( true) {
//				Thread.sleep(Long.MAX_VALUE);
//			}
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
