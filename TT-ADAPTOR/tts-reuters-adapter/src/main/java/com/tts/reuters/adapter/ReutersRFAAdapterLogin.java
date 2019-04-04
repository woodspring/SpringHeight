package com.tts.reuters.adapter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.omm.OMMElementList;
import com.reuters.rfa.omm.OMMEncoder;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.omm.OMMState;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.rdm.RDMMsgTypes;
import com.reuters.rfa.rdm.RDMUser;
import com.reuters.rfa.session.omm.OMMConnectionEvent;
import com.reuters.rfa.session.omm.OMMItemEvent;
import com.reuters.rfa.session.omm.OMMItemIntSpec;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.reuters.adapter.utils.ReutersGenericOMMParser;


public class ReutersRFAAdapterLogin implements Client
{
	private ReutersRFAAdapter _mainApp = null;
	private Handle _loginHandle        = null;
	
	private String _rfaUser            = null;
	
	private static ByteArrayOutputStream baosMsg = new ByteArrayOutputStream();
	private static Logger _rfaAppLogger          = LoggerFactory.getLogger("ReutersAppLogger");
	private static Logger _rfaMsgLogger          = LoggerFactory.getLogger("ReutersMsgLogger");
		
	private static final String APP_ID     = "256";
	private static final String POSITION   = "1.1.1.1/net";
	
	public ReutersRFAAdapterLogin(ReutersRFAAdapter mainApp) {
		this._mainApp     = mainApp;
		this._loginHandle = null;
	}
		
	public void sendLoginRequest()	{
		OMMMsg rfaLoginMsg        = encodeLoginReqMsg();
		OMMItemIntSpec rfaIntSpec = new OMMItemIntSpec();
		
		baosMsg.reset();
		PrintStream psLogin = new PrintStream(baosMsg);
		ReutersGenericOMMParser.parseMsg(rfaLoginMsg, psLogin);
		String loginMsg = new String(baosMsg.toByteArray(), StandardCharsets.UTF_8);
		_rfaMsgLogger.info("LOGIN REQ. >> \n" + loginMsg);
		
		rfaIntSpec.setMsg(rfaLoginMsg);
		_loginHandle = _mainApp.get_ommConsumer().registerClient(_mainApp.get_eventQueue(), rfaIntSpec, this, null);
		_rfaAppLogger.info("RFA Login LOGIN REQ SENT @ " + System.currentTimeMillis());
	}
	
	private OMMMsg encodeLoginReqMsg()	{
        OMMEncoder encoder = _mainApp.get_ommEncoder();
        OMMPool pool       = _mainApp.get_ommPool();
       
        encoder.initialize(OMMTypes.MSG, 500);
        
        OMMMsg loginReqMsg = pool.acquireMsg();
        loginReqMsg.setMsgType(OMMMsg.MsgType.REQUEST);
        loginReqMsg.setMsgModelType(RDMMsgTypes.LOGIN);
        loginReqMsg.setIndicationFlags(OMMMsg.Indication.REFRESH);
        loginReqMsg.setAttribInfo(null, _rfaUser, RDMUser.NameType.USER_NAME);
        
        encoder.encodeMsgInit(loginReqMsg, OMMTypes.ELEMENT_LIST, OMMTypes.NO_DATA);
        encoder.encodeElementListInit(OMMElementList.HAS_STANDARD_DATA, (short)0, (short)0);
        encoder.encodeElementEntryInit(RDMUser.Attrib.ApplicationId, OMMTypes.ASCII_STRING);
        encoder.encodeString(APP_ID, OMMTypes.ASCII_STRING);
        encoder.encodeElementEntryInit(RDMUser.Attrib.Position, OMMTypes.ASCII_STRING);
        encoder.encodeString(POSITION, OMMTypes.ASCII_STRING);
        encoder.encodeElementEntryInit(RDMUser.Attrib.Role, OMMTypes.UINT);
        encoder.encodeUInt((long)RDMUser.Role.CONSUMER);
        encoder.encodeAggregateComplete();

        OMMMsg encMsg = (OMMMsg)encoder.getEncodedObject();
        pool.releaseMsg(loginReqMsg);

        return(encMsg);
    }
	
	public void closeLoginRequest()	{
		if(_loginHandle != null)
			_mainApp.get_ommConsumer().unregisterClient(_loginHandle);
		
		_loginHandle = null;
		_mainApp     = null;
		
		_rfaAppLogger.info("RFA Login CLOSING LOGIN HANDLE...");
	}
	
	@Override
	public void processEvent(Event event) {
		if(event.getType() == Event.COMPLETION_EVENT)	{
            _rfaAppLogger.warn("RFA Login Received a COMPLETION_EVENT. >> " + event.getHandle());
            return;
        }
        
        if(event.getType() == Event.OMM_CONNECTION_EVENT)	{
            OMMConnectionEvent connectionEvent = ((OMMConnectionEvent)event);
            
            StringBuffer sb = new StringBuffer();            
            sb.append("RFA Login Receive an OMM_CONNECTION_EVENT")
           	  .append("\n\tName: " + connectionEvent.getConnectionName())
              .append("\n\tStatus: " + connectionEvent.getConnectionStatus().toString())
              .append("\n\tHost: " + connectionEvent.getConnectedHostName())
              .append("\n\tPort: " + connectionEvent.getConnectedPort())
              .append("\n\tComponentVersion: " + connectionEvent.getConnectedComponentVersion());
            
            _rfaAppLogger.warn(sb.toString());
            
            sb = null;
            return;
        }
        
        if(event.getType() != Event.OMM_ITEM_EVENT)	{
        	 _rfaAppLogger.warn("RFA Login Received an unsupported Event type. >> " + event.getType());
            _mainApp.doApplicationCleanup(1);
            return;
        }

        
        _rfaAppLogger.info("RFA Login Received Login Response @ " + System.currentTimeMillis());

        OMMItemEvent ie = (OMMItemEvent)event;
        OMMMsg respMsg  = ie.getMsg();
                
        
        baosMsg.reset();        
        PrintStream psLogin = new PrintStream(baosMsg);        
                
        //ReutersGenericOMMParser.parse(respMsg);
        ReutersGenericOMMParser.parseMsg(respMsg, psLogin);
        String loginResp = new String(baosMsg.toByteArray(), StandardCharsets.UTF_8);
        _rfaMsgLogger.info("LOGIN RESP. >> \n" + loginResp);
        
        
        if(respMsg.getMsgModelType() != RDMMsgTypes.LOGIN)	{
            _rfaAppLogger.error("RFA Login Received a NON-LOGIN model type.");
            _mainApp.doApplicationCleanup(1);            
            return;
        }

        if(respMsg.isFinal())	{
            _rfaAppLogger.error("RFA Login Login Response message is final.");
            _mainApp.set_successLogin(false);                       
            return;
        }

        if ((respMsg.getMsgType() == OMMMsg.MsgType.STATUS_RESP) && (respMsg.has(OMMMsg.HAS_STATE))
                && (respMsg.getState().getStreamState() == OMMState.Stream.OPEN)
                && (respMsg.getState().getDataState() == OMMState.Data.OK))
        {
        	_rfaAppLogger.info("RFA Login Received Login STATUS OK Response");
            _mainApp.set_successLogin(true);
            _mainApp.set_currentStatus(IQfixRoutingAgent.SESSION_TO_LOGON);
        }
        else	{
        	_rfaAppLogger.error("RFA Login Received Login Response >> " + OMMMsg.MsgType.toString(respMsg.getMsgType()));
        	_mainApp.set_successLogin(false);
        }
	}
	
	public Handle getLoginHandle()	{
        return _loginHandle;
    }
	
	public void set_rfaUser(String _rfaUser) {
		this._rfaUser = _rfaUser;
	}
	public String get_rfaUser() {
		return _rfaUser;
	}
}