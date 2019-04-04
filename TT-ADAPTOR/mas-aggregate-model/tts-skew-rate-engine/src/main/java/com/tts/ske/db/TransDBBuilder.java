/*******************************************************************************
 * Copyright (c) 2016 TickTrade
 * All rights reserved. 
 *
 * Contributors:
 *     TickTrade - Implementation
 *******************************************************************************/
package com.tts.ske.db;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.tts.entity.session.TradingSession;
import com.tts.entity.system.SystemProperty;
import com.tts.service.IDbService;
import com.tts.service.db.AutoCoverTransactionDataService;
import com.tts.service.db.ICustomerProfileService;
import com.tts.service.db.TradeBlotterDataService;
import com.tts.service.db.TradeBuilderDataService;
import com.tts.service.db.TransactionSummaryDataService;
import com.tts.service.sa.transaction.TransWrapper;
import com.tts.util.AppContext;
import com.tts.util.constant.SysProperty;
import com.tts.util.exception.DataServiceException;
import com.tts.util.flag.PostTradeStatusFlag.PostTradeStatus;
import com.tts.vo.CounterPartyVo;
import com.tts.vo.ITransaction;
import com.tts.vo.TradeBlotterVo;
import com.tts.vo.TransQuerySearchCriteriaVo;
import com.tts.vo.TransactionSummaryVo;

@Transactional(readOnly = false)	
public class TransDBBuilder {
	
	@Inject
	private IDbService<SystemProperty, Long> systemPropertyService;	
	
	protected final TradeBuilderDataService tradeBuilderDataService;
	protected final TradeBlotterDataService tradeBlotterDataService;
	protected final TransactionSummaryDataService transactionSummaryDateService;
	
	public TransDBBuilder() {
		tradeBlotterDataService = TradeBlotterDataService.getInstance();
		transactionSummaryDateService = TransactionSummaryDataService.getInstance();
		tradeBuilderDataService = (TradeBuilderDataService) AppContext.getContext().getBean("tradeBuilderDataService"); 
	}
	
	protected TransWrapper submitTradeFull(ITransaction tradeOrder, Long tradingSessionId, Long channelId, Long transParentId, CounterPartyVo providerParty, Long userId, String cmt) throws Exception {
		TransWrapper wrapper = null;
		wrapper = getAutoCoverTransactionDataService().submitTradeFull(tradeOrder, tradingSessionId, channelId, transParentId, null, cmt, providerParty, null, null, userId);
		return wrapper;
	}
	
	protected long updateFxLeg(Long transId, String tradeRate) {
		 return getAutoCoverTransactionDataService().updateFxLeg(transId, tradeRate);		
	}
	
	protected void updateTrade(Long transId, String tradeState, Integer productId, boolean updateTradeStateOnly, String message, String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String refId, Long userId, String settleDate, String timeOptionPriceDate) {	
		boolean isUseTransRefForAutoCover = false;		
		getAutoCoverTransactionDataService().updateTrade(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, settleDate, timeOptionPriceDate, !isUseTransRefForAutoCover, userId);
	}
	
	protected void updateTradeContract(Long transId, String transContractTypeCd, String transContractRef, Long userId) {
		getAutoCoverTransactionDataService().updateTradeContract(transId, transContractTypeCd, transContractRef, userId);
	}
	
	protected void updateTradeForSwap(Long transId, String tradeState, Integer productId, boolean updateTradeStateOnly, String message, String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String refId, Long userId, String settleDate, String spotRateFar, String forwardPointFar, String allInPriceFar, String ccy1AmtFar, String ccy2AmtFar, String settleDateFar) {	
		boolean isUseTransRefForAutoCover = false;		
		getAutoCoverTransactionDataService().updateTradeForSwap(transId, tradeState, productId, updateTradeStateOnly, message, spotRate, forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, settleDate, spotRateFar, forwardPointFar, allInPriceFar, ccy1AmtFar, ccy2AmtFar,  settleDateFar, !isUseTransRefForAutoCover, userId);
	}
	public List<CounterPartyVo> findCounterPartyByType(final String customerSubType, final String accountType) throws DataServiceException {
		final ICustomerProfileService custProfService = AppContext.getContext().getBean("customerProfileService",
				ICustomerProfileService.class);
		return custProfService.findCounterPartyByType(customerSubType, accountType);
	}
	
	public TradeBlotterVo getTradeBlotterByTransId(final Long transId) {
		return tradeBlotterDataService.findByTransId(transId, true);
	}
	
	public List<TransactionSummaryVo> getTradeBlotterByParentTransId(final Long transId) {
		TransQuerySearchCriteriaVo criteria = new TransQuerySearchCriteriaVo();
		criteria.setParentTransId(transId);
		
		return transactionSummaryDateService.find(criteria, true,1, 5);
	}
	
	public List<TradeBlotterVo> getTradeBlotterByTransState(final String stateCd) {
		return tradeBlotterDataService.findByTransState(stateCd);
	}
	
	protected AutoCoverTransactionDataService getAutoCoverTransactionDataService() {
		return (AutoCoverTransactionDataService)AppContext.getContext().getBean("autoCoverTransactionDataService");	
	}

	public String getParentTradeTimeoutThreshold() {
		List<SystemProperty> sysPropListTimeoutThreshold = systemPropertyService.findByCriteria(new Specification<SystemProperty>() {
			@Override
			public Predicate toPredicate(Root<SystemProperty> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.and(cb.equal(root.get("group"), SysProperty.GroupCd.ACE_PROP), 
						      cb.equal(root.get("key1"), SysProperty.Key1.PARENT_TRADE_TIMEOUT_THRESHOLD), 
						      cb.equal(root.get("expired"), 'N'));
			}
		});
		
		if( sysPropListTimeoutThreshold == null ) return null;
		
		Iterator<SystemProperty> itr = sysPropListTimeoutThreshold.iterator();
		while( itr.hasNext() ) {
			SystemProperty s = (SystemProperty)itr.next();
			return s.getValue();
		}
		
		return null;
	}
		
	public void copySwapTransaction(long transId) {
		tradeBuilderDataService.copySwapTransaction(transId);
	}

	public String getAutoCoverTradeTimeoutThreshold()	{
		List<SystemProperty> sysPropListTimeoutThreshold = systemPropertyService.findByCriteria(new Specification<SystemProperty>() {
			@Override
			public Predicate toPredicate(Root<SystemProperty> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.and(cb.equal(root.get("group"), SysProperty.GroupCd.ACE_PROP), 
						      cb.equal(root.get("key1"), SysProperty.Key1.AC_TRADE_TIMEOUT_THRESHOLD), 
						      cb.equal(root.get("expired"), 'N'));
			}
		});
		
		if(sysPropListTimeoutThreshold == null) return(null);
		
		Iterator<SystemProperty> itr = sysPropListTimeoutThreshold.iterator();
		while(itr.hasNext()) {
			SystemProperty s = (SystemProperty)itr.next();
			return s.getValue();
		}
		
		return(null);
	}
	
	@SuppressWarnings("unchecked")
	public TradingSession findTradingSessionById(final Long tradingSessionId) {
		try {
			IDbService<TradingSession, Long> tradingSessionService = (IDbService<TradingSession, Long>) AppContext.getContext()
					.getBean("tradingSessionService");
			List<TradingSession> tradingSessions = tradingSessionService.findByCriteria(new Specification<TradingSession>() {

				@Override
				public Predicate toPredicate(Root<TradingSession> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					return cb.equal(root.get("pk"), tradingSessionId);
				}
			});

			if (tradingSessions != null && tradingSessions.size() > 0) {
				return tradingSessions.get(0);
			} else {
				return null;
			}
		} catch (Exception ex) {
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	public List<SystemProperty> getTRSSystemProperties() {
		IDbService<SystemProperty, Long> systemPropertyService = (IDbService<SystemProperty, Long>)AppContext.getContext().getBean("systemPropertyService");	
		
		List<SystemProperty> sysPropList = systemPropertyService.findByCriteria(new Specification<SystemProperty>() {
			@Override
			public Predicate toPredicate(Root<SystemProperty> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.and(cb.equal(root.get("group"), SysProperty.GroupCd.TRS_PROP), 
						      cb.equal(root.get("expired"), 'N'));
			}
		});
		
		if( sysPropList == null ) return null;			
		
		return sysPropList;
	}
	
	@SuppressWarnings("unchecked")
	public List<SystemProperty> getConstantsSystemProperties() {
		IDbService<SystemProperty, Long> systemPropertyService = (IDbService<SystemProperty, Long>)AppContext.getContext().getBean("systemPropertyService");	

		List<SystemProperty> sysPropList = systemPropertyService.findByCriteria(new Specification<SystemProperty>() {
			@Override
			public Predicate toPredicate(Root<SystemProperty> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.and(cb.equal(root.get("group"), SysProperty.GroupCd.CONSTANTS), 
						      cb.equal(root.get("expired"), 'N'));
			}
		});

		if( sysPropList == null ) return null;			

		return sysPropList;
	}

	protected void updateTransStpStatus(Long transId, PostTradeStatus stpStateFlag, long userId) {
		getAutoCoverTransactionDataService().updateStpStatus(transId, stpStateFlag, userId);
	}
	
}
