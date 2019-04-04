package com.tts.mlp.data.provider;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.google.gson.Gson;
import com.tts.mlp.data.provider.vo.InstrumentDefinitionVo;
import com.tts.mlp.data.provider.vo.InstrumentRateVo;
import com.tts.mlp.data.provider.vo.MarketRawDataVo;
import com.tts.mlp.data.provider.vo.MarketRawDataVo.RawDataType;
import com.tts.util.collection.formatter.DoubleFormatter;

public class MarketDataProvider implements IMarketRawDataProvider {
	public static final Pattern pattern = Pattern.compile("(\\S{6}) (\\S{2}) FWD");
//	public static final String[] symbols= new String[] { "USDTRY", "EURUSD", "EURTRY"}; 
	public static final String[] symbols1= new String[] { 
			"AUDUSD", 
			"NZDUSD", 
			"CHFUSD",
			"USDKRW", "USDRUB", "USDCAD", "USDJPY", "USDNOK", "USDSGD", "USDTRY",
			"EURUSD", "EURTRY", "EURJPY", "EURCHF", "EURGBP", "EURNZD", "EURCAD", "EURNOK",
			"GBPUSD", "GBPCAD", "GBPTRY", "GBPCHF", "GBPJPY" 
	}; 
	public static final String[] symbols2 = new String[] { "USDCAD", "USDJPY", "USDCHF", "USDNOK", "USDSEK", "AUDUSD",
			"NZDUSD", "AUDCHF", "AUDJPY", "CHFJPY", "CHFNOK", "EURUSD", "EURCAD", "EURCHF", "EURGBP", "EURJPY",
			"EURNOK", "EURSEK", "GBPUSD", "GBPCAD", "GBPNOK", "GBPSEK", "NOKJPY", "SEKJPY", "NZDCAD", "NZDJPY", "NZDNOK",
			"NZDSEK", "USDTRY", "EURTRY", "GBPTRY", "USDZAR", "USDKRW"
	};
	public static final String[] symbols=symbols2;

	private final MarketDataStore store;
	private volatile long refreshTimestamp;
	
	public MarketDataProvider() {
		this.store = new MarketDataStore();
		this.refreshTimestamp = System.currentTimeMillis();
	}
	
	/* (non-Javadoc)
	 * @see com.tts.mlp.data.provider.IMarketDataProvider#refresh()
	 */
	@Override
	public void refresh() throws FileNotFoundException, InterruptedException {
		HtmlUnitDriver unitDriver = new HtmlUnitDriver();
		List<MarketRawDataVo> spot_data = new ArrayList<>();
		List<MarketRawDataVo> fwd_data = new ArrayList<>();
		for (String s : symbols) {
			try {
				List<MarketRawDataVo> sym_data = grepSwapPointsAndSpotRate(s, unitDriver);
				for ( MarketRawDataVo r : sym_data) {
					if ( r.getType() == RawDataType.SPOT_RATE) {
						spot_data.add(r);
					} else if  (r.getType() == RawDataType.SWAP_POINTS) {
						fwd_data.add(r);
					}
				}
				Thread.sleep(3300);
			} catch (Exception e ) {
				
			}
		}
		persistData(spot_data, fwd_data);
		this.refreshTimestamp = System.currentTimeMillis();
	}
	
	/* (non-Javadoc)
	 * @see com.tts.mlp.data.provider.IMarketDataProvider#getSpotData(java.lang.String)
	 */
	@Override
	public MarketRawDataVo getSpotData(String symbol){ 
		return store.getSpotData(symbol);
	}
	
	/* (non-Javadoc)
	 * @see com.tts.mlp.data.provider.IMarketDataProvider#getFwdData(java.lang.String)
	 */
	@Override
	public List<MarketRawDataVo> getFwdData(String symbol){ 
		return store.getFwdData(symbol);
	}	
	
	private void persistData(List<MarketRawDataVo> spot_data, List<MarketRawDataVo> fwd_data) {
		store.updateAllSpotData(spot_data);
		store.updateAllFwdData(fwd_data);		
	}

	private static List<MarketRawDataVo> grepSwapPointsAndSpotRate(String symbol,  HtmlUnitDriver unitDriver) throws FileNotFoundException, InterruptedException {
		 List<MarketRawDataVo> all_data = new ArrayList<>();
		 String formattedSymbol = symbol.toLowerCase();
		formattedSymbol = formattedSymbol.substring(0, 3) + "-" + formattedSymbol.substring(3,6);
		// open google.com webpage
		unitDriver.get("https://ca.investing.com/currencies/" + formattedSymbol + "-forward-rates");
		Thread.sleep(1000);

        
		// find the search edit box on the google page
		WebElement table = unitDriver.findElement(By.id("curr_table"));
		WebElement tableBody = table.findElement(By.tagName("tbody"));
		List<WebElement> records = tableBody.findElements(By.tagName("tr"));
		
		for ( WebElement row :records) {
			
			List<WebElement> elements = row.findElements(By.tagName("td"));
			String line = elements.get(1).getText();
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				String bidPoints= elements.get(2).getText();
				String askPoints= elements.get(3).getText();
				String tenor = matcher.group(2);
				
				if ( "SW".equals(tenor)) {
					tenor  = "1W";
				}
				if ( tenor != null ){
					all_data.add(new MarketRawDataVo(RawDataType.SWAP_POINTS, symbol, tenor, new BigDecimal(bidPoints).doubleValue(), -1.0d, new BigDecimal(askPoints).doubleValue()));
				} 
			}
		}

		WebElement quotes_summary_secondary_data = unitDriver.findElement(By.id("quotes_summary_secondary_data"));
		Pattern p = Pattern.compile("Bid/Ask: (\\S+) / (\\S+)");
		
		Matcher m = p.matcher(quotes_summary_secondary_data.getText());
		String bidRate = null, askRate = null;
		if ( m.find()) {
			bidRate = m.group(1);
			askRate = m.group(2);
		}

		WebElement spotRate = unitDriver.findElement(By.id("last_last"));
		String spotRateStr = spotRate.getText();
		all_data.add(new MarketRawDataVo(RawDataType.SPOT_RATE, symbol, new BigDecimal(bidRate).doubleValue(), new BigDecimal(spotRateStr).doubleValue(), new BigDecimal(askRate).doubleValue()));
		return all_data;
	}
	
	public void generateFileOldFormat() {
		PrintWriter out = null, hout = null, secondaryOut = null, spotOut = null;
		try {
			out = new PrintWriter("fc.csv");
			hout = new PrintWriter("harmoni_fc.csv");
			secondaryOut = new PrintWriter("bidAskSpread.txt");
			spotOut = new PrintWriter("random.seed.data.txt");

			
			for ( String symbol: symbols) {
				int pointValue = 4;
				int swapPrecision = 3;
				if ( symbol.endsWith("JPY")) {
					pointValue = 2;
				}
				MarketRawDataVo spotData = this.getSpotData(symbol);
				if ( spotData != null ) {
					spotOut.println(String.format("%s,%s", symbol, spotData.getMid()));
					double bidAskSpread = (spotData.getAsk() - spotData.getBid()) * Math.pow(10.0, pointValue);
					secondaryOut.println(String.format("%s,%s",symbol, DoubleFormatter.convertToString(bidAskSpread, 0, RoundingMode.HALF_EVEN)));
				} else {
					System.out.println(symbol + " has no spot data");
				}
				
				BigDecimal onBidPoints = null;
				BigDecimal onAskPoints = null;
				boolean hasHarmoniFavour = false;
				List<MarketRawDataVo> fwdData = this.getFwdData(symbol);

				if ( fwdData != null ) {
					for ( MarketRawDataVo data: fwdData ) {
						String bidValue = DoubleFormatter.convertToString(data.getBid(), swapPrecision, RoundingMode.FLOOR);
						String askValue = DoubleFormatter.convertToString(data.getAsk(), swapPrecision, RoundingMode.CEILING);
						if ( "ON".equals(data.getName2()) ) {
							onBidPoints = new BigDecimal(bidValue);
							onAskPoints = new BigDecimal(askValue);
							hasHarmoniFavour = true;
						} 
						out.println(String.format("<%s%s=>,%s,%s", 
								symbol, 
								data.getName2(), 
								bidValue,
								askValue));
	
						if ( hasHarmoniFavour ) {
							if ( "ON".equals(data.getName2()) ) {
								hout.println(String.format("<%s%s=>,%s,%s", 
										symbol, 
										data.getName2(), 
										bidValue,
										askValue));
							} else  {
								String hbidPoints = new BigDecimal(bidValue).add(onBidPoints).toString();
								String haskPoints = new BigDecimal(askValue).add(onAskPoints).toString();
								hout.println(String.format("<%s%s=>,%s,%s", 
										symbol, 
										data.getName2(), 
										hbidPoints,
										haskPoints));
							}
						}
					}
				} else {
					System.out.println(symbol + " has no swap points data");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if ( hout != null) {
					hout.flush();
					hout.close();
				}
			} catch (Exception e)  { 
				
			}
			try {
				if ( out != null) {
					out.flush();
					out.close();
				}
			} catch (Exception e)  { 
				
			}
			try {
				if ( secondaryOut != null) {
					secondaryOut.flush();
					secondaryOut.close();
				}
			} catch (Exception e)  { 
				
			}
			try {
				if ( spotOut != null) {
					spotOut.flush();
					spotOut.close();
				}
			} catch (Exception e)  { 
				
			}
		}
		
		

	}

	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		MarketDataProvider p = new MarketDataProvider();
		//p.refresh();
		//p.generateFileOldFormat();
		InstrumentDefinitionVo id = new InstrumentDefinitionVo(1, "USDCAD",  5,  4);
		id.setLqyStructure(new long[] { 1000000, 3000000, 5000000, 7000000, 10000000, 20000000});
		SimulatedInstrumentRateVo r = new SimulatedInstrumentRateVo(id);
		r.randomWalk(p.getSpotData("USDCAD"));
		
		Gson gson = new Gson();
		System.out.println(gson.toJson(r));
	}

	@Override
	public long getDataRefreshTimestamp() {
		return refreshTimestamp;
	}
}
