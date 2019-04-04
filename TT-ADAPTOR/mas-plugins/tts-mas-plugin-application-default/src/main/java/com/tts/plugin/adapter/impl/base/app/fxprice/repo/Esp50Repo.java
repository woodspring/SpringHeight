package com.tts.plugin.adapter.impl.base.app.fxprice.repo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fix.support.util.ConfigProperty;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.support.vo.IEspRepo;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.fix50.MarketDataSnapshotFullRefresh;

public class Esp50Repo implements IEspRepo<quickfix.fix50.MarketDataSnapshotFullRefresh> {
	private static final Logger logger = LoggerFactory.getLogger(Esp50Repo.class);
	
	private static final boolean DEBUG = ConfigProperty.getSystemProperty("MARKET_REPO_DEBUG", true);
	
	@SuppressWarnings("unused")
	private final int lastLookSize;
	
	Map<String, Map<String, FbArrayStore>> store;
	
	public Esp50Repo(int size, String[] symbols, String[] allTenors) {
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
		boolean found = false;
		Map<String, FbArrayStore> symbolStore = this.store.get(symbol);
		if ( symbolStore != null ) {
			FbArrayStore s = symbolStore.get(tenor);
			if ( s != null ) {
				s.registerNewPrice(fb, src);
				found = true;
			}
		}
		if ( !found ) {
			if ( symbolStore == null ) {
				logger.debug("symbolStore is null for " + symbol);
			} else {
				logger.debug("tenorStore is null !?! for " + symbol);
			}
		}	
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
		StringBuilder sb = null;
		if ( DEBUG && "USDCAD".equals(symbol)) {
			sb= new StringBuilder(String.format("Looking up %s\n", quoteRefId));
		}
		FbArrayStore fbArrayStore = this.store.get(symbol).get(tenor);
		long lastSeq = fbArrayStore.getConsumerSeq();
		IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh>[] all = fbArrayStore.getStore();
		long lastTarget = lastSeq - all.length;
		for ( long l = lastSeq; l > lastTarget; l-- ) {
			int i = (int) (l % all.length);
			IEspRepo.FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh> a = all[i];
			synchronized ( a ) {
				if ( a.getFullBook() == null || a.getSource() == null ) {
					continue;
				}
				if ( DEBUG && sb != null ) {
					sb.append(i ).append("         ");
					sb.append(a.getFullBook().getQuoteRefId());
				}
				quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntry = new quickfix.fix50.MarketDataSnapshotFullRefresh.NoMDEntries();
				String qId = null;
				try {
					a.getSource().getGroup(1, noMDEntry);
					qId = noMDEntry.getMDEntryID().getValue();
					if ( DEBUG && sb != null ) {
						sb.append("->").append(qId);
					}
				} catch (FieldNotFound e) {
					e.printStackTrace();
				}

				if ( a.getFullBook() != null && quoteRefId.equals(a.getFullBook().getQuoteRefId() ) ) {
					if ( DEBUG && sb != null ) {
						sb.append("*");
					}
					if ( DEBUG && sb != null ) {
						logger.debug(sb.toString());
					}
					if ( quoteRefId.contains(qId)) {
						return a;
					}
				}
				if ( DEBUG && sb != null ) {
					sb.append("\n");
				}
			}
		}
		if (DEBUG && sb != null ) {
			logger.debug(sb.toString());
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
			s.setFullBookAndSource(fb, src);
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
		
		long getConsumerSeq() {
			return consumerSeq;
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
