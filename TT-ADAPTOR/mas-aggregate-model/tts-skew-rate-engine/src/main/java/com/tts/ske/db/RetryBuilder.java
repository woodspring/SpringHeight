package com.tts.ske.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.trade.TradeMessage.Transaction;
import com.tts.service.sa.transaction.TransWrapper;
import com.tts.util.flag.PostTradeStatusFlag.PostTradeStatus;
import com.tts.vo.CounterPartyVo;
import com.tts.vo.ITransaction;

public class RetryBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(RetryBuilder.class);

	private long getRetryCount() {
		return 5L;
	}
	
	private long getRetryWaitTime() {
		return 500L;
	}		
	
	private boolean handleRetryException(String func, long retryCount, long retryWaitTime, Exception exc) {
		int tradeBuilderRetryCount = (int) getRetryCount() ;
		log.error("[" + func + "] - Fail to save to DB (Retry Count(DB): " + tradeBuilderRetryCount + ", Retry Count(Remain): " + retryCount + ", WaitTime " + retryWaitTime + "s).", exc);
		if( tradeBuilderRetryCount == retryCount ) {
			if( retryWaitTime > 0 ) {
				try { Thread.sleep(retryWaitTime); } catch (Exception exc2) {}
			}
		}
		if( retryCount > 0 ) {
			return true;
		}
		return false;
	}
	
	public TransWrapper submitTradeFull(ITransaction tradeOrder, Long tradingSessionId, Long channelId, Long transParentId, CounterPartyVo providerParty, Long userId, String cmt) throws Exception {
		return retry_submitTradeFull(tradeOrder, tradingSessionId, channelId, transParentId, providerParty, userId, cmt, getRetryCount(), getRetryWaitTime());	
	}
	
	public long updateFxLeg(Long transId, String tradeRate) {
		return retry_updateFxLeg(transId, tradeRate, getRetryCount(), getRetryWaitTime());	
	}
	
	public void updateTrade(Long transId, String tradeState, Integer productId, boolean updateTradeStateOnly, String message, String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String settleDate, String timeOptionPriceDate, String refId, Long userId) {
		retry_updateTrade(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, settleDate, timeOptionPriceDate, userId, getRetryCount(), getRetryWaitTime());	
	}
	
	public void updateTradeContract(Long transId, String transContractTypeCd, String transContractRef, Long userId) {
		retry_updateTradeContract(transId, transContractTypeCd, transContractRef, userId, getRetryCount(), getRetryWaitTime());
	}

	public void updateTradeForSwap(Long transId, String tradeState, Integer productId, boolean updateTradeStateOnly, String message, String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String settleDate, String spotRateFar, String forwardPointFar, String allInPriceFar, String ccy1AmtFar, String ccy2AmtFar, String settleDateFar, String refId, Long userId) {
		retry_updateTradeForSwap(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, settleDate, spotRateFar,  forwardPointFar,  allInPriceFar,  ccy1AmtFar,  ccy2AmtFar, settleDateFar, userId, getRetryCount(), getRetryWaitTime());
	}
	
	public void copySwapTransaction(Long transId) {
		retry_copySwapTransaction(transId, getRetryCount(), getRetryWaitTime());
	}
	
	public void updateTransStpStatus(Long transId, PostTradeStatus stpStateFlag, long userId) {
		retry_updateTransStpStatus(transId, stpStateFlag, userId, getRetryCount(), getRetryWaitTime());		
	}
	
	public TransWrapper retry_submitTradeFull(ITransaction tradeOrder, Long tradingSessionId, Long channelId, Long transParentId, CounterPartyVo providerParty, Long userId, String cmt, long retryCount, long retryWaitTime) throws Exception {
		String func = "retry_submitTradeFull";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		TransWrapper wrapper = null;
		try {				
			TransDBBuilder dbBuilder = new TransDBBuilder();
			wrapper = dbBuilder.submitTradeFull(tradeOrder, tradingSessionId, channelId, transParentId, providerParty, userId, cmt);
		} catch ( Exception exc ) {
			if( handleRetryException(func, retryCount, retryWaitTime, exc) ) {
				retryCount--;
				wrapper = retry_submitTradeFull(tradeOrder, tradingSessionId, channelId, transParentId, providerParty, userId, cmt, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
		return wrapper;
	}
	
	public long retry_updateFxLeg(Long transId, String tradeRate, long retryCount, long retryWaitTime) {
		String func = "retry_updateFxLeg";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		try {					
			TransDBBuilder dbBuilder = new TransDBBuilder();
			return dbBuilder.updateFxLeg(transId, tradeRate);		
		} catch ( Exception exc ) {
			if( handleRetryException(func, retryCount, retryWaitTime, exc) ) {
				retryCount--;
				return retry_updateFxLeg(transId, tradeRate, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
	}
	
	public void retry_updateTradeForSwap(Long transId, String tradeState, Integer productId, boolean updateTradeStateOnly, String message, String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String refId, String settleDate, String spotRateFar, String forwardPointFar, String allInPriceFar, String ccy1AmtFar, String ccy2AmtFar, String settleDateFar, Long userId, long retryCount, long retryWaitTime) {	
		String func = "retry_updateTradeForSwap";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		try {	
			TransDBBuilder dbBuilder = new TransDBBuilder();
			dbBuilder.updateTradeForSwap(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, userId, settleDate, spotRateFar, forwardPointFar, allInPriceFar, ccy1AmtFar, ccy2AmtFar, settleDateFar);
		} catch ( Exception exc ) {
			if( handleRetryException(func, retryCount, retryWaitTime, exc) ) {
				retryCount--;
				retry_updateTradeForSwap(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, settleDate, spotRateFar,  forwardPointFar,  allInPriceFar,  ccy1AmtFar,  ccy2AmtFar, settleDateFar, userId, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
	}
	
	public void retry_copySwapTransaction(Long transId, long retryCount, long retryWaitTime) {	
		String func = "retry_copySwapTransaction";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		try {	
			TransDBBuilder dbBuilder = new TransDBBuilder();
			dbBuilder.copySwapTransaction(transId);
		} catch ( Exception exc ) {
			if( handleRetryException(func, retryCount, retryWaitTime, exc) ) {
				retryCount--;
				retry_copySwapTransaction(transId, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
	}
	
	public void retry_updateTrade(Long transId, String tradeState, Integer productId, boolean updateTradeStateOnly, String message, String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String refId, String settleDate, String timeOptionPriceDate, Long userId, long retryCount, long retryWaitTime) {	
		String func = "retry_updateTrade";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		try {	
			TransDBBuilder dbBuilder = new TransDBBuilder();
			dbBuilder.updateTrade(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, userId, settleDate, timeOptionPriceDate);
		} catch ( Exception exc ) {
			if( handleRetryException(func, retryCount, retryWaitTime, exc) ) {
				retryCount--;
				retry_updateTrade(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, settleDate, timeOptionPriceDate, userId, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
	}
	
	public void retry_updateTradeContract(Long transId, String transContractTypeCd, String transContractRef, Long userId, long retryCount, long retryWaitTime) {
		String func = "retry_updateTrade";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		try {
			TransDBBuilder dbBuilder = new TransDBBuilder();
			dbBuilder.updateTradeContract(transId, transContractTypeCd, transContractRef, userId);
		} catch (Exception exc) {
			if (handleRetryException(func, retryCount, retryWaitTime, exc)) {
				retryCount--;
				retry_updateTradeContract(transId, transContractTypeCd, transContractRef, userId, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
	}
	
	public void retry_updateTransStpStatus(Long transId, PostTradeStatus stpStateFlag, long userId, long retryCount, long retryWaitTime) {
		String func = "retry_updateTransStpStatus";
		log.info("[" + func + "] IN (Retry: " + retryCount + ")");
		try {	
			TransDBBuilder dbBuilder = new TransDBBuilder();
			dbBuilder.updateTransStpStatus(transId, stpStateFlag, userId);
		} catch ( Exception exc ) {
			if( handleRetryException(func, retryCount, retryWaitTime, exc) ) {
				retryCount--;
				retry_updateTransStpStatus(transId, stpStateFlag, userId, retryCount, retryWaitTime);
			} else {
				throw exc;
			}
		} finally {
			log.info("[" + func + "] OUT");
		}
	}
}
