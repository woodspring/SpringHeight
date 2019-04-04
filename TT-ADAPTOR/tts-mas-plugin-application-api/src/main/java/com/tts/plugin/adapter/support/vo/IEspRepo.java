package com.tts.plugin.adapter.support.vo;

import com.tts.message.market.FullBookStruct.FullBook;

public interface IEspRepo<T> {

	void registerPrice(String symbol, String tenor, FullBook.Builder fb, T src);
	
	FullBookSrcWrapper<T>[] findPriceBySymbolTenor(String symbol, String tenor);
	
	FullBookSrcWrapper<T> findPriceBySymbolTenorQuoteIdSeq(String symbol, String tenor, String quoteRefId, long sequence);
	
	FullBookSrcWrapper<T> findPriceBySymbolTenorQuoteId(String symbol, String tenor, String quoteRefId);
	
	FullBookSrcWrapper<T> findLatestPriceBySymbol(String symbol);
	
	public static class FullBookSrcWrapper<T> {
		
		volatile FullBook.Builder fb;
		volatile T src;
					
		public FullBook.Builder getFullBook() {
			return fb;
		}

		public T getSource() {
			return src;
		}

		public void setFullBookAndSource(FullBook.Builder fb, T src) {
			this.fb = fb;
			this.src = src;
		}
		
	}

}
