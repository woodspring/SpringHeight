package com.tts.mde.spot.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.algo.IMDPriceAndExceAlgo;
import com.tts.mde.plugin.AggAlgoManager;
import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.IMrEndpoint;
import com.tts.mde.spot.IMrSubscriptionHandler;
import com.tts.mde.spot.vo.MdSubscriptionType;
import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IInstrumentDetailProvider;
import com.tts.mde.support.IMarketDataHandler;
import com.tts.mde.support.IPublishingEndpoint;
import com.tts.mde.support.config.MDSubscription;
import com.tts.mde.support.config.SpotMarketDataStreamSet.SpotMarketDataStream;
import com.tts.mde.support.impl.SessionInfoVo;
import com.tts.mde.vo.IMDProvider;
import com.tts.mde.vo.MarketDataProviderVo;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.util.AppContext;

public class MrSubscriptionHandlerManager implements IMarketDataHandler {
	private final static List<IMrEndpoint.OutboundType> OUTBOUND_TYPES__ALL  = Arrays.asList( new IMrEndpoint.OutboundType[]  { IMrEndpoint.OutboundType.RAW, IMrEndpoint.OutboundType.VWAP } );
	private final static List<IMrEndpoint.OutboundType> OUTBOUND_TYPES__RAW  = Arrays.asList( new IMrEndpoint.OutboundType[]  { IMrEndpoint.OutboundType.RAW  } );
	private final static List<IMrEndpoint.OutboundType> OUTBOUND_TYPES__VWAP = Arrays.asList( new IMrEndpoint.OutboundType[]  { IMrEndpoint.OutboundType.VWAP } );
	private final static List<IMrEndpoint.OutboundType> OUTBOUND_TYPES__CONSOLIDATED= Arrays.asList( new IMrEndpoint.OutboundType[]  { IMrEndpoint.OutboundType.CONSOLIDATED } );

	
	private final static Logger logger = LoggerFactory.getLogger(MrSubscriptionHandlerManager.class);
	private final static AtomicInteger HANDLER_COUNT = new AtomicInteger(0);
	
	private final Object monitor = new Object();
	private final IFxCalendarBizServiceApi fxCalendarBizService;
	private final SessionInfoVo sessionInfo;
	private final LPSubscriptionManager lpSubscriptionManager;
	private final IPublishingEndpoint pEndpoint;
	private final List<IMrSubscriptionHandler> autoMrSubscriptionHandlers = new ArrayList<>();
	private final List<IMrSubscriptionHandler> onDemandMrSubscriptionHandlers = new ArrayList<>();
	private final Map<String, IMrSubscriptionHandler> onDemandMrSubscriptionHandlerMap = new HashMap<>();
	private final Map<String, IMrSubscriptionHandler> handlerIdMap = new HashMap<>();
	private final IInstrumentDetailProvider instrumentDetailProvider;
	private final AggAlgoManager aggAlgoManager = new AggAlgoManager();
	private Set<String> compositeLqySrc;
	private Map<String, Map<String, SubscriptionHandlerWrapper>> compositeSrcMap;

	public MrSubscriptionHandlerManager(
			LPSubscriptionManager lpSubscriptionManager, 
			IFxCalendarBizServiceApi fxCalendarBizService,
			SessionInfoVo sessionInfo, 
			IInstrumentDetailProvider instrumentDetailProvider,
			List<SpotMarketDataStream> autoStreams,
			List<IMrSubscriptionHandler> previousOndemandSubscriptionHandlers) {
		this.lpSubscriptionManager = lpSubscriptionManager;
		this.fxCalendarBizService = fxCalendarBizService;
		this.sessionInfo = sessionInfo;
		this.instrumentDetailProvider = instrumentDetailProvider;
		this.pEndpoint = AppContext.getContext().getBean(IPublishingEndpoint.class);
		addSubscription(autoStreams);
		if (previousOndemandSubscriptionHandlers !=null  ) {
			reloadOldSubscriptionHandler(previousOndemandSubscriptionHandlers);
		}
	}

	private void reloadOldSubscriptionHandler(List<IMrSubscriptionHandler> previousOndemandSubscriptionHandlers) {

		
	}

	public IMrSubscriptionHandler addSubscription(PriceSubscriptionRequest r) {
		String symbol = r.getQuoteParam().getCurrencyPair();
		String algoType = r.getQuoteParam().getAlgoType();
		String algoNm = r.getQuoteParam().getAlgoNm();
		String source = r.getRateSource().getSpecificSourceNmCount() > 0 ? r.getRateSource().getSpecificSourceNm(0) : null;
		String topic = r.getTopic();
		
		if ( "VWAP".equals(algoType)) {
			algoNm = algoType;
		}
		logger.info(String.format("Receiving new MR price subscription request. %s %s %s %s %s", symbol, algoType, algoNm, source, topic));
		
		OndemandMrSubscriptionHandler h = null;
		
		boolean shortTermSubscription = false;
		String handlerId = Integer.toString(HANDLER_COUNT.incrementAndGet());
		if ( r.getRateRequestType() == PriceSubscriptionRequest.RateRequestType.RFQ) {
			shortTermSubscription = true;
			ArrayList<MdSubscriptionVo> outSubs= new ArrayList<>();
			for ( IMDProvider lp : sessionInfo.getMDprovidersExternal()) {
				if ( lp.isRFSenabled()) {
					MdSubscriptionVo sub = new MdSubscriptionVo(MdSubscriptionType.RFS, lp.getAdapterNm(), lp.getSourceNm(), r.getQuoteParam().getCurrencyPair(), 120000, new double[] { new BigDecimal(r.getQuoteParam().getSize()).doubleValue() });
					outSubs.add(sub);
				}
			}

			MrSubscriptionProperties.Builder props = new MrSubscriptionProperties.Builder();
			props.setHandlerId(handlerId);
			props.setSymbol(r.getQuoteParam().getCurrencyPair());
			if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.RAW) {
				props.setInterestedOutboundTypes(OUTBOUND_TYPES__RAW);
			} else 			if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.VWAP) {
				props.setInterestedOutboundTypes(OUTBOUND_TYPES__VWAP);
			}
			ILiquidityPool liquidityPool = lpSubscriptionManager.getLiquidityPool(symbol, outSubs, shortTermSubscription);
			MrMessageBusEndPoint endpoint = new MrMessageBusEndPoint(topic, pEndpoint);
			h = new BestPriceRfsSubscriptionHandler(
					handlerId,
					endpoint, 
					props.build(), 
					liquidityPool, 
					fxCalendarBizService, 
					sessionInfo, 
					instrumentDetailProvider.getInstrumentDetail(symbol));
		} else if ( compositeLqySrc.contains(source)  ) {
			SubscriptionHandlerWrapper wrapper = compositeSrcMap.get(symbol).get(source);
			ILiquidityPool liquidityPool = wrapper.getLiquidityPool();
			AutoMrSubscriptionHandler autoHandler = (AutoMrSubscriptionHandler) wrapper.getHandler();
			MrSubscriptionProperties.Builder props = autoHandler.getProperties().toBuilder();
			if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.RAW) {
				props.setInterestedOutboundTypes(OUTBOUND_TYPES__RAW);
			} else 			if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.CONSOLIDATED) {
				props.setInterestedOutboundTypes(OUTBOUND_TYPES__CONSOLIDATED);
			} else 			if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.VWAP) {
				props.setInterestedOutboundTypes(OUTBOUND_TYPES__VWAP);
			}
			props.setHandlerId(handlerId);
			MrMessageBusEndPoint endpoint = new MrMessageBusEndPoint(topic, pEndpoint);
			h = new OndemandMrSubscriptionHandler(
					handlerId,
					endpoint, 
					props.build(), 
					liquidityPool, 
					fxCalendarBizService, 
					sessionInfo, 
					instrumentDetailProvider.getInstrumentDetail(symbol),
					autoHandler.getMdeAggAlgo(),
					false);
		} else {
			Map<String, SubscriptionHandlerWrapper> wrapperMap = compositeSrcMap.get(symbol);
			Collection<SubscriptionHandlerWrapper> wrappers = wrapperMap.values();
			
			MdSubscriptionVo matchedLpSubscription = null;
			MrSubscriptionProperties.Builder props = null;
			for (SubscriptionHandlerWrapper w : wrappers ) {
				for ( MdSubscriptionVo subscription : w.getRequiredLpSubscription()) {
					if ( source.equalsIgnoreCase(subscription.getSource()) ) {
						matchedLpSubscription = subscription;
						AutoMrSubscriptionHandler autoHandler = (AutoMrSubscriptionHandler) w.getHandler();
						props = autoHandler.getProperties().toBuilder();
						props.setHandlerId(handlerId);
						if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.RAW) {
							props.setInterestedOutboundTypes(OUTBOUND_TYPES__RAW);
						} else 			if ( IMrEndpoint.OutboundType.fromString(algoNm) == IMrEndpoint.OutboundType.RAW) {
							props.setInterestedOutboundTypes(OUTBOUND_TYPES__VWAP);
						}
						break;
					}
				}
				if ( matchedLpSubscription != null ) {
					break;
				}
			}
			
			if ( matchedLpSubscription!= null ) {
				ILiquidityPool liquidityPool = lpSubscriptionManager.getLiquidityPool(symbol, Arrays.asList(new MdSubscriptionVo[] {matchedLpSubscription} ), shortTermSubscription);
				MrMessageBusEndPoint endpoint = new MrMessageBusEndPoint(topic, pEndpoint);
				IMDPriceAndExceAlgo algo = aggAlgoManager.getAlgoByName("VwapByPx101");				
				
				h = new OndemandMrSubscriptionHandler(
						handlerId,
						endpoint, 
						props.build(), 
						liquidityPool, 
						fxCalendarBizService, 
						sessionInfo, 
						instrumentDetailProvider.getInstrumentDetail(symbol),
						algo,
						true
						);
			}
		}
		
		if ( h != null ) {
			synchronized ( monitor ) {
				onDemandMrSubscriptionHandlers.add(h);
				onDemandMrSubscriptionHandlerMap.put(topic, h);
				handlerIdMap.put(handlerId, h);
			}
			logger.debug("Created MR handler of type, " + h.getClass().getName() + ", " + h);
		}
		
		return h;
	}

	private void addSubscription(List<SpotMarketDataStream> streams) {
		ArrayList<IMrSubscriptionHandler> handlers =  new ArrayList<>(streams.size());
		Set<String> directSrc = new HashSet<>();
		Set<String> compositeSrc = new HashSet<>();

		Map<String, Map<String, SubscriptionHandlerWrapper>> compositeSrcMap = new HashMap<>();
		Set<String> spotCcyPairs = new HashSet<>(streams.size());
		List<String> ccyPairWithInternalLqy = new ArrayList<>();
		
		for (SpotMarketDataStream s : streams) {
			String symbol = s.getCurrencyPair();
			spotCcyPairs.add(symbol);
			Map<String, SubscriptionHandlerWrapper> ccyPairComposite = compositeSrcMap.get(symbol);
			if ( ccyPairComposite == null ) {
				ccyPairComposite = new HashMap<>();
				compositeSrcMap.put(symbol, ccyPairComposite);
			}
			List<String> sources = new ArrayList<>();
			for ( MDSubscription sub : s.getSubscriptions().getSubscription() ) {
				directSrc.add(sub.getSourceNm());
				sources.add(sub.getSourceNm());
				if ( "INTERNAL".equals(sub.getSourceNm())  ) {
					ccyPairWithInternalLqy.add(symbol);
				}
			}
			compositeSrc.add(s.getOutboundQualifier());
			
			SubscriptionHandlerWrapper wrapper = addSubscription(s);
			ccyPairComposite.put(s.getOutboundQualifier(), wrapper);
			handlers.add(wrapper.getHandler());
		}

		this.compositeLqySrc = compositeSrc;
		this.compositeSrcMap = compositeSrcMap;
		
		synchronized ( monitor ) {
			this.autoMrSubscriptionHandlers.addAll(handlers);
		}
	}

	public IMrSubscriptionHandler findHandlerById(String id) {
		IMrSubscriptionHandler h = null;
		synchronized ( monitor ) {
			h = handlerIdMap.get(id);
		}
		return h;
	}
	
	private SubscriptionHandlerWrapper addSubscription(SpotMarketDataStream s) {
		
		List<MdSubscriptionVo> requiredSub = new ArrayList<>();
		String symbol = s.getCurrencyPair();
		for ( MDSubscription sub : s.getSubscriptions().getSubscription()) { 
			MarketDataProviderVo provider = (MarketDataProviderVo) this.sessionInfo.getMDproviderBySourceNm(sub.getSourceNm());
			MdSubscriptionVo subVo = new MdSubscriptionVo(MdSubscriptionType.ESP, provider.getAdapterNm(),  sub.getSourceNm(), symbol, provider.getDefaultMdValidIntervalInMilli());
			requiredSub.add(subVo);
		}
		
		ILiquidityPool lpool = lpSubscriptionManager.getLiquidityPool(symbol, requiredSub, false);
		MrMessageBusEndPoint mrEndpoint = new MrMessageBusEndPoint(symbol, s.getOutboundQualifier(), pEndpoint);
		
		long[] outboundRungs = null;
		for ( com.tts.mde.support.config.Property p : s.getAlgoProperties().getProperty() ) {
			if ( "rungs".equals(p.getKey())) {
				String[] rungss = p.getValue().split(",");
				outboundRungs = new long[rungss.length];
				int i = 0;
				for ( String rungs : rungss) {
					outboundRungs[i++] = Long.parseLong(rungs);
				}
			}
		}
		
		IMDPriceAndExceAlgo algo = aggAlgoManager.getAlgoByName(s.getAlgoNm());
		
		HANDLER_COUNT.incrementAndGet();
		String handlerId = s.getOutboundQualifier() + '/' + symbol;
		AutoMrSubscriptionHandler handler = new AutoMrSubscriptionHandler(
				s.getOutboundQualifier(),
				handlerId,
				mrEndpoint, 
				new MrSubscriptionProperties(symbol, handlerId, outboundRungs, OUTBOUND_TYPES__ALL), 
				lpool,
				fxCalendarBizService,
				sessionInfo,
				instrumentDetailProvider.getInstrumentDetail(symbol),
				algo);
		synchronized ( monitor ) {
			handlerIdMap.put(handlerId, handler);
		}
		return new SubscriptionHandlerWrapper(handler, lpool, requiredSub);
	}


	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void atPublish(long masGlobalSeq, IPublishingEndpoint publishingEndpoint) {
		long startTime = System.currentTimeMillis();
		List<IMrSubscriptionHandler> auto = null, onDemand = null;
		synchronized ( monitor ) {
			auto = new ArrayList<>( autoMrSubscriptionHandlers );
			onDemand = new ArrayList<>( onDemandMrSubscriptionHandlers );
		}
		for (IMrSubscriptionHandler mrHandler: auto ) {
			mrHandler.onPublish(masGlobalSeq);
		}
		
		for (IMrSubscriptionHandler mrHandler : onDemand ) {
			mrHandler.onPublish(masGlobalSeq);
		}
		long endTime = System.currentTimeMillis();
		long escaped = endTime - startTime;

		if ( escaped > 1000L || (masGlobalSeq % 100) == 0 ) {
			logger.debug("injection for " + masGlobalSeq + " took " + escaped + " " + auto.size() + " " + onDemand.size());
		}
	}

	public void removeSubscription(PriceSubscriptionRequest r) {
		IMrSubscriptionHandler h = onDemandMrSubscriptionHandlerMap.get(r.getTopic());
		if ( h != null ) {
			synchronized ( monitor ) {
				onDemandMrSubscriptionHandlers.remove(h);
				onDemandMrSubscriptionHandlerMap.remove(r.getTopic());
			}
		}
	}
	
	private static class SubscriptionHandlerWrapper {
		private final IMrSubscriptionHandler handler;
		private final ILiquidityPool liquidityPool;
		private final List<MdSubscriptionVo> requiredLpSubscription;
		
		SubscriptionHandlerWrapper(IMrSubscriptionHandler handler, ILiquidityPool liquidityPool,
				List<MdSubscriptionVo> requiredLpSubscription) {
			super();
			this.handler = handler;
			this.liquidityPool = liquidityPool;
			this.requiredLpSubscription = requiredLpSubscription;
		}

		public IMrSubscriptionHandler getHandler() {
			return handler;
		}

		public ILiquidityPool getLiquidityPool() {
			return liquidityPool;
		}

		public List<MdSubscriptionVo> getRequiredLpSubscription() {
			return requiredLpSubscription;
		}
		
		
	}

}
