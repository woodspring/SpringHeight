package com.tts.mlp.data.provider;

import java.io.FileNotFoundException;
import java.util.List;

import com.tts.mlp.data.provider.vo.MarketRawDataVo;

public interface IMarketRawDataProvider {

	void refresh() throws FileNotFoundException, InterruptedException;

	MarketRawDataVo getSpotData(String symbol);

	List<MarketRawDataVo> getFwdData(String symbol);
	
	long getDataRefreshTimestamp();

}
