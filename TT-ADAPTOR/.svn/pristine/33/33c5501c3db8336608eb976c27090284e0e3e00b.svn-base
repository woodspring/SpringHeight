package com.tts.mlp.app.price.data.refresh;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.tts.mlp.app.price.data.IUpdatableMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.Tick;


public class WebRefresher implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(WebRefresher.class);
	private static final String REST_SERVICE_URI;
	private static final String[] DEFAULT_SYMBOLS = new String[] { "USDCAD", "USDJPY", "USDCHF", "USDNOK", "USDSEK", "AUDUSD",
			"NZDUSD", "AUDCHF", "AUDJPY", "CHFJPY", "CHFNOK", "EURUSD", "EURCAD", "EURCHF", "EURGBP", "EURJPY",
			"EURNOK", "EURSEK", "GBPCAD", "GBPNOK", "GBPSEK", "NOKJPY", "SEKJPY", "NZDCAD", "NZDJPY", "NZDNOK",
			"NZDSEK", "USDTRY", "EURTRY", "GBPTRY" 
	};
	
	private volatile int  failedCount = 0;
	private volatile long lastServerChangeTime = -1;

    private final List<String> symbols;
    private final IUpdatableMarketPriceProvider localPriceProvider;
    private final ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
    
    static {
    	String refreshTarget = System.getenv("MD_REFRESH_TARGET");
    	if ( refreshTarget == null || refreshTarget.length() == 0) {
    		refreshTarget = System.getProperty("md.refresh.target");
    		if ( refreshTarget == null || refreshTarget.length() == 0) {
    			refreshTarget = "localhost:18080";
    		}
    	} 
    	REST_SERVICE_URI = refreshTarget;
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
		RestTemplate restTemplate = new RestTemplate();
		Long l  = restTemplate.getForObject("http://" + REST_SERVICE_URI+"/getDataRefreshTimestamp", Long.class);
		if ( l != null && l.longValue() > 0 ) {
			if ( this.lastServerChangeTime != l.longValue()) {
				refreshRequired = true;
			}
			lastServerChangeTime = l.longValue();
		}
		if ( refreshRequired ) {
			logger.info("Refreshing Market Data with " + REST_SERVICE_URI);
			for (String symbol : this.symbols ) {
				InstrumentRateVo d = restTemplate.getForObject("http://" + REST_SERVICE_URI+"/getSpotData/" + symbol, InstrumentRateVo.class);
							
				Instrument c = localPriceProvider.getCurrentPrice(symbol);
		    	Instrument i = new Instrument(c);
		    	
		    	i.getBidTicks().clear();
		    	i.getAskTicks().clear();
		    	
		    	i.setTobMidPrice(new BigDecimal(d.getTobMidPrice()).doubleValue());
		    	long[] lqLevels = new long[d.getBidTicks().size()];
		    	int k=0;
		    	for ( TickEntry te : d.getBidTicks()) {
		    		int level = k++;
		    		lqLevels[level] = te.getQuantity();
		    		Tick t = new Tick(new BigDecimal(te.getPrice()).doubleValue(), new BigDecimal(te.getQuantity()).doubleValue(), level);
		    		i.getBidTicks().add(t);
		    	}
		    	k=0;
		    	for ( TickEntry te : d.getAskTicks()) {
		    		int level = k++;
		    		Tick t = new Tick(new BigDecimal(te.getPrice()).doubleValue(), new BigDecimal(te.getQuantity()).doubleValue(), level);
		    		i.getAskTicks().add(t);
		    	}
			    i.setLqLevels(lqLevels);	
			    localPriceProvider.updateMarketBook(symbol, i);
			}
			
		} else {
			logger.info("No new update of market data from " + REST_SERVICE_URI);
		}
		failedCount = 0;
	} 
	
	@Override
	public void run() {
		try {
			doRefresh();
		} catch ( org.springframework.web.client.ResourceAccessException e) {
			failedCount++;
			if ( (failedCount % 100) == 1) {
				String msg = "Unable to refresh Market data with " + REST_SERVICE_URI + ". Check Property, MD_REFRESH_TARGET";
				logger.error(msg);
				System.err.println(msg);
			}
		}
		
	}
	
}
