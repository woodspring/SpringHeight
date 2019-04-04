package com.tts.mas.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ReutersBeanConfigCondition implements Condition {

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metaData) {
		String activateReutersTest = ctx.getEnvironment().getProperty("ACTIVATE.REUTERS.TEST");
		
		if((activateReutersTest == null) || ((activateReutersTest != null) && (activateReutersTest.trim().equalsIgnoreCase("N"))))
			return(true);
		
		return(false);
	}
}

class ReutersBeanTestConfigCondition implements Condition {

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metaData) {
		String activateReutersTest = ctx.getEnvironment().getProperty("ACTIVATE.REUTERS.TEST");
				
		if((activateReutersTest != null) && (activateReutersTest.trim().equalsIgnoreCase("Y")))
			return(true);
		
		return(false);
	}
}