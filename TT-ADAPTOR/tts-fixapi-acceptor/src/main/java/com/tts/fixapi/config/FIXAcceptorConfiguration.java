package com.tts.fixapi.config;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.fixapi.impl.FIXAcceptorDefaultIntegrationPlugin;
import com.tts.fixapi.type.IFIXAcceptorIntegrationPlugin;
import com.tts.fixapi.type.IFIXAcceptorMessageProcessor;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.roe.impl.DefaultROEMessageProcessor;
import com.tts.service.config.JdbcConfiguration;
import com.tts.service.config.ServicesConfiguration;
import com.tts.util.AppUtils;
import com.tts.util.config.UtilConfiguration;


@Configuration
@Import({ UtilConfiguration.class, JdbcConfiguration.class, ServicesConfiguration.class, MessageConfiguration.class })
public class FIXAcceptorConfiguration {
	private static final Logger logger =  LoggerFactory.getLogger("FixAPILogger");
	private static final String DEFAULT_FIX_API_PROPERTIES = "fixAcceptorConfig_%s.properties";
	private static final String PROP_LOCATION = "env-resources/";
	
	@Bean
	public IFIXAcceptorIntegrationPlugin fixIntegrationPlugin()	{
		return new FIXAcceptorDefaultIntegrationPlugin();
	}
	
	@Bean
	public IFIXAcceptorMessageProcessor roeMessageProcessor()	{
		return new DefaultROEMessageProcessor();
	}
	
	@Bean
    public FixApplicationProperties applicationProperties()	{
		FixApplicationProperties p = new FixApplicationProperties();
		
		try	{		
			String configFile = System.getProperty("PROP_FIX_API_CONFIG");
				   configFile = ((configFile == null) || (configFile.trim().length() <= 0))? DEFAULT_FIX_API_PROPERTIES: configFile;
				  		
	    	String env        = AppUtils.getActiveEnvironment();
	    	String configPath = String.format((PROP_LOCATION + configFile), env);
	    	
	    	logger.info("Loading FIX API Configuration from " + configPath);
	    		    	
	    	InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(configPath);
	    	if(is != null)	{
	    		p.load(is);
	    		is.close();
	    	}
	    	else
	    		System.err.println("<<<     MISSING FIX API CONFIGURATION PROPERTY FILE: " + configPath + "     >>>");
		}
		catch(Exception exp)	{
			logger.error("Exception loading @Bean FixApplicationProperties. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
    	
    	logger.info("FIX API isEmpty: " + String.valueOf(p.isEmpty()));
    	return p;    	
    }
}
