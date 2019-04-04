package com.tts.mde.spot.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tts.mde.provider.MDProviderStateManager;
import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.mde.support.IInstrumentDetailProvider;
import com.tts.mde.support.IMarketDataHandler;
import com.tts.mde.support.IPublishingEndpoint;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.support.impl.SessionInfoVo;
import com.tts.mde.vo.IMDProvider;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.config.AggConfigStruct.AggSourceIndividualStatus;
import com.tts.message.config.AggConfigStruct.AggSourceStatus;
import com.tts.message.config.AggConfigStruct.AggSourceStatusOverview;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;

public class LPSubscriptionManager implements IMsgListener, IMarketDataHandler {

	private final Map<String, LPSingleCcyPairSubscriptionManager> m;
	private final LPSingleCcyPairSubscriptionManager[] managers;
	private final Map<String, Boolean> srcStatusMap = new ConcurrentHashMap<>();
	private final IMsgSender msgSender;
	
	public LPSubscriptionManager(List<String> _ccyPairs, IMsgSender ctrlMsgSender, SessionInfoVo sessionInfo, IInstrumentDetailProvider instrumentDetailProvider, ISchedulingWorker schedulingWorker) {
		HashMap<String, LPSingleCcyPairSubscriptionManager> map = new HashMap<>();
		IMsgSenderFactory sFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		MDProviderStateManager mdProviderStateManager = AppContext.getContext().getBean(MDProviderStateManager.class);

		IMsgSender msgSender = sFactory.getMsgSender(true, false);
		msgSender.init();
		
		ArrayList<String> ccyPairs = new ArrayList<>(_ccyPairs);
		if ( !ccyPairs.contains("USDCAD")) {
			ccyPairs.add("USDCAD");
		}	
		if ( !ccyPairs.contains("EURUSD")) {
			ccyPairs.add("EURUSD");
		}
		int ccyPairSize = ccyPairs.size();
		this.managers = new LPSingleCcyPairSubscriptionManager[ccyPairSize];
		for ( int i = 0; i < ccyPairSize; i++) {
			String ccyPair = ccyPairs.get(i);
			LPSingleCcyPairSubscriptionManager manager = new LPSingleCcyPairSubscriptionManager(ccyPair, ctrlMsgSender,sessionInfo, mdProviderStateManager, instrumentDetailProvider, schedulingWorker);
			map.put(ccyPair, manager);
			managers[i] = manager;
		}
		this.m = Collections.unmodifiableMap(map);
		this.msgSender = msgSender;
		for (IMDProvider provider : sessionInfo.getMDprovidersExternal() ) {
			this.srcStatusMap.put(provider.getSourceNm(), Boolean.TRUE);
		}
	}
	
	public void destroy() {
		
		if ( this.m != null ) {
			List<LPSingleCcyPairSubscriptionManager> l = new ArrayList<>(m.values());
			for ( LPSingleCcyPairSubscriptionManager m : l) {
				m.destroy();
			}
		}

	}
	
	public ILiquidityPool getLiquidityPool(String symbol, List<MdSubscriptionVo> subs, boolean shortTermSubscription) {
		LPSingleCcyPairSubscriptionManager ccyManager= this.m.get(symbol); 
		if ( ccyManager == null ) {
			return null;
		}
		return ccyManager.getLiquidityPool(subs, shortTermSubscription);	
	}

	@Override
	public void onMessage(TtMsg msg, IMsgSessionInfo arg1, IMsgProperties arg2) {
		try {
			RawMarketBook mb = RawMarketBook.parseFrom(msg.getParameters());
			String symbol = mb.getSymbol();
			LPSingleCcyPairSubscriptionManager ccyPairManager = m.get(symbol);
			if (ccyPairManager != null ) {
				ccyPairManager.onMarketData( mb);
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void atPublish(long masGlobalSeq, IPublishingEndpoint publishingEndpoint) {
		for (LPSingleCcyPairSubscriptionManager manager: managers) {
			manager.atPublish(masGlobalSeq, publishingEndpoint);
		}
		if ( (masGlobalSeq % 13) == 0)  {
			publishSrcStatus();
		}
	}
	
	private void publishSrcStatus() {
		AggSourceStatusOverview.Builder status = AggSourceStatusOverview.newBuilder();
		Set<Entry<String, Boolean>> entrySet = srcStatusMap.entrySet();
		for (Entry<String, Boolean> statusEntry : entrySet) {
			AggSourceIndividualStatus.Builder statusIndividual = AggSourceIndividualStatus.newBuilder();
			statusIndividual.setSourceNm(statusEntry.getKey());
			statusIndividual.setStatus(statusEntry.getValue() ? AggSourceStatus.ACTIVE : AggSourceStatus.SUSPENDED);
			
			status.addSourceStatus(statusIndividual);
		}
		AggSourceStatusOverview _statusOverview = status.build();
		TtMsg t = TtMsgEncoder.encode(_statusOverview);
		
		this.msgSender.send(IEventMessageTypeConstant.Agg.AGG_SOURCE_STATUS_OVERVIEW, t);
	}

	public void setSrcEnable(String srcName, boolean isEnable) {
		for (LPSingleCcyPairSubscriptionManager manager: managers) {
			manager.setSrcEnabled(srcName, isEnable);
		}
		srcStatusMap.put(srcName, isEnable);
		publishSrcStatus();

	}
	
}
