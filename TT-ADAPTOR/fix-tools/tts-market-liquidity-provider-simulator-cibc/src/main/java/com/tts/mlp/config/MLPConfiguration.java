package com.tts.mlp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.mlp.app.price.data.IUpdatableMarketPriceProvider;
import com.tts.mlp.app.price.data.UpdatableMarketPriceProvider;
import com.tts.mlp.app.price.data.refresh.WebRefresher;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.util.config.UtilConfiguration;

@Configuration
@Import({UtilConfiguration.class})
public class MLPConfiguration {

	@Bean
	public PriceSubscriptionRegistry priceSubscriptionRegister() {
		return  new PriceSubscriptionRegistry();
	}
	
	@Bean
	public IUpdatableMarketPriceProvider marketPriceProvider() {
		return new UpdatableMarketPriceProvider(
			"app-resources/random.seed.data.txt", 
			"instrument.cfg");
	}
	
	@Bean(destroyMethod = "destroy")
	public WebRefresher webRefresher() {
		return new WebRefresher(marketPriceProvider());
	}

}
