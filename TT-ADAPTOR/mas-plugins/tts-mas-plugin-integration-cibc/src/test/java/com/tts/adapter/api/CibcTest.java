package com.tts.adapter.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import org.springframework.core.io.ClassPathResource;

import com.google.protobuf.TextFormat;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.plugin.adapter.impl.cibc.dialect.CibcResponseDialectHelper;
import com.tts.plugin.adapter.impl.cibc.validate.CIBCSpotPublishValidator;

import quickfix.DataDictionary;

public class CibcTest {

	public static void main(String[] args) throws Exception {
		ClassPathResource cpr = new ClassPathResource("CIBCFIX50.xml");
		DataDictionary dd = new DataDictionary(cpr.getFile().getAbsolutePath());
		dd.setCheckUnorderedGroupFields(true);
		CibcResponseDialectHelper h = new CibcResponseDialectHelper();

		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(new File("C:\\tmp\\20170526-QT\\server-FixMarketLogger.2017-05-26.1.log")));
		FullBook.Builder fbUSDCAD = FullBook.newBuilder();
		PrintWriter out = new PrintWriter("USDCAD@0526.txt");
		while ((line = br.readLine()) != null) {
			if( line.indexOf("35=W") > 0 && line.indexOf("55=USDCAD") > 0) {
				quickfix.fix50.MarketDataSnapshotFullRefresh messageUSDCAD = new quickfix.fix50.MarketDataSnapshotFullRefresh();
				messageUSDCAD.fromString( line, dd, true );
				try {
					h.convertAndUpdate(messageUSDCAD, fbUSDCAD);
					//out.println(TextFormat.shortDebugString(fbUSDCAD));
				} catch (Exception e ) {
					System.out.println("Exception for message at " + messageUSDCAD.getHeader().getString(52));
				}
			}
		}
	}
}
