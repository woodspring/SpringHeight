package com.tts.mlp.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mlp.app.price.tenor.TenorActualDateRelationshipManager;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.vo.TenorVo;

public class ForwardCurveDataManager {
	private static final ZoneId defaultTimeZone = ZoneId.of("America/New_York");
	private final static Logger logger = LoggerFactory.getLogger(ForwardCurveDataManager.class);
	private static final String CSV_COMMENT_PREFIX = "#";
	private static final String CSV_COLUMN_SEPERATOR = ",";
	
	
	
	private static com.tts.mlp.app.price.tenor.TenorActualDateRelationshipManager tenorActualDateRelationshipManager; 
	
	private static String pathToFile = null;
	static boolean keepRunning = true;

	static WatchService watchService;
	
	static HashMap<String,double[]> fcMap = null;
	static Map<String,List<SwapPointsMaturityDateHolder>> fcDateDataMap = null;

	public ForwardCurveDataManager() {
		logger.info("Constructor called");
	}
	public static void start(String pathToFileParam) {
		pathToFile = pathToFileParam;
		// Try and open file ... if does not exist then don't bother starting..its as if start has never been called
		if (pathToFile == null) {
			return;
		}
		try {
			File newFile = new File(pathToFile);
			if (newFile.exists() == false) {
				return;
			}
			loadFromFile(newFile);
			tenorActualDateRelationshipManager = new TenorActualDateRelationshipManager(fcMap.keySet().toArray(new String[0]));

			Thread t = new Thread() {
				@Override
				public void run() {
					Thread.currentThread().setName("file-watcher");
					logger.info("Watching file "+pathToFile);
					try {
						watchService = FileSystems.getDefault().newWatchService();
						Path path = Paths.get(newFile.getParent());
						path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
								StandardWatchEventKinds.ENTRY_MODIFY);
						while (keepRunning) {
							WatchKey key = watchService.take();
							for (WatchEvent<?> watchEvent : key.pollEvents()) {
								final Kind<?> kind = watchEvent.kind();

								if (kind == StandardWatchEventKinds.OVERFLOW) {
									continue;
								}

								@SuppressWarnings("unchecked")
								final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
								final Path entry = watchEventPath.context();

								if (entry.endsWith(newFile.getName())) {
									loadFromFile(newFile);
								}
							}
							key.reset();
							if (!key.isValid()) {
								break;
							}
						}
					} catch (Throwable t) {

					}
				}
			};
			t.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void destroy() {
		logger.info("destroy called");
		stop();
	}
	
	static void stop() {
		keepRunning = false;
		if (watchService != null) {
			try {
				watchService.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	static void loadFromFile(File file) {
		int adjustment = 0;
		HashMap<String,double[]> fcMapNew = new HashMap<String,double[]>();
		HashMap<String,List<SwapPointsMaturityDateHolder>> fcDatesMapNew = new HashMap<String,List<SwapPointsMaturityDateHolder>>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)))) {

			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0 || line.startsWith(CSV_COMMENT_PREFIX)) {
					continue;
				} else {
					String[] parts = line.replace("<", "").replace("=>", "").split("\\,");
					if(parts.length == 3) {
						//Need to look up the scaling for the currency pair..
						
						double[] rates = new double[2];
						try {
							String symbolTenor = parts[0];
							symbolTenor = symbolTenor.trim();
							rates[0] = Double.parseDouble(parts[1]);
							rates[1] = Double.parseDouble(parts[2]);
							fcMapNew.put(symbolTenor, rates);
							int dayToMaturity = 0;
							String symbol = symbolTenor.substring(0, 6);
							String tenor = symbolTenor.substring(6);
							if ( "SN".equals(tenor)) {
								tenor = "1D";
							} else if ( "SW".equals(tenor)) {
								tenor = "1W";
							}
							
							if ( tenor.endsWith("D")) {
								dayToMaturity = Integer.parseInt(tenor.replace("D", ""));
							} else if (tenor.endsWith("W")) {
								dayToMaturity = Integer.parseInt(tenor.replace("W", "")) * 7;
							} else if (tenor.endsWith("M")) {
								dayToMaturity = Integer.parseInt(tenor.replace("M", "")) * 30;
							} else if (tenor.endsWith("Y")) {
								dayToMaturity = Integer.parseInt(tenor.replace("Y", "")) * 365;
							} 
							if ( dayToMaturity > 0 ) {
								dayToMaturity += adjustment;
								if (fcDatesMapNew.get(symbol) == null ) {
									fcDatesMapNew.put(symbol, new ArrayList<SwapPointsMaturityDateHolder>());
								}
								if ( "USDCAD".equals(symbol) || "USDTRY".equals(symbol) || "USDRUB".equals(symbol)) {
									dayToMaturity += 1;
								} else {
									dayToMaturity += 2;
								}
								fcDatesMapNew.get(symbol).add(new SwapPointsMaturityDateHolder(dayToMaturity, rates));
							}
						} catch (Throwable t) {
							continue;
						}
					}
				}
			}

		} catch (Exception e) {
		}

		fcDateDataMap = fcDatesMapNew;
		
		fcMap = fcMapNew;
	}
	
	public static double[] getSwapPoints(String symbol, LocalDate d) { 
		int days = ChronologyUtil.daysBetween(LocalDate.now(defaultTimeZone), d);
		List<SwapPointsMaturityDateHolder> symbolData = fcDateDataMap.get(symbol);
		SwapPointsMaturityDateHolder nearData = null, farData = null;
		for ( SwapPointsMaturityDateHolder data: symbolData) {
			if ( data.getDays() <= days) {
				nearData = data;
			}
			if ( data.getDays() >= days) {
				farData = data;
				break;
			}
		}
		if ( nearData!= null && farData != null && nearData == farData ) {
			return nearData.getSwapPoints();
		} else if ( nearData!= null && farData != null ) {
			int nearDays = days - nearData.days;
			int farDays = farData.days - days;
			return new double[] { (nearData.getSwapPoints()[0] * farDays + farData.getSwapPoints()[1] * nearDays) / (nearDays + farDays), (nearData.getSwapPoints()[1] * farDays + farData.getSwapPoints()[1] * nearDays) / (nearDays + farDays) };
		}
		
		double[] dd = new double[] { 100.0d , 200.0d };
		return dd;
	}
	
	public static double[] getSwapPoints(String symbol, String tenor) {
		double[] d =  fcMap.get(symbol+tenor);
		if ( d == null ) {
			if ( "1W".equals(tenor) ) {
				d=   fcMap.get(symbol+"SW");
			}
		}
		return d;
	}

	public static double GetFwdPoints(String symbol, String tenor, char side) {
		double[] rates = fcMap.get(symbol+tenor);
		if(rates == null) {
			return 0.0;
		}
		if(tenor.equals(TenorVo.NOTATION_TOMORROWNIGHT) == false && tenor.equals(TenorVo.NOTATION_OVERNIGHT) == false) {
			//Turn ascii '0' or '1' into a useful number
			return rates[side-'0'];
		} else {
			if(tenor.equals(TenorVo.NOTATION_TOMORROWNIGHT)) {
				if(side == '0')
					return -1 * rates[1];
				else
					return -1 * rates[0];
			} else {
				//If its ON then look for a TN and add in its impact
				double result;
				if(side == '0')
					result = -1 * rates[1];
				else
					result = -1 * rates[0];
				double[] ratesTN = fcMap.get(symbol+TenorVo.NOTATION_TOMORROWNIGHT);
				if(ratesTN!= null) {
					if(side == '0')
						result = result + (-1 * ratesTN[1]);
					else
						result = result + (-1 * ratesTN[0]);
				}
//				if(symbol.equals("GBPUSD")) {
//					logger.info((side == '0' ? "bid ": "ask ")+" cumulative pts = "+result);
//				}
				return result;
			}
		}
	}

	public static void registerSymbolTenorDate(String symbol, String tenor, String actualDate) {
		tenorActualDateRelationshipManager.registerSymbolTenorDate(symbol, tenor, actualDate);
	}

	public static String findTenorByDate(String symbol, String actualDate) {
		return tenorActualDateRelationshipManager.findTenorByDate(symbol, actualDate);
	}

	static class SwapPointsMaturityDateHolder {
		final int days;
		final double[] swapPoints;
		
		public SwapPointsMaturityDateHolder(int days, double[] swapPoints) {
			super();
			this.days = days;
			this.swapPoints = swapPoints;
		}
		public int getDays() {
			return days;
		}
		public double[] getSwapPoints() {
			return swapPoints;
		}
		
		
		
	}
}
