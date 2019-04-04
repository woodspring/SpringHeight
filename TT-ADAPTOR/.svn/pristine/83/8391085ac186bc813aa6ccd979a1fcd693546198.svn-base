package com.tts.mas.function;

import java.util.function.Function;

import com.tts.plugin.adapter.api.dialect.IMkRequestDialectHelper;

public class RequestIdBuilderFunction implements Function<Long, String> {
	
	private final String symbol;
	private final String tenor;
	private final IMkRequestDialectHelper dialect;
	
	public RequestIdBuilderFunction(String symbol, String tenor, IMkRequestDialectHelper dialect) {
		super();
		this.symbol = symbol;
		this.tenor = tenor;
		this.dialect = dialect;
	}

	@Override
	public String apply(Long t) {
		return dialect.buildRequestId(t, symbol, tenor);
	}

}
