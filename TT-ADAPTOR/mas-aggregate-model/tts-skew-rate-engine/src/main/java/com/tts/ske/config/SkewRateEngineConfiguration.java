package com.tts.ske.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.protocol.config.MessageConfiguration;
import com.tts.service.biz.calendar.FxCalendarBizServiceImpl;
import com.tts.service.biz.calendar.ICachedFxCalendarBizServiceApi;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.service.biz.calendar.event.FxCalendarEventSubscriber;
import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.service.biz.instrument.util.SymbolIdMapper;
import com.tts.service.biz.price.convention.PriceConventionManager;
import com.tts.service.config.JdbcConfiguration;
import com.tts.service.config.ServicesConfiguration;
import com.tts.ske.app.DataFlowController;
import com.tts.ske.app.TtsTradairQfixApplication;
import com.tts.ske.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.ske.support.BankLiquidityStore;
import com.tts.util.config.UtilConfiguration;

@Configuration
@Import({ UtilConfiguration.class, JdbcConfiguration.class, ServicesConfiguration.class, MessageConfiguration.class })
public class SkewRateEngineConfiguration {

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
	public ICachedFxCalendarBizServiceApi fxCalendarBizServiceApi() {
		return new com.tts.service.biz.calendar.CachedFxCalendarBizService(symbolIdMapper().getSymbols().toArray(new String[0]));
	}
	
	/***
	 * 
	 * 
	 * 
	 */

	@Bean
	public BankLiquidityStore bankLiquidityStore() {
		return new BankLiquidityStore();
	}


	@Bean(initMethod = "init", destroyMethod = "destroy")
	public DataFlowController dataFlowController() {
		return new DataFlowController();
	}
	
	@Bean
	public PriceSubscriptionRegistry priceSubscriptionRegistry() {
		return new PriceSubscriptionRegistry();
	}
	
	@Bean
	public quickfix.Application qfixApp() {
		return new TtsTradairQfixApplication(priceSubscriptionRegistry(),  dataFlowController());
	}
}
