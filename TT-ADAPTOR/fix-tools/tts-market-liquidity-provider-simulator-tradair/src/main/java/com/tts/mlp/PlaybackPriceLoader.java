package com.tts.mlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

import com.tts.mlp.app.price.data.IMarketPriceProvider;
import com.tts.mlp.price.data.Fix44MarketPriceProvider;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.InvalidMessage;

public class PlaybackPriceLoader {
	private final static int DEFAULT_MAX_PLAYBACK_LOAD_COUNT = 50000;
	
	private final static int MAX_PLAYBACK_LOAD_COUNT;
	
	static {
		int i = DEFAULT_MAX_PLAYBACK_LOAD_COUNT;
		String maxBP = System.getProperty("MAX_PLAYBACK_LOAD");
		if ( maxBP == null ) {
			i = DEFAULT_MAX_PLAYBACK_LOAD_COUNT;
		} else {
			try {
				i = Integer.parseInt(maxBP);
			} catch (Exception e) {
				i = DEFAULT_MAX_PLAYBACK_LOAD_COUNT;
			}
		}
		MAX_PLAYBACK_LOAD_COUNT = i;
	}
	
	public IMarketPriceProvider<quickfix.fix44.MarketDataSnapshotFullRefresh> buildFixPriceProvider(String resourceLocation, DataDictionary dd) {
		HashMap<String, LinkedList<quickfix.fix44.MarketDataSnapshotFullRefresh>> repo = new HashMap<String, LinkedList<quickfix.fix44.MarketDataSnapshotFullRefresh>>();
		URL u = Thread.currentThread().getContextClassLoader().getResource(resourceLocation);
		if ( "file".equals(u.getProtocol())) {
			int loadCount = 0;
			try ( BufferedReader br = new BufferedReader(new FileReader(new File(u.toURI()))))  {
				String line = null;
				while ( loadCount < MAX_PLAYBACK_LOAD_COUNT && (line = br.readLine()) != null) {
					if ( line.indexOf("35=W") > 0 ) {
				        quickfix.fix44.MarketDataSnapshotFullRefresh message = new quickfix.fix44.MarketDataSnapshotFullRefresh();
				        try {
							message.fromString( line, dd, true );
							
							String symbol = message.getSymbol().getValue();
							LinkedList<quickfix.fix44.MarketDataSnapshotFullRefresh> data = repo.get(symbol);
							if (data == null ) {
								data = new LinkedList<quickfix.fix44.MarketDataSnapshotFullRefresh>();
								repo.put(symbol, data);
							}
							data.add(message);
							loadCount++;
						} catch (InvalidMessage e) {
							e.printStackTrace();
						} catch (FieldNotFound e) {
							e.printStackTrace();
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
		}
		return Fix44MarketPriceProvider.from(repo);
	}

	public static void main(String[] args) throws ConfigError {
        DataDictionary dd = new DataDictionary( "app-resources/TradAirFIX44.xml" );

        dd.setCheckUnorderedGroupFields( true );

        PlaybackPriceLoader l = new PlaybackPriceLoader();
        IMarketPriceProvider p = l.buildFixPriceProvider("defaultPlayBack.log", dd);


	}

}
