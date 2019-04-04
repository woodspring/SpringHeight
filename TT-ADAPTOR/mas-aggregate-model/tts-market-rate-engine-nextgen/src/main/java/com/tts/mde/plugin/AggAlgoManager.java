package com.tts.mde.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tts.mde.algo.IMDPriceAndExceAlgo;
import com.tts.util.plugin.AbstractPlugInManager;

public class AggAlgoManager extends AbstractPlugInManager<IMDPriceAndExceAlgo> {

	private final Map<String, IMDPriceAndExceAlgo> algoMap;
	
	public AggAlgoManager() {
		super(IMDPriceAndExceAlgo.class);
		
		HashMap<String, IMDPriceAndExceAlgo> _algoMap = new HashMap<>();
		
		if ( loader != null ) {
			for (IMDPriceAndExceAlgo algo : loader ) {
				_algoMap.put(algo.getName(), algo);
			}
		}
		algoMap = Collections.unmodifiableMap(_algoMap);
	}

	public IMDPriceAndExceAlgo getAlgoByName(String name) {
		return algoMap.get(name);
	}
}
