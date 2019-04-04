package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.app.IPublishingApp;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.FullBookBuilderFullScanStructureDifferentiator;
import com.tts.service.biz.price.structure.diff.full.IFullScanStructureDifferentiator;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;
import com.tts.util.flag.IndicativeFlag.IndicativeSubStatus;
import com.tts.util.flag.RateChangeIndFlag;
import com.tts.util.flag.RateChangeIndFlag.Change;
import com.tts.vo.TenorVo;

public class FbPricePublishHandler implements BiFunction<Message.Builder, Long, Message.Builder> {
	private static final String LOGMSG__PATTERN__DETECTED_VALUE_DATE_OUT_OF_SYNC_FOR_S_S_S = "Detected value date out of sync for %s, %s %s";

	private final static Logger logger = LoggerFactory.getLogger(IPublishingApp.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);

	private final SessionInfo sessionInfo;
	private final IPublishingEndpoint endpoint;
	private final PriceStructureRepo<FullBook.Builder> structureRepo;
	private final IFullScanStructureDifferentiator<FullBook.Builder> differentiator;
	private final IPublishValidator<FullBook.Builder> validator;
	private final ITraderAdjustmentApp traderAdjustmentApp; 
	private final boolean valueDateCheck;
	
	public FbPricePublishHandler(
			IPublishingEndpoint publishingEndpoint, 
			SessionInfo sessionInfo, 
			IPublishValidator<FullBook.Builder> validator, 
			ITraderAdjustmentApp traderAdjustmentApp) {
		boolean vDateCheck = true;
		try {
			String setting = System.getenv("MARKET_VALUE_DATE_VALIDATION_WITH_INCOMING_DATA");
			if ( setting == null || setting.length() == 0 ) {
				vDateCheck = true;
			} else {
				vDateCheck = Boolean.parseBoolean(setting);
			}
		} catch (Exception e ) {
			vDateCheck = true;
		}
		this.valueDateCheck = vDateCheck;
		this.endpoint = publishingEndpoint;
		this.sessionInfo = sessionInfo;		
		this.structureRepo = new PriceStructureRepo<FullBook.Builder>(AppContext.getContext().getBean(ISymbolIdMapper.class));
		this.differentiator = new FullBookBuilderFullScanStructureDifferentiator();
		this.traderAdjustmentApp = traderAdjustmentApp;
		this.validator = validator;
	}
	@Override
	public Message.Builder apply(Message.Builder t, Long masGlobalSeq) {
		final SessionInfo sessionInfo = this.sessionInfo;
		if(t instanceof FullBook.Builder == false) {
			return null;
		}
		long bzStartTime = System.currentTimeMillis();

		FullBook.Builder fbBuilder = (FullBook.Builder) t;
		String symbol = fbBuilder.getSymbol();
		IndividualInfoVo  individualInfo = sessionInfo.getMarketDataset().getMarketStructureByTypeAndSymbol(
				AppType.SPOTADAPTER.getPublishingFormatType().toString(), symbol);
		if ( individualInfo == null ) {
			return fbBuilder;
		}
		long indicativeFlag = individualInfo.getIndicativeFlag();
		long indicativeSubStatus = individualInfo.getIndicativeSubStatus();
		
		if (individualInfo.getTradeDateString() == null) {
			indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
					IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
			indicativeSubStatus = IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus,
					IndicativeFlag.IndicativeSubStatus.CONFIGURATION_TradeDateNotDefined);
		} else {
			fbBuilder.setTradeDate(individualInfo.getTradeDateString());
		}

//		long[] expectedLiquidityLevels = individualInfo.getLiquidities();
//		boolean valid = validFullBookLiquidity(fbBuilder, expectedLiquidityLevels);
//		if (!valid) {
//			indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
//					IndicativeFlag.IndicativeReason.MA_MarketRateConfigurationError);
//			indicativeSubStatus = IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus,
//					IndicativeSubStatus.CONFIGURATION_MktStructNotMatched);
//		}
		fbBuilder.setTradingSession(sessionInfo.getTradingSessionId());
		fbBuilder.setTopOfBookIdx(0);
		
		fbBuilder = validator.validate(fbBuilder);
		indicativeFlag = IndicativeFlag.combineIndicativeFlag(indicativeFlag, fbBuilder.getIndicativeFlag());

		int rateChangeIndFlag = RateChangeIndFlag.NO_CHANGE;
		try {
			boolean isStructureChanged = structureRepo.hasStructureChanged(symbol, differentiator, fbBuilder);
			if ( isStructureChanged ) {
				rateChangeIndFlag = RateChangeIndFlag.setChanged(rateChangeIndFlag, Change.Structure);
			} 
		} catch (Exception e) {
			rateChangeIndFlag = RateChangeIndFlag.setChanged(rateChangeIndFlag, Change.Structure);
			logger.warn("Error in hashing pricing, " + TextFormat.shortDebugString(fbBuilder));
		}

		int tenorCount = fbBuilder.getTenorsCount();
		String internalValueDate = individualInfo.getValueDateMap().get(TenorVo.NOTATION_SPOT);

		if ( tenorCount > 0 ) {
			Tenor.Builder tenor = fbBuilder.getTenorsBuilder(0);

			if ( TenorVo.NOTATION_SPOT.equals(tenor.getName())
					&& individualInfo.getValueDateMap() != null
					&& tenor.hasActualDate()) {
				if ( internalValueDate.equals(tenor.getActualDate()) ) {
					fbBuilder.setSpotValueDate(internalValueDate);
				} else if ( valueDateCheck ){
					String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());
					fbBuilder.clearAskTicks();
					fbBuilder.clearBidTicks();
					indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag, IndicativeReason.MA_MarketRateConfigurationError);
					indicativeSubStatus = IndicativeFlag.setIndicativeSubStatus(indicativeSubStatus, IndicativeSubStatus.VALIDATION_TradeDateValueDateOutOfSync);
					String msg = String.format(LOGMSG__PATTERN__DETECTED_VALUE_DATE_OUT_OF_SYNC_FOR_S_S_S, symbol, internalValueDate, tenor.getActualDate());
					logger.debug(msg);
					monitorAgent.logError("MarketValidation", topic, MonitorConstant.FXADT.ERROR_FAIL_ACCESS_EXTERNAL_SOURCE, 
							msg );
				}
			} else {
				fbBuilder.setSpotValueDate(internalValueDate);
			}
		} else {		
			fbBuilder.setSpotValueDate(internalValueDate);
		}
		long bzEndTime = System.currentTimeMillis();
		if ( logger.isTraceEnabled() && (bzEndTime - bzStartTime ) > 10  ) {
			logger.trace("bizLogic for " + fbBuilder.getSymbol() + " took more than 30milli, " +  bzStartTime + "->" +  bzEndTime);
		}
		
		fbBuilder.setRateChangeInd(rateChangeIndFlag);
		fbBuilder.setIndicativeFlag(indicativeFlag);
		fbBuilder.setIndicativeSubFlag(indicativeSubStatus);
		fbBuilder.setSequence(masGlobalSeq);
		fbBuilder.getLatencyBuilder().setFaSendTimestamp(System.currentTimeMillis());
		FullBook.Builder fb = FullBook.newBuilder(fbBuilder.build());
		if ( traderAdjustmentApp != null ) { 
			fbBuilder = traderAdjustmentApp.adjustBook(symbol, fb);
		}
		fb.clearTenors();
		long publishTimeStart = System.currentTimeMillis();
  		endpoint.publish(
				String.format(IEventMessageTypeConstant.Market.TOPIC_SPOT_TEMPLATE, fbBuilder.getSymbol()), 
				fb.build());
		long publishTimeEnd = System.currentTimeMillis();
		if (  logger.isTraceEnabled() && (publishTimeEnd - publishTimeStart ) > 10  ) {
			logger.trace("publishing " + fbBuilder.getSymbol() + " took more than 30milli, " +  publishTimeStart + "->" + publishTimeEnd);
		}
		return fbBuilder;
	}


}
