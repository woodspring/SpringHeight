package com.tts.mas;

import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import com.tts.mas.test.fwd.dummy.DummySymbolMapper;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.service.biz.price.structure.PriceStructureRepo;
import com.tts.service.biz.price.structure.diff.full.FullBookBuilderFullScanStructureDifferentiator;

public class PriceRepoTest {

	static String USDJPY_1 = "symbol: \"USDJPY\" askTicks { level: 1 rate: \"112.663\" size: 500000 } askTicks { level: 2 rate: \"112.663\" size: 1000000 } askTicks { level: 3 rate: \"112.664\" size: 2000000 } askTicks { level: 4 rate: \"112.664\" size: 3000000 } askTicks { level: 5 rate: \"112.664\" size: 4000000 } askTicks { level: 6 rate: \"112.664\" size: 5000000 } askTicks { level: 7 rate: \"112.665\" size: 8000000 } askTicks { level: 8 rate: \"112.665\" size: 10000000 } askTicks { rate: \"112.666\" size: 20000000 spotRate: \"112.666\" } bidTicks { level: 1 rate: \"112.658\" size: 500000 } bidTicks { level: 2 rate: \"112.658\" size: 1000000 } bidTicks { level: 3 rate: \"112.657\" size: 2000000 } bidTicks { level: 4 rate: \"112.657\" size: 3000000 } bidTicks { level: 5 rate: \"112.657\" size: 4000000 } bidTicks { level: 6 rate: \"112.657\" size: 5000000 } bidTicks { level: 7 rate: \"112.657\" size: 8000000 } bidTicks { level: 8 rate: \"112.656\" size: 10000000 } bidTicks { level: 9 rate: \"112.656\" size: 20000000 } bidTicks { rate: \"112.655\" size: 20000000 spotRate: \"112.655\" } sequence: 705430000000000354 updateTimestamp: 1486088144901 latency { faSendTimestamp: 1486088144926 faReceiveTimestamp: 1486088144901 } topOfBookIdx: 0 tradingSession: 10 indicativeFlag: 0 indicativeSubFlag: 0 tradeDate: \"20170203\" quoteRefId: \"1486088144901\" rateChangeInd: 0";
	static String USDJPY_2 = "symbol: \"USDJPY\" askTicks { level: 1 rate: \"112.555\" size: 500000 } askTicks { level: 2 rate: \"112.555\" size: 1000000 } askTicks { level: 3 rate: \"112.556\" size: 2000000 } askTicks { level: 4 rate: \"112.556\" size: 3000000 } askTicks { level: 5 rate: \"112.556\" size: 4000000 } askTicks { level: 6 rate: \"112.557\" size: 5000000 } askTicks { level: 7 rate: \"112.557\" size: 8000000 } askTicks { level: 8 rate: \"112.557\" size: 10000000 } askTicks { level: 9 rate: \"112.558\" size: 20000000 } bidTicks { level: 1 rate: \"112.548\" size: 500000 } bidTicks { level: 2 rate: \"112.548\" size: 1000000 } bidTicks { level: 3 rate: \"112.547\" size: 2000000 } bidTicks { level: 4 rate: \"112.547\" size: 3000000 } bidTicks { level: 5 rate: \"112.546\" size: 4000000 } bidTicks { level: 6 rate: \"112.546\" size: 5000000 } bidTicks { level: 7 rate: \"112.546\" size: 8000000 } bidTicks { level: 8 rate: \"112.546\" size: 10000000 } sequence: 705430000000003006 updateTimestamp: 1486088809754 latency { faReceiveTimestamp: 1486088809754 } topOfBookIdx: 0 tradingSession: 10 indicativeFlag: 0 indicativeSubFlag: 0 tradeDate: \"20170203\" quoteRefId: \"1486088809754\" rateChangeInd: 0";
	
	public static void main(String[] args) throws ParseException {
		DummySymbolMapper m = new DummySymbolMapper();;
		
		PriceStructureRepo<FullBook.Builder>  structureRepo = new PriceStructureRepo<FullBook.Builder>( m);
		FullBook.Builder b1 = FullBook.newBuilder();
		FullBook.Builder b2 = FullBook.newBuilder();
		TextFormat.merge(USDJPY_1, b1);
		TextFormat.merge(USDJPY_2, b2);

		boolean bb1 = structureRepo.hasStructureChanged("USDJPY", new FullBookBuilderFullScanStructureDifferentiator(), b1);
		System.out.println(bb1);
		boolean bb2 = structureRepo.hasStructureChanged("USDJPY", new FullBookBuilderFullScanStructureDifferentiator(), b1);
		System.out.println(bb2);


	}
}
