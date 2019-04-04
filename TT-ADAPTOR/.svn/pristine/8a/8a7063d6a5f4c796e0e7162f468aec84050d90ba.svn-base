package com.tts.mde.spot.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.mde.spot.IDirectLiquidityPool;
import com.tts.mde.spot.vo.MdConditionVo;
import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.mde.vo.RawLiquidityVo;

public class SingleSrcLiquidityPool implements IDirectLiquidityPool {
	private final static Logger logger = LoggerFactory.getLogger(IDirectLiquidityPool.class);
	private final MdSubscriptionVo subscription;
	private volatile long quoteUpdateCount;
	private volatile RawLiquidityVo[] bid = null;
	private volatile RawLiquidityVo[] ask = null;
	private volatile boolean isSuspendedForAggregation = false;
	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();	
	private final Lock readLock = rwl.readLock();
	private final Lock writeLock = rwl.writeLock();

	public SingleSrcLiquidityPool(MdSubscriptionVo sub) {
		this.subscription = sub;
		logger.debug("Created liquidityPool for " + sub.getSymbol() + "@" + sub.getIdentifer());
	}

	@Override
	public RawLiquidityVo[] getBidLqy(boolean ignoreAggSubspensionFlag) {
		if  (!ignoreAggSubspensionFlag && this.isSuspendedForAggregation ) {
			return null;
		}
		RawLiquidityVo[] bid = null;
		try {
			readLock.lock();
			if ( this.bid != null ){
				bid = Arrays.copyOfRange(this.bid, 0, this.bid.length);
			}
			
		} finally {
			readLock.unlock();
		}
		return bid;
	}

	@Override
	public RawLiquidityVo[] getAskLqy(boolean ignoreAggSubspensionFlag) {
		if  (!ignoreAggSubspensionFlag && this.isSuspendedForAggregation ) {
			return null;
		}
		RawLiquidityVo[] ask = null;
		try {
			readLock.lock();
			if ( this.ask != null) {
				ask = Arrays.copyOfRange(this.ask, 0, this.ask.length);
			}
		} finally {
			readLock.unlock();
		}
		return ask;
	}
	
	/* (non-Javadoc)
	 * @see com.tts.mde.spot.IMarketDataListener#replaceAllQuotes(com.tts.mde.vo.RawLiquidityVo[], com.tts.mde.vo.RawLiquidityVo[])
	 */
	@Override
	public void replaceAllQuotes(RawLiquidityVo[] bid, RawLiquidityVo[] ask) {
		try {
			writeLock.lock();		
			if ( bid != null) {
				this.bid = Arrays.copyOfRange(bid, 0, bid.length);
			}
			if ( ask != null ) {
				this.ask = Arrays.copyOfRange(ask, 0, ask.length);
			}
			quoteUpdateCount++;
		} finally {
			writeLock.unlock();
		}
	}
	
	public MdConditionVo validate() {
		
		long lastReceivedTime = -1L;
		int validBidQCount = 0;
		int validAskQCount = 0;
		long quoteValidInterval = this.subscription.getQuoteValidInterval();

		try {
			readLock.lock();
		
			if ( this.bid != null ) {
				for (int i = 0; i < this.bid.length; i++) {
					if (this.bid[i].isValid()) {
						long receivedTime = this.bid[i].getReceivedTime();
						long marketDataAge = System.currentTimeMillis() - receivedTime;
						if (quoteValidInterval > 0 && marketDataAge > quoteValidInterval) {
							this.bid[i].flagInvalid();
							logger.info("flag bid invalid as marketDataAge(" + marketDataAge+") > quoteValidInterval(" +quoteValidInterval+") in " + subscription.getIdentifer());
						} else {
							validBidQCount++;
						}
						if (receivedTime > lastReceivedTime) {
							lastReceivedTime = receivedTime;
						}
					}
				}
			}
			if (this.ask != null ) {
				for (int i = 0; i < this.ask.length; i++) {
					if (ask[i].isValid()) {
						long receivedTime = ask[i].getReceivedTime();
						long marketDataAge = System.currentTimeMillis() - receivedTime;
						if (quoteValidInterval > 0 && marketDataAge > quoteValidInterval) {
							ask[i].flagInvalid();
							logger.info("flag ask invalid as marketDataAge(" + marketDataAge+") > quoteValidInterval(" +quoteValidInterval+") in " + subscription.getIdentifer());
						} else {
							validAskQCount++;
						}
						if (receivedTime > lastReceivedTime) {
							lastReceivedTime = receivedTime;
						}
					}
				}
			}
		} finally {
			readLock.unlock();
		}
		return new MdConditionVo(lastReceivedTime, validBidQCount, validAskQCount);
	}

	@Override
	public void setIsSubspendForAggregation(boolean isSuspended) {
		this.isSuspendedForAggregation = isSuspended;
	}

	public MdSubscriptionVo getSubscription() {
		return subscription;
	}

	@Override
	public List<QuoteUpdateStatVo> getQuoteUpdateCount() {
		ArrayList<QuoteUpdateStatVo> a = new ArrayList<>(1);
		long _quoteUpdateCount = -1;
		try {
			readLock.lock();
			_quoteUpdateCount = this.quoteUpdateCount;
		} finally {
			readLock.unlock();
		}
		QuoteUpdateStatVo q = new QuoteUpdateStatVo(subscription.getSource(), _quoteUpdateCount);
		
		a.add(q);
		return a;
	}
	
	

}
