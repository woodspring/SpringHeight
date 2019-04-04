package com.tts.mlp.data.provider;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.tts.mlp.data.provider.vo.MarketRawDataVo;
import com.tts.util.collection.FixedWidthFileStore;

public class MarketDataStore {
	final FixedWidthFileStore spotStore;
	final FixedWidthFileStore fwdStore;
	final HashMap<String, MarketRawDataVo> spotData = new HashMap<>();
	final HashMap<String, ArrayList<MarketRawDataVo>> fwdData = new HashMap<>();

	public MarketDataStore() {
		FixedWidthFileStore _spotStore = null;
		FixedWidthFileStore _fwdStore = null;
		try {
			_spotStore = new FixedWidthFileStore(160, "dataStore/spot.store");
			_fwdStore = new FixedWidthFileStore(175, "dataStore/fwd.store");
		} catch (FileNotFoundException e) {

		}
		this.spotStore = _spotStore;
		this.fwdStore = _fwdStore;
		
		reloadFromStore(_spotStore, _fwdStore);
	}

	private void reloadFromStore(FixedWidthFileStore _spotStore, FixedWidthFileStore _fwdStore) {
		List<String>  spotDataInFile = _spotStore.getAllEntries();
		Gson gson = new Gson();
		for (String s : spotDataInFile) {
			MarketRawDataVo raw = gson.fromJson(s.trim(), MarketRawDataVo.class);
			spotData.put(raw.getName(), raw);
		}
		
		List<String> fwdDataInFile = _fwdStore.getAllEntries();
		for (String s : fwdDataInFile ) {
			MarketRawDataVo raw = gson.fromJson(s.trim(), MarketRawDataVo.class);
			if (fwdData.get( raw.getName()) == null ) {
				fwdData.put(raw.getName(), new ArrayList<MarketRawDataVo>());
			}
			fwdData.get( raw.getName()).add(raw);
		}
	}

	public synchronized void updateAllSpotData(List<MarketRawDataVo> all_data) {
		int i = 0;
		spotData.clear();
		Gson gson = new Gson();
		for ( i = 0; i <all_data.size(); i++ ) {
			MarketRawDataVo value = all_data.get(i);
			spotData.put(value.getName(),value );
			spotStore.updateEntry(i, gson.toJson(value));
		}
	}

	public synchronized void updateAllFwdData(List<MarketRawDataVo> all_data) {
		int i = 0;
		fwdData.clear();
		Gson gson = new Gson();

		for ( i = 0; i <all_data.size(); i++ ) {
			MarketRawDataVo marketRawDataVo = all_data.get(i);
			if ( fwdData.get(marketRawDataVo.getName()) == null )  {
				fwdData.put(marketRawDataVo.getName(), new ArrayList<MarketRawDataVo>());
			}
			fwdData.get(marketRawDataVo.getName()).add(marketRawDataVo);
			fwdStore.updateEntry(i, gson.toJson(marketRawDataVo));
		}
	}

	public synchronized MarketRawDataVo getSpotData(String symbol) {
		return spotData.get(symbol);
	}

	public synchronized List<MarketRawDataVo> getFwdData(String symbol) {
		return fwdData.get(symbol);
	}

}
