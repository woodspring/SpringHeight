package com.tts.fa.qfx.impl.initiator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import quickfix.LogUtil;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.SessionConnector;


public class TtsFixEventHandlingStrategy implements EventHandlingStrategy {

    private final ConcurrentMap<SessionID, MessageDispatchingThread> dispatchers = new ConcurrentHashMap<>();
    private final SessionConnector sessionConnector;
    private final int queueCapacity;
    private volatile Executor executor;

    public TtsFixEventHandlingStrategy(SessionConnector connector, int queueCapacity) {
        sessionConnector = connector;
        this.queueCapacity = queueCapacity;
    }

    public void setExecutor(Executor executor) {
		this.executor = executor;
	}

    @Override
    public void onMessage(Session quickfixSession, Message message) {
        MessageDispatchingThread dispatcher = dispatchers.get(quickfixSession.getSessionID());
        if (dispatcher == null) {
            dispatcher = dispatchers.computeIfAbsent(quickfixSession.getSessionID(), sessionID -> {
               final MessageDispatchingThread newDispatcher = new MessageDispatchingThread(quickfixSession, queueCapacity, executor);
                startDispatcherThread(newDispatcher);
                return newDispatcher;
            });
        }
        if (message != null) {
            dispatcher.enqueue(message);
        }
    }

    /**
     * The SessionConnector is not directly required for thread-per-session handler - we don't multiplex
     * between multiple sessions here.
     * However it is made available here for other callers (such as SessionProviders wishing to register dynamic sessions).
     * @return the SessionConnector
     */
    @Override
    public SessionConnector getSessionConnector() {
        return sessionConnector;
    }

    protected void startDispatcherThread(MessageDispatchingThread dispatcher) {
        dispatcher.start();
    }

    public void stopDispatcherThreads() {
        // dispatchersToShutdown is backed by the map itself so changes in one are reflected in the other
        final Collection<MessageDispatchingThread> dispatchersToShutdown = dispatchers.values();
        for (final MessageDispatchingThread dispatcher : dispatchersToShutdown) {
            dispatcher.stopDispatcher();
        }

        // wait for threads to stop
        while (!dispatchersToShutdown.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            for (final Iterator<MessageDispatchingThread> iterator = dispatchersToShutdown
                    .iterator(); iterator.hasNext();) {
                final MessageDispatchingThread messageDispatchingThread = iterator.next();
                if (messageDispatchingThread.isStopped()) {
                    iterator.remove();
                }
            }
        }
    }

	/**
	 * A stand-in for the Thread class that delegates to an Executor.
	 * Implements all the API required by pre-existing QFJ code.
	 */
	protected static abstract class ThreadAdapter implements Runnable {

		private final Executor executor;
		private final String name;

		public ThreadAdapter(String name, Executor executor) {
			this.name = name;
			this.executor = executor != null ? executor : new DedicatedThreadExecutor(name);
		}

		public void start() {
			executor.execute(this);
		}

		@Override
		public final void run() {
			Thread currentThread = Thread.currentThread();
			String threadName = currentThread.getName();
			try {
				if (!name.equals(threadName)) {
					currentThread.setName(name + " (" + threadName + ")");
				}
				doRun();
			} finally {
				currentThread.setName(threadName);
			}
		}

		abstract void doRun();

		/**
		 * An Executor that uses it's own dedicated Thread.
		 * Provides equivalent behavior to the prior non-Executor approach.
		 */
		static final class DedicatedThreadExecutor implements Executor {

			private final String name;
			
			DedicatedThreadExecutor(String name) {
				this.name = name;
			}

			@Override
			public void execute(Runnable command) {
				new Thread(command, name).start();
			}

		}

	}

	protected class MessageDispatchingThread extends ThreadAdapter {
        private final Session quickfixSession;
        private final BlockingQueue<Message> messages;
        private volatile boolean stopped;
        private volatile boolean stopping;

        private MessageDispatchingThread(Session session, int queueCapacity, Executor executor) {
            super("QF/J Session dispatcher: " + session.getSessionID(), executor);
            quickfixSession = session;
            messages = new LinkedBlockingQueue<>(queueCapacity);
        }

        public void enqueue(Message message) {
            if (message == END_OF_STREAM && stopping) {
                return;
            }
            try {
                messages.put(message);
            } catch (final InterruptedException e) {
                quickfixSession.getLog().onErrorEvent(e.toString());
            }
        }

        public int getQueueSize() {
            return messages.size();
        }

        @Override
        void doRun() {
            while (!stopping) {
                try {
                    final Message message = getNextMessage(messages);
                    if (message == null) {
                        // no message available in polling interval
                        continue;
                    }
                    quickfixSession.next(message);
                    if (message == END_OF_STREAM) {
                        stopping = true;
                    }
                } catch (final InterruptedException e) {
                    LogUtil.logThrowable(quickfixSession.getSessionID(),
                            "Message dispatcher interrupted", e);
                    stopping = true;
                } catch (final Throwable e) {
                    LogUtil.logThrowable(quickfixSession.getSessionID(),
                            "Error during message processing", e);
                }
            }
            if (!messages.isEmpty()) {
                final List<Message> tempList = new ArrayList<>();
                messages.drainTo(tempList);
                for (Message message : tempList) {
                    try {
                        quickfixSession.next(message);
                    } catch (final Throwable e) {
                        LogUtil.logThrowable(quickfixSession.getSessionID(),
                                "Error during message processing", e);
                    }
                }
            }

            dispatchers.remove(quickfixSession.getSessionID());
            stopped = true;
        }

        public void stopDispatcher() {
            enqueue(END_OF_STREAM);
            stopping = true;
            stopped = true;
        }

        public boolean isStopped() {
            return stopped;
        }
    }

    protected MessageDispatchingThread getDispatcher(SessionID sessionID) {
        return dispatchers.get(sessionID);
    }

    /**
     * Get the next message from the messages {@link java.util.concurrent.BlockingQueue}.
     * <p>
     * We do not block indefinitely as that would prevent this thread from ever stopping
     *
     * @see #THREAD_WAIT_FOR_MESSAGE_MS
     * @param messages
     * @return next message or null if nothing arrived within the timeout period
     * @throws InterruptedException
     */
    protected Message getNextMessage(BlockingQueue<Message> messages) throws InterruptedException {
        return messages.poll(THREAD_WAIT_FOR_MESSAGE_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getQueueSize() {
        int ret = 0;
        for (final MessageDispatchingThread mdt : dispatchers.values()) {
            ret += mdt.getQueueSize();
        }
        return ret;
    }

    @Override
    public int getQueueSize(SessionID sessionID) {
        MessageDispatchingThread dispatchingThread = dispatchers.get(sessionID);
        if (dispatchingThread != null) {
            return dispatchingThread.getQueueSize();
        }
        return 0;
    }

}
