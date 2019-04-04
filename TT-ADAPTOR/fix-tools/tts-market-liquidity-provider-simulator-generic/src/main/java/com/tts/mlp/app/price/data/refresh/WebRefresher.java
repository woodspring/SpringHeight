package com.tts.mlp.app.price.data.refresh;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.app.price.data.IUpdatableMarketPriceProvider;
import com.tts.mlp.data.provider.MarketDataFetcher;
import com.tts.mlp.data.provider.vo.InstrumentRateVo;
import com.tts.mlp.data.provider.vo.TickEntry;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.Tick;


public class WebRefresher implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(WebRefresher.class);
	private static final String[] DEFAULT_SYMBOLS = new String[] { "USDCAD", "USDJPY", "USDCHF", "USDNOK", "USDSEK", "AUDUSD",
			"NZDUSD", "AUDCHF", "AUDJPY", "CHFJPY", "CHFNOK", "EURUSD", "EURCAD", "EURCHF", "EURGBP", "EURJPY",
			"EURNOK", "EURSEK", "GBPCAD", "GBPNOK", "GBPSEK", "NOKJPY", "SEKJPY", "NZDCAD", "NZDJPY", "NZDNOK",
			"NZDSEK", "USDTRY", "EURTRY", "GBPTRY" 
	};
	
	private static final boolean FORCE_REFRESH;
	
	private volatile int  failedCount = 0;
	private volatile long lastServerChangeTime = -1;

    private final List<String> symbols;
    private final IUpdatableMarketPriceProvider localPriceProvider;
    private final ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
    
    static { 	
    	String forceRefreshStr = System.getenv("MD_FORCE_REFRESH");
    	if ( forceRefreshStr == null || forceRefreshStr.length() == 0) {
    		forceRefreshStr = System.getProperty("md.force_refresh");
    		if ( forceRefreshStr == null || forceRefreshStr.length() == 0) {
    			forceRefreshStr = "false";
    		}
    	} 
    	FORCE_REFRESH = Boolean.parseBoolean(forceRefreshStr);
    }
    
    public WebRefresher(IUpdatableMarketPriceProvider localPriceProvider) {
    	this.symbols = Arrays.asList(DEFAULT_SYMBOLS);
    	this.localPriceProvider = localPriceProvider;
    	s.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
    }
      
    public void destroy() {
    	s.shutdownNow();
    }
    
	public void doRefresh() throws org.springframework.web.client.ResourceAccessException {
		boolean refreshRequired = false;
		MarketDataFetcher fetcher = new MarketDataFetcher();
		Long l  = fetcher.getRefreshTimestamp();
		if ( l != null && l.longValue() > 0 ) {
			if ( this.lastServerChangeTime != l.longValue()) {
				refreshRequired = true;
			}
			lastServerChangeTime = l.longValue();
		}
		if ( FORCE_REFRESH || refreshRequired ) {
			logger.info("Refreshing Market Data...");
			
			for (String symbol : this.symbols ) {
				InstrumentRateVo d = fetcher.getSpotData(symbol);
							
				Instrument c = localPriceProvider.getCurrentPrice(symbol);
		    	Instrument i = new Instrument(c);
		    			    	
		    	i.setTobMidPrice(new BigDecimal(d.getTobMidPrice()).doubleValue());
		    	for ( Tick te : i.getBidTicks()) {
		    		double rate = findGTAPriceBID(te.getQuantity(), d);
		    		te.setPrice(rate);
		    	}
		    	for ( Tick te : i.getAskTicks()) {
		    		double rate = findGTAPriceASK(te.getQuantity(), d);
		    		te.setPrice(rate);
		    	}
			    localPriceProvider.updateMarketBook(symbol, i);
			}
			
		} else {
			logger.info("No new update of market data...");
		}
		failedCount = 0;
	} 
	
	private double findGTAPriceASK(Double quantity, InstrumentRateVo d) {
    	for ( TickEntry te : d.getAskTicks()) {
    		if ( te.getQuantity() >= quantity) {
    			return new BigDecimal(te.getPrice()).doubleValue();
    		}
    	}
    	return 0;
	}

	private double findGTAPriceBID(double quantity, InstrumentRateVo d) {
    	for ( TickEntry te : d.getBidTicks()) {
    		if ( te.getQuantity() >= quantity) {
    			return new BigDecimal(te.getPrice()).doubleValue();
    		}
    	}
    	return 0;
	}

	@Override
	public void run() {
		try {
			doRefresh();
		} catch ( org.springframework.web.client.ResourceAccessException e) {
			failedCount++;
			if ( (failedCount % 100) == 1) {
				String msg = "Unable to refresh Market data...";
				logger.error(msg);
				System.err.println(msg);
			}
		}
		
	}
	
}
