package com.tts.plugin.adapter.impl.base.app.fxprice.repo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.fix50.MarketDataSnapshotFullRefresh;

public class Esp50RepoV2 implements IEspRepo<quickfix.fix50.MarketDataSnapshotFullRefresh> {
	
	private static final Logger logger = LoggerFactory.getLogger(Esp50RepoV2.class);
	@SuppressWarnings("unused")
	private final int lastLookSize;
	
	Map<String, Map<String, FbArrayStore>> store;
	
	public Esp50RepoV2(int size, String[] symbols, String[] allTenors) {
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
	public void registerPrice(String symbol, String tenor, FullBook.Builder fb, quickfix.fix50.MarketDataSnapshotFullRefresh src) {
		this.store.get(symbol).get(tenor).registerNewPrice(fb, src);;
	}

	@Override
	public IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] findPriceBySymbolTenor(
			String symbol, String tenor) {
		return this.store.get(symbol).get(tenor).getStore();
	}
	
	
	
	@Override
	public IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> findPriceBySymbolTenorQuoteIdSeq(
			String symbol, String tenor, String quoteRefId, long sequence) {
		return findPriceBySymbolTenorQuoteId(symbol, tenor, quoteRefId);
	}

	@Override
	public IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> findPriceBySymbolTenorQuoteId(
			String symbol, String tenor, String quoteRefId) {
		IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] all = this.store.get(symbol).get(tenor).getStore();
		for ( IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> a: all ) {
			synchronized (a) {
				if ( a.getFullBook() != null && quoteRefId.equals(a.getFullBook().getQuoteRefId() ) ) {
					return a;
				}
			}
		}
		return null;
	}
	
	static class FbArrayStore {
		final boolean debugFlag;
		final int size;
		final IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] store;
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
		
		public IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] getStore() {
			if ( debugFlag  ) {
				StringBuilder sbFb = new StringBuilder();
				
				StringBuilder sbFix = new StringBuilder();

				for ( IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> s : store) {
					sbFb.append(s.getFullBook().getQuoteRefId()).append(',');
					quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
					try {
						s.getSource().getGroup(1, noMDEntry);
						sbFix.append(noMDEntry.getQuoteEntryID().getValue()).append(',');

					} catch (FieldNotFound e) {
						e.printStackTrace();
					}
				}
				
				logger.debug(sbFb.toString() + '\n' + sbFix.toString());

			}
			return store;
		}
		
		public IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> getLatestEsp() {
			long currseq = consumerSeq;
			int idx = (int) (currseq % size);

			IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> s = store[idx];
			if(s != null) {
				return(s);
			}
			return(null);
		}

		void registerNewPrice(FullBook.Builder fb, quickfix.fix50.MarketDataSnapshotFullRefresh src) {
			long currSeq = producerSeq.getAndIncrement();
			IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> s = store[(int) (currSeq % size)];
			
			synchronized (s) {
				s.setFullBookAndSource(fb, src);
			}
			if ( currSeq > consumerSeq) {
				consumerSeq = currSeq;
			}
		}
		
		@SuppressWarnings("unchecked")
		static IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] newFbArrayStore(int size) {
			 IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] s = new IEspRepo.FullBookSrcWrapper[size];
			 for ( int i = 0; i < size; i++ ) {
				 s[i] = new IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>();
			 }
			 return s;
		}
		
	}

	@Override
	public com.tts.plugin.adapter.support.vo.IEspRepo.FullBookSrcWrapper<MarketDataSnapshotFullRefresh> findLatestPriceBySymbol(String symbol) {
		FbArrayStore fbStore = store.get(symbol).get("SPOT");
		IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> mktData = fbStore.getLatestEsp();
		
		if(mktData != null)
			return(mktData);
		
		return(null);
	}
}
