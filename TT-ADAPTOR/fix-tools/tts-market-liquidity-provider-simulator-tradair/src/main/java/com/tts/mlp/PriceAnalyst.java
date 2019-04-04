package com.tts.mlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.tts.util.chronology.ChronologyUtil;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.InvalidMessage;
import quickfix.StringField;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

public class PriceAnalyst {

	public static class StatVo {

		private long maxGap = -1;
		private String symbol;
		private String requestId;
		public long last = -1;
		public long start = -1;
		private long seq = -1;
	}

	public static void main(String[] args) throws InvalidMessage, ConfigError, FieldNotFound {

		BufferedReader br = null;

		try {

			String sCurrentLine;
			DataDictionary dd = new DataDictionary("app-resources/TTFIX44.xml");

			br = new BufferedReader(new FileReader("C:\\Users\\LAWRENCE.TT\\Desktop\\Logs22\\tradair-0422.log"));
			HashMap<String, StatVo> requestStatMap = new HashMap<String, StatVo>(3000);
			while ((sCurrentLine = br.readLine()) != null) {
				try {
					if (sCurrentLine.indexOf("35=W") >0 ){
						quickfix.fix44.MarketDataSnapshotFullRefresh r = new MarketDataSnapshotFullRefresh();
						r.fromString(sCurrentLine, dd, false);
						String sender = r.getHeader().getField(new StringField(49)).getValue();
						String msgSeq = r.getHeader().getString(34);
						String reqId = r.getMDReqID().getValue();
						String symbol = r.getSymbol().getValue();
						if ("TRADAIR-FIXGATEWAY".equals(sender)) {
							long sentTime = r.getHeader().getUtcTimeStamp(52).getTime();
							StatVo stat = requestStatMap.get(reqId);
							if (stat == null) {
								stat = new StatVo();
								stat.symbol = symbol;
								stat.requestId = reqId;
								requestStatMap.put(reqId, stat);
							}
							stat.symbol = symbol;
							if (stat.last > 0) {
								long gap = sentTime - stat.last;
								if ( gap >  ChronologyUtil.MILLIS_IN_SECOND  *5) {
									System.out.println(gap + "\t\t" + sCurrentLine);
								}
								if ( gap > stat.maxGap ) {
									stat.maxGap = gap;
									stat.seq = Long.parseLong(msgSeq);
								}
							}
							stat.last = sentTime;
							if (stat.start == -1) {
								stat.start = sentTime;
							}
						}
					}
				} catch (FieldNotFound e) {

				}

			}
			for ( Entry<String, StatVo> e :requestStatMap.entrySet()) {
				StringBuilder s = new StringBuilder();
				s.append(e.getValue().symbol).append(' ');
				s.append(e.getValue().requestId).append(' ');
				s.append(e.getValue().start).append(' ');
				s.append(e.getValue().maxGap).append('(').append(e.getValue().seq).append(") ");
				s.append(e.getValue().last);
				System.out.println(s.toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

}
