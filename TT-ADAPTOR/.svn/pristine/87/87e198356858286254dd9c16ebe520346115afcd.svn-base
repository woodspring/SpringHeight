package com.tts.fixapi.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.fixapi.core.FIXAcceptorMessageBuilderInvoker;
import com.tts.fixapi.core.FIXAcceptorSession;
import com.tts.fixapi.type.IFIXAcceptorIntegrationPlugin;
import com.tts.fixapi.type.IFIXAcceptorMessageBuilder;
import com.tts.fixapi.type.IFIXAcceptorMessageBuilder.MessageBuilder;
import com.tts.fixapi.type.IFIXAcceptorMessageDispatcher;
import com.tts.fixapi.type.IFIXAcceptorMessageProcessor;
import com.tts.fixapi.type.IFIXAcceptorRoutingAgent;
import com.tts.service.db.IAccountProfileService;
import com.tts.util.AppContext;
import com.tts.vo.CustomerAccountVo;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;


@SuppressWarnings("unused")
public class FIXAcceptorApplication extends MessageCracker implements IFIXAcceptorMessageDispatcher {
	private static final String SEPARATOR = ".";
	private static final Logger logger = LoggerFactory.getLogger("FixAPILogger");
	private final Map<Class<?>, FIXAcceptorMessageBuilderInvoker> methodInvokers;
		
	private final SessionSettings fixSessionSettings;
	private final IFIXAcceptorIntegrationPlugin fixPlugin;
	private final IFIXAcceptorMessageBuilder fixMsgBuilder;
	private final IFIXAcceptorRoutingAgent fixRoutingAgent;
	private final IAccountProfileService profService;
	private final IFIXAcceptorMessageProcessor roeMsgProcessor;
	
	public FIXAcceptorApplication(SessionSettings fixSessionSettings, IFIXAcceptorIntegrationPlugin fixPlugin) {
		this.fixSessionSettings = fixSessionSettings;
		this.fixPlugin          = fixPlugin;
		this.fixMsgBuilder      = fixPlugin.getFIXMessageBuilder();
		this.fixRoutingAgent    = fixPlugin.getFIXRoutingAgent();
		this.methodInvokers     = new ConcurrentHashMap<>();
		
		this.profService        = AppContext.getContext().getBean(IAccountProfileService.class);
		
		
		/**
		 * 	TODO
		 * 	Use AppContext.getContext().getBeansOfType(arg0) to find
		 * 	all Classes implementing IFIXAcceptorMessageProcessor.class
		 * 	Helps to find all parties interested in Messages
		 * 	Getting INTERESTED SENDERS can be used to dynamically send messages.
		 */
		this.roeMsgProcessor    = AppContext.getContext().getBean(IFIXAcceptorMessageProcessor.class);
		
		/**
		 *	Assign Message Sender to ROE Message Processor.
		 *	Used By Message Processor to Send Response.
		 */
		roeMsgProcessor.setFIXMessageDispatcher(this);
		
		initializeMessageBuilderMethods();
		logger.info("FIX Acceptor Application Initialized...");
	}
	
	private void initializeMessageBuilderMethods()	{
		logger.info("Scanning For Message Builder Methods...");
		
		Class<?> msgBuilderClass = fixMsgBuilder.getClass();
		for(Method method: msgBuilderClass.getMethods())	{
			if(isMessageBuilderMethod(method))	{
				Class<?> fixMessageType = method.getParameterTypes()[0];
				FIXAcceptorMessageBuilderInvoker invoker = new FIXAcceptorMessageBuilderInvoker(fixMsgBuilder, method);
				FIXAcceptorMessageBuilderInvoker existingInvoker = methodInvokers.get(fixMessageType);
				if(existingInvoker != null)		{
					logger.error("Duplicate Message Builder Method found for " + fixMessageType.getTypeName());
					throw new RedundantHandlerException(fixMessageType, existingInvoker.getInvokeMethod(), method);
				}
					
				methodInvokers.put(fixMessageType, invoker);
			}
		}
		
		logger.info("Completed Scanning For Message Builder Methods... Found " + methodInvokers.size());
	}
	
	private boolean isMessageBuilderMethod(Method method)	{
		Class<?>[] parameterTypes = method.getParameterTypes();
		return(isMessageBuilderAnnotated(method)
				&& parameterTypes.length == 2
				&& Message.class.isAssignableFrom(parameterTypes[0])
				&& parameterTypes[1] == Hashtable.class);
	}
	
	private boolean isMessageBuilderAnnotated(Method method)	{
		return(method.isAnnotationPresent(MessageBuilder.class));
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		// TODO Auto-generated method stub
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		crack(message, sessionId);
	}

	@Override
	public void onCreate(SessionID sessionId) {
		logger.info("onCreate: " + sessionId.toString());
	}

	@Override
	public void onLogon(SessionID sessionId) {
		logger.info("onLogon: " + sessionId.toString());
		logger.info("beginString: " + sessionId.getBeginString() + ", senderCompId: " + sessionId.getSenderCompID() + ", targetCompId: " + sessionId.getTargetCompID());
		logger.info("FIX Session Version: " + (FIXAcceptorSession.fromString(sessionId.getBeginString())).getFixVersion());
		
		String key = sessionId.getSenderCompID() +  SEPARATOR + sessionId.getTargetCompID();
		
		try	{
			fixRoutingAgent.registerSession(sessionId);
			
			
			//	Find CUSTOMER_LEGAL_NM from Session Data
			String[] temp = sessionId.getTargetCompID().split("\\-");
			String custLegalName = temp[0];
			
			List<CustomerAccountVo> listCustomerAccount = profService.findByCustomerLegalNm(custLegalName, false);
			if((listCustomerAccount != null) && (listCustomerAccount.size() > 0))	{
				
				if(listCustomerAccount.size() > 1)	{
					logger.warn("MULTIPLE CUSTOMER CONFIGURATION FOUND FOR " + custLegalName);
					logger.warn("ACCEPTING CONFIGURATION. ACC. ID: " + listCustomerAccount.get(0).getAccountId() + " CUSTOMER: " + custLegalName);
				}
				
				FIXAcceptorSession fixSession = new FIXAcceptorSession(sessionId, FIXAcceptorSession.fromString(sessionId.getBeginString()),
						listCustomerAccount.get(0).getAccountId(), custLegalName);
				fixAppSessions.put(key, fixSession);

				
				roeMsgProcessor.notifySessionConnectionForCustomer(listCustomerAccount.get(0), sessionId);
			}
		}
		catch(Exception exp)	{
			logger.error("Exception onLogon. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}

	@Override
	public void onLogout(SessionID sessionId) {
		logger.info("onLogout: " + sessionId.toString());
		
		fixRoutingAgent.unregisterSession(sessionId);
		
		//	Clean up Client Details on disconnection/logout
		String key = sessionId.getSenderCompID() +  SEPARATOR + sessionId.getTargetCompID();
		FIXAcceptorSession fixSession = fixAppSessions.remove(key);
		if(fixSession != null)	{	
			roeMsgProcessor.notifySessionDisconnectionForCustomer(fixSession.getAccountId(), sessionId);
		}
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		// TODO Auto-generated method stub
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		// TODO Auto-generated method stub
	}
	
	
	
	@Handler
	public void onBusinessMessageReceived(quickfix.fix44.NewOrderSingle orderSingleMessage, SessionID sessionId)	{
		try	{
			logger.debug("Received FIX.4.4 NewOrderSingle: " + orderSingleMessage.toString());
			
			String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
			FIXAcceptorSession nosSession = fixAppSessions.get(key);
						
			String orderSender = orderSingleMessage.getHeader().getString(49);
			String orderTarget = orderSingleMessage.getHeader().getString(56);
			
			boolean possDupFlag = false; 
			if(orderSingleMessage.getHeader().isSetField(43))
				possDupFlag = orderSingleMessage.getHeader().getBoolean(43);
			
			String expDate   = "";
			if(orderSingleMessage.isSetField(432))
				expDate = orderSingleMessage.getExpireDate().getValue();
			
			Hashtable<String, Object> nosParams = new Hashtable<>();
			
			String clOrdLinkID = orderSingleMessage.getClOrdLinkID().getValue();
			clOrdLinkID        = ((clOrdLinkID == null) || (clOrdLinkID.trim().length() <= 0))? String.valueOf(System.currentTimeMillis()): clOrdLinkID;
						
			nosParams.put("clOrdID", orderSingleMessage.getClOrdID().getValue());
			nosParams.put("orderQty", orderSingleMessage.getOrderQty().getValue());
			nosParams.put("side", orderSingleMessage.getSide().getValue());
			nosParams.put("ordType", orderSingleMessage.getOrdType().getValue());
			nosParams.put("price", orderSingleMessage.getPrice().getValue());
			nosParams.put("timeInForce", orderSingleMessage.getTimeInForce().getValue());
			nosParams.put("currency", orderSingleMessage.getCurrency().getValue());
			nosParams.put("symbol", orderSingleMessage.getSymbol().getValue());
			nosParams.put("transactTime", orderSingleMessage.getTransactTime().getValue().getTime());
			nosParams.put("clOrdLinkID", clOrdLinkID);
			nosParams.put("expireDate", expDate);
			nosParams.put("possDupFlag", possDupFlag);
			
			roeMsgProcessor.processNewOrderSingle(orderSender, orderTarget, nosParams, nosSession.getAccountId());
		}
		catch(Exception exp)	{
			logger.error("Exception Processing quickfix.fix44.NewOrderSingle. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	@Handler
	public void onBusinessMessageReceived(quickfix.fix50.NewOrderSingle orderSingleMessage, SessionID sessionId)	{
		try	{
			logger.debug("Received FIX.5.0 NewOrderSingle: " + orderSingleMessage.toString());
			
			String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
			FIXAcceptorSession nosSession = fixAppSessions.get(key);
						
			String orderSender = orderSingleMessage.getHeader().getString(49);
			String orderTarget = orderSingleMessage.getHeader().getString(56);
			
			boolean possDupFlag = false; 
			if(orderSingleMessage.getHeader().isSetField(43))
				possDupFlag = orderSingleMessage.getHeader().getBoolean(43);
			
			String expDate   = "";
			if(orderSingleMessage.isSetField(432))
				expDate = orderSingleMessage.getExpireDate().getValue();
			
			Hashtable<String, Object> nosParams = new Hashtable<>();
			
			String clOrdLinkID = orderSingleMessage.getClOrdLinkID().getValue();
			clOrdLinkID        = ((clOrdLinkID == null) || (clOrdLinkID.trim().length() <= 0))? String.valueOf(System.currentTimeMillis()): clOrdLinkID;
						
			nosParams.put("clOrdID", orderSingleMessage.getClOrdID().getValue());
			nosParams.put("orderQty", orderSingleMessage.getOrderQty().getValue());
			nosParams.put("side", orderSingleMessage.getSide().getValue());
			nosParams.put("ordType", orderSingleMessage.getOrdType().getValue());
			nosParams.put("price", orderSingleMessage.getPrice().getValue());
			nosParams.put("timeInForce", orderSingleMessage.getTimeInForce().getValue());
			nosParams.put("currency", orderSingleMessage.getCurrency().getValue());
			nosParams.put("symbol", orderSingleMessage.getSymbol().getValue());
			nosParams.put("transactTime", orderSingleMessage.getTransactTime().getValue().getTime());
			nosParams.put("clOrdLinkID", clOrdLinkID);
			nosParams.put("expireDate", expDate);
			nosParams.put("possDupFlag", possDupFlag);
			
			roeMsgProcessor.processNewOrderSingle(orderSender, orderTarget, nosParams, nosSession.getAccountId());
		}
		catch(Exception exp)	{
			logger.error("Exception Processing quickfix.fix44.NewOrderSingle. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	@Handler
	public void onBusinessMessageReceived(quickfix.fix44.OrderCancelRequest orderCancelReqMessage, SessionID sessionId)	{
		try	{
			logger.debug("Received FIX.4.4 OrderCancelRequest: " + orderCancelReqMessage.toString());
			
			String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
			FIXAcceptorSession nosSession = fixAppSessions.get(key);
						
			String orderSender = orderCancelReqMessage.getHeader().getString(49);
			String orderTarget = orderCancelReqMessage.getHeader().getString(56);
			
			Hashtable<String, Object> ocrParams = new Hashtable<>();
			
			ocrParams.put("clOrdID", orderCancelReqMessage.getClOrdID().getValue());
			ocrParams.put("origClOrdID", orderCancelReqMessage.getOrigClOrdID().getValue());
			ocrParams.put("side", orderCancelReqMessage.getSide().getValue());
			ocrParams.put("symbol", orderCancelReqMessage.getSymbol().getValue());
			ocrParams.put("transactTime", orderCancelReqMessage.getTransactTime().getValue().getTime());
			ocrParams.put("clOrdLinkID", orderCancelReqMessage.getClOrdLinkID().getValue());
			
			roeMsgProcessor.processOrderCancelRequest(orderSender, orderTarget, ocrParams, nosSession.getAccountId());
			
		}
		catch(Exception exp)	{
			logger.error("Exception Processing quickfix.fix44.OrderCancelRequest. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	@Handler
	public void onBusinessMessageReceived(quickfix.fix50.OrderCancelRequest orderCancelReqMessage, SessionID sessionId)	{
		try	{
			logger.debug("Received FIX.5.0 OrderCancelRequest: " + orderCancelReqMessage.toString());
			
			String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
			FIXAcceptorSession nosSession = fixAppSessions.get(key);
						
			String orderSender = orderCancelReqMessage.getHeader().getString(49);
			String orderTarget = orderCancelReqMessage.getHeader().getString(56);
			
			Hashtable<String, Object> ocrParams = new Hashtable<>();
			
			ocrParams.put("clOrdID", orderCancelReqMessage.getClOrdID().getValue());
			ocrParams.put("origClOrdID", orderCancelReqMessage.getOrigClOrdID().getValue());
			ocrParams.put("side", orderCancelReqMessage.getSide().getValue());
			ocrParams.put("symbol", orderCancelReqMessage.getSymbol().getValue());
			ocrParams.put("transactTime", orderCancelReqMessage.getTransactTime().getValue().getTime());
			ocrParams.put("clOrdLinkID", orderCancelReqMessage.getClOrdLinkID().getValue());
			
			roeMsgProcessor.processOrderCancelRequest(orderSender, orderTarget, ocrParams, nosSession.getAccountId());
		}
		catch(Exception exp)	{
			logger.error("Exception Processing quickfix.fix50.OrderCancelRequest. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	@Handler
	public void onBusinessMessageReceived(quickfix.fix44.OrderStatusRequest orderStatusReqMessage, SessionID sessionId)	{
		try	{
			//logger.debug("Received FIX.4.4 OrderStatusRequest: " + orderStatusReqMessage.toString());
			
			String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
			FIXAcceptorSession nosSession = fixAppSessions.get(key);
						
			String orderSender = orderStatusReqMessage.getHeader().getString(49);
			String orderTarget = orderStatusReqMessage.getHeader().getString(56);
			
			Hashtable<String, Object> ocrParams = new Hashtable<>();
			
			ocrParams.put("clOrdID", orderStatusReqMessage.getClOrdID().getValue());
			ocrParams.put("secondaryClOrdID", orderStatusReqMessage.getSecondaryClOrdID().getValue());
			ocrParams.put("ordStatusReqID", orderStatusReqMessage.getOrdStatusReqID().getValue());
			
			roeMsgProcessor.processOrderStatusRequest(orderSender, orderTarget, ocrParams, nosSession.getAccountId());
		}
		catch(Exception exp)	{
			logger.error("Exception Processing quickfix.fix44.OrderStatusRequest. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}
	
	@Handler
	public void onBusinessMessageReceived(quickfix.fix50.OrderStatusRequest orderStatusReqMessage, SessionID sessionId)	{
		try	{
			//logger.debug("Received FIX.5.0 OrderStatusRequest: " + orderStatusReqMessage.toString());
			
			String key = sessionId.getSenderCompID() + SEPARATOR + sessionId.getTargetCompID();
			FIXAcceptorSession nosSession = fixAppSessions.get(key);
						
			String orderSender = orderStatusReqMessage.getHeader().getString(49);
			String orderTarget = orderStatusReqMessage.getHeader().getString(56);
			
			Hashtable<String, Object> ocrParams = new Hashtable<>();
			
			ocrParams.put("clOrdID", orderStatusReqMessage.getClOrdID().getValue());
			ocrParams.put("secondaryClOrdID", orderStatusReqMessage.getSecondaryClOrdID().getValue());
			ocrParams.put("ordStatusReqID", orderStatusReqMessage.getOrdStatusReqID().getValue());
			
			roeMsgProcessor.processOrderStatusRequest(orderSender, orderTarget, ocrParams, nosSession.getAccountId());
		}
		catch(Exception exp)	{
			logger.error("Exception Processing quickfix.fix50.OrderStatusRequest. " + exp.getMessage());
			logger.error("Exception: ", exp);
			exp.printStackTrace();
		}
	}

	
	
	@Override
	public SessionID sendExecutionReport(Hashtable<String, Object> msgParam, String targetSession) {
		Message message     = null;
		SessionID sessionId = null;
				
		if(targetSession == null)	{
			logger.error("FAILED TO SEND EXECUTION REPORT. CUSTOMER ACC. ID: " + String.valueOf(msgParam.get("accountId")));
			logger.error("MISSING REQUIRED PROPERTY CONFIGURATION.");
			return(sessionId);
		}
		
		try	{
			FIXAcceptorSession fixSession = fixAppSessions.get(targetSession);
			if(fixSession == null)
				logger.error("FIX SESSION NOT FOUND. SESSION: " + targetSession);
			
			Class<?> exeReportVersion = fixSession.getExecutionReportMessageVersion();
			FIXAcceptorMessageBuilderInvoker exeReportBuilder = methodInvokers.get(exeReportVersion);
			message = (Message) exeReportBuilder.invokeMethod(msgParam);
			
			logger.debug("Target: " + targetSession + " ROE Execution Report: " + message.toString());
			sessionId = fixRoutingAgent.sendMessge(message, targetSession);
		}
		catch(ClassNotFoundException cnfExp)	{
			logger.error("ClassNotFoundException When Sending Execution Report. " + cnfExp.getMessage());
			logger.error("ClassNotFoundException: " , cnfExp);
			cnfExp.printStackTrace();
			sessionId = null;
		}
		catch(IllegalArgumentException | IllegalAccessException | InvocationTargetException | InstantiationException invokeExp) {
			logger.error("MultipleException When Sending Execution Report. " + invokeExp.getMessage());
			logger.error("MultipleException: " , invokeExp);
			invokeExp.printStackTrace();
			sessionId = null;
		}
		catch(Exception exp) {
			logger.error("Exception When Sending Execution Report. " + exp.getMessage());
			logger.error("Exception: " , exp);
			exp.printStackTrace();
			sessionId = null;
		}
		
		return(sessionId);
	}
	
	@Override
	public SessionID sendOrderCancelReject(Hashtable<String, Object> msgParam, String targetSession) {
		Message message     = null;
		SessionID sessionId = null;
				
		if(targetSession == null)	{
			logger.error("FAILED TO SEND CANCEL REJECT. CUSTOMER ACC. ID: " + String.valueOf(msgParam.get("accountId")));
			logger.error("MISSING REQUIRED PROPERTY CONFIGURATION.");
			return(sessionId);
		}
		
		try	{
			FIXAcceptorSession fixSession = fixAppSessions.get(targetSession);
			if(fixSession == null)
				logger.error("FIX SESSION NOT FOUND. SESSION: " + targetSession);
			
			Class<?> cancelRejectVersion = fixSession.getOrderCancelRejectMessageVersion();
			FIXAcceptorMessageBuilderInvoker cancelRejectBuilder = methodInvokers.get(cancelRejectVersion);
			message = (Message) cancelRejectBuilder.invokeMethod(msgParam);
			
			logger.debug("Target: " + targetSession + " ROE Order Cancel Reject: " + message.toString());
			sessionId = fixRoutingAgent.sendMessge(message, targetSession);
		}
		catch(ClassNotFoundException cnfExp)	{
			logger.error("ClassNotFoundException When Sending Order Cancel Reject. " + cnfExp.getMessage());
			logger.error("ClassNotFoundException: " , cnfExp);
			cnfExp.printStackTrace();
			sessionId = null;
		}
		catch(IllegalArgumentException | IllegalAccessException | InvocationTargetException | InstantiationException invokeExp) {
			logger.error("MultipleException When Sending Order Cancel Reject. " + invokeExp.getMessage());
			logger.error("MultipleException: " , invokeExp);
			invokeExp.printStackTrace();
			sessionId = null;
		}
		catch(Exception exp) {
			logger.error("Exception When Sending Order Cancel Reject. " + exp.getMessage());
			logger.error("Exception: " , exp);
			exp.printStackTrace();
			sessionId = null;
		}
		
		return(sessionId);
	}
}
