package com.tts.fixapi.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Message;


public class FIXAcceptorMessageBuilderInvoker {
	private static final Logger logger = LoggerFactory.getLogger("FixAPILogger");
	private final Object invokeTarget;
	private final Method invokeMethod;
	
	public FIXAcceptorMessageBuilderInvoker(Object target, Method method)	{
		this.invokeTarget = target;
		this.invokeMethod = method;
	}

	public Object getInvokeTarget() {
		return(invokeTarget);
	}

	public Method getInvokeMethod() {
		return(invokeMethod);
	}
	
	public Message invokeMethod(Hashtable<String, Object> tagValues) 
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException	{
		Message fixMessage = null;
		
		logger.debug("Invoking Method " + invokeMethod.getName() + " Param " + invokeMethod.getParameterTypes()[0].getTypeName());
		Object methodResponse = invokeMethod.invoke(invokeTarget, (invokeMethod.getParameterTypes()[0]).newInstance(), tagValues);
		if(methodResponse != null)
			fixMessage = (Message)methodResponse;
		
		return(fixMessage);
	}
	
	
	
	@SuppressWarnings("serial")
	public class RedundantHandlerException extends RuntimeException {
		private final Class<?> messageClass;
		private final Method originalMethod;
		private final Method redundantMethod;

		public RedundantHandlerException(Class<?> messageClass, Method originalMethod, Method redundantMethod) {
			this.messageClass = messageClass;
			this.originalMethod = originalMethod;
			this.redundantMethod = redundantMethod;
		}

		@Override
		public String getMessage()	{
			return("Duplicate Message Builder Found for " + messageClass + ", Orginal Method: " + originalMethod 
					+ ", Redundant Method: " + redundantMethod);
		}
		
		@Override
		public String toString() {
			return("Duplicate Message Builder Found for " + messageClass + ", Orginal Method: " + originalMethod 
					+ ", Redundant Method: " + redundantMethod);
		}
	}
}
