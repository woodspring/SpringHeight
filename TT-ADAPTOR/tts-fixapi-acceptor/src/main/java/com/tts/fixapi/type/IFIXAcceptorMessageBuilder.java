package com.tts.fixapi.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Hashtable;

import quickfix.Message;


public interface IFIXAcceptorMessageBuilder {
	
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MessageBuilder	{
	
	}
	
	public Message buildExecutionReport(quickfix.fix44.ExecutionReport message, Hashtable<String, Object> param);
	
	public Message buildExecutionReport(quickfix.fix50.ExecutionReport message, Hashtable<String, Object> param);
	
	public Message buildOrderCancelReject(quickfix.fix44.OrderCancelReject message, Hashtable<String, Object> param);
	
	public Message buildOrderCancelReject(quickfix.fix50.OrderCancelReject message, Hashtable<String, Object> param);
}
