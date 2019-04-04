//package com.tts.mas;
//
//import org.springframework.context.annotation.AnnotationConfigApplicationContext;
//
//import com.tts.mas.config.MasConfiguration;
//import com.tts.mas.manager.SessionInfoManager;
//import com.tts.mas.support.PublishingEndpointSystemOutImpl;
//import com.tts.message.market.FullBookStruct.FullBook;
//import com.tts.message.market.FullBookStruct.FullBook.Builder;
//import com.tts.message.market.FullBookStruct.Tick;
//import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
//import com.tts.plugin.adapter.impl.base.app.fxprice.FbPricePublishHandler;
//import com.tts.plugin.adapter.impl.cibc.dialect.CibcRequestDialectHelper;
//import com.tts.plugin.adapter.impl.cibc.dialect.CibcResponseDialectHelper;
//import com.tts.plugin.adapter.support.vo.SessionInfo;
//import com.tts.plugin.adapter.support.vo.IEspRepo.FullBookSrcWrapper;
//import com.tts.util.constant.TradeConstants.TradeAction;
//import com.tts.vo.NumberVo;
//
//import quickfix.DataDictionary;
//
//public class CibcTest {
//
//	public static void main(String[] args) throws Exception {
//		String qMsg = "8=FIXT.1.19=209735=W34=15349=BANK_ESP_PRICES52=20160412-23:35:53.38856=TTS_ESP_PRICES55=USDCAD167=FXSPOT262=USDCAD.SPOT.615100000000000152268=20269=0270=1.19394271=992000272=20160412276=A290=163=01026=1.193941027=0278=1460504153320269=0270=1.19389271=1992000272=20160412276=A290=263=01026=1.193891027=0278=1460504153320269=0270=1.19384271=2992000272=20160412276=A290=363=01026=1.193841027=0278=1460504153320269=0270=1.19379271=3992000272=20160412276=A290=463=01026=1.193791027=0278=1460504153320269=0270=1.19374271=4992000272=20160412276=A290=563=01026=1.193741027=0278=1460504153320269=0270=1.19349271=9992000272=20160412276=A290=663=01026=1.193491027=0278=1460504153320269=0270=1.19324271=14992000272=20160412276=A290=763=01026=1.193241027=0278=1460504153320269=0270=1.19299271=19992000272=20160412276=A290=863=01026=1.192991027=0278=1460504153320269=0270=1.19274271=24992000272=20160412276=A290=963=01026=1.192741027=0278=1460504153320269=0270=1.19249271=29992000272=20160412276=A290=1063=01026=1.192491027=0278=1460504153320269=1270=1.1941271=1000000272=20160412276=A290=163=01026=1.19411027=0278=1460504153320269=1270=1.19415271=2000000272=20160412276=A290=263=01026=1.194151027=0278=1460504153320269=1270=1.1942271=3000000272=20160412276=A290=363=01026=1.19421027=0278=1460504153320269=1270=1.19425271=4000000272=20160412276=A290=463=01026=1.194251027=0278=1460504153320269=1270=1.1943271=5000000272=20160412276=B290=563=01026=1.19431027=0278=1460504153320269=1270=1.19455271=10000000272=20160412276=B290=663=01026=1.194551027=0278=1460504153320269=1270=1.1948271=15000000272=20160412276=B290=763=01026=1.19481027=0278=1460504153320269=1270=1.19505271=20000000272=20160412276=B290=863=01026=1.195051027=0278=1460504153320269=1270=1.1953271=25000000272=20160412276=B290=963=01026=1.19531027=0278=1460504153320269=1270=1.19555271=30000000272=20160412276=B290=1063=01026=1.195551027=0278=146050415332010=100";
////		String cibcQMsg = "8=FIXT.1.19=181935=W49=CIBC_QUESTRADE_MKD_UAT56=QUESTRADE_MKD_UAT115=QUESTRADE128=QUESTRADE34=9521552=20160413-13:37:59.631262=USDCAD.SPOT.61508000000000003755=USDCAD167=FXSPOT268=14269=0278=IMYWJN3T116270=1.28011000271=500000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28011000269=0278=IMYWJN3T116270=1.28011000271=1000000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28011000269=0278=IMYWJN3T116270=1.28008000271=3000000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28008000269=0278=IMYWJN3T116270=1.28008000271=5000000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28008000269=0278=IMYWJN3T116270=1.28008000271=10000000272=20160413276=B299=IMYWJN3T11663=064=201604141026=1.28008000269=0278=IMYWJN3T116270=1.28003000271=20000000272=20160413276=B299=IMYWJN3T11663=064=201604141026=1.28003000269=0278=IMYWJN3T116270=1.27999000271=25000000272=20160413276=B299=IMYWJN3T11663=064=201604141026=1.27999000269=1278=IMYWJN3T116270=1.28015000271=500000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28015000269=1278=IMYWJN3T116270=1.28015000271=1000000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28015000269=1278=IMYWJN3T116270=1.28017000271=3000000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28017000269=1278=IMYWJN3T116270=1.28017000271=5000000272=20160413276=A299=IMYWJN3T11663=064=201604141026=1.28017000269=1278=IMYWJN3T116270=1.28038000271=10000000272=20160413276=B299=IMYWJN3T11663=064=201604141026=1.28038000269=1278=IMYWJN3T116270=1.28042000271=20000000272=20160413276=B299=IMYWJN3T11663=064=201604141026=1.28042000269=1278=IMYWJN3T116270=1.28046000271=25000000272=20160413276=B299=IMYWJN3T11663=064=201604141026=1.2804600010=144";
//		String cibcQMsg2 = "8=FIXT.1.19=179135=W49=CIBC_QUESTRADE_MKD_UAT56=QUESTRADE_MKD_UAT115=QUESTRADE128=QUESTRADE34=1567952=20160414-14:59:03.056262=USDCAD.SPOT.61517000000000004355=USDCAD167=FXSPOT268=14269=0278=IN0EVQOY24270=1.28379000271=500000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28379000269=0278=IN0EVQOY24270=1.28379000271=1000000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28379000269=0278=IN0EVQOY24270=1.28376000271=3000000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28376000269=0278=IN0EVQOY24270=1.28376000271=5000000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28376000269=0278=IN0EVQOY24270=1.28375000271=10000000272=20160414276=B299=IN0EVQOY2463=064=201604151026=1.28375000269=0278=IN0EVQOY24270=1.28372000271=20000000272=20160414276=B299=IN0EVQOY2463=064=201604151026=1.28372000269=0278=IN0EVQOY24270=1.28369000271=25000000272=20160414276=B299=IN0EVQOY2463=064=201604151026=1.28369000269=1278=IN0EVQOY24270=1.28383000271=500000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28383000269=1278=IN0EVQOY24270=1.28383000271=1000000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28383000269=1278=IN0EVQOY24270=1.28385000271=3000000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28385000269=1278=IN0EVQOY24270=1.28385000271=5000000272=20160414276=A299=IN0EVQOY2463=064=201604151026=1.28385000269=1278=IN0EVQOY24270=1.28386000271=10000000272=20160414276=B299=IN0EVQOY2463=064=201604151026=1.28386000269=1278=IN0EVQOY24270=1.28392000271=20000000272=20160414276=B299=IN0EVQOY2463=064=201604151026=1.28392000269=1278=IN0EVQOY24270=1.28395000271=25000000272=20160414276=B299=IN0EVQOY2463=064=201604151026=1.2839500010=248";
//		String cibcQMsg3q = "8=FIXT.1.19=036135=S49=CIBC_MKD56=TICKTRADE_MKD52=20170804-14:31:42.17834=2115=TICKTRADE128=TICKTRADE131=TTS731060000000000000117=J5XYXWL9338IMSIT1::5301=055=USDCAD167=FXSWAP64=20170815193=20170822134=1000000135=100000015=USD132=1.265082133=1.266312188=1.2654190=1.2663189=-0.000318191=0.0000126050=1.2655476051=1.265682642=-0.000753643=0.00028240=G10=213";
//
////		DataDictionary dd = new DataDictionary( "app-resources/TTFIX50.xml" );
//        DataDictionary dd = new DataDictionary( "app-resources/CIBCFIX50.xml" );
//
//        dd.setCheckUnorderedGroupFields( true );
//        //quickfix.fix50.MarketDataSnapshotFullRefresh message = new quickfix.fix50.MarketDataSnapshotFullRefresh();
//        quickfix.fix50.Quote message = new  quickfix.fix50.Quote();
//    //    message.fromString( qMsg, dd, true );
////        message.fromString( cibcQMsg, dd, true );
//        message.fromString( cibcQMsg3q, dd, true );
//        
//        System.out.println(message);
////
//      CibcResponseDialectHelper h = new CibcResponseDialectHelper();
//      QuoteVo q = h.convert(message);
//      System.out.println(q);
////        FullBook.Builder fbB = FullBook.newBuilder();
////        h.convertAndUpdate(message, fbB);
////        
////		AnnotationConfigApplicationContext ctx = null;
////		ctx = new AnnotationConfigApplicationContext();
////			
////			ctx.register(MasConfiguration.class);
////			ctx.refresh();
////			ctx.registerShutdownHook();
////        
////        
////			
////		FbPricePublishHandler ha = new FbPricePublishHandler(new PublishingEndpointSystemOutImpl(), ctx.getBean(SessionInfo.class));
////		ha.apply(fbB, 9999999999999L);
////        
////		FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh > p =  new FullBookSrcWrapper<quickfix.fix50.MarketDataSnapshotFullRefresh >();;
////		p.setFullBookAndSource(fbB, message);
////		
////		
////        
////        /*********************************************
////         * 
////         
////        
////		boolean tradeOnCcy2 = false;
////		NumberVo tradeSize = null, ccy1TradeSize = null;
////		long ccy1TradeSizeLong = -1l;
////		if ( tradeOnCcy2) {
////			tradeSize = NumberVo.getInstance("1290000");
////		} else {
////			tradeSize = NumberVo.getInstance("1000001");
////			ccy1TradeSize = tradeSize;
////			ccy1TradeSizeLong = ccy1TradeSize.getLongValueFloored();
////		}
////
////		FullBook.Builder fb = p.getFullBook();
////		int numOfRung = fb.getAskTicksCount();
////		Tick selectedAskTick = null, selectedBidTick = null;
////		
////		for (int i = 0; i < numOfRung; i++ ) {
////			long size = fb.getAskTicks(i).getSize();
////			//NumberVo sizeVo = NumberVo.getInstance(size + ".00");
////			if (tradeOnCcy2) {
////				if ( TradeAction.BUY.equals("B")) {
////					ccy1TradeSize = tradeSize.divide(fb.getAskTicks(i).getRate());
////					ccy1TradeSizeLong = ccy1TradeSize.getLongValueFloored();
////				} else {
////					ccy1TradeSize = tradeSize.divide(fb.getBidTicks(i).getRate());
////					ccy1TradeSizeLong = ccy1TradeSize.getLongValueFloored();
////				}
////			}
////			if ( ccy1TradeSizeLong <= size) {
////				selectedAskTick = fb.getAskTicks(i);
////				selectedBidTick = fb.getBidTicks(i);
////				break;
////			}
////		}
////		
////        
////
////        QuoteVo q = h.convert(p.getSource(), selectedBidTick.getSize());
////
////        System.out.println("[ccy1] buy price: " + q.getOfferPx() + " sell price: "  + q.getBidPx() );
////        
////        /**********************************************
////         * 
////         */
//        //ctx.close();
//	}
//
//}
