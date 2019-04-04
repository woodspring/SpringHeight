package com.tts.mlp.data.provider;

import com.tts.mlp.data.provider.vo.InstrumentRateVo;
import com.tts.mlp.data.provider.vo.InstrumentSwapPointsVo;
import com.tts.mlp.data.provider.vo.SwapPointEntityVo;

public class TestMain {

	public static void mainFwd(String[] args) {
		System.setProperty("md.refresh.target", "192.168.11.138:18080");
		MarketDataFetcher fetcher = new  		MarketDataFetcher();
		InstrumentSwapPointsVo data = fetcher.getSwapPoints("EURGBP");
		
		SwapPointEntityVo e = data.getFwdPointsEntries().get(0);;
		System.out.println(e.getAskSwapPoint());
	}
	public static void mainSpot(String[] args) {
		System.setProperty("md.refresh.target", "192.168.11.138:18080");
		MarketDataFetcher fetcher = new  		MarketDataFetcher();
		InstrumentRateVo data = fetcher.getSpotData("USDCAD");
		
		System.out.println(data.getBidTicks().get(0).getPrice());
		
		
	}
	
	public static void main(String[] args) {
		mainFwd(args);
	}
}
