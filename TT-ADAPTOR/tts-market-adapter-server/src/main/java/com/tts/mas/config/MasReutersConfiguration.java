package com.tts.mas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.reuters.adapter.ReutersMockAdapter;
import com.tts.reuters.adapter.ReutersRFAAdapter;

@Configuration
public class MasReutersConfiguration {

	@Bean
	@Conditional(ReutersBeanConfigCondition.class)
	public IReutersApp reutersRFAAdapter()	{		
		return new ReutersRFAAdapter();
	}
	
	@Bean
	@Conditional(ReutersBeanTestConfigCondition.class)
	public IReutersApp reutersMockAdapter()	{
		return new ReutersMockAdapter();
	}
}