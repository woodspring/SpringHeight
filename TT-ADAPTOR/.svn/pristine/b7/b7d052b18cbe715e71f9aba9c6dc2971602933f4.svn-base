package com.tts.fixapi.core;

import javax.management.JMException;
import javax.management.ObjectName;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fixapi.log.TtsFileLogFactoryV2;
import com.tts.fixapi.type.IFIXAcceptorMessageDispatcher;
import com.tts.util.AppUtils;

import quickfix.Acceptor;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;


public class FixAcceptor {
	private static final Logger logger = LoggerFactory.getLogger("FixAPILogger");
	
	private final SessionSettings fixSessionSettings;
	private final IFIXAcceptorMessageDispatcher fixApplication;
	private final Acceptor fixSocketAcceptor;
	
	private final JmxExporter jmxExporter;
    private final ObjectName connectorObjectName;
    
    private boolean socketAcceptorStarted = false;
	
	public FixAcceptor(SessionSettings fixSessionSettings, IFIXAcceptorMessageDispatcher fixApplication) throws ConfigError, JMException, FieldConvertError	{
		this.fixSessionSettings = fixSessionSettings;
		this.fixApplication     = fixApplication;
		
		MessageStoreFactory storeFactory = new FileStoreFactory(this.fixSessionSettings);
		//LogFactory logFactory            = new FileLogFactory(this.fixSessionSettings);
		LogFactory logFactory            = new TtsFileLogFactoryV2(this.fixSessionSettings);
		MessageFactory messageFactory    = new DefaultMessageFactory();
				
		fixSocketAcceptor = new SocketAcceptor(this.fixApplication, storeFactory, this.fixSessionSettings, logFactory, messageFactory);
		
		jmxExporter  = new JmxExporter();
        connectorObjectName = jmxExporter.register(fixSocketAcceptor);
        logger.info("FIX Acceptor Initialized for New Connection(s)...");
	}
	
	public synchronized boolean start()	{
		try	{
			if(!socketAcceptorStarted)	{
				fixSocketAcceptor.start();
				socketAcceptorStarted = true;
				logger.info("FIX Acceptor Listener Started...");
			}
		}
		catch(Exception exp) {
			logger.error("FIX Acceptor Listener Failed. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
		
		return(socketAcceptorStarted);
	}
	
	public synchronized void stop() {
		try	{
			jmxExporter.getMBeanServer().unregisterMBean(connectorObjectName);
		} catch (Exception exp) {
			logger.error("FIX Acceptor Failed to unregister acceptor from JMX. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
				
		fixSocketAcceptor.stop(false);
		socketAcceptorStarted = false;
	}
	
	public SessionSettings getSessionSettings() {
		return fixSessionSettings;
	}
}
