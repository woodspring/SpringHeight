package com.tts.mlp.data.provider;

import org.springframework.web.client.RestTemplate;

import com.tts.mlp.data.provider.vo.InstrumentRateVo;
import com.tts.mlp.data.provider.vo.InstrumentSwapPointsVo;

public class MarketDataFetcher {

    
	public InstrumentRateVo getSpotData(String symbol) {
		RestTemplate restTemplate = new RestTemplate();

		return  restTemplate.getForObject("http://" + REST_SERVICE_URI+"/getSpotData/" + symbol, InstrumentRateVo.class);
	}
	
	public InstrumentSwapPointsVo getSwapPoints(String symbol) {
		
		RestTemplate restTemplate = new RestTemplate();

		return  restTemplate.getForObject("http://" + REST_SERVICE_URI+"/getSwapPoints/" + symbol, InstrumentSwapPointsVo.class);
	}
	
	public Long getRefreshTimestamp() {
		RestTemplate restTemplate = new RestTemplate();

		return restTemplate.getForObject("http://" + REST_SERVICE_URI+"/getDataRefreshTimestamp", Long.class);
	}
	
	private static final String REST_SERVICE_URI;

	
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
}
