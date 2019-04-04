package com.tts.plugin.adapter.impl.base.app.fxprice.repo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.vo.TenorVo;

import quickfix.fix44.MarketDataSnapshotFullRefresh;

public class Esp44Repo implements IEspRepo<quickfix.fix44.MarketDataSnapshotFullRefresh> {
	@SuppressWarnings("unused")
	private final int lastLookSize;
	
	Map<String, Map<String, FbArrayStore>> store;
	
	public Esp44Repo(int size, String[] symbols, String[] allTenors) {
		Map<String, Map<String, FbArrayStore>> _store = new HashMap<String, Map<String, FbArrayStore>>(symbols.length);
		for ( String symbol: symbols) {
			Map<String, FbArrayStore> individualTenor = new HashMap<String, FbArrayStore>();
			for ( String stdTenor : allTenors) {
				individualTenor.put(stdTenor, new FbArrayStore(size, TenorVo.NOTATION_SPOT.equals(stdTenor) && "USDCAD".equals(symbol)));
			}
			_store.put(symbol,  Collections.unmodifiableMap(individualTenor));
		}
		this.store = Collections.unmodifiableMap(_store);
		this.lastLookSize = size;
	}

	@Override
	public void registerPrice(String symbol, String tenor, FullBook.Builder fb, quickfix.fix44.MarketDataSnapshotFullRefresh src) {
		this.store.get(symbol).get(tenor).registerNewPrice(fb, src);;
	}

	@Override
	public IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>[] findPriceBySymbolTenor(
			String symbol, String tenor) {
		return this.store.get(symbol).get(tenor).getStore();
	}
	
	
	
	@Override
	public IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> findPriceBySymbolTenorQuoteIdSeq(
			String symbol, String tenor, String quoteRefId, long sequence) {
		return findPriceBySymbolTenorQuoteId(symbol, tenor, quoteRefId);
	}

	@Override
	public IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> findPriceBySymbolTenorQuoteId(
			String symbol, String tenor, String quoteRefId) {
		IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>[] all = this.store.get(symbol).get(tenor).getStore();
		for ( IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> a: all ) {
			if ( a.getFullBook() != null && quoteRefId.equals(a.getFullBook().getQuoteRefId() ) ) {
				return a;
			}
		}
		return null;
	}
	
	static class FbArrayStore {
		final boolean debugFlag;
		final int size;
		final IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>[] store;
		final AtomicLong producerSeq = new AtomicLong(1);
		volatile long consumerSeq = 0;
		
		FbArrayStore(int size) {
			this(size, false);
		}
		
		FbArrayStore(int size, boolean debug) {
			this.debugFlag = debug;
			this.size = size;
			this.store = newFbArrayStore(size);
		}
		
		public IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>[] getStore() {
			return store;
		}
		
		public IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> getLatestEsp() {
			long currseq = consumerSeq;
			int idx = (int) (currseq % size);

			IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> s = store[idx];
			if(s != null) {
				return(s);
			}
			return(null);
		}

		void registerNewPrice(FullBook.Builder fb, quickfix.fix44.MarketDataSnapshotFullRefresh src) {
			long currSeq = producerSeq.getAndIncrement();
			IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> s = store[(int) (currSeq % size)];
			s.setFullBookAndSource(fb, src);
			if ( currSeq > consumerSeq) {
				consumerSeq = currSeq;
			}
		}
		
		@SuppressWarnings("unchecked")
		static IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>[] newFbArrayStore(int size) {
			 IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>[] s = new IEspRepo.FullBookSrcWrapper[size];
			 for ( int i = 0; i < size; i++ ) {
				 s[i] = new IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh>();
			 }
			 return s;
		}
		
	}

	@Override
	public com.tts.plugin.adapter.support.vo.IEspRepo.FullBookSrcWrapper<MarketDataSnapshotFullRefresh> findLatestPriceBySymbol(String symbol) {
		FbArrayStore fbStore = store.get(symbol).get("SPOT");
		IEspRepo.FullBookSrcWrapper<quickfix.fix44.MarketDataSnapshotFullRefresh> mktData = fbStore.getLatestEsp();
		
		if(mktData != null)
			return(mktData);
		
		return(null);
	}
}
