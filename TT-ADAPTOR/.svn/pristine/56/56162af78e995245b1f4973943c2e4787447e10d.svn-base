package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.tts.message.latency.LatencyStruct.Latency;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.InterestRateCurveStruct.InterestRateCurve;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.app.IPublishingApp;
import  com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.util.AppContext;
import com.tts.util.flag.IndicativeFlag;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;

public class MasterPriceStore<TARGET_TYPE extends Message.Builder> {
	private final static Logger logger = LoggerFactory.getLogger(IPublishingApp.class);
    private final IndividualPriceStore<TARGET_TYPE>[] individualDataStores;

    private final ISymbolIdMapper symbolIdMapper;
	    
    @SuppressWarnings("unchecked")
	public MasterPriceStore(String[] symbols, AppType parentType) throws Exception {
    	
    	this.symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);
    	int size = Math.max(symbols.length, symbolIdMapper.getSymbols().size());
    	
    	this.individualDataStores = new IndividualPriceStore[size];

    	for (String symbol: symbols) {
    		int symbolId = symbolIdMapper.map(symbol);
    		IndividualPriceStore<TARGET_TYPE> priceStore = new IndividualPriceStore<TARGET_TYPE>(AppType.SPOTADAPTER == parentType && "USDCAD".equals(symbol));
   			priceStore.setStoreInitializerFunction(new StoreInitializer(symbol, parentType));
    		
    		priceStore.init();
    		individualDataStores[symbolId] = priceStore;
    	}
    }
    
	public <U> void applyToAll(BiFunction<Message.Builder, U, Message.Builder> f, U l) {
		final IndividualPriceStore<TARGET_TYPE>[] individualDataStores = this.individualDataStores;
		boolean showTimestamp =  (logger.isTraceEnabled() && f instanceof FbPricePublishHandler);
		
		StringBuilder sb = null;
		if ( showTimestamp ) {
			sb = new StringBuilder();
		}
		for (int i = 0; i < individualDataStores.length; i++ ) {
			long startTime = System.currentTimeMillis();
			
			if ( individualDataStores[i] != null ) {
				individualDataStores[i].updateLatest(f, l);
			}
			if ( sb != null ) {
				sb.append(i).append('=').append( (System.currentTimeMillis() - startTime) ).append(' ');
			}
		}
		if ( sb != null){
			logger.trace(sb.toString());
		}
	}
    
	public IndividualPriceStore<TARGET_TYPE> getPriceStore(String symbol) {
		final int symbolId = symbolIdMapper.map(symbol);
		return individualDataStores[symbolId];
	}
	




	private static class StoreInitializer implements Function<Message.Builder[], Message.Builder[]> {
		
		private final String symbol;
		private final AppType parentType;
		private final long defaultNoDataIndicativeFlag;
		
		private StoreInitializer(String symbol, AppType parentType) {
			this.symbol = symbol;
			this.parentType = parentType;
			
			long indicativeFlag = IndicativeFlag.setIndicative(IndicativeFlag.TRADABLE, IndicativeReason.MA_NoData);
			this.defaultNoDataIndicativeFlag = indicativeFlag;
		}

		@Override
		public Message.Builder[] apply(Message.Builder[] t) {
			for ( int i  = 0 ; i < t.length; i ++) {
				Latency.Builder lb = Latency.newBuilder();
				if ( AppType.SPOTADAPTER == parentType) {
					FullBook.Builder fbBuilder = FullBook.newBuilder();
					fbBuilder.setSymbol(symbol);
					fbBuilder.setLatency(lb);
					fbBuilder.setIndicativeFlag(defaultNoDataIndicativeFlag);
					t[i] = fbBuilder;
				} else if ( AppType.FCADAPTER == parentType) {
					ForwardCurve.Builder fcBuilder = ForwardCurve.newBuilder();
					fcBuilder.setSymbol(symbol);
					fcBuilder.setLatency(lb);
					fcBuilder.setIndicativeFlag(defaultNoDataIndicativeFlag);
					

					t[i] = fcBuilder;
				} else if ( AppType.IRADAPTER == parentType) {
					InterestRateCurve.Builder irBuilder = InterestRateCurve.newBuilder();
					irBuilder.setSymbol(symbol);
					irBuilder.setLatency(lb);
					irBuilder.setIndicativeFlag(defaultNoDataIndicativeFlag);

					t[i] = irBuilder;
				}
			}
			return t;
		}
		
	}
	
	
}
