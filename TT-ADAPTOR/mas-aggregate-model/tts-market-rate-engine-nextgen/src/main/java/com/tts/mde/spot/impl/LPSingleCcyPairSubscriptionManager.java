package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.provider.MDProviderStateManager;
import com.tts.mde.spot.IDirectLiquidityPool;
import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.vo.MdConditionVo;
import com.tts.mde.spot.vo.MdSubscriptionType;
import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IInstrumentDetailProvider;
import com.tts.mde.support.IPublishingEndpoint;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.support.impl.SessionInfoVo;
import com.tts.mde.vo.IMDProvider;
import com.tts.mde.vo.MarketDataProviderVo;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.mde.vo.RawLiquidityVo.LiquidityType;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest.RateRequestType;
import com.tts.message.eas.request.SubscriptionStruct.RateSource;
import com.tts.message.market.MarketStruct.MkBookType;
import com.tts.message.market.MarketStruct.RawLiquidityEntry;
import com.tts.message.market.MarketStruct.RawMarketBook;
import com.tts.message.util.TtMsgEncoder;
import com.tts.protocol.platform.IMsgSender;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.vo.TenorVo;

public class LPSingleCcyPairSubscriptionManager  {
	private final static SubscriptionComparator subscriptionSorter = new SubscriptionComparator();
	private final static Logger logger = LoggerFactory.getLogger(LPSingleCcyPairSubscriptionManager.class);
	private static final long TOLERANCE_TIME = 60 * ChronologyUtil.MILLIS_IN_SECOND;

	private final static boolean DISABLE_SUBSCRIPTION_RETRY;
	
	static {
		String disable_subscription_retry = System.getProperty("DISABLE_SUBSCRIPTION_RETRY");
		DISABLE_SUBSCRIPTION_RETRY = Boolean.parseBoolean(disable_subscription_retry);
	}
	
	private final int precision;
	private final List<IDirectLiquidityPool> monitoringPools = new CopyOnWriteArrayList<>();
	private final ConcurrentHashMap<String, ILiquidityPool> idPoolMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, IDirectLiquidityPool> reqIdPoolMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> lastSubscribeTimeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> reqIdLastCancelTimeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> subscriptionReqIdMap = new ConcurrentHashMap<>();
	private final Map<String, Integer> adapterSourceIdMap;
	private final String ccyPair;
	private final IMsgSender ctrlMsgSender;
	private final GlobalReqIdProvider globalReqIdProvider;
	private final MDProviderStateManager mdProviderStateManager;
	private final ISchedulingWorker schedulingWorker;
	private final boolean debug;
	
	public LPSingleCcyPairSubscriptionManager(
			String ccyPair, 
			IMsgSender ctrlMsgSender, 
			SessionInfoVo sessionInfo,
			MDProviderStateManager mdProviderStateManager, 
			IInstrumentDetailProvider instrumentDetailProvider, ISchedulingWorker schedulingWorker ) {
		
		boolean  injectionDebug = false;
		String injectionDebugStr = System.getProperty("injection.debug");
		if (injectionDebugStr != null && !injectionDebugStr.isEmpty()  ) {
			injectionDebug = Boolean.parseBoolean(injectionDebugStr);
		}		
		
		Map<String, Integer> _adapterSourceIdMap = new HashMap<String, Integer>();
		for ( IMDProvider p : sessionInfo.getMDprovidersExternal()) {
			_adapterSourceIdMap.put(p.getSourceNm(), ((MarketDataProviderVo) p).getInternalAssignedProviderId());
		}
		this.adapterSourceIdMap = Collections.unmodifiableMap(_adapterSourceIdMap);
		this.precision = instrumentDetailProvider.getInstrumentDetail(ccyPair).getPrecision();
		this.ccyPair = ccyPair;
		this.ctrlMsgSender = ctrlMsgSender;
		this.globalReqIdProvider = AppContext.getContext().getBean(GlobalReqIdProvider.class);
		this.mdProviderStateManager = mdProviderStateManager;
		this.schedulingWorker = schedulingWorker;
		this.debug = injectionDebug && "EURUSD".equals(ccyPair);
	}

	public ILiquidityPool getLiquidityPool(List<MdSubscriptionVo> subs, boolean shortTermSubscription) {
		Collections.sort(subs, subscriptionSorter);
		ILiquidityPool rte = null;
		int numberOfSubscriptions = subs.size();
		logger.debug("working on getting liquidity pool for " + numberOfSubscriptions + " " + ccyPair + " subscriptions");
		
		String subscriptionIds = getSubscriptionsIdentity(subs);
		
		if ( shortTermSubscription) {
			SingleSrcLiquidityPool	newPool = new SingleSrcLiquidityPool(subs.get(0));
			String reqId = globalReqIdProvider.getReqId(subs);
			reqIdPoolMap.put(reqId, newPool);
			doSendSubscription(subs.get(0).getAdapter(), subs, reqId);

			return newPool;
		}
		
		rte = idPoolMap.get(subscriptionIds);
		
		if ( rte != null ) {
			logger.debug("found matching pool for subscription(s), " + subscriptionIds + ", ... DONE");
			return rte;
		} else {
			logger.debug("matching pool for subscription(s), " + subscriptionIds + " cannot be found.. composing one");

			if ( numberOfSubscriptions > 0 ) {
				List<ILiquidityPool> relatedLiquidityPools = new ArrayList<>(numberOfSubscriptions);
				List<IDirectLiquidityPool> newPools = new ArrayList<>(numberOfSubscriptions);

				HashMap<String, List<MdSubscriptionVo>> requiredNewSub = new HashMap<String, List<MdSubscriptionVo>>(numberOfSubscriptions);
				for ( MdSubscriptionVo sub : subs ) {
					String singleSubscriptionId = getSubscriptionsIdentity(sub);
					ILiquidityPool p = idPoolMap.get(singleSubscriptionId);
					if ( p != null ) {
						logger.debug("found matching pool for single subscription, " + singleSubscriptionId);
						relatedLiquidityPools.add(p);
					} else {
						logger.debug("no matching pool for subscription, " + singleSubscriptionId );

						String adapter = sub.getAdapter();
						List<MdSubscriptionVo> adapterSub = requiredNewSub.get(adapter);
						if ( adapterSub == null) {
							adapterSub = new ArrayList<MdSubscriptionVo>(numberOfSubscriptions);
							requiredNewSub.put(adapter, adapterSub);
						}
						adapterSub.add(sub);
					}
				}	
				for ( Entry<String, List<MdSubscriptionVo>> e : requiredNewSub.entrySet()) {
					List<MdSubscriptionVo> newSubs = e.getValue();
					
					for ( MdSubscriptionVo sub : newSubs) {
//						String reqId = doSendSubscription(e.getKey(), Arrays.asList(new MdSubscriptionVo[] { sub} ));
//
//						String singleSubscriptionId = getSubscriptionsIdentity(sub);
//						SingleSrcLiquidityPool  singleSrcPool = new SingleSrcLiquidityPool(sub);
//						reqIdPoolMap.put(reqId, singleSrcPool);
//						idPoolMap.put(singleSubscriptionId, singleSrcPool);
//						newPools.add(singleSrcPool);
//						lastSubscribeTimeMap.put(sub.getIdentifer(), System.currentTimeMillis());
//						subscriptionReqIdMap.put(sub.getIdentifer(), reqId);
//						relatedLiquidityPools.add(singleSrcPool);
						
						if (!shortTermSubscription) {
							SelfManagedLiquidityPool singleSrcPool = new SelfManagedLiquidityPool(sub, this, mdProviderStateManager);
							String singleSubscriptionId = getSubscriptionsIdentity(sub);
							idPoolMap.put(singleSubscriptionId, singleSrcPool);
							newPools.add(singleSrcPool);
							relatedLiquidityPools.add(singleSrcPool);
						}
					}
					
				}
				if ( !shortTermSubscription) {
					monitoringPools.addAll(newPools);
				}
				
				if ( relatedLiquidityPools.size() == 1) {
					rte = relatedLiquidityPools.get(0);
				} else {
					rte = new MultiSrcLiquidityPool(relatedLiquidityPools);
					idPoolMap.put(subscriptionIds, rte);
					lastSubscribeTimeMap.put(subscriptionIds, System.currentTimeMillis());
				}
			}
		}
		logger.debug("working on getting liquidity pool for " + numberOfSubscriptions + " " + ccyPair + " subscriptions ... DONE");

		return rte;
	}

	private String doSendSubscription(String adapter, List<MdSubscriptionVo> subs, String _reqId) {
	
		String reqId = _reqId;
		
		if ( reqId == null ) {
			reqId = globalReqIdProvider.getReqId(subs);
		}

		PriceSubscriptionRequest.Builder subReqB = PriceSubscriptionRequest.newBuilder();
		subReqB.getQuoteParamBuilder().setCurrencyPair(ccyPair);
		subReqB.setRequestId(reqId);
		String replyTopic = "TTS.MD.FX.FA.SPOT." + ccyPair + "." + adapter.toUpperCase();
		subReqB.setTopic(replyTopic.toUpperCase());
		
		if ( subs.get(0).getMdSubscriptionType() == MdSubscriptionType.RFS) {
			String spDate = AppContext.getContext().getBean(IFxCalendarBizServiceApi.class).getForwardValueDate(ccyPair, TenorVo.NOTATION_SPOT);
			subReqB.setRateRequestType( RateRequestType.RFS );
			subReqB.getQuoteParamBuilder().setSize(Double.toString(subs.get(0).getQuantities1()[0]));
			subReqB.getQuoteParamBuilder().getNearDateDetailBuilder().setPeriodCd(TenorVo.NOTATION_SPOT);
			subReqB.getQuoteParamBuilder().getNearDateDetailBuilder().setPeriodValue("-1");
			subReqB.getQuoteParamBuilder().getNearDateDetailBuilder().setActualDate(spDate);
			subReqB.getQuoteParamBuilder().setNotionalCurrency(ccyPair.substring(0, 3));
		}
		
		StringBuilder sources = new StringBuilder();
		
		for ( MdSubscriptionVo sub : subs) {
			subReqB.getRateSourceBuilder().addSpecificSourceNm(sub.getSource());
			sources.append(sub.getSource()).append(' ');
		}
		PriceSubscriptionRequest subReq = subReqB.build();
		TtMsg ttMsg = TtMsgEncoder.encode(subReq);
		logger.info(String.format("Subscribing %s from %s.%s, requestId=%s", ccyPair, adapter, sources.toString(),
				reqId));

		String outboundTopic = "TTS.CTRL.EVENT.REQUEST.FA.SUBSCRIBE." + adapter + "." + ccyPair;
		
		schedulingWorker.scheduleAtFixedDelay(new Runnable() {

			@Override
			public void run() {
				ctrlMsgSender.send(outboundTopic.toUpperCase(), ttMsg);
			}
			
		}, ChronologyUtil.MILLIS_IN_SECOND);
		

		return reqId;
	}
	
	private synchronized void doUnsubscribeSubscription(String adapter, String source, String requestId) {
		Long lastCancelTime = reqIdLastCancelTimeMap.get(requestId);
		
		if ( lastCancelTime != null && (System.currentTimeMillis() - lastCancelTime) < 10000 ) {
			return;
		}
		PriceSubscriptionRequest.Builder subReqB = PriceSubscriptionRequest.newBuilder();
		subReqB.getQuoteParamBuilder().setCurrencyPair(this.ccyPair);
		subReqB.setRequestId(requestId);
		
		if ( source != null && !source.equals(adapter) ) {
			RateSource.Builder rsB = RateSource.newBuilder();
			rsB.addSpecificSourceNm(source);
			subReqB.setRateSource(rsB);
		}

		PriceSubscriptionRequest subReq = subReqB.build();
		TtMsg ttMsg = TtMsgEncoder.encode(subReq);
		String adapterSourceIdentifer = adapter + "." + source;

		logger.info(String.format("Unsubscribing %s from %s, requestId=%s", this.ccyPair, adapterSourceIdentifer, requestId));

		String outboundTopic = "TTS.CTRL.EVENT.REQUEST.FA.UNSUBSCRIBE." + adapter.toUpperCase() + "."
				+ this.ccyPair;
		ctrlMsgSender.send(outboundTopic.toUpperCase(), ttMsg);
		reqIdLastCancelTimeMap.put(requestId, System.currentTimeMillis());
	}
	
	private String getSubscriptionsIdentity(List<MdSubscriptionVo> subs) {
		StringBuilder sb = new StringBuilder();
		for (MdSubscriptionVo sub: subs ) {
			sb.append(getSubscriptionsIdentity(sub));
		}
		return sb.toString();
	}
	
	private String getSubscriptionsIdentity(MdSubscriptionVo sub) {
		return sub.getIdentifer();
	}
	
	private static class SubscriptionComparator implements Comparator<MdSubscriptionVo> {
                                                                                                                                                                                                                                                                                                                           
		                                                                                                                                                                                                                                                                                                                             
		@Override
		public int compare(MdSubscriptionVo o1, MdSubscriptionVo o2) {
			return o1.getIdentifer().compareTo(o2.getIdentifer());
		}
		
	}
	
	public void onMarketData(RawMarketBook mb) {
		IDirectLiquidityPool p = reqIdPoolMap.get(mb.getRequestId())	;	
		if ( p != null ) {
			RawLiquidityVo[] bids = transform(mb.getBidQuoteList(), mb, true);
			RawLiquidityVo[] asks = transform(mb.getAskQuoteList(), mb, false);
	
			p.replaceAllQuotes(bids, asks);
		} else {
			logger.debug(" unknown reqId, " + mb.getRequestId()  );
			doUnsubscribeSubscription(mb.getAdapter(), null, mb.getRequestId());
		}
	}

	private RawLiquidityVo[] transform(List<RawLiquidityEntry> quoteList, RawMarketBook mb, boolean isBid) {
		RoundingMode rm = isBid ? RoundingMode.FLOOR : RoundingMode.CEILING;
		RawLiquidityVo[] array = new RawLiquidityVo[quoteList.size()];
		for ( int i = 0 ; i < quoteList.size() ; i++ ) {
			RawLiquidityEntry q = quoteList.get(i);
			String src = q.getSource();
			if ( src == null || src.isEmpty()) {
				src = mb.getAdapter();
			}
			int adapterSourceId = this.adapterSourceIdMap.get(src);

			String quoteRefId = null;
			LiquidityType liquidityType = null;
			if ( mb.getMkBookType() == MkBookType.LADDER) {
				liquidityType = LiquidityType.LADDER;
			} else if ( mb.getMkBookType() == MkBookType.LADDER_WITH_MULTI_HIT_ALLOW) {
				liquidityType = LiquidityType.LADDER_WITH_MULTIHIT_ALLOWED;
			} else if ( mb.getMkBookType() == MkBookType.RAW) {
				liquidityType = LiquidityType.RAW_QUOTE;
			}
			
			double rate = q.getRate();
			double fwdPts = 0.0d;
			double spotRate = rate;
			if ( q.hasAdditionalInfo()) {
				spotRate = q.getAdditionalInfo().getSpotRate();
				fwdPts = q.getAdditionalInfo().getFwdPts();
				array[i] = new RawLiquidityVo(
						q.getSize(), 
						mb.getUpdateTimeStamp(), 
						DoubleFormatter.roundDouble(rate, precision, rm),
						DoubleFormatter.roundDouble(fwdPts, precision, rm), 
						DoubleFormatter.roundDouble(spotRate, precision, rm), 
						q.getQuoteId(),
						src, 
						mb.getAdapter(), 
						quoteRefId, 
						adapterSourceId, 
						liquidityType);
			} else {
				array[i] = new RawLiquidityVo(
						q.getSize(), 
						mb.getUpdateTimeStamp(), 
						DoubleFormatter.roundDouble(rate, precision, rm),
						fwdPts, 
						spotRate, 
						q.getQuoteId(),
						src, 
						mb.getAdapter(), 
						quoteRefId, 
						adapterSourceId, 
						liquidityType);
			}
		}
		return array;
	}

	public void atPublish(long masGlobalSeq, IPublishingEndpoint publishingEndpoint) {
		validate();
	}
	
	private void validate() {
		for ( IDirectLiquidityPool pool : monitoringPools) {
			MdConditionVo condition = pool.validate();
//			if ( condition.getValidAskDataCount() == 0 ||  condition.getValidBidDataCount() == 0 || condition.getLastReceivedTime() == -1L ) {
//				String singleSubscriptionId = getSubscriptionsIdentity(pool.getSubscription());
//				String existingReqId = subscriptionReqIdMap.get(singleSubscriptionId);
//				long lastSubscribeTime = lastSubscribeTimeMap.get(singleSubscriptionId);
//				
//				if ( !DISABLE_SUBSCRIPTION_RETRY && (System.currentTimeMillis() - lastSubscribeTime) > TOLERANCE_TIME) {
//					if ( existingReqId != null ) {
//						doUnsubscribeSubscription(pool.getSubscription().getAdapter(), pool.getSubscription().getSource(), existingReqId);
//						reqIdPoolMap.remove(existingReqId);
//					}
//					
//					String newReqId = doSendSubscription(pool.getSubscription().getAdapter(), Arrays.asList(new MdSubscriptionVo[] { pool.getSubscription() }) );
//					lastSubscribeTimeMap.put(singleSubscriptionId, System.currentTimeMillis());
//					reqIdPoolMap.put(newReqId, pool);
//					subscriptionReqIdMap.put(singleSubscriptionId, newReqId);
//
//				}
//			}		
		}
		if ( debug ) {
			logger.debug("validated " + monitoringPools.size() + " pools for " + ccyPair);
		}
	}
	
	public void setSrcEnabled(String srcName, boolean isEnable) {
		for ( IDirectLiquidityPool pool: monitoringPools ) {
			if ( pool.getSubscription().getSource().equalsIgnoreCase(srcName)) {
				pool.setIsSubspendForAggregation(!isEnable);
			}
		}
	}

	public void destroy() {
		ArrayList<IDirectLiquidityPool> _monitoringPools = new ArrayList<>(this.monitoringPools);
		this.monitoringPools.clear();
		
		for ( IDirectLiquidityPool pool: _monitoringPools ) {
			if ( pool instanceof SelfManagedLiquidityPool ) {
				((SelfManagedLiquidityPool) pool).destory();
			}
			MdSubscriptionVo subscription = pool.getSubscription();
			doUnsubscribeSubscription(subscription.getAdapter(), subscription.getSource(), subscriptionReqIdMap.get( getSubscriptionsIdentity(subscription)));
		}	
		
	}

	public void doSendSubscriptionAndRegister(String adapterNm, List<MdSubscriptionVo> asList, SingleSrcLiquidityPool singleSrcPool) {
		String singleSubscriptionId = getSubscriptionsIdentity(asList.get(0));
		Long lastSubscribeTime = lastSubscribeTimeMap.get(singleSubscriptionId);
		
		if ( lastSubscribeTime != null && (System.currentTimeMillis() - lastSubscribeTime) < 5000 ) {
			return;
		}
		String reqId = globalReqIdProvider.getReqId(asList);
		reqIdPoolMap.put(reqId, singleSrcPool);
		doSendSubscription(adapterNm, asList, reqId);
		lastSubscribeTimeMap.put(singleSubscriptionId, System.currentTimeMillis());
		subscriptionReqIdMap.put(singleSubscriptionId, reqId);	
	}
}
