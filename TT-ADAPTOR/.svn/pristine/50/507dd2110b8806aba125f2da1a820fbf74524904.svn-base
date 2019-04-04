package com.tts.plugin.adapter.impl.base.app.roe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.trade.RestingOrderMessage.RestingOrder;
import com.tts.message.trade.RestingOrderMessage.RestingOrderSubmitRequest;
import com.tts.message.trade.RestingOrderMessage.RestingOrderSubmitRequest.RestingOrderSubmitType;
import com.tts.message.trade.RestingOrderMessage.RestingOrderSummaryRequest;
import com.tts.message.util.TtMsgEncoder;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.dialect.IResponseDialectHelper;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.impl.base.app.AbstractSubscribingApp;
import com.tts.plugin.adapter.support.ICertifiedPublishingEndpoint;
import com.tts.plugin.adapter.support.IPublishingEndpoint;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.util.AppUtils;
import com.tts.util.constant.RestingOrderConstants;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.UnsupportedMessageType;
import quickfix.field.TimeInForce;


public class CIBCROEAppImpl extends AbstractSubscribingApp implements IFixListener	{
	public final static String NAME_TTS_ROE  = "ROE_IN_FIXADP_CIBC";
	private static final String SYSTEM_USER  = "system";
	private static final long SYSTEM_USER_ID = 1L;
	
	public static Logger logger;
	
	private boolean sendHeatBandNotification = false;
	private String sendHeatBandTarget        = null;
	private String sendOrderTarget           = null;
	private String migrateAllOrders          = null;
	private String sendOrdStatusTarget       = null;
	
	private final IResponseDialectHelper responseDialectHelper;
	private final IMkQfixApp qfixApp;
	
	private IMsgSenderFactory msgSenderFact;
	private IMsgSender msgSenderQ;
	private IMsgSender msgSender;
		
	public CIBCROEAppImpl(IMkQfixApp qfixApp, ISchedulingWorker worker, SessionInfo sessionInfo,
			IPublishingEndpoint iPublishingEndpoint, ICertifiedPublishingEndpoint iCertifiedPublishingEndpoint,
			IFixIntegrationPluginSpi iFixIntegrationPluginSpi) {
		super(qfixApp, worker, sessionInfo, iPublishingEndpoint, iCertifiedPublishingEndpoint, iFixIntegrationPluginSpi);
		
		AppUtils.createCustomRollingFileAppender("server-FixRoeCIBC." + AppUtils.getAppName() + ".log", "FIXROELogger", AppUtils.LOGLEVEL__DEBUG);
		logger = LoggerFactory.getLogger("FIXROELogger");

		this.qfixApp = qfixApp;
				
		this.msgSenderFact    = AppContext.getContext().getBean(IMsgSenderFactory.class);
		this.msgSender        = this.msgSenderFact.getMsgSender(false, false);
		this.msgSenderQ       = this.msgSenderFact.getMsgSender(false, false, true);
		this.responseDialectHelper = iFixIntegrationPluginSpi.getResponseDialectHelper();
		
		
		sendOrderTarget     = System.getenv("MARKET_ROE_EXEC_TARGETS");
		//migrateAllOrders   = System.getenv("MARKET_ROE_MIGRATE_ORDER");
		//sendHeatBandTarget = System.getenv("MARKET_ROE_HEAT_BAND_TARGETS");
		sendOrdStatusTarget = System.getenv("MARKET_ROE_STATUS_TARGETS");
		sendHeatBandNotification = ((sendHeatBandTarget != null) && (!sendHeatBandTarget.trim().isEmpty()))? true: false;
		
		if((sendOrderTarget != null) && (!sendOrderTarget.trim().isEmpty())) {
			qfixApp.setSessionResponseProcessor(sendOrderTarget, this);
		}
		if((sendOrdStatusTarget != null) && (!sendOrdStatusTarget.trim().isEmpty())) {
			qfixApp.setSessionResponseProcessor(sendOrdStatusTarget, this);
		}
		//doROEActiveOrderDataMigration();
		
		logger.info("<<<	Initialized ROEAppImpl		>>>");
		logger.info("MARKET_ROE_EXEC_TARGETS >>> " +  sendOrderTarget);
		logger.info("MARKET_ROE_HEAT_BAND_TARGETS >>> " +  sendHeatBandTarget);
		logger.info("MARKET_ROE_STATUS_TARGETS >>> " +  sendOrdStatusTarget);
	}
	
	@SuppressWarnings("unused")
	private void doROEActiveOrderDataMigration()	{
		boolean fixLoggedOn  = false;
		logger.info("MARKET_ROE_MIGRATE_ORDER - " + String.valueOf(migrateAllOrders));
		
		fixLoggedOn = ((sendOrderTarget != null) && (!sendOrderTarget.trim().isEmpty()) && (qfixApp.isLoggedOn(sendOrderTarget)));
		
		
		if((fixLoggedOn) && (migrateAllOrders != null) && (migrateAllOrders.equals("ALL")))	{
			RestingOrderSummaryRequest.Builder summaryRequest = RestingOrderSummaryRequest.newBuilder();
			
			summaryRequest.setRequestor("FXADT-ORD");
			logger.info("Requesting Order Migration for All Active Orders. " + TextFormat.printToString(summaryRequest.build()));
			
			TtMsg msgSummaryReq = TtMsgEncoder.encode(summaryRequest.build());
			msgSender.send(IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ROR_ORDERINFO, msgSummaryReq);
		}
	}

	@Override
	public void onRequest(String topic, TtMsg message) {
		RestingOrderSubmitRequest roeSMessage = null;
		RestingOrder roeMessage = null;
		String ttsRequestId     = null;
		String possDupFlag      = null;
		String extOrdId         = null;
		boolean successDelivery    = false;
		boolean isSessionConnected = false;
				
		try {
			if(sendHeatBandNotification && topic.equals(IEventMessageTypeConstant.Transactional.TRAN_ROR_TEMPERATURE))	{
				roeMessage = RestingOrder.parseFrom(message.getParameters());
				if(roeMessage.hasExternalOrderId())
					extOrdId = roeMessage.getExternalOrderId();
				
				if((extOrdId != null) && (!extOrdId.trim().isEmpty()))
					qfixApp.sendOrderHeatBandNotification(roeMessage, sendHeatBandTarget, AppType.ROETRADEADAPTER);
			}
			
			if(topic.equals(IEventMessageTypeConstant.Transactional.TRAN_ROR_ORDERSTATUS))	{
				roeSMessage  = RestingOrderSubmitRequest.parseFrom(message.getParameters());
				roeMessage   = roeSMessage.getRestingOrder(0);
				
				RestingOrderSubmitType submitType = roeSMessage.getSubmitType();
				logger.debug("ROE Msg For Processing. SubmitType: " + submitType + " " + TextFormat.printToString(roeSMessage));
				
				if(RestingOrderSubmitType.STATUS.equals(submitType) && roeMessage != null)	{
					String orderID = String.valueOf(roeMessage.getOrderId());		
					String clOrdID = roeMessage.getExternalOrderId();
					
					isSessionConnected = ((sendOrdStatusTarget == null) || (sendOrdStatusTarget.trim().isEmpty()))? false: qfixApp.isLoggedOn(sendOrdStatusTarget);
					if(isSessionConnected)
						qfixApp.sendOMSOrderStatusRequest(orderID, clOrdID, sendOrdStatusTarget, AppType.ROETRADEADAPTER);
				}
			}
			
			if(topic.equals(IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ROR_TO_MR_ORDER))	{
				roeSMessage  = RestingOrderSubmitRequest.parseFrom(message.getParameters());
				roeMessage   = roeSMessage.getRestingOrder(0);
				ttsRequestId = roeSMessage.getRequestId();
				possDupFlag  = roeSMessage.getPossDupFlag();
				
				RestingOrderSubmitType submitType = roeSMessage.getSubmitType();
				isSessionConnected = ((sendOrderTarget == null) || (sendOrderTarget.trim().isEmpty()))? false: qfixApp.isLoggedOn(sendOrderTarget);
				
				logger.info("ROE Msg For Processing. SubmitType: " + submitType + " Req. Id: " + ttsRequestId
						    + "Session: " + String.valueOf(isSessionConnected)
						    + " " + TextFormat.printToString(roeSMessage));
								
				if(RestingOrderSubmitType.SUBMIT.equals(submitType) && roeMessage != null)	{
					
					if(isSessionConnected)	{
						long expiryDateTime = roeMessage.getExpiryDate().getTimestamp();
						successDelivery     = qfixApp.sendOMSNewOrderSingleRequest(roeMessage, sendOrderTarget, AppType.ROETRADEADAPTER, possDupFlag, expiryDateTime);
					}
					else	{
						/**
						 * 	BY DEFAULT REJECT NEW ORDER SINGLE MESSAGE IF FIX SESSION NOT CONNECTED.
						 */
						
						RestingOrder.Builder roeMsgBuilder = roeMessage.toBuilder();
						roeMsgBuilder.setStatus(RestingOrderConstants.OrderStateCd.REJECTED);
						roeMsgBuilder.setStatusMessage("ORDER REJECTED:SESSION OFFLINE");
						
						broadcastUpdatedOrder(ttsRequestId, roeMsgBuilder);
					}
				}
				else if(RestingOrderSubmitType.CANCEL.equals(submitType) && roeMessage != null)	{
					
					if(isSessionConnected)	{
						String clOrdID  = String.valueOf(roeMessage.getOrderId());
						clOrdID         = ("CXL" + clOrdID);
						successDelivery = qfixApp.sendOMSOrderCancelRequest(clOrdID, roeMessage, sendOrderTarget, AppType.ROETRADEADAPTER);
					}
					else	{
						/**
						 * 	BY DEFAULT REJECT CANCEL REQUEST MESSAGE IF FIX SESSION NOT CONNECTED.
						 */
						
						RestingOrder.Builder roeMsgBuilder = roeMessage.toBuilder();
						roeMsgBuilder.setStatus(RestingOrderConstants.OrderStateCd.ACTIVE);
						roeMsgBuilder.setStatusMessage("CANCEL REJECTED:SESSION OFFLINE");
						
						broadcastUpdatedOrder(ttsRequestId, roeMsgBuilder);
					}
				}
				else if(RestingOrderSubmitType.HEAT_BAND.equals(submitType) && roeMessage != null && sendHeatBandNotification)
					qfixApp.sendOrderHeatBandNotification(roeMessage, sendHeatBandTarget, AppType.ROETRADEADAPTER);
			}				
		} 
		catch (InvalidProtocolBufferException ipbExp) {
			logger.error("InvalidProtocolBufferException While Processing Msg @ " + NAME_TTS_ROE + " " + ipbExp.getMessage());
			logger.error("InvalidProtocolBufferException: ", ipbExp);
		}
	}

	@Override
	public String[] getDesiredTopics() {
		return new String[] { IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_ALL_ORDER, 
				  			  IEventMessageTypeConstant.Transactional.TRAN_ROR_ORDERSTATUS };
	}

	@Override
	public ChangeTradingSessionBehavior getChangeTradingSessionBehavior() {
		return ChangeTradingSessionBehavior.NO_CHANGE;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getName() {
		return NAME_TTS_ROE;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onMessage(Message message, IQfixRoutingAgent routingAgent)
			throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		String ttsRequestId = "";
		String orderId      = "";
		RestingOrder.Builder roeBuilder = RestingOrder.newBuilder();
		roeBuilder.setUpdateUserId(SYSTEM_USER_ID);
		roeBuilder.setUpdateUserName(SYSTEM_USER);
		
		logger.info("ROE FIX Response Msg: " + message.toString());
				
		if((message != null)  && (message instanceof quickfix.fix50.ExecutionReport))
			orderId = responseDialectHelper.convertRestOrderResponseAndUpdate((quickfix.fix50.ExecutionReport) message, roeBuilder);
				
		if((message != null)  && (message instanceof quickfix.fix50.OrderCancelReject))
			orderId = responseDialectHelper.convertRestOrderResponseAndUpdate((quickfix.fix50.OrderCancelReject) message, roeBuilder);
		
		
		logger.info("BroadCasting ROE FIX Response Msg: " + orderId);
		broadcastUpdatedOrder(ttsRequestId, roeBuilder);
	}

	@Override
	public void onFixSessionLogoff() {
		//	TODO
	}
	
	@Override
	public void onFixSessionLogon() {
		//	TODO
	}
	
	private boolean broadcastUpdatedOrder(String ttsRequestId, RestingOrder.Builder restingOrderBuilder)	{
		RestingOrderSubmitRequest.Builder restOrdResp = RestingOrderSubmitRequest.newBuilder();
		restOrdResp.setRequestId("");
		restOrdResp.addRestingOrder(restingOrderBuilder.build());
		
		TtMsg msgROE    =  TtMsgEncoder.encode(restOrdResp.build());
		boolean success = msgSenderQ.send(IEventMessageTypeConstant.RestingOrderMgmt.RR_TRAN_MR_TO_ROR_STATUS, msgROE);
		logger.info("Send: " +  String.valueOf(success) + ", Status: " + restingOrderBuilder.getStatus() + " ROE Response Msg: " 
		                     + TextFormat.printToString(restOrdResp.build()));
		
		return(success);
	}
}