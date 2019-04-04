package com.tts.plugin.adapter.impl.base.app;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;

public class QuoteStackRepo {
	public final static String PREFIX_FOR_USE_SUB_QID = "MULTIQ";

	private final static int DEFAULT_STACK_SIZE = 120;
	private final ConcurrentHashMap<String, QuoteStack> storage 
						= new ConcurrentHashMap<String, QuoteStack>(100);
	
	public void addQuote(final String quoteReqId, final QuoteVo quote) {
		storage.compute(quoteReqId, new BiFunction<String, QuoteStack, QuoteStack>() {

			@Override
			public QuoteStack apply(String arg0, QuoteStack arg1) {
				QuoteStack workingWithStack = arg1;
				if ( arg1 == null ) {
					workingWithStack = new QuoteStack(DEFAULT_STACK_SIZE);
				}
				workingWithStack.addQuote(quote);
				return workingWithStack;
			}
			
		});
	}
	
	public final QuoteVo findQuote(final String quoteReqId, final String quoteId) {
		QuoteStack stack = storage.get(quoteReqId);
		if ( stack == null ) return null;
		
		return stack.findQuote(quoteId);
	}
	
	public final void removeQuoteByRef(final String quoteRefId){
		storage.remove(quoteRefId);
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder();
		
		for ( Entry<String, QuoteStack> e: storage.entrySet() ) {
			QuoteStack q = e.getValue();
			sb.append('\n');
			sb.append(e.getKey()).append(':');
			for ( int i = 0; i < q.size; i++ ) {
				sb.append(q.array[i].getQuoteId()).append(' ');
			}
		}
		sb.append('\n');
		return sb.toString();
	}
	
	
	private static class QuoteStack {
		
		private final AtomicLong idxProvider;
		private final QuoteVo[] array;
		private final int size;
		
		QuoteStack(int size) {
			this.size = size;
			this.idxProvider = new AtomicLong(0);
			this.array = new QuoteVo[size];
		}
		
		private void addQuote(QuoteVo quote) {
			final int idx = (int) (idxProvider.getAndIncrement() % size);
			array[idx] = quote;
		}
		
		private QuoteVo findQuote(String quoteId) {
			long currentIdx = idxProvider.get();
			boolean found = false;
			QuoteVo rte = null;
			for ( int i = 0; !found &&  i <= size; i++ ) {
				int idx = (int) ((currentIdx - i) % size);
				if ( array[idx] != null ) {
					if (quoteId.equals(array[idx].getQuoteId()) ) {
						rte = array[idx].deepClone();
						found = true;
					} 
				}
			}
			return rte;
		}
	}
	
	
}
