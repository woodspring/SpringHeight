package com.tts.fixapi;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.fixapi.config.FIXAcceptorConfiguration;
import com.tts.fixapi.core.FixAcceptor;
import com.tts.fixapi.impl.FIXAcceptorApplication;
import com.tts.fixapi.type.IFIXAcceptorIntegrationPlugin;
import com.tts.fixapi.type.IFIXAcceptorMessageDispatcher;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;

import quickfix.ConfigError;
import quickfix.SessionSettings;


public class FIXAcceptorMain {
	private static final String DEFAULT_FIX_CONFIG_PROPERTIES = "fix.api.sessions.cfg";
	private static final String PROP_LOCATION = "env-resources/";
	private static final Logger logger =  LoggerFactory.getLogger("FixAPILogger");
	
	private AnnotationConfigApplicationContext appContext = null;
	private SessionSettings fixSessionSettings            = null;
	private FixAcceptor fixAcceptor                       = null;
	private IFIXAcceptorIntegrationPlugin fixPlugin       = null;
	private IFIXAcceptorMessageDispatcher fixApplicaiton  = null;
	
	private String fixApiSessionConfigFile = null;
	
	public FIXAcceptorMain()	{
		logger.info("<<<     STARTING FIX API ACCEPTOR MAIN...     >>>");
		
		appContext = new AnnotationConfigApplicationContext();
		
		appContext.register(FIXAcceptorConfiguration.class);
		appContext.refresh();
		
		logger.info("FIX API Configuration Completed...");
		
		FixApplicationProperties appProp = AppContext.getContext().getBean(FixApplicationProperties.class);
		if(appProp.isEmpty())	{
			logger.error("FIX API CONFIGURATION PROPERTY FILE IS NOT VALID.");
			logger.error("EXITING FIX ACCEPTOR.");
			System.exit(1);
		}
		
		fixApiSessionConfigFile = appProp.getProperty("FIX.API.SESSION.CONFIG.FILE");
		fixApiSessionConfigFile = ((fixApiSessionConfigFile == null) || (fixApiSessionConfigFile.trim().length() <= 0))? DEFAULT_FIX_CONFIG_PROPERTIES: fixApiSessionConfigFile;
		
		logger.info("Loading FIX API Session Configuration from " + fixApiSessionConfigFile);
		fixSessionSettings = loadFixSessionConfigFile(fixApiSessionConfigFile);
		if(fixSessionSettings == null)	{
			logger.error("FIX API SESSION CONFIGURATION IS NOT VALID.");
			logger.error("EXITING FIX ACCEPTOR.");
			System.exit(1);
		}
		
		fixPlugin = AppContext.getContext().getBean(IFIXAcceptorIntegrationPlugin.class);
		startFIXSession();
		
		try	{
			while(true)	{
				Thread.sleep(ChronologyUtil.MILLIS_IN_DAY);
			}
		}
		catch(InterruptedException iExp)	{
			logger.error("Fix Acceptor Main Thread Sleep Interrupted. " + iExp.getMessage());
			logger.error("InterruptedException: " + iExp);
			iExp.printStackTrace();
		}
	}
	
	public void startFIXSession()	{
		boolean socketAcceptorStarted = false;
		
		try	{
			fixApplicaiton = new FIXAcceptorApplication(fixSessionSettings, fixPlugin);
			fixAcceptor    = new FixAcceptor(fixSessionSettings, fixApplicaiton);
			
			socketAcceptorStarted = fixAcceptor.start();
			if(!socketAcceptorStarted)	{
				logger.error("EXITING FIX ACCEPTOR.");
				System.exit(1);
			}
			
			logger.info("FIX Acceptor Started Successfully!");
		}
		catch(Exception exp)	{
			logger.error("Exception Starting FIX Acceptor. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	public void stopFIXSession()	{
		try	{
			fixAcceptor.stop();
			logger.info("FIX Acceptor Stopped Successfully!");
		}
		catch(Exception exp)	{
			logger.error("Exception Stopping FIX Acceptor. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	private SessionSettings loadFixSessionConfigFile(String configFileName)	{
		InputStream isConfig = null;
		SessionSettings fixSesSettings = null;
		
		try	{
			isConfig = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROP_LOCATION + configFileName);
			if(isConfig == null)	{
				logger.error("MISSING FIX SESSION CONFIGURATION FILE: " + configFileName);
				return(null);
			}
			
			fixSesSettings = new SessionSettings(isConfig);
			logger.info("FIX SESSION CONFIGURATION FILE " + configFileName + " LOADED...");
		}
		catch(ConfigError ceExp)	{
			logger.error("Exception Loading Fix Session Configuration File. " + ceExp.getMessage());
			logger.error("ConfigError: ", ceExp);
			ceExp.printStackTrace();
			fixSesSettings = null;
		}
		finally {
			try	{
				isConfig.close();
			}
			catch(Exception exp)	
			{	}
			
			isConfig = null;
		}
		
		return(fixSesSettings);
	}
	
	public static void main(String... args) {
		new FIXAcceptorMain();
	}
}
