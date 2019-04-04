package com.tts.mas.config;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.mas.feature.position.TraderAdjustmentApp;
import com.tts.mas.manager.SessionInfoManager;
import com.tts.mas.manager.plugin.FixIntegrationPluginManager;
import com.tts.mas.manager.plugin.MasApplicationPluginManager;
import com.tts.mas.support.CachedFxCalendarBizServiceWrapper;
import com.tts.mas.support.CertifiedPublishingEndpointMessageBusImpl;
import com.tts.mas.support.InstrumentDetailProvider;
import com.tts.mas.support.MasGlobalSequenceProvider;
import com.tts.mas.support.MasSchedulingWorkerImpl;
import com.tts.mas.support.PublishingEndpointMessageBusImpl;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.IMasApplicationPluginSpi;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.plugin.adapter.api.setting.IFixSettingBuilder;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp;
import com.tts.plugin.adapter.impl.base.app.QuoteStackRepo;
import com.tts.plugin.adapter.impl.base.app.trade.TradeQuoteRepo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IFxCalendarBizServiceApi;
import com.tts.plugin.adapter.support.IInstrumentDetailProvider;
import com.tts.plugin.adapter.support.IMasGlobolSequenceProvider;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.protocol.platform.heartbeat.IHeartbeatSender;
import com.tts.protocol.platform.heartbeat.impl.HeartbeatSenderImpl;
import com.tts.service.biz.calendar.FxCalendarBizServiceImpl;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.calendar.event.FxCalendarEventSubscriber;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.instrument.util.SymbolIdMapper;
import com.tts.service.biz.price.convention.PriceConventionManager;
import com.tts.service.config.JdbcConfiguration;
import com.tts.service.config.ServicesConfiguration;
import com.tts.service.db.RuntimeDataService;
import com.tts.util.AppUtils;
import com.tts.util.config.UtilConfiguration;
import com.tts.util.constant.SysProperty;

import quickfix.ConfigError;

@Configuration
@Import({UtilConfiguration.class, JdbcConfiguration.class, ServicesConfiguration.class, MessageConfiguration.class})
public class MasConfiguration {
	
	private static Logger logger = LoggerFactory.getLogger(MasConfiguration.class);
	

    /********************************************
     * VO
     ********************************************/

    @Bean
    public SessionInfo sessionInfo() {
    	return new SessionInfo();
    }
    
    /********************************************
     * Util
     ********************************************/
    
    @Bean
    public IFxCalendarBizService fxCalendarBizService() {
    	return new FxCalendarBizServiceImpl();
    }
    
	@Bean
	public PriceConventionManager priceConventionManager() {
		return new PriceConventionManager();
	}
	
	@Bean(initMethod = "init", destroyMethod = "destroy")
	public FxCalendarEventSubscriber fxCalendarEventSubscriber() {
		return new FxCalendarEventSubscriber();
	}
	
	@Bean
	public ISymbolIdMapper symbolIdMapper() {
		return new SymbolIdMapper();
	}
	
	@Bean
	public IMasGlobolSequenceProvider masGlobalSequenceProvider() {
		return new MasGlobalSequenceProvider();
	}
	
	@Bean
	public QuoteStackRepo quoteStackRepo() {
		return new QuoteStackRepo();
	}
	
	@Bean
	public TradeQuoteRepo tradeQuoteRepo() {
		return new TradeQuoteRepo();
	}
    /********************************************
     * Managers
     ********************************************/
    @Bean(initMethod = "init")
    public SessionInfoManager sessionInfoManager() {
    	return new SessionInfoManager();
    }
    
    @Bean
    public MasApplicationPluginManager masApplicationPluginManager() {
    	return new MasApplicationPluginManager();
    }
    
    @Bean
    public FixIntegrationPluginManager masIntegrationPluginManager() {
    	return new FixIntegrationPluginManager();
    }
    
    /***************************************
     * configuration bean
     ***************************************/
    
    @Bean
    public FixApplicationProperties applicationProperties() throws IOException, quickfix.ConfigError {
    	String env = AppUtils.getActiveEnvironment();
    	String appCategory = System.getProperty("APP_CATEGORY", "all");
    	String configPath = null;
    	if ( appCategory == null || "all".equals(appCategory) ) {
    		configPath = String.format("env-resources/adapter/adapterConfig_%s.properties", env);
    	} else {
    		configPath = String.format("env-resources/adapter/adapterConfig_%s.properties", env);
    	}
    	
    	boolean resourceExist = false;
    	URL u = Thread.currentThread().getContextClassLoader().getResource(configPath);
    	if ( "file".equals(u.getProtocol()) ) {
    		File f;
			try {
				f = new File(u.toURI());
				resourceExist = f.exists();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}   		
    	}
    	if ( !resourceExist) {
    		logger.debug("Unable to find applicationProperties, " + configPath);
    		configPath = "env-resources/adapter/adapterConfig.properties";
    	}
    	FixApplicationProperties p = new FixApplicationProperties(configPath);    	
		return p;    	
    }


    /*****************************************
     * Integration Bean Linking
     *****************************************/
    
    @Bean
    public IFixIntegrationPluginSpi masIntegrationPlugin() {
    	return masIntegrationPluginManager().getActiveIntegrationPlugin();
    }

    @Bean
    public IFixSetting fixSetting() throws IOException, ConfigError {
		IFixSettingBuilder fixSettingBuilder = masIntegrationPlugin().getFixSettingBuilder();
		fixSettingBuilder.setFixApplicationProperties(applicationProperties());
		return fixSettingBuilder.build();
    }
    
    
    @Bean quickfix.SessionSettings quickfixSessionSetting() throws IOException, ConfigError {
    	return fixSetting().getQuickfixSessionSetting();
    }
    /******************************************
     * supporting bean
     ****************************************/
	@Bean(destroyMethod = "shutdownNow")
	public ScheduledExecutorService schedulerService() {
		return Executors.newScheduledThreadPool(AppType.values().length );
	}
	
	@Bean
	public ConcurrentTaskScheduler taskScheduler() {
		ConcurrentTaskScheduler taskScheduler = new ConcurrentTaskScheduler();
		taskScheduler.setScheduledExecutor(schedulerService());
		return taskScheduler;
	}
    
	@Bean(initMethod = "init", destroyMethod = "destroy")
	public IPublishingEndpoint publishingEndpoint() {
		return new PublishingEndpointMessageBusImpl();
	}
    
	@Bean(initMethod = "init", destroyMethod = "destroy")
	public ICertifiedPublishingEndpoint certifiedPublishingEndpoint() {
		return new CertifiedPublishingEndpointMessageBusImpl();
	}
        
	@Bean(initMethod = "init", destroyMethod = "destroy")
	public ISchedulingWorker schedulingWorker() {
		return new MasSchedulingWorkerImpl(schedulerService());
	}
	
	@Bean
	public IFxCalendarBizServiceApi fxCalendarBizServiceApi() {
		return new CachedFxCalendarBizServiceWrapper();
	}
	
	@Bean
	public IInstrumentDetailProvider instrumentDetailProvider() {
		return new InstrumentDetailProvider();
	}
    
	
    /******************************************
     * Application Bean Linking
     ***************************************/
    
	@Bean
	public IMasApplicationPluginSpi masApplicationPlugin() {
		return masApplicationPluginManager().getActiveApplicationPlugin();
	}
	
	@Bean
	public ITraderAdjustmentApp traderAdjustmentApp() {
		return new TraderAdjustmentApp(symbolIdMapper().getSymbols().toArray(new String[0]));
	}
	
	/********************************************
	 * Heartbeat
	 ********************************************/
	
	@Bean(initMethod = "init", destroyMethod = "destroy")
	public IHeartbeatSender heartbeatSender() {
		HeartbeatSenderImpl hbSender = new HeartbeatSenderImpl();
		hbSender.setNonCertifiedHeartbeatEnabled(true);
		hbSender.setTaskScheduler(taskScheduler());
		hbSender.setTicketName("MDE");
		
		String hbInterval = RuntimeDataService.getRunTimeData(SysProperty.GroupCd.CONSTANTS, SysProperty.Key1.HEARTBEAT_INTERVAL, null);
		if (hbInterval != null && !hbInterval.isEmpty()) {
			hbSender.setIntervalInMillis(Long.valueOf(hbInterval));
		}
		
		return hbSender;
	}
}
