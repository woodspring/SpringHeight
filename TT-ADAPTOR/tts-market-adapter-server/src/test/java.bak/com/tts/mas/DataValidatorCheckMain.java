package com.tts.mas;

import com.tts.util.flag.IndicativeFlag;

import quickfix.DataDictionary;

public class DataValidatorCheckMain {
	static String s0 = "8=FIXT.1.19=64935=W49=CIBC_TICKTRADE_ESP_CERT56=TICKTRADE_ESP_CERT115=TICKTRADE128=TICKTRADE34=9252=20150811-18:02:19.397262=53221000000000000055=EURUSD167=FXSPOT268=4269=0278=ID7NO2MA5270=1.10311000271=500000.00000000272=20150811276=A299=ID7NO2MA563=064=201508131026=1.10311000269=0278=ID7NO2MA5270=1.10311000271=1000000.00000000272=20150811276=A299=ID7NO2MA563=064=201508131026=1.10311000269=1278=ID7NO2MA5270=1.10321000271=500000.00000000272=20150811276=A299=ID7NO2MA563=064=201508131026=1.10321000269=1278=ID7NO2MA5270=1.10321000271=1000000.00000000272=20150811276=A299=ID7NO2MA563=064=201508131026=1.1032100010=155";

	static String s1 = "8=FIXT.1.19=196935=W34=14549=CIBC_TICKTRADE_ESP_CERT52=20150901-03:47:16.45056=TICKTRADE_ESP_CERT55=AUDCAD167=FXSPOT262=535000000000000007268=20269=0270=1.00873271=1000000272=20150901276=A290=163=01026=1.00873278=1441079236441269=0270=1.00868271=2000000272=20150901276=A290=263=01026=1.00868278=1441079236441269=0270=1.00863271=3000000272=20150901276=A290=363=01026=1.00863278=1441079236441269=0270=1.00858271=4000000272=20150901276=A290=463=01026=1.00858278=1441079236441269=0270=1.00853271=5000000272=20150901276=A290=563=01026=1.00853278=1441079236441269=0270=1.00828271=10000000272=20150901276=A290=663=01026=1.00828278=1441079236441269=0270=1.00803271=15000000272=20150901276=A290=763=01026=1.00803278=1441079236441269=0270=1.00778271=20000000272=20150901276=A290=863=01026=1.00778278=1441079236441269=0270=1.00728271=30000000272=20150901276=A290=963=01026=1.00728278=1441079236441269=0270=1.00678271=40000000272=20150901276=A290=1063=01026=1.00678278=1441079236441269=1270=1.00883271=1000000272=20150901276=A290=163=01026=1.00883278=1441079236441269=1270=1.00888271=2000000272=20150901276=A290=263=01026=1.00888278=1441079236441269=1270=1.00893271=3000000272=20150901276=A290=363=01026=1.00893278=1441079236441269=1270=1.00898271=4000000272=20150901276=A290=463=01026=1.00898278=1441079236441269=1270=1.00903271=5000000272=20150901276=A290=563=01026=1.00903278=1441079236441269=1270=1.00928271=10000000272=20150901276=A290=663=01026=1.00928278=1441079236441269=1270=1.00953271=15000000272=20150901276=A290=763=01026=1.00953278=1441079236441269=1270=1.00978271=20000000272=20150901276=A290=863=01026=1.00978278=1441079236441269=1270=1.01028271=30000000272=20150901276=A290=963=01026=1.01028278=1441079236441269=1270=1.01078271=40000000272=20150901276=A290=1063=01026=1.01078278=144107923644110=195";
    
	static String s2 = "8=FIXT.1.19=123535=W34=18049=CIBC_TICKTRADE_ESP_CERT52=20150901-04:04:12.06456=TICKTRADE_ESP_CERT55=USDJPY167=FXSPOT262=535000000000000008268=12269=0270=117.824271=1000000272=20150901276=A290=163=01026=117.824278=1441080252040269=0270=117.814271=3000000272=20150901276=A290=263=01026=117.814278=1441080252040269=0270=117.804271=5000000272=20150901276=A290=363=01026=117.804278=1441080252040269=0270=117.754271=15000000272=20150901276=A290=463=01026=117.754278=1441080252040269=0270=117.729271=20000000272=20150901276=A290=563=01026=117.729278=1441080252040269=0270=117.704271=25000000272=20150901276=A290=663=01026=117.704278=1441080252040269=1270=117.834271=1000000272=20150901276=A290=163=01026=117.834278=1441080252040269=1270=117.844271=3000000272=20150901276=A290=263=01026=117.844278=1441080252040269=1270=117.854271=5000000272=20150901276=A290=363=01026=117.854278=1441080252040269=1270=117.904271=15000000272=20150901276=A290=463=01026=117.904278=1441080252040269=1270=117.929271=20000000272=20150901276=A290=563=01026=117.929278=1441080252040269=1270=117.954271=25000000272=20150901276=A290=663=01026=117.954278=144108025204010=084";
	public static void main( String[] args )
            throws Exception {
			System.out.println(IndicativeFlag.getIndicativeReasons(70368744177664L));
            DataDictionary dd = new DataDictionary( "app-resources/TTFIX50.xml" );
            dd.setCheckUnorderedGroupFields( true );
            quickfix.Message message = new quickfix.Message();
            message.fromString( s2, dd, true );
            // double check
            dd.validate( message );
            System.out.println( "validated successfully!");


        }
}
