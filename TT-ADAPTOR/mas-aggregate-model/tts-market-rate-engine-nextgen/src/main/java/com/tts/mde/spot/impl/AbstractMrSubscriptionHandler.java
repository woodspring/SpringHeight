package com.tts.mde.spot.impl;

import java.math.RoundingMode;
import java.util.List;

import com.tts.mde.spot.ILiquidityPool;
import com.tts.mde.spot.IMrEndpoint;
import com.tts.mde.vo.RawLiquidityVo;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.util.collection.formatter.DoubleFormatter;

public  abstract class AbstractMrSubscriptionHandler {
	
	private final String handlerId;

	private final ILiquidityPool liquidityPool;
	private final IMrEndpoint mrEndpoint;
	private final MrSubscriptionProperties properties;
	
	public AbstractMrSubscriptionHandler(
			String handlerId,
			IMrEndpoint mrEndpoint, 
			MrSubscriptionProperties properties, 
			ILiquidityPool liquidityPool) {
		this.handlerId = handlerId;
		this.liquidityPool = liquidityPool;
		this.mrEndpoint = mrEndpoint;
		this.properties = properties;
	}
	
	public abstract void onPublish(long masGlobalSeq);

	public ILiquidityPool getLiquidityPool() {
		return liquidityPool;
	}
		
	public MrSubscriptionProperties getProperties() {
		return properties;
	}

	public IMrEndpoint getMrEndpoint() {
		return mrEndpoint;
	}

	public String getHandlerId() {
		return handlerId;
	}

	protected FullBook.Builder createRawBookFromRawLqy(List<RawLiquidityVo> bids, List<RawLiquidityVo> asks, int instrumentPrecision) {
		FullBook.Builder book = FullBook.newBuilder();
		for (RawLiquidityVo bid: bids) {
			book.addBidTicks(convertTo(bid,instrumentPrecision));
		}
		for (RawLiquidityVo ask: asks) {
			book.addAskTicks(convertTo(ask,instrumentPrecision));
		}
		return book;
		
	}
	
	private static Tick.Builder convertTo(RawLiquidityVo raw, int instrumentPrecision) {
		return convertTo(instrumentPrecision, raw, RoundingMode.UNNECESSARY);
	}
	
	private static Tick.Builder convertTo(int precision, RawLiquidityVo raw, RoundingMode rm) {
		Tick.Builder t = Tick.newBuilder();
		t.setRate(DoubleFormatter.convertToString(raw.getRate(), precision, rm));
		t.setSize(raw.getSize());
		if ( raw.getLiquidityProviderSrc() != null) {
			t.setSourceNm(raw.getLiquidityProviderSrc());
		}
		return t;
	}
	
}
