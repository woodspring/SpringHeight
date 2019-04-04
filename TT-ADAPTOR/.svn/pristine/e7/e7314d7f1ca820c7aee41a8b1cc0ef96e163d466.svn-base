package com.tts.marketclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.marketclient.app.TtsTradAirClientApp;
import com.tts.util.config.UtilConfiguration;

@Configuration
@Import({UtilConfiguration.class})
public class ClientAppConfiguration {

	@Bean
	public TtsTradAirClientApp app() {
		return new TtsTradAirClientApp();
	}
}
