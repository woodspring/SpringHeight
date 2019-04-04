package com.tts.plugin.adapter.impl.cibc.validate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;
import com.tts.util.flag.IndicativeFlag;

public class CIBCSpotPublishValidator implements IPublishValidator<FullBook.Builder> {
	private final static Logger logger = LoggerFactory.getLogger(CIBCSpotPublishValidator.class);

	private static final String LOGMSG__FLAGGED_AS_NO_MARKET_DATA_AS_NO_ASK_TICK_OR_BID_TICK = ": Flagged as No Market Data as no ask tick or bid tick";

	
	@Override
	public FullBook.Builder validate(FullBook.Builder fbBuilder) {
		long indicativeFlag = fbBuilder.getIndicativeFlag();
		String symbol = fbBuilder.getSymbol();
		if ( fbBuilder.getAskTicksCount() ==0 && fbBuilder.getBidTicksCount() ==0 ) {
			indicativeFlag = IndicativeFlag.setIndicative(indicativeFlag,
						IndicativeFlag.IndicativeReason.MA_NoData);		
			logger.debug(symbol + LOGMSG__FLAGGED_AS_NO_MARKET_DATA_AS_NO_ASK_TICK_OR_BID_TICK);
		} else 	if ( fbBuilder.getAskTicksCount() != fbBuilder.getBidTicksCount() ) {
			fbBuilder = handlerUnevenRungs(fbBuilder);
		}

		fbBuilder.setIndicativeFlag(indicativeFlag);
		return fbBuilder;
	}
	
	private FullBook.Builder handlerUnevenRungs(FullBook.Builder fbBuilder) {
		FullBook.Builder newFbBuilder = FullBook.newBuilder(fbBuilder.build());
		newFbBuilder.clearAskTicks();
		newFbBuilder.clearBidTicks();
		int askTickCount = fbBuilder.getAskTicksCount();
		int bidTickCount = fbBuilder.getBidTicksCount();
		long maxAsk = -1;
		long maxBid = -1;
		
		HashSet<Long> askTickRungSizes = new HashSet<Long>(askTickCount) ;
		HashSet<Long> bidTickRungSizes = new HashSet<Long>(bidTickCount) ;
		HashSet<Long> finalRungSizes = new HashSet<Long>(askTickCount + bidTickCount) ;

		for ( int i = 0; i < askTickCount; i++ ) {
			Tick.Builder askTick = fbBuilder.getAskTicksBuilder(i);
			askTickRungSizes.add(askTick.getSize());
			if ( askTick.getSize() > maxAsk ) {
				maxAsk  = askTick.getSize();
			}
		}
		for ( int i = 0; i < bidTickCount; i++ ) {
			Tick.Builder bidTick = fbBuilder.getBidTicksBuilder(i);
			bidTickRungSizes.add(bidTick.getSize());
			if ( bidTick.getSize() > maxBid ) {
				maxBid  = bidTick.getSize();
			}
		}
		
		long maxRungSize = Math.min(maxAsk, maxBid);
		finalRungSizes.addAll(askTickRungSizes);
		finalRungSizes.removeAll(bidTickRungSizes);
		finalRungSizes.addAll(bidTickRungSizes);

		
		Long[] finalRungArr = finalRungSizes.toArray(new Long[0]);
		Arrays.sort(finalRungArr);

		int level = 0;
		List<Tick.Builder> askTicks = fbBuilder.getAskTicksBuilderList();
		List<Tick.Builder> bidTicks = fbBuilder.getBidTicksBuilderList(); 
		
		for ( long rung: finalRungArr) {
			level++;
			if ( rung <= maxRungSize) {
				for ( int i = 0; i < askTickCount; i++ ) {
					Tick.Builder askTick = askTicks.get(i);
					if (askTick.getSize() == rung ) {
						askTick.setLevel(level);
						newFbBuilder.addAskTicks(askTick);
						break;
					} else if (askTick.getSize() > rung ) {
						Tick.Builder newAskTick = Tick.newBuilder(askTicks.get(i).build());
						newAskTick.setSize(rung);
						newAskTick.setLevel(level);
						newFbBuilder.addAskTicks(newAskTick);
						break;				
					}
				}
				for ( int i = 0; i < bidTickCount; i++ ) {
					Tick.Builder bidTick = bidTicks.get(i);
					if (bidTick.getSize() == rung ) {
						bidTick.setLevel(level);
						newFbBuilder.addBidTicks(bidTick);
						break;
					} else if (bidTick.getSize() > rung ) {
						Tick.Builder newBidTick = Tick.newBuilder(bidTicks.get(i).build());
						newBidTick.setSize(rung);
						newBidTick.setLevel(level);
						newFbBuilder.addBidTicks(newBidTick);
						break;				
					}
				}
			}
		}
		fbBuilder.clearAskTicks();
		fbBuilder.clearBidTicks();
		fbBuilder.addAllAskTicks(newFbBuilder.getAskTicksList());
		fbBuilder.addAllBidTicks(newFbBuilder.getBidTicksList());
		return fbBuilder;
	}

}
