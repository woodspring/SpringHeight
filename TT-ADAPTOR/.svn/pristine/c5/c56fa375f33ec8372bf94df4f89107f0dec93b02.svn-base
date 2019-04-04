package com.tts.fa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.tts.fa.app.TtsTradAirAdapterApp;
import com.tts.protocol.config.MessageConfiguration;
import com.tts.util.config.UtilConfiguration;

@Configuration
@Import({UtilConfiguration.class, MessageConfiguration.class})
public class FaConfiguration {

	@Bean
	public TtsTradAirAdapterApp app() {
		return new TtsTradAirAdapterApp();
	}
}
