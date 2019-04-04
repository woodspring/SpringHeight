package com.tts.mde.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.tts.service.biz.tps.TpsClientTransactionDataService;
import com.tts.service.biz.tps.ActdsToTpsAdapter;
import com.tts.service.db.AutoCoverTransactionDataService;
import com.tts.service.sa.transaction.TransWrapper;
import com.tts.util.AppContext;
import com.tts.util.exception.TickTradeException;
import com.tts.vo.CounterPartyVo;
import com.tts.vo.ITransaction;
import com.tts.vo.TraderSalesPartyVo;

public class TransacationService {
	
	private static final Logger log = LoggerFactory.getLogger(TransacationService.class);
	
	private final AutoCoverTransactionDataService autoCoverTransactionDataService;
//	private final TpsClientTransactionDataService tps;
	private final ActdsToTpsAdapter newTps;
	private final boolean useTPS;
	
	public TransacationService() {
		boolean useTPS = false;
		try {
			useTPS = Boolean.parseBoolean(System.getProperty("useTPS"));
			log.debug("[init] useTPS = " + useTPS);
		} catch (Exception e) {
			
		}
		this.useTPS = useTPS;
		this.autoCoverTransactionDataService = (AutoCoverTransactionDataService) AppContext.getContext().getBean("autoCoverTransactionDataService");
//		this.tps = new TpsClientTransactionDataService();
		this.newTps = new ActdsToTpsAdapter();
	}

	public TransWrapper submitTradeFull(ITransaction tradeOrder, Long tradingSessionId, Long channelId,
			Long transParentId, String inSpotSalesSpread, String transComment, CounterPartyVo providerParty,
			TraderSalesPartyVo traderSalesParty, String instrumentOwner, Long userId) throws TickTradeException {
		if ( useTPS ) {
			log.debug("[submitTradeFull] Using TPS implementation");
			return newTps.submitTradeFull(((com.tts.service.biz.transactions.vo.TransactionVo) tradeOrder).toMessage(), tradingSessionId, channelId, transParentId,
					inSpotSalesSpread, transComment, providerParty, traderSalesParty, instrumentOwner, userId);
//			return tps.submitTradeFull(((com.tts.service.biz.transactions.vo.TransactionVo) tradeOrder).toMessage(), tradingSessionId, channelId, transParentId,
//					inSpotSalesSpread, transComment, providerParty, traderSalesParty, instrumentOwner, userId);			
		}
		return autoCoverTransactionDataService.submitTradeFull(tradeOrder, tradingSessionId, channelId, transParentId,
				inSpotSalesSpread, transComment, providerParty, traderSalesParty, instrumentOwner, userId);
	}

	public TransWrapper submitTradePartI(ITransaction tradeOrder, Long tradingSessionId, Long channelId,
			Long transParentId, String inSpotSalesSpread, String transComment, CounterPartyVo providerParty,
			TraderSalesPartyVo traderSalesParty, String instrumentOwner, Long userId) throws TickTradeException {
		if (useTPS) {
			log.debug("[submitTradePartI] Using TPS implementation");
			return newTps.submitTradePartI(((com.tts.service.biz.transactions.vo.TransactionVo) tradeOrder).toMessage(), tradingSessionId, channelId, transParentId,
					inSpotSalesSpread, transComment, providerParty, traderSalesParty, instrumentOwner, userId);
//			return tps.submitTradePartI(((com.tts.service.biz.transactions.vo.TransactionVo) tradeOrder).toMessage(), tradingSessionId, channelId, transParentId,
//					inSpotSalesSpread, transComment, providerParty, traderSalesParty, instrumentOwner, userId);
		}
		return autoCoverTransactionDataService.submitTradePartI(tradeOrder, tradingSessionId, channelId, transParentId,
				inSpotSalesSpread, transComment, providerParty, traderSalesParty, instrumentOwner, userId);
	}

	public TransWrapper submitTradePartII(TransWrapper arg0, ITransaction arg1, Long arg2, Long arg3, Long arg4,
			String arg5, String arg6, CounterPartyVo arg7, Long arg8, String arg9) throws TickTradeException {
		if (useTPS) {
			log.debug("[submitTradePartII] Using TPS implementation");
			return newTps.submitTradePartII(arg0, ((com.tts.service.biz.transactions.vo.TransactionVo) arg1).toMessage(), arg2, arg3, arg4, arg5, arg6, arg7, arg8,
					arg9);
//			return tps.submitTradePartII(arg0, ((com.tts.service.biz.transactions.vo.TransactionVo) arg1).toMessage(), arg2, arg3, arg4, arg5, arg6, arg7, arg8,
//					arg9);
		}
		return autoCoverTransactionDataService.submitTradePartII(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8,
				arg9);
	}

	public void updateTrade(Long transId, String tradeState, boolean updateTradeStateOnly, String message,
			String spotRate, String forwardPoint, String allInPrice, String ccy1Amt, String ccy2Amt, String refId,
			boolean useRef2, Long userId) {
		if (useTPS) {
			log.debug("[updateTrade] Using TPS implementation");
			newTps.updateTrade(transId, tradeState, updateTradeStateOnly, message, spotRate,
					forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, useRef2, userId);
//			tps.updateTrade(transId, tradeState, updateTradeStateOnly, message, spotRate,
//					forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, useRef2, userId);
			return;
		}
		autoCoverTransactionDataService.updateTrade(transId, tradeState, updateTradeStateOnly, message, spotRate,
				forwardPoint, allInPrice, ccy1Amt, ccy2Amt, refId, useRef2, userId);
	}

	public void init() {
//		tps.init();
		newTps.init();
	}
	
	public void destroy() {
//		tps.destroy();
		newTps.destroy();
	}
}
