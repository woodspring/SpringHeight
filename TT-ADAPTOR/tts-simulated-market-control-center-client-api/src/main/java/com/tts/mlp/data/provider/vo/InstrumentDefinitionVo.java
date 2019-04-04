package com.tts.mlp.data.provider.vo;

public class InstrumentDefinitionVo {
	private static final long[] DEFAULT_LQY_STRUCTURE = new long[] { 1000000, 3000000, 5000000, 7000000, 10000000, 20000000} ;
	private final String symbol;
	private final int spotPrecision;
	private final int pointValue;
	private final int id;
	
	private volatile long[] lqyStructure;

	public InstrumentDefinitionVo(int id, String symbol, int spotPrecision, int pointValue) {
		super();
		this.symbol = symbol;
		this.spotPrecision = spotPrecision;
		this.pointValue = pointValue;
		this.id = id;
	}

	public void setLqyStructure(long[] lqyStructure) {
		this.lqyStructure = lqyStructure;
	}

	public  String getSymbol() {
		return symbol;
	}

	public  int getSpotPrecision() {
		return spotPrecision;
	}

	public  int getPointValue() {
		return pointValue;
	}

	public  int getId() {
		return id;
	}

	public  long[] getLqyStructure() {
		if ( lqyStructure == null ) {
			return DEFAULT_LQY_STRUCTURE;
		}
		return lqyStructure;
	}

	
}
