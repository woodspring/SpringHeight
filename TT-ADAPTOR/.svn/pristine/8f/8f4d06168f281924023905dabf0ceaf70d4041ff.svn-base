package com.tts.mas;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.service.biz.calendar.FxCalendarBizServiceImpl;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.util.AppUtils;

public class TestConfiguration {
	@Bean
    public FixApplicationProperties applicationProperties() throws IOException, quickfix.ConfigError {
    	String env = AppUtils.getActiveEnvironment();
    	String appCategory = System.getProperty("APP_CATEGORY", "all");
    	String configPath = null;
    	if ( appCategory == null || "all".equals(appCategory) ) {
    		configPath = String.format("env-resources/adapter/adapterConfig_%s.properties", env);
    	} else {
    		configPath = String.format("env-resources/adapter/adapterConfig_%s.properties", env);

    		//configPath = String.format("env-resources/adapter/adapterConfig_%s_%s.properties", env, appCategory.toLowerCase());
    	}
    	FixApplicationProperties p = new FixApplicationProperties();
		InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(configPath);
		p.load(is);;
		is.close();
    	
		return p;    	
    }
	
	@Bean
	public IFxCalendarBizService fxCalendarBizService() {
		return new FxCalendarBizServiceImpl();
	}
}
