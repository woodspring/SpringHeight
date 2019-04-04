package com.tts.mlp.qfix;


import static quickfix.Acceptor.SETTING_ACCEPTOR_TEMPLATE;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_ADDRESS;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_PORT;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.ObjectName;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.qfix.log.TtsFileLogFactoryV2;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

public class QuickfixEngineContainer {
    private final static Logger logger = LoggerFactory.getLogger(QuickfixEngineContainer.class);
    private final SocketAcceptor acceptor;
    private final Map<InetSocketAddress, List<TemplateMapping>> dynamicSessionMappings = new HashMap<InetSocketAddress, List<TemplateMapping>>();

    private final JmxExporter jmxExporter;
    private final ObjectName connectorObjectName;
    
	public QuickfixEngineContainer(
			InputStream settingFileStream, 
			Application quickfixApplication) throws ConfigError, FieldConvertError, JMException {
		SessionSettings settings = new SessionSettings(settingFileStream);
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new TtsFileLogFactoryV2(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();

        acceptor = new SocketAcceptor(quickfixApplication, messageStoreFactory, settings, logFactory,
                messageFactory);

        configureDynamicSessions(settings, quickfixApplication, messageStoreFactory, logFactory,
                messageFactory);

        jmxExporter = new JmxExporter();
        connectorObjectName = jmxExporter.register(acceptor);
        logger.info("Acceptor registered with JMX, name=" + connectorObjectName);
	}
	

	public void start() throws RuntimeError, ConfigError {
        acceptor.start();
    }

	public void stop() {
        try {
            jmxExporter.getMBeanServer().unregisterMBean(connectorObjectName);
        } catch (Exception e) {
            logger.error("Failed to unregister acceptor from JMX", e);
        }
        acceptor.stop();
    }
	
    private void configureDynamicSessions(SessionSettings settings, Application application,
            MessageStoreFactory messageStoreFactory, LogFactory logFactory,
            MessageFactory messageFactory) throws ConfigError, FieldConvertError {
        //
        // If a session template is detected in the settings, then
        // set up a dynamic session provider.
        //

        Iterator<SessionID> sectionIterator = settings.sectionIterator();
        while (sectionIterator.hasNext()) {
            SessionID sessionID = sectionIterator.next();
            if (isSessionTemplate(settings, sessionID)) {
                InetSocketAddress address = getAcceptorSocketAddress(settings, sessionID);
                getMappings(address).add(new TemplateMapping(sessionID, sessionID));
            }
        }

        for (Map.Entry<InetSocketAddress, List<TemplateMapping>> entry : dynamicSessionMappings
                .entrySet()) {
            acceptor.setSessionProvider(entry.getKey(), new DynamicAcceptorSessionProvider(
                    settings, entry.getValue(), application, messageStoreFactory, logFactory,
                    messageFactory));
        }
    }
    
    private List<TemplateMapping> getMappings(InetSocketAddress address) {
        List<TemplateMapping> mappings = dynamicSessionMappings.get(address);
        if (mappings == null) {
            mappings = new ArrayList<TemplateMapping>();
            dynamicSessionMappings.put(address, mappings);
        }
        return mappings;
    }

    private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID)
            throws ConfigError, FieldConvertError {
        String acceptorHost = "0.0.0.0";
        if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
            acceptorHost = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
        }
        int acceptorPort = (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT);

        return new InetSocketAddress(acceptorHost, acceptorPort);
    }

    private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID)
            throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE)
                && settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
    }
}
