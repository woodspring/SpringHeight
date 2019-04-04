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
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.vo.TenorVo;

public class ForwardCurveDataManager {
	private final static Logger logger = LoggerFactory.getLogger(ForwardCurveDataManager.class);
	private static final String CSV_COMMENT_PREFIX = "#";
	private static final String CSV_COLUMN_SEPERATOR = ",";

	static String pathToFile = null;
	static boolean keepRunning = true;

	static WatchService watchService;
	
	static HashMap<String,double[]> fcMap = null;

	public static void main(String[] args) {
		start(args[0]);
		
		stop();
	}

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
		HashMap<String,double[]> fcMapNew = new HashMap<String,double[]>();
		
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
							rates[0] = Double.parseDouble(parts[1]);
							rates[1] = Double.parseDouble(parts[2]);
							fcMapNew.put(parts[0], rates);
						} catch (Throwable t) {
							continue;
						}
					}
				}
			}

		} catch (Exception e) {
		}

		
		
		fcMap = fcMapNew;
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

}
