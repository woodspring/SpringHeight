package com.tts.mas;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mas.app.MarketQFixApp;
import com.tts.mas.app.controller.DataFlowController;
import com.tts.mas.config.MasConfiguration;
import com.tts.mas.config.MasReutersConfiguration;
import com.tts.mas.manager.SessionInfoManager;
import com.tts.mas.qfx.impl.QuickfixEngineAcceptorContainer;
import com.tts.mas.qfx.impl.QuickfixEngineInitiatorContainer;
import com.tts.mas.vo.LogControlVo;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi.FixVersion;
import com.tts.plugin.adapter.api.IMasApplicationPluginSpi;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.plugin.adapter.impl.base.app.fxprice.IndividualPriceStore;
import com.tts.plugin.adapter.impl.base.app.fxprice.repo.Esp44Repo;
import com.tts.plugin.adapter.impl.base.app.fxprice.repo.Esp50Repo;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;


public class TtsMarketAdapterServerMain {
	private final static Logger logger = LoggerFactory.getLogger(TtsMarketAdapterServerMain.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	private static final int ESP_QUOTE_LOOKUP_SIZE; 
	
	static {
		int size = -1;
		String lookupSizeStr = System.getenv("MARKET_DATA_LOOKUP_SIZE");
		if ( lookupSizeStr != null ) {
			try {
				size = Integer.parseInt(lookupSizeStr);
			} catch (Exception e) {
				
			}
		}
		if ( size < 0 ) {
			size = IndividualPriceStore.INDIVIDUAL_STORE_QUEUE_SIZE;
		}
		ESP_QUOTE_LOOKUP_SIZE = size;
	}
	
	public static void main(String[] args) {
		
		DataFlowController dataFlowController = null;
		String func = "TtsMarketAdapterServerMain.main";
		AppContext.TtsAnnotationConfigApplicationContext ctx = null;
		try {
			//KeyCheck.v();
			ctx = new AppContext.TtsAnnotationConfigApplicationContext();
			
			ctx.register(MasConfiguration.class);
			if ( "REUTERS".equals(System.getenv("MARKET_FWDADAPTER"))) {
				ctx.register(MasReutersConfiguration.class);
			}
			ctx.refresh();
			ctx.registerShutdownHook();
			
			
			IFixIntegrationPluginSpi integrationPlugin = ctx.getBean(IFixIntegrationPluginSpi.class);
			IEspRepo<?> espRepo = null;
			SessionInfo sessionInfo = ctx.getBean(SessionInfo.class);
			String[] all_symbols = ctx.getBean(ISymbolIdMapper.class).getSymbols().toArray(new String[0]);
			String[] all_tenors = SessionInfoManager.STD_TENORS.toArray(new String[0]);
			if ( integrationPlugin.getDefaultFixMsgVersion() == FixVersion.FIX50 ) {
				espRepo = new Esp50Repo(ESP_QUOTE_LOOKUP_SIZE, all_symbols, all_tenors);
			} else if ( integrationPlugin.getDefaultFixMsgVersion() == FixVersion.FIX44 )  {
				espRepo = new Esp44Repo(2, all_symbols, all_tenors);
			}
			
			IFixSetting fixSetting = ctx.getBean(IFixSetting.class);
			
			//MarketQfixApplication qfixApp = new MarketQfixApplication(fixSetting, integrationPlugin);
			boolean mdLogEnabledDefault = false;
			try {
				mdLogEnabledDefault = Boolean.parseBoolean(System.getenv("MARKET_DATA_LOGGING_DYNAMIC_CONTROL_DEFAULT"));
			} catch (Exception e) {
				
			}
			LogControlVo logControl = new LogControlVo( mdLogEnabledDefault);

			MarketQFixApp qfixApp         = new MarketQFixApp(fixSetting, integrationPlugin, logControl);
			
			List<quickfix.SessionSettings> qfixSessionSettings = fixSetting.getQuickfixSessionSettings();
			
			
			IInterfacingAppFactory interfacingAppFactory = ctx
					.getBean(IMasApplicationPluginSpi.class).getInterfacingAppFactory();
			if ( interfacingAppFactory != null  ) {
				interfacingAppFactory.setCertifiedPublishingEndpoint(ctx.getBean(ICertifiedPublishingEndpoint.class));
				interfacingAppFactory.setPublishingEndpoint(ctx.getBean(IPublishingEndpoint.class));
				interfacingAppFactory.setQfixApp(qfixApp);
				interfacingAppFactory.setSchedulingWorker(ctx.getBean(ISchedulingWorker.class));
				interfacingAppFactory.setSessionInfo(sessionInfo);
				interfacingAppFactory.setActiveIntegrationPlugin(integrationPlugin);
				interfacingAppFactory.setEspRepo(espRepo);
			}

			Thread.sleep(5 * ChronologyUtil.MILLIS_IN_SECOND);

			dataFlowController = new DataFlowController(interfacingAppFactory,  qfixApp);
			dataFlowController.init();
			
			
			for (quickfix.SessionSettings qfixSessionSetting: qfixSessionSettings ) {
				Properties p = qfixSessionSetting.getDefaultProperties();
				
				if ( "acceptor".equals(p.get("ConnectionType"))) {		
					QuickfixEngineAcceptorContainer container = new QuickfixEngineAcceptorContainer(
							qfixSessionSetting, 
							qfixApp,
							logControl);
		
					container.start();
				} else {
					QuickfixEngineInitiatorContainer container = new QuickfixEngineInitiatorContainer(
							qfixSessionSetting,
							qfixApp,
							logControl);
					container.logon();
					Thread.sleep(ChronologyUtil.MILLIS_IN_SECOND * 5);
				}
			
			}			
			
			while ( true) {
				Thread.sleep(Long.MAX_VALUE);
			}

		} catch (InterruptedException e) {
			
		} catch (Exception e) {
			monitorAgent.logError(func, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, "Main Exception " + e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if ( dataFlowController != null ) {
				try {
					dataFlowController.destroy();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (ctx != null) {
				ctx.close();
			}
		}
	}

}
