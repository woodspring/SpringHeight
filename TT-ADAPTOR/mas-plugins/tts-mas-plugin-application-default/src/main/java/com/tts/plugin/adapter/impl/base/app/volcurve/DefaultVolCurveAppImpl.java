package com.tts.plugin.adapter.impl.base.app.volcurve;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.market.VolatilityCurveStruct.DeltaInfo;
import com.tts.message.market.VolatilityCurveStruct.VolatilityCurve;
import com.tts.message.market.VolatilityCurveStruct.VolatilityCurve.DeltaConvention;
import com.tts.message.market.VolatilityCurveStruct.VolatilityCurveIndex;
import com.tts.message.market.VolatilityCurveStruct.VolatilityCurveTenorInfo;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.impl.base.app.AbstractPublishingApp;
import com.tts.plugin.adapter.impl.base.app.fxprice.IEspPriceRepoDependent;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.price.IVolatilityCurveServices;
import com.tts.service.biz.price.VolatilityCurveUtil;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;
import com.tts.vo.FxOptionDateVo;
import com.tts.vo.TenorVo;

public class DefaultVolCurveAppImpl extends AbstractPublishingApp implements IEspPriceRepoDependent {
	private static Logger logger     = LoggerFactory.getLogger(DefaultVolCurveAppImpl.class);
	private static final String VOLCURVE_NAME = "VOL_CURVE";
	private static final String RESOURCE_LOC  = "/app-resources/volcurves/";
	private static final String RESOURCE_ENDS_WITH = ".csv";
	private static final String DEFAULT_COMMENT_INDICATOR = "#";
	
	private final IFxCalendarBizService fxCalendarBizService;
	private final IVolatilityCurveServices volcServices;
	
	private IEspRepo<?> espRepo;
	private final String[] _symbols;
	private final Map<String, List<VolatilityCurve.Builder>> mapVolCurveData;
	private final Map<String, VolatilityCurveIndex.Builder> mapVolCurveIndex;
	private WatchService volCurveWatchService;
	private boolean keepRunningWatchService = false;
	
	public DefaultVolCurveAppImpl(IMkQfixApp qfixApp, ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint, ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi IFixIntegrationPluginSpi) {
		
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint, iCertifiedPublishingEndpoint, IFixIntegrationPluginSpi);
		
		this.volcServices = new VolatilityCurveUtil();
		this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
		
		this.mapVolCurveData  = new ConcurrentHashMap<>();
		this.mapVolCurveIndex = new ConcurrentHashMap<>();
		this._symbols = sessionInfo.getMarketDataset().getAvailableSymbolsToArrayByType(AppType.VOLCURVEADAPTER.getPublishingFormatType().toString());
		
		try {
			this.volCurveWatchService = FileSystems.getDefault().newWatchService();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
		}
				
		logger.info("Default Volume Curve Initialized Successfully.");
	}
	
	@Override
	public void atPublish(long masGlobalSeq) {
		logger.trace("Publishing Manual SPOT Prices to the END Point...");
		
		for(String symbol: _symbols)	{
			
			final IndividualInfoVo individualInfo = getSessionInfo().getMarketDataset().getMarketStructureByTypeAndSymbol(AppType.VOLCURVEADAPTER.getPublishingFormatType().toString(), symbol);
			if((individualInfo == null) || (individualInfo.getTopic() == null))	{
				continue;
			}
			
			final String tradeDateStr = individualInfo.getTradeDateString();
			long indicativeFlag       = individualInfo.getIndicativeFlag();
			long indicativeSubStatus  = individualInfo.getIndicativeSubStatus();
						
			List<VolatilityCurve.Builder> volCurveData = mapVolCurveData.get(symbol);
			
			String pubTopic = individualInfo.getTopic();
			
			if((volCurveData != null) && (!volCurveData.isEmpty()))	{
				volCurveData.forEach(volCData -> {
					String expiryDate = null;
					volCData.setTradeDate(tradeDateStr);
					
					try	{
						Long.parseLong(volCData.getTenorInfo().getTenor());
						expiryDate = volCData.getTenorInfo().getTenor();
					}
					catch(NumberFormatException nfExp) {
						LocalDate tradeDate   = individualInfo.getTradeDate();
						TenorVo tenorVo       = TenorVo.fromString(volCData.getTenorInfo().getTenor());
						FxOptionDateVo dateVo = fxCalendarBizService.getFxOptionExpiryDate(symbol, tradeDate, null, tenorVo, PricingConventionConstants.INTERBANK);
						expiryDate            = ChronologyUtil.getDateString(dateVo.getExpiryDate());
					}
					
					double timeFraction = volcServices.calculateTimeFraction(ChronologyUtil.getLocalDateFromString(tradeDateStr), ChronologyUtil.getLocalDateFromString(expiryDate));
					//volCData.setTimeFraction(String.valueOf(timeFraction));
					volCData.getTenorInfoBuilder().setExpiryDate(expiryDate);
					
					volCData.setIndicativeFlag(0);
					volCData.setIndicativeSubFlag(0);
					volCData.setSequence(masGlobalSeq);
					
					String newTopic = String.format(pubTopic, symbol, volCData.getTenorInfo().getTenor());
					updateVolCurveIndexInformation(symbol, volCData.getTenorInfo().getTenor(), expiryDate, newTopic);
					
					logger.trace("@@atPublish <<" + symbol + "--" + newTopic + ">> " + TextFormat.shortDebugString(volCData.build()));
					getPublishingEndpoint().publish(newTopic, volCData.build());
					
					
					/*
					 *	The above Statement publishes the changes with indicator set to 1.
					 *	Since the changes published change the indicator to normal. 
					 */
					volCData.setRateChangeInd(0);
				});
				
				VolatilityCurveIndex.Builder volCurveIndex = mapVolCurveIndex.get(symbol);
				mapVolCurveIndex.get(symbol).setSequence(masGlobalSeq);
				String indexTopic = String.format(IEventMessageTypeConstant.Market.TOPIC_VOL_CURVE_INDEX, symbol);
				
				logger.trace("@@atPublish <<" + symbol + "--" + indexTopic + ">> " + TextFormat.shortDebugString(volCurveIndex.build()));
				getPublishingEndpoint().publish(indexTopic, volCurveIndex.build());
				
			}	/*
			else	{
				String [] tenors = new String[] {"1W", "20170531", "1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "5Y", "7Y", "10Y"};
				indicativeFlag   = IndicativeFlag.setIndicative(indicativeFlag, IndicativeFlag.IndicativeReason.MA_NoData);
				
				VolatilityCurve.Builder missingVolCSymbol = VolatilityCurve.newBuilder();
				missingVolCSymbol.setSymbol(symbol);
				missingVolCSymbol.setTradeDate(tradeDateStr);
				missingVolCSymbol.setIndicativeFlag(indicativeFlag);
				missingVolCSymbol.setIndicativeSubFlag(0);
				missingVolCSymbol.setDeltaConvention(DeltaConvention.FORWARD);
				missingVolCSymbol.setRateChangeInd(0);
				
				for(String tenor: tenors)	{
					missingVolCSymbol.setTenor(tenor);
					
					try	{
						Long.parseLong(tenor);
						missingVolCSymbol.setExpiryDate(tenor);
					}
					catch(NumberFormatException nfExp) {
						LocalDate tradeDate   = individualInfo.getTradeDate();
						TenorVo tenorVo       = TenorVo.fromString(tenor);
						FxOptionDateVo dateVo = fxCalendarBizService.getFxOptionExpiryDate(symbol, tradeDate, null, tenorVo, PricingConventionConstants.INTERBANK);
						missingVolCSymbol.setExpiryDate(ChronologyUtil.getDateString(dateVo.getExpiryDate()));
					}
					
					double timeFraction = volcServices.calculateTimeFraction(ChronologyUtil.getLocalDateFromString(tradeDateStr), ChronologyUtil.getLocalDateFromString(missingVolCSymbol.getExpiryDate()));
					missingVolCSymbol.setTimeFraction(String.valueOf(timeFraction));
					
					String newTopic = String.format(pubTopic, symbol, tenor);
					
					logger.debug("@@atPublish <<" + symbol + "--" + newTopic + ">> " + TextFormat.shortDebugString(missingVolCSymbol.build()));
					getPublishingEndpoint().publish(newTopic, missingVolCSymbol.build());
				}
			}	*/
		}
	}
	
	private void updateVolCurveIndexInformation(String symbol, String tenor, String expiryDate, String topic)	{
		VolatilityCurveIndex.Builder volCurveIndex = mapVolCurveIndex.get(symbol);
		List<VolatilityCurveTenorInfo.Builder> volCurveTenorInfo = volCurveIndex.getTenorInfoBuilderList();
		volCurveTenorInfo.forEach(volCurveTenor -> {
			if(volCurveTenor.getTenor().equals(tenor))	{
				volCurveTenor.setExpiryDate(expiryDate);
				volCurveTenor.setTopic(topic);
				return;
			}
		});
	}

	@Override
	public PublishingFormatType getPublishingFormatType() {
		return PublishingFormatType.FxVanillaOptions;
	}

	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.REFRESH;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getName() {
		return VOLCURVE_NAME;
	}

	@Override
	public void start() {
		try	{
			URI volCurveFileLocation = getClass().getResource(RESOURCE_LOC).toURI();
			logger.info("Loading VolCurve Data File From " + volCurveFileLocation.toString());
			
			Files.newDirectoryStream(Paths.get(volCurveFileLocation), path -> path.toString().endsWith(RESOURCE_ENDS_WITH))
			     .forEach(this::loadVolCurveFromFile);
			
			keepRunningWatchService = true;
			Thread watchThread = new Thread()	{
				
				@SuppressWarnings("unchecked")
				@Override
				public void run()	{
					Thread.currentThread().setName("volCurve_FileWatcher");
					logger.info("Watching for VolCurve Data Creation/Updation");
					
					try {
						Path watchPath = Paths.get(volCurveFileLocation);
						watchPath.register(volCurveWatchService, StandardWatchEventKinds.ENTRY_CREATE,
								StandardWatchEventKinds.ENTRY_MODIFY);
						
						while(keepRunningWatchService)	{
							WatchKey key = volCurveWatchService.take();
							for (WatchEvent<?> watchEvent : key.pollEvents()) {
								final Kind<?> kind = watchEvent.kind();
								if (kind == StandardWatchEventKinds.OVERFLOW) {
									continue;
								}
								
								final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
								final Path newEntry = watchEventPath.context();
								if(newEntry.toString().endsWith(RESOURCE_ENDS_WITH))
									loadVolCurveFromFile(newEntry);
							}
							
							key.reset();
							if (!key.isValid()) {
								break;
							}
						}
						
						logger.info("Stopped Watching for VolCurve Data Creation/Updation");
					} catch (IOException | InterruptedException e) {
						// TODO Auto-generated catch block
					}
				}
			};
			
			watchThread.start();
		} 
		catch(URISyntaxException | IOException exp) {
			logger.error("Error Location VolCurve Data File Location. " + exp.getMessage());
			logger.error("URISyntaxException | IOException : ", exp);
			exp.printStackTrace();
		}
	}

	@Override
	public void stop() {
		keepRunningWatchService =  false;
		
		try {
			if(volCurveWatchService != null)	{
				volCurveWatchService.close();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
		}
		
		volCurveWatchService = null;
	}

	@Override
	public void setEspRepo(IEspRepo<?> espRepo) {
		this.espRepo = espRepo;
	}
	
	public IEspRepo<?> getEspRepo() {
		return this.espRepo;
	}
	
	private void loadVolCurveFromFile(Path fileAbsolutePath)	{
		logger.info("Reading VolCurve Data from " + fileAbsolutePath.toString());
		
		try(Scanner voCurveReader = new Scanner(fileAbsolutePath))	{
			String nextLine      = null;
			String symbol        = null;
			int totalDeltaPoints = 0;		//	No. of Delta Point Available for the VolCurve
			int minRequiredData  = 0;		//	Expected No. of Data based on the No. of Delta Points
			List<String> volCurveFirstLine = null;
			VolatilityCurveIndex.Builder volCurveIndex = null;
			boolean isFirstLine  = true;
			
			while(voCurveReader.hasNext())	{
				nextLine = null;
				nextLine = voCurveReader.nextLine();
				if(nextLine.startsWith(DEFAULT_COMMENT_INDICATOR))
					continue;
				
				List<String> volCurveData = AppUtils.parseCSVLineOfText(nextLine, ' ', ' ');
				if(isFirstLine && (volCurveData != null) && (!volCurveData.isEmpty()))	{
					symbol = volCurveData.remove(0);
					totalDeltaPoints  = volCurveData.size();
					minRequiredData   = totalDeltaPoints * 2;
					volCurveFirstLine = volCurveData; 
					isFirstLine = false;
					
					volCurveIndex = VolatilityCurveIndex.newBuilder();
					volCurveIndex.setSymbol(symbol);
					continue;
				}
				
				
				VolatilityCurveTenorInfo.Builder volCurveTenor = VolatilityCurveTenorInfo.newBuilder();
				volCurveTenor.setTenor(volCurveData.remove(0));
				volCurveTenor.setDataAvailable(true);
				
				if(volCurveData.size() < minRequiredData)	{
					volCurveTenor.setDataAvailable(false);
					volCurveIndex.addTenorInfo(volCurveTenor);
					
					logger.warn(symbol + " VolCurve - Line Skipped due to INVALID DATA FORMAT... DATA INSUFFICIENT...");
					logger.warn(symbol + " SKIPPING Line " + nextLine);
					continue;
				}
				
				volCurveIndex.addTenorInfo(volCurveTenor);
				
				VolatilityCurve.Builder volCurve = VolatilityCurve.newBuilder();
				volCurve.setSymbol(symbol);
				volCurve.setTenorInfo(volCurveTenor);
				
				for(String data: volCurveFirstLine)	{
					DeltaInfo.Builder volCurveDelta = DeltaInfo.newBuilder();
					volCurveDelta.setDeltaPoint(data);
					
					volCurveDelta.setBid(volCurveData.remove(0));
					volCurveDelta.setAsk(volCurveData.remove(0));
					
					volCurve.addDelta(volCurveDelta);
				}
				
				//	Indicate Price Update Change
				volCurve.setRateChangeInd(1);
				volCurve.setDeltaConvention(DeltaConvention.FORWARD);
				
				if(mapVolCurveData.containsKey(symbol))	{
					mapVolCurveData.get(symbol).add(volCurve);
				}
				else	{
					mapVolCurveData.put(symbol, new ArrayList<>());
					mapVolCurveData.get(symbol).add(volCurve);
				}
			}
			
			mapVolCurveIndex.put(symbol, volCurveIndex);
		} 
		catch(IOException exp) {
			logger.error("Error Reading VolCurver Data File. " + exp.getMessage());
			logger.error("IOException : ", exp);
			exp.printStackTrace();
		}
	}
}
