package com.tts.plugin.adapter.support.vo;

/**
 * This object is the wrapper class to holding any market data, that's not
 * directly from the market data provider, usually it's an intermediate format.
 * For example, when the IFormatConversionHandler splits the original market
 * data by symbols, this object will be the wrapped market data for individual
 * symbols that was split by the IFormatConversionHandler
 * 
 *  
 */
public class ContextVo {

	public String symbol;

	public Object context;

	public Class<?> contextType;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Object getContext() {
		return context;
	}

	public void setContext(Object context) {
		this.context = context;
	}

	public Class<?> getContextType() {
		return contextType;
	}

	public void setContextType(Class<?> contextType) {
		this.contextType = contextType;
	}

}
