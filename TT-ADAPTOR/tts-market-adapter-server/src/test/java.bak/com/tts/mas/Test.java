//package com.tts.mas;
//
//import com.tts.fix.support.FixApplicationProperties;
//import com.tts.message.eas.rate.RateConverter;
//import com.tts.message.market.FullBookStruct.FullBook;
//import com.tts.plugin.adapter.impl.base.util.VWAPUtil;
//import com.tts.plugin.adapter.impl.ykb.dialect.YkbResponseDialectHelper;
//import com.tts.vo.TickBookVo;
//
//import quickfix.DataDictionary;
//
//public class Test {
//	public static void main(String[] args) throws Exception {
//		FixApplicationProperties p = new FixApplicationProperties();
//		p.setProperty("spot.vwap", "true");
//		
//		String qMsg = "8=FIX.4.49=49335=W34=9949=TRADAIR-FIXGATEWAY52=20170130-17:57:51.39856=TTradingTEST-STREAMING55=EUR/USD262=705110000000000006268=10269=0270=1.0675615=EUR271=2000000269=0270=1.067515=EUR271=3000000269=0270=1.0674915=EUR271=3000000269=0270=1.067415=EUR271=5000000269=0270=1.0673715=EUR271=5000000269=1270=1.0675715=EUR271=1000000269=1270=1.0675815=EUR271=1000000269=1270=1.0677115=EUR271=6000000269=1270=1.0677715=EUR271=5000000269=1270=1.0677815=EUR271=500000010=017";
//		String qMsg2 = "8=FIX.4.49=45735=W34=245349=TRADAIR-FIXGATEWAY52=20170130-17:58:43.69856=TTradingTEST-STREAMING55=USD/JPY262=705110000000000004268=9269=0270=113.58715=USD271=1000000269=0270=113.58615=USD271=4000000269=0270=113.58515=USD271=5000000269=0270=113.58115=USD271=3000000269=1270=113.59515=USD271=1000000269=1270=113.615=USD271=3000000269=1270=113.60115=USD271=1000000269=1270=113.60215=USD271=3000000269=1270=113.60315=USD271=500000010=009";
//		String qMsg3 = "8=FIX.4.49=88135=W34=498549=TRADAIR-FIXGATEWAY52=20170208-07:23:22.49956=TTradingTEST-STREAMING55=USD/JPY262=706030000000027961268=10269=0270=112.30815=USD271=8500000272=20170208273=07:23:22.444282=Agg269=0270=112.30715=USD271=17500000272=20170208273=07:23:22.444282=Agg269=0270=112.30615=USD271=29500000272=20170208273=07:23:22.444282=Agg269=0270=112.30515=USD271=17150000272=20170208273=07:23:22.444282=Agg269=0270=112.30415=USD271=25500000272=20170208273=07:23:22.444282=Agg269=1270=112.31215=USD271=1500000272=20170208273=07:23:22.444282=Agg269=1270=112.31315=USD271=1000000272=20170208273=07:23:22.444282=Agg269=1270=112.31415=USD271=7500000272=20170208273=07:23:22.444282=Agg269=1270=112.31515=USD271=6500000272=20170208273=07:23:22.444282=Agg269=1270=112.31615=USD271=8000000272=20170208273=07:23:22.444282=Agg10=174";
//		
//        DataDictionary dd = new DataDictionary( "app-resources/TradAirFIX44.xml" );
//
//        dd.setCheckUnorderedGroupFields( true );
//        quickfix.fix44.MarketDataSnapshotFullRefresh messageEURUSD = new quickfix.fix44.MarketDataSnapshotFullRefresh();
//        messageEURUSD.fromString( qMsg, dd, true );
//        quickfix.fix44.MarketDataSnapshotFullRefresh messageUSDJPY = new quickfix.fix44.MarketDataSnapshotFullRefresh();
//        messageUSDJPY.fromString( qMsg2, dd, true );
//        quickfix.fix44.MarketDataSnapshotFullRefresh messageUSDJPY2= new quickfix.fix44.MarketDataSnapshotFullRefresh();
//        messageUSDJPY2.fromString( qMsg3, dd, true );
//        
//        YkbResponseDialectHelper h = new YkbResponseDialectHelper(p);
//        FullBook.Builder fbBEURUSD = FullBook.newBuilder();
//        h.convertAndUpdate(messageEURUSD, fbBEURUSD);
//        FullBook.Builder fbBUSDJPY = FullBook.newBuilder();
//        h.convertAndUpdate(messageUSDJPY, fbBUSDJPY);
//        FullBook.Builder fbBUSDJPY2 = FullBook.newBuilder();
//        h.convertAndUpdate(messageUSDJPY2, fbBUSDJPY2);
//        
//        
//        TickBookVo tickBookEURUSD = RateConverter.convertFullBookToTickBook(fbBEURUSD.build());
//        TickBookVo tickBookUSDJPY = RateConverter.convertFullBookToTickBook(fbBUSDJPY.build());
//        TickBookVo tickBookUSDJPY2 = RateConverter.convertFullBookToTickBook(fbBUSDJPY2.build());
//
//        //System.out.println(tickBookEURUSD.fullDebugString());
//        System.out.println(tickBookUSDJPY2.fullDebugString());
//
//        TickBookVo vwapEURUSD = VWAPUtil.calculateVWAP(tickBookEURUSD, new long[] { 1000000, 2000000, 3000000, 4000000,  5000000, 10000000, 15000000 } );
//        //System.out.println(vwapEURUSD.fullDebugString());
//
//        TickBookVo vwapUSDJPY2 = VWAPUtil.calculateVWAP(tickBookUSDJPY2, new long[] { 500000, 1000000, 2000000, 3000000, 4000000,  5000000, 8000000, 10000000, 20000000 } );
//        System.out.println(vwapUSDJPY2.fullDebugString());
//
//	}
//
//
//}
