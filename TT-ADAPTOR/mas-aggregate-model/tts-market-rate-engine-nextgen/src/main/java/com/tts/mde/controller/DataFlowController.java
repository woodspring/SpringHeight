package com.tts.mde.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.entity.session.TradingSession;
import com.tts.mde.ems.ExecutionManagementService;
import com.tts.mde.plugin.EmbeddedAdapterManager;
import com.tts.mde.provider.MDProviderStateManager;
import com.tts.mde.provider.SessionInfoProvider;
import com.tts.mde.service.IMDEmbeddedAdapter;
import com.tts.mde.spot.impl.LPSubscriptionManager;
import com.tts.mde.spot.impl.MrSubscriptionHandlerManager;
import com.tts.mde.spot.impl.SpotMarketDataReceiver;
import com.tts.mde.support.ICertifiedPublishingEndpoint;
import com.tts.mde.support.IFxCalendarBizServiceApi;
import com.tts.mde.support.IInstrumentDetailProvider;
import com.tts.mde.support.IMarketDataHandler;
import com.tts.mde.support.config.Adapter;
import com.tts.mde.support.config.Adapter.SourceConfig;
import com.tts.mde.support.config.Adapters;
import com.tts.mde.support.config.FwdPtsMarketDataStreamSet;
import com.tts.mde.support.config.FwdPtsMarketDataStreamSet.FwdPtsMarketDataStream;
import com.tts.mde.support.config.LPProduct;
import com.tts.mde.support.config.LPProducts;
import com.tts.mde.support.config.MDSubscription;
import com.tts.mde.support.config.MDSubscriptions;
import com.tts.mde.support.config.MarketDataSet;
import com.tts.mde.support.config.MarketDataSetConfig;
import com.tts.mde.support.config.MarketDataSetSchedule;
import com.tts.mde.support.config.MarketDataType;
import com.tts.mde.support.config.MarketDataTypes;
import com.tts.mde.support.config.SpotMarketDataStreamSet;
import com.tts.mde.support.config.SpotMarketDataStreamSet.SpotMarketDataStream;
import com.tts.mde.support.config.SubscriptionType;
import com.tts.mde.support.config.SubscriptionTypes;
import com.tts.mde.support.impl.InjectionWorker;
import com.tts.mde.support.impl.MdeSchedulingWorkerImpl;
import com.tts.mde.support.impl.SessionInfoVo;
import com.tts.mde.vo.IMDProvider;
import com.tts.mde.vo.LiquidityProviderVo;
import com.tts.mde.vo.MarketDataProviderVo;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.CommonStruct.LocaleInfo;
import com.tts.message.common.CommonStruct.UILabel;
import com.tts.message.common.service.ITradingSessionAware;
import com.tts.message.config.AggConfigStruct.AggInstrument;
import com.tts.message.config.AggConfigStruct.AggInstrumentConfigResponse;
import com.tts.message.config.AggConfigStruct.AggSourceChangeRequest;
import com.tts.message.config.AggConfigStruct.AggSourceStatus;
import com.tts.message.config.AggConfigStruct.OrdCapability;
import com.tts.message.config.AggConfigStruct.SourceAndCapability;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.market.MarketMarkerStruct.MMAvailableInstrumentRequest;
import com.tts.message.market.MarketMarkerStruct.MMAvailableInstrumentResponse;
import com.tts.message.system.RolloverStruct.RolloverNotification;
import com.tts.message.system.SystemStatusStruct.ChangeTradingSession;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.message.util.TtMsgEncoder;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.protocol.platform.event.IEventMessageTypeConstant.Data;
import com.tts.protocol.platform.event.IEventMessageTypeConstant.Transactional;
import com.tts.service.db.ICustomerProfileService;
import com.tts.service.db.TradingSessionManager;
import com.tts.util.AppContext;
import com.tts.util.exception.InvalidServiceException;
import com.tts.util.flag.IndicativeFlag;
import com.tts.vo.CounterPartyVo;

public class DataFlowController implements ITradingSessionAware, IMsgListener {
	private static final String EXEC_REPORT_INCOMING_TOPIC = String.format(Transactional.TRAN_INFO_TEMPLATE, "FA",
			Data.RateType.MARKET);

	private static final String EXEC_REPORT_INBOUND_TOPIC = EXEC_REPORT_INCOMING_TOPIC;

	private static final Logger logger = LoggerFactory.getLogger(DataFlowController.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

	private static final String AGG_CUST_NM = System.getenv("AGG_CUST_NM");
	private static final String AGG_ACCT_NM = System.getenv("AGG_ACCT_NM");

	private static final String[] CTRL_TOPICS_INTERESTED = new String[] {
			IEventMessageTypeConstant.REM.CE_TRADING_SESSION, IEventMessageTypeConstant.System.FX_ROLLOVER_EVENT,
			IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_AVAILABLE_INSTRUMENT_REQUEST,
			IEventMessageTypeConstant.Agg.ALL_REQUEST_TO_AGG, IEventMessageTypeConstant.Agg.NEW_ORDER_FROM_BANK_TRADER,
			EXEC_REPORT_INCOMING_TOPIC };

	private final EmbeddedAdapterManager embeddedAdapterManager = new EmbeddedAdapterManager();
	private final ICertifiedPublishingEndpoint certifiedPublishingEndpoint;
	private final SessionInfoProvider sessionInfoProvider;
	private final ArrayList<IMsgReceiver> ctrlMsgReceivers = new ArrayList<>();
	private final IMsgSender ctrlMsgSender;
	private final IInstrumentDetailProvider instrumentDetailProvider;
	private final InjectionWorker injectionWorker = new InjectionWorker();
	private final IFxCalendarBizServiceApi fxCalendarBizService;
	private final ScheduledExecutorService executorService;
	private final MdeSchedulingWorkerImpl scheduleWorker;
	private final SessionInfoVo sessionInfo = new SessionInfoVo();
	private final MDProviderStateManager mdProviderStateManager;

	private volatile IMsgReceiver hedgingRequestReceiver = null;
	private volatile List<IMarketDataHandler> fwdMdHandlers = null;
	private volatile List<IMDEmbeddedAdapter> activatedEmbeddedAdapters;

	private volatile MrSubscriptionHandlerManager mrSubscriptionHandlerManager = null;
	private volatile LPSubscriptionManager lpSubscriptionManager = null;
	private final ExecutionManagementService emsService;

	private final SpotMarketDataReceiver spotMarketDataReceiver;


	public DataFlowController() {
		SessionInfoProvider sessionInfoProvider = AppContext.getContext().getBean(SessionInfoProvider.class);
		IFxCalendarBizServiceApi fxCalendarBizServiceApi = AppContext.getContext()
				.getBean(IFxCalendarBizServiceApi.class);
		IMsgSenderFactory msgSenderFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		IInstrumentDetailProvider instrumentDetailProvider = AppContext.getContext()
				.getBean(IInstrumentDetailProvider.class);

		IMsgSender ctrlMsgSender = msgSenderFactory.getMsgSender(false, false, false);
		ctrlMsgSender.init();

		this.executorService = Executors.newScheduledThreadPool(10);
		this.scheduleWorker = new MdeSchedulingWorkerImpl(this.executorService);
		this.instrumentDetailProvider = instrumentDetailProvider;
		this.fxCalendarBizService = fxCalendarBizServiceApi;
		this.ctrlMsgSender = ctrlMsgSender;
		this.sessionInfoProvider = sessionInfoProvider;
		this.certifiedPublishingEndpoint = AppContext.getContext().getBean(ICertifiedPublishingEndpoint.class);
		this.emsService = new ExecutionManagementService(fxCalendarBizService, sessionInfo, instrumentDetailProvider);
		this.mdProviderStateManager = AppContext.getContext().getBean(MDProviderStateManager.class);
		this.spotMarketDataReceiver = new SpotMarketDataReceiver();
	}

	@Override
	public void switchTradingSession(String newTradingSessionName) throws InvalidServiceException {
		this.sessionInfo.setGlobalIndicativeFlag(IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE,
				IndicativeFlag.IndicativeReason.MA_MarketRateTradingSessionChange));
		if (sessionInfo != null) {
			cleanup();
		}

		try {
			Thread.sleep(4000L);
		} catch (InterruptedException e) {

		}

		TradingSession tradingSession = TradingSessionManager.getInstance().getSessionByName(newTradingSessionName);
		sessionInfo.setTradingSessionId(tradingSession.getPk());
		MarketDataSetConfig config = sessionInfoProvider.getOrReloadMarketDataSetConfig();

		if (config == null) {
			return;
		}
		MarketDataSet activeSet = findActiveMarketDataSet(config, tradingSession.getName());
		loadMDproviderExternal(config);
		List<IMDEmbeddedAdapter> activatedEmbeddedAdapters = new ArrayList<>();
		FwdPtsMarketDataStreamSet fwdcMdConfig = activeSet.getFwdPtsMarketDataStreamSet();
		List<IMarketDataHandler> _fwdMdHandlers = new ArrayList<>();

		if (fwdcMdConfig != null) {
			HashMap<String, List<String>> subscriptionMapping = new HashMap<>();
			for (FwdPtsMarketDataStream cfg : fwdcMdConfig.getFwdPtsMarketDataStream()) {
				MDSubscriptions subscriptions = cfg.getSubscriptions();
				for (MDSubscription subscription : subscriptions.getSubscription()) {
					String sourceNm = subscription.getSourceNm();

					List<String> sourceSubscriptions = subscriptionMapping.get(sourceNm);
					if (sourceSubscriptions == null) {
						sourceSubscriptions = new ArrayList<>();
						subscriptionMapping.put(sourceNm, sourceSubscriptions);
					}
					sourceSubscriptions.add(cfg.getCurrencyPair());
				}
			}

			Set<String> embeddedAdapterToBeActivated = new HashSet<>();
			embeddedAdapterManager.setRunnableWorker(scheduleWorker); //**TODO: review scope
			embeddedAdapterManager.setSessionInfo(sessionInfo);//**TODO: review scope
			for (Entry<String, List<String>> sourceSub : subscriptionMapping.entrySet()) {
				MarketDataProviderVo mdProvider = (MarketDataProviderVo) sessionInfo
						.getMDproviderBySourceNm(sourceSub.getKey());
				if (mdProvider.isEmbedded()) {
					embeddedAdapterToBeActivated.add(mdProvider.getAdapterNm());
					IMDEmbeddedAdapter adapter = embeddedAdapterManager.getMDEmbeddedAdapter(mdProvider.getAdapterNm());
					IMarketDataHandler handler = adapter.addSubscriptions(sourceSub.getKey(), sourceSub.getValue());
					adapter.init(); //**TODO: store reference
					activatedEmbeddedAdapters.add(adapter);
					_fwdMdHandlers.add(handler);
				}
			}

		}

		AggInstrumentConfigResponse.Builder configRespBuilder = AggInstrumentConfigResponse.newBuilder();
		configRespBuilder.setTradingSessionNm(newTradingSessionName);

		SpotMarketDataStreamSet spotSet = activeSet.getSpotMarketDataStreamSet();
		List<SpotMarketDataStream> streams = spotSet.getSpotMarketDataStream();
		Map<String, Map<String, List<String>>> compositeSrcMap = new HashMap<>();
		Set<String> spotCcyPairs = new HashSet<>(streams.size());
		Set<String> uniqueQualifiedStreamNms = new HashSet<>();
		for (SpotMarketDataStream s : streams) {
			String currencyPair = s.getCurrencyPair();
			spotCcyPairs.add(currencyPair);
			Map<String, List<String>> ccyPairComposite = compositeSrcMap.get(currencyPair);
			if (ccyPairComposite == null) {
				ccyPairComposite = new HashMap<>();
				compositeSrcMap.put(currencyPair, ccyPairComposite);
			}
			List<String> sources = new ArrayList<>();
			for (MDSubscription sub : s.getSubscriptions().getSubscription()) {
				sources.add(sub.getSourceNm());
			}
			ccyPairComposite.put(s.getOutboundQualifier(), sources);
			uniqueQualifiedStreamNms.add(s.getOutboundQualifier());
		}
		for (String ccyPair : spotCcyPairs) {
			AggInstrument.Builder configInstrument = AggInstrument.newBuilder();
			configInstrument.setInternalLiquidityEnabled(false);
			configInstrument.setSymbol(ccyPair);
			configRespBuilder.addInstrument(configInstrument);
		}

		loadInternalTradingParties(uniqueQualifiedStreamNms);
		for (IMDProvider provider : sessionInfo.getMDprovidersExternal()) {
			if (provider instanceof LiquidityProviderVo) {

				LiquidityProviderVo lp = (LiquidityProviderVo) provider;
				String src = lp.getSourceNm();
				if ("CIBC".equals(src) || "JPMC".equals(src) || "BAML".equals(src)) {
					SourceAndCapability.Builder sc = SourceAndCapability.newBuilder();
					sc.setSourceNm(src);
					sc.setAccountId("" + lp.getFxSPOTCounterParty().getAccountId());
					sc.setCounterPartyId("" + lp.getFxSPOTCounterParty().getCustomerId());
					sc.setAccountNm(lp.getFxSPOTCounterParty().getAccountNm());
					sc.setCounterPartyNm(lp.getFxSPOTCounterParty().getCustomerNm());
					OrdCapability.Builder oc = OrdCapability.newBuilder();
					oc.setAlgoNm("RAW");
					UILabel.Builder uiRAW = UILabel.newBuilder();
					uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("RAW"));
					oc.addOrdType("FOK");
					oc.setAlgoNmUiLabel(uiRAW);
					sc.addOrderingCapability(oc);
					configRespBuilder.addSourceAndCapability(sc);
				} else if ("UBS".equals(src) || "CITI".equals(src)) {
					SourceAndCapability.Builder sc = SourceAndCapability.newBuilder();
					sc.setSourceNm(src);
					sc.setAccountId("" + lp.getFxSPOTCounterParty().getAccountId());
					sc.setCounterPartyId("" + lp.getFxSPOTCounterParty().getCustomerId());
					sc.setAccountNm(lp.getFxSPOTCounterParty().getAccountNm());
					sc.setCounterPartyNm(lp.getFxSPOTCounterParty().getCustomerNm());
					OrdCapability.Builder ocRaw = OrdCapability.newBuilder();
					ocRaw.setAlgoNm("RAW");
					UILabel.Builder uiRAW = UILabel.newBuilder();
					uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("RAW"));
					ocRaw.addOrdType("FOK");
					ocRaw.addOrdType("IOC");
					ocRaw.setAlgoNmUiLabel(uiRAW);
					sc.addOrderingCapability(ocRaw);
					ocRaw.setAlgoNmUiLabel(uiRAW);
					OrdCapability.Builder ocVWAPIOC = OrdCapability.newBuilder();
					ocVWAPIOC.setAlgoNm("VWAP");
					UILabel.Builder uiVWAP = UILabel.newBuilder();
					uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("VWAP"));
					ocVWAPIOC.addOrdType("IOC");
					ocVWAPIOC.setAlgoNmUiLabel(uiVWAP);
					sc.addOrderingCapability(ocVWAPIOC);
					configRespBuilder.addSourceAndCapability(sc);
				} else if ("MS".equals(src) || "GS".equals(src)) {
					SourceAndCapability.Builder sc = SourceAndCapability.newBuilder();
					sc.setSourceNm(src);
					sc.setAccountId("" + lp.getFxSPOTCounterParty().getAccountId());
					sc.setCounterPartyId("" + lp.getFxSPOTCounterParty().getCustomerId());
					sc.setAccountNm(lp.getFxSPOTCounterParty().getAccountNm());
					sc.setCounterPartyNm(lp.getFxSPOTCounterParty().getCustomerNm());
					OrdCapability.Builder ocRaw = OrdCapability.newBuilder();
					ocRaw.setAlgoNm("RAW");
					UILabel.Builder uiRAW = UILabel.newBuilder();
					uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("RAW"));
					ocRaw.addOrdType("FOK");
					ocRaw.addOrdType("IOC");
					ocRaw.setAlgoNmUiLabel(uiRAW);
					sc.addOrderingCapability(ocRaw);

					OrdCapability.Builder ocVWAP = OrdCapability.newBuilder();
					ocVWAP.setAlgoNm("VWAP");
					UILabel.Builder uiVWAP = UILabel.newBuilder();
					uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("VWAP"));
					ocVWAP.addOrdType("IOC");
					ocVWAP.addOrdType("MKT");
					ocVWAP.setAlgoNm("VWAP");
					ocVWAP.setAlgoNmUiLabel(uiVWAP);
					sc.addOrderingCapability(ocVWAP);

					configRespBuilder.addSourceAndCapability(sc);
				}
			}
		}
		for (LiquidityProviderVo internalParty : sessionInfo.getInternalTradingParties()) {
			String src = internalParty.getSourceNm();
			if (src.contains("POOL") || src.startsWith("AGG")) {
				SourceAndCapability.Builder sc = SourceAndCapability.newBuilder();
				sc.setSourceNm(src);
				sc.setAccountId("" + internalParty.getFxSPOTCounterParty().getAccountId());
				sc.setCounterPartyId("" + internalParty.getFxSPOTCounterParty().getCustomerId());
				sc.setAccountNm(internalParty.getFxSPOTCounterParty().getAccountNm());
				sc.setCounterPartyNm(internalParty.getFxSPOTCounterParty().getCustomerNm());
				OrdCapability.Builder ocRaw = OrdCapability.newBuilder();
				ocRaw.setAlgoNm("RAW");
				UILabel.Builder uiRAW = UILabel.newBuilder();
				uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("RAW"));
				ocRaw.addOrdType("FOK");
				// ocRaw.addOrdType("IOC");
				ocRaw.setAlgoNmUiLabel(uiRAW);
				sc.addOrderingCapability(ocRaw);
				OrdCapability.Builder ocConsolidated = OrdCapability.newBuilder();
				ocConsolidated.setAlgoNm("CONSOLIDATED");
				UILabel.Builder uiConsolidated = UILabel.newBuilder();
				uiConsolidated.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("CONSOLIDATED"));
				ocConsolidated.addOrdType("IOC");
				// ocRaw.addOrdType("IOC");
				ocConsolidated.setAlgoNmUiLabel(uiConsolidated);
				sc.addOrderingCapability(ocConsolidated);
				OrdCapability.Builder ocVWAPIOC = OrdCapability.newBuilder();
				ocVWAPIOC.setAlgoNm("VWAP");
				UILabel.Builder uiVWAP = UILabel.newBuilder();
				uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("VWAP"));
				ocVWAPIOC.addOrdType("MKT");
				ocVWAPIOC.addOrdType("IOC");
				ocVWAPIOC.setAlgoNmUiLabel(uiVWAP);
				sc.addOrderingCapability(ocVWAPIOC);
				// OrdCapability.Builder ocVWAPplus =
				// OrdCapability.newBuilder();
				// ocVWAPplus.setAlgoNm("VWAP+");
				// ocVWAPplus.addOrdType("IOC");
				// UILabel.Builder uiVWAPplus = UILabel.newBuilder();
				// uiRAW.addLocaleInfo(LocaleInfo.newBuilder().setDisplayName("en").setLocale("VWAP+"));
				// ocVWAPplus.setAlgoNmUiLabel(uiVWAPplus);
				// sc.addOrderingCapability(ocVWAPplus);
				configRespBuilder.addSourceAndCapability(sc);
			}
		}
		this.sessionInfo.setAggInstrumentConfigResponse(configRespBuilder);
		List<String> ccyPairList = Collections.unmodifiableList(new ArrayList<String>(spotCcyPairs));
		LPSubscriptionManager lpSubscriptionManager = new LPSubscriptionManager(ccyPairList, ctrlMsgSender, sessionInfo,
				instrumentDetailProvider, scheduleWorker);
		MrSubscriptionHandlerManager mrSubscriptionHandlerManager = new MrSubscriptionHandlerManager(
				lpSubscriptionManager, fxCalendarBizService, sessionInfo, instrumentDetailProvider, streams, null);
		this.spotMarketDataReceiver.setSpotDataListener(lpSubscriptionManager);

		ArrayList<IMarketDataHandler> marketDataHandlers = new ArrayList<IMarketDataHandler>();
		marketDataHandlers.addAll(_fwdMdHandlers);
		marketDataHandlers.add(lpSubscriptionManager);
		marketDataHandlers.add(mrSubscriptionHandlerManager);

		this.injectionWorker.setMarketDataHandlers(marketDataHandlers);
		this.fwdMdHandlers = Collections.unmodifiableList(_fwdMdHandlers);
		this.mrSubscriptionHandlerManager = mrSubscriptionHandlerManager;
		this.lpSubscriptionManager = lpSubscriptionManager;
		this.activatedEmbeddedAdapters = activatedEmbeddedAdapters;
		System.out.println(TextFormat.printToString(sessionInfo.getAggInstrumentConfigResponse()));

		mdProviderStateManager.setAdapterConfig(config);
		this.sessionInfo.setGlobalIndicativeFlag(IndicativeFlag.TRADABLE);

	}

	/**
	 * Create Liquidity Pool / Internal Trading Parties
	 */
	public void loadInternalTradingParties(Set<String> uniqueQualifiedStreamNms) {

		CounterPartyVo internalAgg = getInternalAggParty();
		List<LiquidityProviderVo> internalTradingRepParties = new ArrayList<>();
		for (String uniqueQualifiedStreamNm : uniqueQualifiedStreamNms) {
			LiquidityProviderVo internalTradeRepLp = new LiquidityProviderVo(-1, null, uniqueQualifiedStreamNm, false,
					false, true);
			internalTradeRepLp.setFxSPOTCounterParty(internalAgg);
			internalTradingRepParties.add(internalTradeRepLp);
		}
		sessionInfo.setInternalTradingRepParties(internalTradingRepParties);
	}

	private void loadMDproviderExternal(MarketDataSetConfig config) {
		ICustomerProfileService customerProfileService = (ICustomerProfileService) AppContext.getContext()
				.getBean("customerProfileService");
		HashMap<String, CounterPartyVo> localCacheCounterParty = new HashMap<>();
		List<IMDProvider> mdProviders = new ArrayList<>();
		int adapterSourceCount = 1;
		Adapters adapters = config.getAdapters();
		for (Adapter adapter : adapters.getAdapter()) {
			String adapterNm = adapter.getAdapterNm();
			for (SourceConfig sc : adapter.getSourceConfig()) {
				IMDProvider mdprovider = null;
				String srcNm = sc.getSourceNm();
				LPProducts ps = sc.getProducts();
				MarketDataTypes mdts = sc.getMarketDataTypes();
				boolean spotData = false, forwardPointsData, interestRateData = false, volatilityData = false;
				boolean spotContract = false, outrightContract = false, swapContract, 
						timeOptionContract = false,
						ndfContract = false;

				for (MarketDataType t : mdts.getMarketDataType()) {
					if (t == MarketDataType.SPOT) {
						spotData = true;
					} else if (t == MarketDataType.FORWARD_POINTS) {
						forwardPointsData = true;
					} else if (t == MarketDataType.INTEREST_RATE) {
						interestRateData = true;
					} else if (t == MarketDataType.VOLATILITY) {
						volatilityData = true;
					}
				}
				if (ps != null) {
					boolean isESPenabled = false, isRFSenabled = false;
					CounterPartyVo spotParty = null, outrightParty = null;
					for (LPProduct lpProduct : ps.getProduct()) {
						if ("FXSPOT".equals(lpProduct.getProductNm())) {
							spotContract = true;
							SubscriptionTypes sts = lpProduct.getSubscriptionTypes();
							for (SubscriptionType st : sts.getSubscriptionType()) {
								if (st == SubscriptionType.ESP) {
									isESPenabled = isESPenabled || true;
								} else if (st == SubscriptionType.RFS) {
									isRFSenabled = isRFSenabled || true;
								}
							}
							spotParty = findCounterPartyOfTradingParty(customerProfileService, srcNm,
									lpProduct.getLPCustNmInTTS(), lpProduct.getLPAcctNmInTTS(), localCacheCounterParty);
						} else if ("FXOUTRIGHTFORWARDS".equals(lpProduct.getProductNm())) {
							spotContract = true;
							SubscriptionTypes sts = lpProduct.getSubscriptionTypes();
							for (SubscriptionType st : sts.getSubscriptionType()) {
								if (st == SubscriptionType.ESP) {
									isESPenabled = isESPenabled || true;
								} else if (st == SubscriptionType.RFS) {
									isRFSenabled = isRFSenabled || true;
								}
							}
							outrightParty = findCounterPartyOfTradingParty(customerProfileService, srcNm,
									lpProduct.getLPCustNmInTTS(), lpProduct.getLPAcctNmInTTS(), localCacheCounterParty);
						}
					}
					if (mdprovider == null) {
						mdprovider = new LiquidityProviderVo(adapterSourceCount++, adapterNm, srcNm, isRFSenabled,
								isESPenabled, false);
					}
					((LiquidityProviderVo) mdprovider).setFxSPOTCounterParty(spotParty);
					((LiquidityProviderVo) mdprovider).setFxOutrightCounterParty(outrightParty);

				}
				if (mdprovider == null) {
					mdprovider = new MarketDataProviderVo(adapterSourceCount++, adapterNm, srcNm, false, false);
				}
				if (adapter.isIsEmbedded() != null && adapter.isIsEmbedded()) {
					((MarketDataProviderVo) mdprovider).setEmbedded(true);
				}
				mdProviders.add(mdprovider);
			}
		}
		this.sessionInfo.setMDproviderExternal(mdProviders);
	}

	private CounterPartyVo findCounterPartyOfTradingParty(ICustomerProfileService customerProfileService, String srcNm,
			String lpCustNmInTTS, String lpAcctNmInTTS, HashMap<String, CounterPartyVo> localCacheCounterParty) {
		String cacheKey = srcNm + "-" + lpCustNmInTTS + "-" + lpAcctNmInTTS;
		CounterPartyVo rte = localCacheCounterParty.get(cacheKey);
		if (rte == null) {
			List<CounterPartyVo> cps = customerProfileService.findCounterPartyByNm(lpCustNmInTTS, lpAcctNmInTTS, "EXT",
					1);
			if (cps != null && cps.size() >= 1) {
				rte = cps.get(0);
				localCacheCounterParty.put(cacheKey, rte);
			}
		}
		return rte;
	}

	private CounterPartyVo getInternalAggParty() {
		ICustomerProfileService customerProfileService = (ICustomerProfileService) AppContext.getContext()
				.getBean("customerProfileService");

		List<CounterPartyVo> cps = customerProfileService.findCounterPartyByNm(AGG_CUST_NM, AGG_ACCT_NM, "EXT", 1);

		return cps.get(0);
	}

	private void cleanup() {
		spotMarketDataReceiver.setSpotDataListener(null);
		if (this.lpSubscriptionManager != null) {
			this.lpSubscriptionManager.destroy();
			this.lpSubscriptionManager = null;
			this.mrSubscriptionHandlerManager = null;
			this.injectionWorker.setMarketDataHandlers(null);
		}
		if ( activatedEmbeddedAdapters != null && activatedEmbeddedAdapters.size() > 0 ) {
			for ( IMDEmbeddedAdapter a : activatedEmbeddedAdapters ) {
				a.destroy();
			}
			activatedEmbeddedAdapters = null;
		}
		System.gc();
	}

	public void init() {
		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);

		for (String topic : CTRL_TOPICS_INTERESTED) {
			IMsgReceiver msgReceiver = msgReceiverFactory.getMsgReceiver(false, false);
			msgReceiver.setListener(this);
			msgReceiver.setTopic(topic);
			msgReceiver.init();
			this.ctrlMsgReceivers.add(msgReceiver);
		}

		switchTradingSession(TradingSessionManager.getInstance().getActiveSessionNm());
		this.injectionWorker.start();

		// IMsgReceiver hedgingRequestReceiver =
		// msgReceiverFactory.getMsgReceiver(false, false, true);
		// hedgingRequestReceiver.setListener(this);
		// hedgingRequestReceiver.setTopic(IEventMessageTypeConstant.Market.TOPIC_TRADE_ALL);
		// hedgingRequestReceiver.init();
		// this.hedgingRequestReceiver = hedgingRequestReceiver;
	}

	public void destroy() {
		this.injectionWorker.stop();

		if (this.hedgingRequestReceiver != null) {
			this.hedgingRequestReceiver.destroy();
		}
		if (this.ctrlMsgReceivers.size() > 0) {
			for (IMsgReceiver receiver : ctrlMsgReceivers) {
				receiver.destroy();
			}
			this.ctrlMsgReceivers.clear();
		}
		this.executorService.shutdownNow();
	}

	@Override
	public void onMessage(TtMsg message, IMsgSessionInfo arg1, IMsgProperties msgProperties) {
		String func = "EventController.onEvent".intern();
		String eventType = msgProperties.getSendTopic();

		try {
			monitorAgent.debug(String.format("OnEvent(SYSTEM): %s", eventType));

			if (IEventMessageTypeConstant.REM.CE_TRADING_SESSION.equals(eventType)) {
				ChangeTradingSession tradingSession = ChangeTradingSession.parseFrom(message.getParameters());
				logger.info("<CE_TRADING_SESSION>: " + TextFormat.shortDebugString(tradingSession));
				if (tradingSession.hasChangeTo()) {
					String toTradingSession = tradingSession.getChangeTo().getTradingSessionNm();
					switchTradingSession(toTradingSession);
				}
			} else if (IEventMessageTypeConstant.System.FX_ROLLOVER_EVENT.equals(eventType)) {
				RolloverNotification rolloverNotification = RolloverNotification.parseFrom(message.getParameters());
				logger.info("<FX_ROLLOVER_EVENT>: " + TextFormat.shortDebugString(rolloverNotification));
				fxCalendarBizService.onRolloverEvent(rolloverNotification);
			} else if (EXEC_REPORT_INBOUND_TOPIC.equals(eventType)) {
				ExecutionReportInfo executionReportInfo = ExecutionReportInfo.parseFrom(message.getParameters());
				emsService.handleExecutionResult(executionReportInfo);
				// String symbol = executionReportInfo.getSymbol();
				// this.certifiedPublishingEndpoint.publish(IEventMessageTypeConstant.AutoCover.TRADE_STATUS_FROM_MR_EVENT,
				// executionReportInfo);

			} else if (eventType.endsWith("TRANINFO.MR")) {
				Transaction transactionMessage = Transaction.parseFrom(message.getParameters());
				String symbol = transactionMessage.getSymbol();

			} else if (IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_AVAILABLE_INSTRUMENT_REQUEST
					.equals(eventType)) {
				MMAvailableInstrumentRequest req = MMAvailableInstrumentRequest.parseFrom(message.getParameters());
				MMAvailableInstrumentResponse.Builder respBuilder = MMAvailableInstrumentResponse.newBuilder();
				// if (this.sessionInfo.getCurrencyPairWithInternalLqy() !=
				// null) {
				// respBuilder.addAllSymbol(this.sessionInfo.getCurrencyPairWithInternalLqy());
				// }
				respBuilder.setTradingSessionNm(req.getTradingSessionNm());
				MMAvailableInstrumentResponse resp = respBuilder.build();
				TtMsg ttMsg = TtMsgEncoder.encode(resp);
				arg1.getReplySender().sendReply(ttMsg, msgProperties);
			} else if (IEventMessageTypeConstant.Agg.AGG_AVAILABLE_INSTRUMENT_REQUEST.equals(eventType)) {
				AggInstrumentConfigResponse resp = this.sessionInfo.getAggInstrumentConfigResponse().build();
				TtMsg ttMsg = TtMsgEncoder.encode(resp);
				arg1.getReplySender().sendReply(ttMsg, msgProperties);
				logger.info("<AGG_AVAILABLE_INSTRUMENT_REQUEST>: Replied: " + TextFormat.shortDebugString(resp));
			} else if (IEventMessageTypeConstant.Agg.NEW_MR_SUBSCRIPTION_REQUEST.equals(eventType)) {
				PriceSubscriptionRequest r = PriceSubscriptionRequest.parseFrom(message.getParameters());
				logger.info("<NEW_MR_SUBSCRIPTION_REQUEST>: " + TextFormat.shortDebugString(r));
				this.mrSubscriptionHandlerManager.addSubscription(r);
			} else if (IEventMessageTypeConstant.Agg.CANCEL_MR_SUBSCRIPTION_REQUEST.equals(eventType)) {
				PriceSubscriptionRequest r = PriceSubscriptionRequest.parseFrom(message.getParameters());
				logger.info("<CANCEL_MR_SUBSCRIPTION_REQUEST>: " + TextFormat.shortDebugString(r));
				this.mrSubscriptionHandlerManager.removeSubscription(r);
			} else if (IEventMessageTypeConstant.Agg.AGG_SOURCE_STATUS_UPDATE.equals(eventType)) {
				AggSourceChangeRequest r = AggSourceChangeRequest.parseFrom(message.getParameters());
				logger.info("<AGG_SOURCE_STATUS_UPDATE>: " + TextFormat.shortDebugString(r));
				this.lpSubscriptionManager.setSrcEnable(r.getSourceNm(),
						r.getNewStatus() == AggSourceStatus.SUSPENDED ? false : true);
			} else if (IEventMessageTypeConstant.Agg.NEW_ORDER_FROM_BANK_TRADER.equals(eventType)) {
				com.tts.message.trade.TradeMessage.Transaction transaction = com.tts.message.trade.TradeMessage.Transaction
						.parseFrom(message.getParameters());
				logger.info("<NEW_ORDER_FROM_BANK_TRADER>: " + TextFormat.shortDebugString(transaction));
				emsService.handleExecution(transaction, mrSubscriptionHandlerManager);
			}

		} catch (Exception ex) {
			monitorAgent.logError(func, eventType, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, ex.getMessage(), ex);
		}
	}

	private MarketDataSet findActiveMarketDataSet(MarketDataSetConfig config, String newTradingSessionName) {
		MarketDataSet activeSet = null;
		String activeMarketDataSetNm = null;

		List<MarketDataSetSchedule> schedules = config.getMarketDataSetSchedules();
		if (schedules != null) {
			for (MarketDataSetSchedule schedule : schedules) {
				if (schedule.getTradingSession() != null) {
					for (String tradingSesion : schedule.getTradingSession().getTradingSession()) {
						if (newTradingSessionName.equals(tradingSesion)) {
							activeMarketDataSetNm = schedule.getMarketDataSetNm();
							break;
						}
					}
				}
			}
		}
		List<MarketDataSet> sets = config.getMarketDataSets();
		for (MarketDataSet set : sets) {
			if (activeMarketDataSetNm != null && activeMarketDataSetNm.equals(set.getMarketDataSetNm())) {
				activeSet = set;
				break;
			}
		}
		return activeSet;
	}
}
