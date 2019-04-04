package com.tts.mlp;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.config.MLPConfiguration;
import com.tts.util.AppContext;
import com.tts.util.AppJFigLocator;
import com.tts.web.EmbeddedJetty;

public class MarketControlCenterMain {
	static final Logger log = LoggerFactory.getLogger(MarketControlCenterMain.class);
	public static void main(String[] args) throws Exception {
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			System.setProperty(AppJFigLocator.CONFIG_DIRECTORY_PROPERTY, "env-resources/simulator/");
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();

			ctx.register(MLPConfiguration.class);
			ctx.refresh();
			ctx.registerShutdownHook();
			
			String[] beanNames = ctx.getBeanDefinitionNames();
	        Arrays.sort(beanNames);
	        log.info("List all spring beans :");
	        for (String beanName : beanNames) {
	        	log.info("\t"+beanName);
	        }
	        			
			EmbeddedJetty webEngine = new EmbeddedJetty();
			//This call blocks in jetty
			webEngine.startJetty(18080, ctx);
			
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
