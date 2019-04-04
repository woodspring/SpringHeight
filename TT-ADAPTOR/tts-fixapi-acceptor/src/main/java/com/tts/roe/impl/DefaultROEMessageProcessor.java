package com.tts.roe.impl;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.fixapi.type.IFIXAcceptorMessageDispatcher;
import com.tts.fixapi.type.IFIXAcceptorMessageProcessor;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.system.UserSessionStruct.UserSessionInfo;
import com.tts.message.trade.RestingOrderMessage.RestingOrder;
import com.tts.message.trade.RestingOrderMessage.RestingOrderStatus;
import com.tts.message.trade.RestingOrderMessage.RestingOrderSubmitRequest;
import com.tts.message.trade.RestingOrderMessage.RestingOrderSubmitRequest.RestingOrderSubmitType;
import com.tts.message.util.StaticDataUtils;
import com.tts.message.util.TtMsgEncoder;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.constant.ChannelConstants;
import com.tts.util.constant.RestingOrderConstants;
import com.tts.util.constant.RestingOrderConstants.OrderDurationType;
import com.tts.util.constant.RestingOrderConstants.OrderType;
import com.tts.util.constant.TradeConstants.TradeAction;
import com.tts.util.exception.TickTradeErrorCode;
import com.tts.vo.CustomerAccountVo;

import quickfix.SessionID;
import quickfix.field.ExecType;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdType;
import quickfix.field.Side;
import quickfix.field.TimeInForce;

public class DefaultROEMessageProcessor implements IMsgListener, IFIXAcceptorMessageProcessor {
	private static final Logger logger = LoggerFactory.getLogger("FixAPILogger");
	private static final String SEPARATOR    = ".";
	private static final String SYSTEM_USER  = "system";
	private static final long SYSTEM_USER_ID = 1L;
	
	final Map<Long, CustomerAccountVo> customerAccounts  = new ConcurrentHashMap<>();
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	private static final String PENDING      = RestingOrderConstants.OrderStateCd.ACTIVE;
	private static final String DONE         = RestingOrderConstants.OrderStateCd.DONE;
	private static final String CANCELLED    = RestingOrderConstants.OrderStateCd.CANCELLED;
	private static final String REJECTED     = RestingOrderConstants.OrderStateCd.REJECTED;
	
	private IMsgSenderFactory msgSenderFact;
	private IMsgSender msgSender;
	private IMsgReceiver[] msgReceivers = null;
	
	private final FixApplicationProperties appProp;
	private IFIXAcceptorMessageDispatcher fixMsgSender = null;
	private Object lockObject = new Object();
	private final Map<Long, LinkedList<Hashtable<String, Object>>> msgsNotTransmitted = new ConcurrentHashMap<>();
	
	private final static List<String> INTERESTRED_NON_PRESISTANCE_TOPICS = Arrays.asList(new String[] {
			IEventMessageTypeConstant.Transactional.TRAN_ROR_STATUS
	});
	
	public DefaultROEMessageProcessor()	{
		this.msgSenderFact = AppContext.getContext().getBean(IMsgSenderFactory.class);
		this.msgSender     = this.msgSenderFact.getMsgSender(false, false);
		this.appProp       = AppContext.getContext().getBean(FixApplicationProperties.class);
		
		initMsgListeners();
		logger.info("Successfully Initialized DefaultROEMessageProcessor...");
	}
	
	@Override
	public void setFIXMessageDispatcher(IFIXAcceptorMessageDispatcher msgSender)	{
		this.fixMsgSender = msgSender;
	}
	
	public void initMsgListeners()	{
		final IMsgReceiver[] _msgReceivers = new IMsgReceiver[INTERESTRED_NON_PRESISTANCE_TOPICS.size()];
		final IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		
		String topic = null;
		int topicCnt = 0;
		
		for(; topicCnt < INTERESTRED_NON_PRESISTANCE_TOPICS.size(); ) {
			topic = INTERESTRED_NON_PRESISTANCE_TOPICS.get(topicCnt);
			IMsgReceiver msgReceiver = msgReceiverFactory.getMsgReceiver(false, false);
			msgReceiver.setTopic(topic);
			msgReceiver.setListener(this);
			msgReceiver.init();
			_msgReceivers[topicCnt] = msgReceiver;
			
			++topicCnt;
		}
		
		this.msgReceivers = _msgReceivers;
	}
	
	public void destroyMsgListeners()	{
		if(msgReceivers != null)	{
			for(IMsgReceiver msgReceiver: msgReceivers) {
				if(msgReceiver != null) {
					msgReceiver.destroy();
				}
			}
		}
	}
	
	
	
	@Override
	public void notifySessionConnectionForCustomer(CustomerAccountVo customer, SessionID sessionId) {
		customerAccounts.put(customer.getAccountId(), customer);
		
		if(!msgsNotTransmitted.containsKey(customer.getAccountId()))
			msgsNotTransmitted.put(customer.getAccountId(), new LinkedList<>() );
		
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.INFO_TEMPLATE, AppUtils.getAppName());
		monitorAgent.logError(customer.getCustomerNm() + ":onLogon", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
				"ROE Order Component Connected. Customer: " + customer.getCustomerNm() + ", Account: " + customer.getAccountId() +", Session: " + sessionId.toString());
		
		String ordMsgTarget =  getCustomerOrderMsgTarget(customer);
		if(ordMsgTarget == null)	{
			logger.error("MISSING ORD.MSG.TARGET CONFIGURATION FOR " + customer.getShortName() + " IN CONFIGURATION. MESSAGE DELIVERY WILL BE AFFECTED.");
			topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());
			monitorAgent.logError("ROEQfixApp:onLogon", topic, MonitorConstant.ROE.ERROR_FIX_MSG_TARGET_NOT_FOUND, 
					"Missing Configuration ORD.MSG.TARGET. CUSTOMER: " + customer.getCustomerNm() + " ACCOUNT: " + customer.getAccountId());
		}
		
		/**
		 * 	Send Pending Messages only when ORDER Session is Connected.
		 */
		String sessionKey   = sessionId.getSenderCompID() + SEPARATOR +sessionId.getTargetCompID();
		if(ordMsgTarget.equals(sessionKey))
			transmitPendingMessages(customer.getAccountId());
	}

	@Override
	public void notifySessionDisconnectionForCustomer(Long accountId, SessionID sessionId) {
		CustomerAccountVo customer = customerAccounts.get(accountId);
		
		String topic = String.format(IEventMessageTypeConstant.Control.Notification.ERROR_TEMPLATE, AppUtils.getAppName());
		monitorAgent.logError(customer.getCustomerNm() + ":onLogout", topic, MonitorConstant.FXADT.INFO_DISCONNECTED_TO_MARKET_ACCESS_PLATFORM, 
				"ROE Order Component Disconnected. Customer: " + customer.getCustomerNm() + ", Account: " + customer.getAccountId() +", Session: " + sessionId.toString());
		
		removeAllHeatBandForCustomer(customerAccounts.get(accountId));
	}
	
	
	
	@Override
	public void processNewOrderSingle(final String orderSender, final String orderTarget, final Hashtable<String, Object> orderParam, 
			final Long accountId) {
		RestingOrder.Builder newRestingOrder = RestingOrder.newBuilder();
		CustomerAccountVo orderCustomer = customerAccounts.get(accountId);
		
		newRestingOrder.setProduct(StaticDataUtils.getProductName(2L));
		newRestingOrder.setSize(String.valueOf(orderParam.get("orderQty")));
		newRestingOrder.setTradeAction(getTradeAction((char)orderParam.get("side")));
		newRestingOrder.setOrderType(getOrderType((char)orderParam.get("ordType")));
		newRestingOrder.setMarketTargetRate(String.valueOf(orderParam.get("price")));
		
		newRestingOrder.setBankTargetRate(String.valueOf(orderParam.get("price")));
		newRestingOrder.setExpiryDateTypeCd(getTimeInForce((char)orderParam.get("timeInForce")));
		newRestingOrder.setNotionalCurrency(String.valueOf(orderParam.get("currency")));
		newRestingOrder.setSymbol(String.valueOf(orderParam.get("symbol")));
		newRestingOrder.setTransactTimeStamp(Long.parseLong(String.valueOf(orderParam.get("transactTime"))));
		
		String wlRequestId = String.valueOf(orderParam.get("clOrdLinkID"));
		wlRequestId        = orderCustomer.getCustomerNm() + "-" + wlRequestId;
		
		//newRestingOrder.setExpiryDate(String.valueOf(orderParam.get("expireDate")));
		newRestingOrder.setRequestId(wlRequestId);
		newRestingOrder.setPossDupFlag((Boolean.parseBoolean(String.valueOf(orderParam.get("possDupFlag"))))? "Y": "N");
				
		newRestingOrder.setSalesMargin("0.000");
		newRestingOrder.setCoreMargin("0.000");
		newRestingOrder.setBankFillRate("0.000");
		newRestingOrder.setClientFillRate("0.000");
		newRestingOrder.setMarketFillRate("0.000");
		
		newRestingOrder.setChannelId(SYSTEM_USER_ID);
		newRestingOrder.setChannelNm(ChannelConstants.SDP);
		newRestingOrder.setCreateUserId(SYSTEM_USER_ID);
		newRestingOrder.setCreateUserName(SYSTEM_USER);
		newRestingOrder.setUpdateUserId(SYSTEM_USER_ID);
		newRestingOrder.setUpdateUserName(SYSTEM_USER);
		
		newRestingOrder.setExternalOrderId(String.valueOf(orderParam.get("clOrdID")));
		newRestingOrder.setAccountId(String.valueOf(orderCustomer.getAccountId()));
		newRestingOrder.setAccountNm(orderCustomer.getShortName());
		newRestingOrder.setCustomerId(String.valueOf(orderCustomer.getCustomerId()));
		newRestingOrder.setCustomerNm(orderCustomer.getCustomerNm());
				
		UserSessionInfo.Builder userSessInfo = UserSessionInfo.newBuilder();
		userSessInfo.setUserNm(SYSTEM_USER);
		userSessInfo.setUserId(SYSTEM_USER_ID);
		userSessInfo.setUserExtId(SYSTEM_USER_ID);
		
		newRestingOrder.setUserSessionInfo(userSessInfo);
				
		RestingOrderSubmitRequest.Builder restingOrderRequest = RestingOrderSubmitRequest.newBuilder();
		restingOrderRequest.addRestingOrder(newRestingOrder.build());
		restingOrderRequest.setRequestId(wlRequestId);
		restingOrderRequest.setSubmitType(RestingOrderSubmitType.SUBMIT);				
		
		logger.debug("Resting Order for NewOrderSingle: " + TextFormat.shortDebugString(restingOrderRequest.build()));
		
		TtMsg msgROE =  TtMsgEncoder.encode(restingOrderRequest.build());
		
		msgSender.send(IEventMessageTypeConstant.ClientTrader.ORDER_REQUEST_EVENT, msgROE);
		logger.info("Resting Order(NewOrderSingle) Send to ROE for Processing. Ext Msg Id: " + String.valueOf(orderParam.get("clOrdID")));
	}
	
	@Override
	public void processOrderCancelRequest(final String orderSender, final String orderTarget, final Hashtable<String, Object> orderParam,
			final Long accountId) {
		RestingOrder.Builder cancelRestingOrder = RestingOrder.newBuilder();
		CustomerAccountVo orderCustomer = customerAccounts.get(accountId);
		
		cancelRestingOrder.setTradeAction(getTradeAction((char)orderParam.get("side")));
		cancelRestingOrder.setSymbol(String.valueOf(orderParam.get("symbol")));
		cancelRestingOrder.setCreateTimestamp(Long.parseLong(String.valueOf(orderParam.get("transactTime"))));
		cancelRestingOrder.setTransactTimeStamp(Long.parseLong(String.valueOf(orderParam.get("transactTime"))));
		cancelRestingOrder.setOrderId(Long.parseLong(String.valueOf(orderParam.get("origClOrdID"))));
		cancelRestingOrder.setExternalOrderId(String.valueOf(orderParam.get("clOrdID")));
		
		cancelRestingOrder.setUpdateUserId(SYSTEM_USER_ID);
		cancelRestingOrder.setUpdateUserName(SYSTEM_USER);
		cancelRestingOrder.setAccountId(String.valueOf(orderCustomer.getAccountId()));
		cancelRestingOrder.setAccountNm(orderCustomer.getShortName());
		cancelRestingOrder.setCustomerId(String.valueOf(orderCustomer.getCustomerId()));
		cancelRestingOrder.setCustomerNm(orderCustomer.getCustomerNm());
		
		String wlRequestId = String.valueOf(orderParam.get("clOrdLinkID"));
		wlRequestId        = orderCustomer.getCustomerNm() + "-" + wlRequestId;
		
		RestingOrderSubmitRequest.Builder restingOrderRequest = RestingOrderSubmitRequest.newBuilder();
		restingOrderRequest.addRestingOrder(cancelRestingOrder.build());
		restingOrderRequest.setRequestId(wlRequestId);
		restingOrderRequest.setSubmitType(RestingOrderSubmitType.CANCEL);				
		
		logger.debug("Resting Order for OrderCancelRequest: " + TextFormat.shortDebugString(restingOrderRequest.build()));
		
		TtMsg msgROE =  TtMsgEncoder.encode(restingOrderRequest.build());
		
		msgSender.send(IEventMessageTypeConstant.ClientTrader.ORDER_REQUEST_EVENT, msgROE);
		logger.info("Resting Order(OrderCancelRequest) Send to ROE for Processing. Ext Msg Id: " + String.valueOf(orderParam.get("clOrdID")));
	}
	
	@Override
	public void processOrderStatusRequest(final String orderSender, final String orderTarget, final Hashtable<String, Object> orderParam,
			final Long accountId) {
		RestingOrder.Builder ordHeatBandChange = RestingOrder.newBuilder();
		CustomerAccountVo orderCustomer = customerAccounts.get(accountId);
		
		ordHeatBandChange.setOrderId(Long.parseLong(String.valueOf(orderParam.get("secondaryClOrdID"))));
		ordHeatBandChange.setExternalOrderId(String.valueOf(orderParam.get("clOrdID")));
		ordHeatBandChange.setTemperature(Integer.parseInt(String.valueOf(orderParam.get("ordStatusReqID"))));
		
		ordHeatBandChange.setAccountId(String.valueOf(orderCustomer.getAccountId()));
		ordHeatBandChange.setCustomerId(String.valueOf(orderCustomer.getCustomerId()));
		
		//logger.debug("Resting Order for HeatBandChangeRequest: " + TextFormat.shortDebugString(ordHeatBandChange.build()));
		
		TtMsg msgROE =  TtMsgEncoder.encode(ordHeatBandChange.build());
		
		//msgSender.send(IEventMessageTypeConstant.Transactional.TRAN_ROR_COM_TEMPERATURE, msgROE);
		//logger.info("Resting Order(HeatBandChangeRequest) Send to ROE for Processing. Ext Msg Id: " + String.valueOf(orderParam.get("clOrdID")));
	}
	
	
	
	@Override
	public void onMessage(TtMsg ttMsg, IMsgSessionInfo sessionInfo, IMsgProperties msgProperties) {
		String messageTopic     = msgProperties.getSendTopic();
		ByteString msgParameter = ttMsg.getParameters();
		
		try	{
			if(messageTopic.equals(IEventMessageTypeConstant.Transactional.TRAN_ROR_STATUS))	{
				RestingOrderStatus orderStatus = RestingOrderStatus.parseFrom(msgParameter);
				processRestingOrderStatusUpdate(orderStatus);
			}
		}
		catch(InvalidProtocolBufferException ipbExp)	{
			logger.error("Exception Processing Topic " +  messageTopic + " @DefaultROEMessageProcessor " + ipbExp.getMessage());
			logger.error("InvalidProtocolBufferException: ", ipbExp);
			ipbExp.printStackTrace();
		}
	}
	
	private void processRestingOrderStatusUpdate(RestingOrderStatus orderStatus)	{
		logger.info("Received Order Status Update..." + TextFormat.printToString(orderStatus));
		
		boolean isOrderSuccess = orderStatus.getSuccess();
		int orderStatusCode    = orderStatus.getStatusCd(0);
		String accountId       = null;
		SessionID sessionId    = null;
		String targetSession   = null;
		
		if(isOrderSuccess && orderStatusCode == 0)	{
			try	{
				Hashtable<String, Object> exeReportParam =  new Hashtable<>();
				RestingOrder restOrder = orderStatus.getRestingOrder(0);
				accountId = restOrder.getAccountId();
				
				exeReportParam.put("accountId", restOrder.getAccountId());
				exeReportParam.put("clOrdID", restOrder.getExternalOrderId());
				exeReportParam.put("ordStatus", getExecType(restOrder.getStatus()));
				exeReportParam.put("orderQty", restOrder.getSize());
				exeReportParam.put("symbol", restOrder.getSymbol());
				exeReportParam.put("currency", restOrder.getNotionalCurrency());
				
				String price         = restOrder.getMarketTargetRate();
				String bankFillPrice = restOrder.getBankFillRate();
				bankFillPrice        = ((bankFillPrice == null) || (bankFillPrice.trim().length() <= 0))? price: bankFillPrice;
				
				exeReportParam.put("price", restOrder.getMarketTargetRate());
				exeReportParam.put("bankFillPrice", bankFillPrice);
				exeReportParam.put("ordType", getOrderType(restOrder.getOrderType()));
				exeReportParam.put("side", getTradeAction(restOrder.getTradeAction()));
				exeReportParam.put("secondaryOrderID", restOrder.getOrderId());
				exeReportParam.put("transactTime", restOrder.getTransactTimeStamp());
				exeReportParam.put("text", restOrder.getStatusMessage());
											
				String wlRequestId = restOrder.getRequestId();
				String[] temp      = wlRequestId.split("\\-");
				if(temp.length > 1)
					exeReportParam.put("clOrdLinkID", temp[1]);
				
				targetSession = getCustomerOrderMsgTarget(customerAccounts.get(Long.valueOf(accountId)));
				sessionId     = fixMsgSender.sendExecutionReport(exeReportParam, targetSession);
				if(sessionId == null)	{
					synchronized(lockObject) {
						msgsNotTransmitted.get(Long.valueOf(restOrder.getAccountId())).add(exeReportParam);
					}			
					logger.warn("Message Not Transmitted. Queued For Later Delivery. Customer: " + restOrder.getAccountId() 
								+ " Id: " + restOrder.getOrderId() + " Ext. Id: " + restOrder.getExternalOrderId());
				}
			}
			catch(Exception exp)	{
				logger.error("Exception Processing  Order Status Update(IF1) " + exp.getMessage());
				logger.error("Exception: ", exp);
				exp.printStackTrace();
			}
		}
		
		if(!isOrderSuccess && (orderStatusCode == TickTradeErrorCode.ROR.ERROR_ORDER_CAPTURE_DISABLED
				|| orderStatusCode == TickTradeErrorCode.ROR.ERROR_ORDER_MGMT_RULE_NOT_DEFINED))	{
			try	{
				Hashtable<String, Object> exeReportParam =  new Hashtable<>();
				RestingOrder restOrder = orderStatus.getRestingOrder(0);
				accountId = restOrder.getAccountId();
				
				exeReportParam.put("accountId", restOrder.getAccountId());
				exeReportParam.put("clOrdID", restOrder.getExternalOrderId());
				exeReportParam.put("ordStatus", ExecType.REJECTED);
				exeReportParam.put("orderQty", restOrder.getSize());
				exeReportParam.put("symbol", restOrder.getSymbol());
				exeReportParam.put("currency", restOrder.getNotionalCurrency());
				
				String price         = restOrder.getMarketTargetRate();
				String bankFillPrice = restOrder.getBankFillRate();
				bankFillPrice        = ((bankFillPrice == null) || (bankFillPrice.trim().length() <= 0))? price: bankFillPrice;
				
				exeReportParam.put("price", restOrder.getMarketTargetRate());
				exeReportParam.put("bankFillPrice", bankFillPrice);
				exeReportParam.put("ordType", getOrderType(restOrder.getOrderType()));
				exeReportParam.put("side", getTradeAction(restOrder.getTradeAction()));
				exeReportParam.put("secondaryOrderID", restOrder.getOrderId());
				exeReportParam.put("transactTime", restOrder.getTransactTimeStamp());
				exeReportParam.put("text", restOrder.getStatusMessage());
				exeReportParam.put("rejReason", OrdRejReason.EXCHANGE_CLOSED);
							
				String wlRequestId = restOrder.getRequestId();
				String[] temp      = wlRequestId.split("\\-");
				if(temp.length > 1)
					exeReportParam.put("clOrdLinkID", temp[1]);
				
				targetSession = getCustomerOrderMsgTarget(customerAccounts.get(Long.valueOf(accountId)));
				sessionId = fixMsgSender.sendExecutionReport(exeReportParam, targetSession);
				if(sessionId == null)	{
					synchronized(lockObject) {
						msgsNotTransmitted.get(Long.valueOf(restOrder.getAccountId())).add(exeReportParam);
					}			
					logger.warn("Message Not Transmitted. Queued For Later Delivery. Customer: " + restOrder.getAccountId() 
								+ " Id: " + restOrder.getOrderId() + " Ext. Id: " + restOrder.getExternalOrderId());
				}
			}
			catch(Exception exp)	{
				logger.error("Exception Processing  Order Status Update(IF2) " + exp.getMessage());
				logger.error("Exception: ", exp);
				exp.printStackTrace();
			}
		}
		
		if(!isOrderSuccess && (orderStatusCode == TickTradeErrorCode.ROR.ERROR_NO_CANCEL_DUE_TO_ORDER_CAPTURE_DISABLED
				|| orderStatusCode == TickTradeErrorCode.ROR.ERROR_NO_CANCEL_DUE_TO_ORDER_LOCKED_BY_USER))	{
			try	{
				Hashtable<String, Object> cancelRejectParam =  new Hashtable<>();
				RestingOrder restOrder = orderStatus.getRestingOrder(0);
				accountId = restOrder.getAccountId();
				
				cancelRejectParam.put("accountId", restOrder.getAccountId());
				cancelRejectParam.put("clOrdID", restOrder.getExternalOrderId());
				cancelRejectParam.put("ordStatus", getExecType(restOrder.getStatus()));
				cancelRejectParam.put("orderQty", restOrder.getSize());
				cancelRejectParam.put("symbol", restOrder.getSymbol());
				cancelRejectParam.put("currency", restOrder.getNotionalCurrency());
				
				cancelRejectParam.put("ordType", getOrderType(restOrder.getOrderType()));
				cancelRejectParam.put("side", getTradeAction(restOrder.getTradeAction()));
				cancelRejectParam.put("origClOrdID", restOrder.getOrderId());
				cancelRejectParam.put("transactTime", restOrder.getTransactTimeStamp());
				cancelRejectParam.put("text", restOrder.getStatusMessage());
								
				String wlRequestId = restOrder.getRequestId();
				String[] temp      = wlRequestId.split("\\-");
				if(temp.length > 1)
					cancelRejectParam.put("clOrdLinkID", temp[1]);
				
				targetSession = getCustomerOrderMsgTarget(customerAccounts.get(Long.valueOf(accountId)));
				sessionId = fixMsgSender.sendOrderCancelReject(cancelRejectParam, targetSession);
			}
			catch(Exception exp)	{
				logger.error("Exception Processing  Order Status Update(IF3) " + exp.getMessage());
				logger.error("Exception: ", exp);
				exp.printStackTrace();
			}
		}
	}
	
	
	
	private void removeAllHeatBandForCustomer(CustomerAccountVo customer)	{
		RestingOrder.Builder ordHeatBandChange = RestingOrder.newBuilder();
		
		ordHeatBandChange.setOrderId(-1);
		ordHeatBandChange.setTemperature(-128);
		
		ordHeatBandChange.setAccountId(String.valueOf(customer.getAccountId()));
		ordHeatBandChange.setCustomerId(String.valueOf(customer.getCustomerId()));
		
		logger.info("Remove All HeatBandChangeRequest: " + TextFormat.shortDebugString(ordHeatBandChange.build()));
		
		TtMsg msgROE =  TtMsgEncoder.encode(ordHeatBandChange.build());
		
		//msgSender.send(IEventMessageTypeConstant.Transactional.TRAN_ROR_COM_TEMPERATURE, msgROE);
		logger.info("Resting Order(Remove All HeatBandChangeRequest) Send to ROE for Processing.");
	}
	
	private String getCustomerOrderMsgTarget(CustomerAccountVo customer)	{
		String ordMsgTarget = null;
		
		String customerName  = customer.getCustomerNm();
		String customerAccId = String.valueOf(customer.getAccountId());
		String msgTargetKey  = customerName + SEPARATOR + customerAccId + SEPARATOR + "ORD.MSG.TARGET";
		
		ordMsgTarget = appProp.getProperty(msgTargetKey);
		ordMsgTarget = ((ordMsgTarget == null) || (ordMsgTarget.trim().length() <= 0))? null: ordMsgTarget;
		
		return(ordMsgTarget);
	}
	
	private synchronized void transmitPendingMessages(Long customerAccid)	{
		Hashtable<String, Object> pendingMsgParam = null;
		SessionID sessionId  = null;
		String targetSession = null;
		
		logger.info("Delivering Pending Messages for Customer Account Id " + customerAccid);
		logger.info(msgsNotTransmitted.get(customerAccid).size() + " Message(s) is/are Pending Messages for Customer Account Id " + customerAccid);
		
		while(msgsNotTransmitted.get(customerAccid).size() > 0) {
			
			synchronized(lockObject) {
				pendingMsgParam = msgsNotTransmitted.get(customerAccid).remove();
			}
			
			logger.info("Pending Message Id: " + pendingMsgParam.get("secondaryOrderID") + " Ext. Ord Ref Id: " + pendingMsgParam.get("clOrdID"));
			targetSession = getCustomerOrderMsgTarget(customerAccounts.get(Long.valueOf(String.valueOf(pendingMsgParam.get("accountId")))));
			sessionId = fixMsgSender.sendExecutionReport(pendingMsgParam, targetSession);
			pendingMsgParam = null;
			sessionId = null;
		}
	}
	
	/*	TradeAction - Application Version	*/
	private String getTradeAction(char side)	{
		return((side == '1')? TradeAction.BUY: TradeAction.SELL);
	}
	/*	TradeAction - Quick Fix Version		*/
	private char getTradeAction(String side)	{
		return((side.equalsIgnoreCase(TradeAction.BUY))? Side.BUY: Side.SELL);
	}
	
	/*	OrderType - Application Version		*/
	private String getOrderType(char ordType)	{
		String orderType = OrderType.LIMIT;
		
		switch(ordType)	{
			case OrdType.LIMIT:
				orderType = OrderType.LIMIT;
				break;
			case OrdType.STOP:
				orderType = OrderType.STOP;
				break;	
		}
		
		return(orderType);
	}
	/*	OrderType - Quick Fix Version		*/
	private char getOrderType(String ordType)	{
		char orderType = OrdType.LIMIT;
		
		switch(ordType)	{
			case OrderType.LIMIT:
				orderType = OrdType.LIMIT;
				break;
			case OrderType.STOP:
				orderType = OrdType.STOP;
				break;
		}
		
		return(orderType);
	}
	
	/*	TimeInForce - Application Version	*/
	private String getTimeInForce(char tifValue)	{
		String timeInForce = OrderDurationType.GOOD_TIL_CANCELLED;
		
		switch(tifValue)	{
			case TimeInForce.GOOD_TILL_CANCEL:
				timeInForce = OrderDurationType.GOOD_TIL_CANCELLED;
				break;
			case TimeInForce.GOOD_TILL_DATE:
				timeInForce = OrderDurationType.GOOD_TIL_DATE;
				break;
		}
		
		return(timeInForce);	
	}
	
	private char getExecType(String ordStsCode)	{
		char execType = ExecType.NEW;
		
		switch(ordStsCode) {
			case PENDING:
				execType = ExecType.NEW;
				break;
			case DONE:
				execType = ExecType.FILL;
				break;
			case CANCELLED:
				execType = ExecType.CANCELED;
				break;
			case REJECTED:
				execType = ExecType.REJECTED;
				break;
			default:
				execType = ExecType.NEW;
				break;
		}
				
		return(execType);
	}
}
