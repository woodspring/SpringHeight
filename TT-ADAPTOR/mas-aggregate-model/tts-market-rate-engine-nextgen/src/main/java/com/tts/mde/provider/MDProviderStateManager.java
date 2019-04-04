package com.tts.mde.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.mde.support.config.Adapter;
import com.tts.mde.support.config.Adapter.SourceConfig;
import com.tts.mde.support.config.LPProduct;
import com.tts.mde.support.config.MarketDataSetConfig;
import com.tts.mde.support.config.OrderTypeConfig;
import com.tts.mde.support.config.OrderingCapability;
import com.tts.mde.support.config.SubscriptionType;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.system.admin.AdapterStruct.AdapterStatus;
import com.tts.message.system.admin.AdapterStruct.AdapterStatusRequest;
import com.tts.message.system.admin.AdapterStruct.FixSessionCapability;
import com.tts.message.system.admin.AdapterStruct.SessionStatus;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;

public class MDProviderStateManager implements IMsgListener {

	private static final String CONSTANT_PREFIX_FOR_ADAPTER_STATUS = String.format(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_INFO_TEMPLATE, "");
	
	public static final Logger logger = LoggerFactory.getLogger(MDProviderStateManager.class);
	
	private IMsgSender msgSender;
	private IMsgReceiver msgReceiver;
	private Map<String, Status> providerStatusMap = new ConcurrentHashMap<>();
	private Map<String, List<IMDProviderStateListener>> providerListenerMap = new ConcurrentHashMap<>();

	
	public void init() {
		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		IMsgSenderFactory   msgSenderFactory   = AppContext.getContext().getBean(IMsgSenderFactory.class);
		
		msgSender = msgSenderFactory.getMsgSender(false, false);
		msgSender.init();
		
		msgReceiver =msgReceiverFactory.getMsgReceiver(false, false);
		msgReceiver.setListener(this);
		msgReceiver.setTopic(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_ALL_INFO);
		msgReceiver.init();
	}
	
	public void destroy() {
		msgReceiver.destroy();
		
	}
	
	public void registerListener(final String adapter, final String source, final String sessionType, final IMDProviderStateListener listener ) {
		String identify = buildIdentify(adapter, source, sessionType);

		providerListenerMap.compute(identify, new BiFunction<String,  List<IMDProviderStateListener>,  List<IMDProviderStateListener>>() {

			@Override
			public  List<IMDProviderStateListener> apply(String t,  List<IMDProviderStateListener> u) {
				if ( u == null ) {
					u = new ArrayList<>();
				}
				u.add(listener);
				return u;
			}
			
		});
	}

	public void unregisterListener(final String adapter, final String source, final String sessionType, final IMDProviderStateListener listener ) {
		String identify = buildIdentify(adapter, source, sessionType);

		providerListenerMap.compute(identify, new BiFunction<String,  List<IMDProviderStateListener>,  List<IMDProviderStateListener>>() {

			@Override
			public  List<IMDProviderStateListener> apply(String t,  List<IMDProviderStateListener> u) {
				if ( u != null ) {		
					u.remove(listener);
				}
				return u;
			}
			
		});
	}
	

	public void updateStatus(final String adapter, final String source, final String sessionType , final long lastOnline, final long lastOffline, final boolean enabled) {
		final String identifier = buildIdentify(adapter, source, sessionType);
		providerStatusMap.compute(identifier, new BiFunction<String, Status, Status>() {

			@Override
			public Status apply(String t, Status u) {
				long _lastOnlineLastValue = -1, _lastOfflineLastValue = -1;
				if ( u == null ) {
					u = new Status();
				} else {
					_lastOnlineLastValue = u.getLastOnlineTime();
					_lastOfflineLastValue = u.getLastOfflineTime();
				}
				u.setLastUpdateTime(System.currentTimeMillis());
				u.setEnabled(enabled);
				u.setLastOnlineTime(lastOnline);
				u.setLastOfflineTime(lastOffline);
				logger.info("SessionStatusChanged(" + t + ")->Online?" + enabled + " lastOnline:" + lastOnline + " lastOffline:" + lastOffline);
				
				if ( lastOnline == _lastOnlineLastValue && _lastOfflineLastValue == lastOffline) {
					//no change
				} else if (enabled && lastOnline > _lastOnlineLastValue ) {
					logger.info("detected new FIX session establishment, " + identifier);
					List<IMDProviderStateListener> l = providerListenerMap.get(identifier);
					if ( l != null ) {
						for (IMDProviderStateListener  i: l ){
							i.doWhenOnline(adapter, source);
						}
					}
				} else if (!enabled && lastOffline > _lastOfflineLastValue) {
					logger.info("detected FIX session Offline, " + identifier);
					List<IMDProviderStateListener> l = providerListenerMap.get(identifier);
					if ( l != null ) {
						for (IMDProviderStateListener  i: l ){
							i.doWhenOffline(adapter, source);
						}
					}
				}
				return u;
			}
			
		});
	}
	
	public boolean isStatusEnabled(final String adapter, final String source, final String sessionType  ) {
		String identifier = buildIdentify(adapter, source, sessionType);
		Status s = providerStatusMap.get(identifier);
		if ( s == null ) {
			return false;
		}
		return s.getEnabled();
	}

	public void setAdapterConfig(MarketDataSetConfig mdsConfig) {
		List<Adapter> config = mdsConfig.getAdapters().getAdapter();
		for ( Adapter a: config) {
			for ( SourceConfig sc: a.getSourceConfig()) {
				HashSet<String> sessionType = new HashSet<String>(); 
				String source = sc.getSourceNm();
				if ( sc.getProducts() !=null && sc.getProducts().getProduct().size() > 0) {
					for (LPProduct p : sc.getProducts().getProduct()) {
						for ( SubscriptionType st : p.getSubscriptionTypes().getSubscriptionType()) {
							if (SubscriptionType.ESP.equals(st) ) {
								sessionType.add(IMDProviderStateListener.CONSTANT_SESSIONTYPE_ESP);
							}
							if (SubscriptionType.RFS.equals(st) ) {
								sessionType.add(IMDProviderStateListener.CONSTANT_SESSIONTYPE_RFS);
							}
						}
						for ( OrderTypeConfig oc : p.getOrderConfig().getOrderTypeConfig()) {
							if ( OrderingCapability.LIMIT_FOK == oc.getOrdTypeNm()
									|| OrderingCapability.MARKET == oc.getOrdTypeNm()
									|| OrderingCapability.PREVIOUSLY_QUOTED_FOK == oc.getOrdTypeNm()
									|| OrderingCapability.PREVIOUSLY_QUOTED_IOC == oc.getOrdTypeNm()) {
								sessionType.add(IMDProviderStateListener.CONSTANT_SESSIONTYPE_IMMEDIATE_ORDER);
							} else if ( OrderingCapability.GTC == oc.getOrdTypeNm()
									|| OrderingCapability.GTD == oc.getOrdTypeNm() ) {
								sessionType.add(IMDProviderStateListener.CONSTANT_SESSIONTYPE_RESTING_ORDER);	
							}
						}
					}
				}
				for (String sessionType1 : sessionType ) {
					requestUpdate(a.getAdapterNm(), source, sessionType1);
				}
			}
		}
	}
	
	private void requestUpdate(String adapterNm, String source, String sessionType1) {
		AdapterStatusRequest.Builder r = AdapterStatusRequest.newBuilder();
		r.setAdapterName(adapterNm);
		r.setSourceNm(source);
		if ( IMDProviderStateListener.CONSTANT_SESSIONTYPE_ESP.equals(sessionType1)) {
			r.setCapability(FixSessionCapability.MARKET_DATA__ESP);
		} else if ( IMDProviderStateListener.CONSTANT_SESSIONTYPE_RFS.equals(sessionType1)) {
			r.setCapability(FixSessionCapability.MARKET_DATA__RFS);
		} else if ( IMDProviderStateListener.CONSTANT_SESSIONTYPE_IMMEDIATE_ORDER.equals(sessionType1)) {
			r.setCapability(FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER);
		} else if ( IMDProviderStateListener.CONSTANT_SESSIONTYPE_RESTING_ORDER.equals(sessionType1)) {
			r.setCapability(FixSessionCapability.ORDERING__GENERAL_RESTING_ORDER);
		} 
		r.setRequestId(Long.toString(System.nanoTime()));
		AdapterStatusRequest rr = r.build();
		TtMsg t = TtMsgEncoder.encode(rr);
		String topic = String.format(IEventMessageTypeConstant.FixAdapter.TOPIC_CTRL_STATUS_REQUEST_TEMPLATE, "AGG", adapterNm.toUpperCase());
		logger.debug("sending adapter status request to " + topic);
		msgSender.send(topic, t);
	}

	private String buildIdentify(final String adapter, final String source, final String sessionType) {
		return adapter + "/" + source + "/" + sessionType;
	}
	
	public static class Status {
		private Boolean enabled = Boolean.FALSE;
		private String fixSessionId = null;
		private long lastOnlineTime = -1;
		private long lastOfflineTime = -1;
		private long lastUpdateTime = -1;
		
		public Boolean getEnabled() {
			return enabled;
		}
		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}
		public long getLastUpdateTime() {
			return lastUpdateTime;
		}
		public void setLastUpdateTime(long lastUpdateTime) {
			this.lastUpdateTime = lastUpdateTime;
		}
		public String getFixSessionId() {
			return fixSessionId;
		}
		public void setFixSessionId(String fixSessionId) {
			this.fixSessionId = fixSessionId;
		}
		public long getLastOnlineTime() {
			return lastOnlineTime;
		}
		public void setLastOnlineTime(long lastOnlineTime) {
			this.lastOnlineTime = lastOnlineTime;
		}
		public long getLastOfflineTime() {
			return lastOfflineTime;
		}
		public void setLastOfflineTime(long lastOfflineTime) {
			this.lastOfflineTime = lastOfflineTime;
		}
	}

	@Override
	public void onMessage(TtMsg message, IMsgSessionInfo arg1, IMsgProperties msgProperties) {
		String eventType = msgProperties.getSendTopic();
		
		try {
			if ( eventType.startsWith(CONSTANT_PREFIX_FOR_ADAPTER_STATUS))  {
				logger.debug("received adapter status from " + eventType);

				AdapterStatus as = AdapterStatus.parseFrom(message.getParameters());
				logger.debug(TextFormat.shortDebugString(as));

				if ( as.getActiveSessionsCount() > 0 ) {
					for ( SessionStatus ss : as.getActiveSessionsList()) {
						if ( ss.hasSourceNm()) {
							String sourceNm = ss.getSourceNm();
							boolean isActive = ss.getStatus() == com.tts.message.system.admin.AdapterStruct.Status.ACTIVE;

							for (FixSessionCapability fsc : ss.getCapabilityList()) {
								String sessionType = null;
								if ( FixSessionCapability.MARKET_DATA__ESP ==fsc) {
									sessionType = IMDProviderStateListener.CONSTANT_SESSIONTYPE_ESP;
								} else if ( FixSessionCapability.MARKET_DATA__RFS==fsc) {
									sessionType = IMDProviderStateListener.CONSTANT_SESSIONTYPE_RFS;
								} else if ( FixSessionCapability.ORDERING__GENERAL_IMMEDIATE_ORDER==fsc) {
									sessionType = IMDProviderStateListener.CONSTANT_SESSIONTYPE_IMMEDIATE_ORDER;
								} else if ( FixSessionCapability.ORDERING__GENERAL_RESTING_ORDER==fsc) {
									sessionType = IMDProviderStateListener.CONSTANT_SESSIONTYPE_RESTING_ORDER;
								} 
								System.out.println(sessionType);
								if (sessionType != null) {
									updateStatus(
											as.getAdapterName(),
											sourceNm, 
											sessionType, 
											ss.getLastOnlineTimestamp(), 
											ss.getLastOfflineTimestamp(), 
											isActive);
								}
							}
						}
					}
				}
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}

	}
}
