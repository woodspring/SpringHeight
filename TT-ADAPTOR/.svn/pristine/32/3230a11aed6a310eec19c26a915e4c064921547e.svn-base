package com.tts.mas.app.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.support.InjectionWorker;
import com.tts.mas.vo.InjectionConfig;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.app.IExternalInterfacingApp;
import com.tts.plugin.adapter.api.app.IExternalInterfacingApp.ChangeTradingSessionBehavior;
import com.tts.plugin.adapter.api.app.IPublishingApp;
import com.tts.plugin.adapter.api.app.ISubscribingApp;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;

public class InterfacingAppController {
	
	private final static List<String> PRESISTANCE_TOPICS = Arrays.asList(new String[] {
			IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ALL_ORDER,
			IEventMessageTypeConstant.Market.TOPIC_TRADE_ALL
	});

	private final static Logger logger = LoggerFactory.getLogger(InterfacingAppController.class);

	private final IInterfacingAppFactory interfacingAppFactory;
	private final IMsgReceiverFactory msgReceiverFactory;
	private final ConcurrentHashMap<String, List<IMsgReceiver>> startedReceivers;
	private final InjectionConfig injectionConfig;
	private volatile List<IExternalInterfacingApp> startedApps = null;

	private volatile InjectionWorker injectionWorker = null;
	
	public InterfacingAppController(IInterfacingAppFactory publishingAppFactory) {
		super();
		this.interfacingAppFactory = publishingAppFactory;
		this.startedReceivers = new ConcurrentHashMap<String, List<IMsgReceiver>>();
		this.msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		this.startedApps = Collections.emptyList();
		this.injectionConfig = new InjectionConfig();
	}
	
	public void init(SessionInfo sessionInfo) throws Exception {
		HashSet<String> startedAppNames = new HashSet<String>();
		for ( IExternalInterfacingApp masApp: this.startedApps) {
			startedAppNames.add(masApp.getName());
		}
		List<IPublishingApp> pApps = new ArrayList<IPublishingApp>(injectionConfig.getPublishingApps());
		ArrayList<IExternalInterfacingApp> _startedApps = new ArrayList<IExternalInterfacingApp>(this.startedApps);
    	String appCategory = System.getProperty("APP_CATEGORY", "all");
		for ( AppType appType: AppType.values()) {
			if ( interfacingAppFactory != null 
					&& ("all".equals(appCategory) || appType.getAppCategory().equals(appCategory)) ) {
				IExternalInterfacingApp masApp = interfacingAppFactory.createApplication(appType);
				if ( masApp != null ) {
					if ( masApp.getChangeTradingSessionBehavior() == ChangeTradingSessionBehavior.NO_CHANGE 
							&& startedAppNames.contains(masApp.getName())) {
						logger.debug(masApp.getName() + " is already running with ChangeTradingSessionBehavior = NO_CHANGE");

						continue;
					}
					
					logger.debug("Starting " + masApp.getName() + ". ChangeTradingSessionBehavior=" + masApp.getChangeTradingSessionBehavior() 
						+ " startedApps.contains(masApp): " + startedApps.contains(masApp));
					masApp.start();
					if ( masApp instanceof IPublishingApp ) {
						pApps.add((IPublishingApp) masApp);
					}
					if ( masApp instanceof ISubscribingApp) {
						ISubscribingApp sApp = (ISubscribingApp) masApp;
						ArrayList<IMsgReceiver> msgReceivers = new ArrayList<IMsgReceiver>();
						for (String topic : sApp.getDesiredTopics() ) {
							IMsgReceiver msgReceiver = null;
							if (PRESISTANCE_TOPICS.contains(topic)) {
								msgReceiver = msgReceiverFactory.getMsgReceiver(false, false, true);
							} else {
								msgReceiver = msgReceiverFactory.getMsgReceiver(false, false);
							}
							msgReceiver.setTopic(topic);
							msgReceiver.setListener(new MessageConnectorListener(sApp));
							msgReceiver.start();
							msgReceivers.add(msgReceiver);
						}
						this.startedReceivers.put(masApp.getName(), Collections.unmodifiableList(msgReceivers));
					}
					_startedApps.add(masApp);
				}

			} 
		}
		
		this.startedApps = _startedApps;
		this.injectionConfig.setPublishingApps(pApps);
		if ( this.injectionWorker == null ) {
			this.injectionWorker = new InjectionWorker(this.injectionConfig);
			this.injectionWorker.start();
		} 
		logger.debug("Started interfacing apps");
	}
	
	public void destroy() {
		destroy(false);
	}
	
	public void destroy(boolean preserveForNext){
		if ( !preserveForNext && this.injectionWorker != null ) {
			injectionWorker.stop();
			injectionWorker = null;
		}

		List<IExternalInterfacingApp> remaining = new ArrayList<IExternalInterfacingApp>(startedApps);
		List<IPublishingApp> remainingPApps = new ArrayList<IPublishingApp>(injectionConfig.getPublishingApps());
		for ( IExternalInterfacingApp masApp: startedApps) {
			if ( !preserveForNext || masApp.getChangeTradingSessionBehavior() == ChangeTradingSessionBehavior.REFRESH) {
				if ( masApp instanceof IPublishingApp) {
					remainingPApps.remove(masApp);
				}
				masApp.stop();
				
				List<IMsgReceiver> list = startedReceivers.get(masApp.getName());
				if ( list != null ) {
					for ( IMsgReceiver receiver: list) {
						receiver.destroy();
					}
					startedReceivers.remove(masApp.getName());
				}
				logger.debug("Stopped " + masApp.getName());
				remaining.remove(masApp);
			}
		}
		this.injectionConfig.setPublishingApps(remainingPApps);
		this.startedApps = Collections.unmodifiableList(remaining);
	}
	
	private static class MessageConnectorListener implements IMsgListener {
		
		private  final ISubscribingApp appListener;
		
		public MessageConnectorListener(ISubscribingApp appListener) {
			super();
			this.appListener = appListener;
		}

		@Override
		public void onMessage(TtMsg ttMsg, IMsgSessionInfo sessionInfo,
				IMsgProperties properties) {
			try {
				appListener.onRequest(properties.getSendTopic(), ttMsg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

