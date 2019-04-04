package com.tts.mlp.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.mlp.data.provider.IMarketRawDataProvider;
import com.tts.mlp.data.provider.InstrumentDefinitionProvider;
import com.tts.mlp.data.provider.MarketDataProvider;
import com.tts.mlp.data.provider.MarketRateBookProvider;
import com.tts.util.config.UtilConfiguration;

@Configuration
@Import({UtilConfiguration.class})
public class MLPConfiguration {

	@Bean 
	public IMarketRawDataProvider marketDataProvider() {
		return new MarketDataProvider();
	}

	@Bean
	public InstrumentDefinitionProvider instrumentDefinitionProvider() {
		return new InstrumentDefinitionProvider();
	}
	

	@Bean
	public MarketRateBookProvider marketRateBookProvider() {
		return new MarketRateBookProvider(Arrays.asList(MarketDataProvider.symbols2), marketDataProvider(), instrumentDefinitionProvider());
	}
}
