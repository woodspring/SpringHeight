package com.tts.ske.qfx.impl.initiator;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionFactory;
import quickfix.SessionSettings;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.initiator.AbstractSocketInitiator;

public class TtsThreadedSocketInitiator extends AbstractSocketInitiator {

    private final TtsFixEventHandlingStrategy eventHandlingStrategy;

    public TtsThreadedSocketInitiator(Application application,
            MessageStoreFactory messageStoreFactory, SessionSettings settings,
            LogFactory logFactory, MessageFactory messageFactory, int queueCapacity) throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, messageFactory);
        eventHandlingStrategy = new TtsFixEventHandlingStrategy(this, queueCapacity);
    }

    public TtsThreadedSocketInitiator(Application application,
            MessageStoreFactory messageStoreFactory, SessionSettings settings,
            LogFactory logFactory, MessageFactory messageFactory) throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, messageFactory);
        eventHandlingStrategy = new TtsFixEventHandlingStrategy(this, DEFAULT_QUEUE_CAPACITY);
    }

    public TtsThreadedSocketInitiator(Application application,
            MessageStoreFactory messageStoreFactory, SessionSettings settings,
            MessageFactory messageFactory, int queueCapacity) throws ConfigError {
        super(application, messageStoreFactory, settings, new ScreenLogFactory(settings),
                messageFactory);
        eventHandlingStrategy = new TtsFixEventHandlingStrategy(this, queueCapacity);
    }

    public TtsThreadedSocketInitiator(Application application,
            MessageStoreFactory messageStoreFactory, SessionSettings settings,
            MessageFactory messageFactory) throws ConfigError {
        super(application, messageStoreFactory, settings, new ScreenLogFactory(settings),
                messageFactory);
        eventHandlingStrategy = new TtsFixEventHandlingStrategy(this, DEFAULT_QUEUE_CAPACITY);
    }

    public TtsThreadedSocketInitiator(SessionFactory sessionFactory, SessionSettings settings, int queueCapacity)
            throws ConfigError {
        super(settings, sessionFactory);
        eventHandlingStrategy = new TtsFixEventHandlingStrategy(this, queueCapacity);
    }

    public TtsThreadedSocketInitiator(SessionFactory sessionFactory, SessionSettings settings)
            throws ConfigError {
        super(settings, sessionFactory);
        eventHandlingStrategy = new TtsFixEventHandlingStrategy(this, DEFAULT_QUEUE_CAPACITY);
    }

    public void start() throws ConfigError, RuntimeError {
        createSessionInitiators();
        startInitiators();
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean forceDisconnect) {
        logoutAllSessions(forceDisconnect);
        stopInitiators();
        eventHandlingStrategy.stopDispatcherThreads();
        
        //TODO: Lawrence fix this
        //Session.unregisterSessions(getSessions());
    }

    public void block() throws ConfigError, RuntimeError {
        throw new UnsupportedOperationException("Blocking not supported: " + getClass());
    }

    @Override
    protected EventHandlingStrategy getEventHandlingStrategy() {
        return eventHandlingStrategy;
    }

}
