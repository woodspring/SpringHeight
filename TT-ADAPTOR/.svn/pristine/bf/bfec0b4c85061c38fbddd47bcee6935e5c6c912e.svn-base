package com.tts.mlp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.mlp.app.ForwardCurveDataManager;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.app.price.data.UpdatableMarketPriceProvider;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.util.config.UtilConfiguration;

@Configuration
@Import({UtilConfiguration.class})
public class MLPConfiguration {

	@Bean
	public PriceSubscriptionRegistry priceSubscriptionRegister() {
		return  new PriceSubscriptionRegistry();
	}
	
	@Bean(destroyMethod = "destroy")
	public ForwardCurveDataManager forwardCurveDataManager() {
		return new ForwardCurveDataManager();
	}
	
	@Bean
	public IRandomMarketPriceProvider IRandomMarketPriceProvider() {
		return new UpdatableMarketPriceProvider(
			"app-resources/random.seed.data.txt",
			"app-resources/bidAskSpread.txt",
			"instrument.cfg");
	}
}
