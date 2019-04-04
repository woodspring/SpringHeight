package com.tts.reuters.adapter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.omm.OMMData;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.rdm.RDMInstrument;
import com.reuters.rfa.rdm.RDMMsgTypes;
import com.reuters.rfa.session.omm.OMMItemEvent;
import com.reuters.rfa.session.omm.OMMItemIntSpec;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.plugin.adapter.support.IReutersRFAMsgListener;
import com.tts.reuters.adapter.utils.ReutersGenericOMMParser;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;


public class ReutersRFAAdapterPriceReq implements Client 
{
	private ReutersRFAAdapter _mainApp           = null;
	private IReutersRFAMsgListener _msgListener  = null;
	private Handle _priceReqHandle               = null;
	
	private static ByteArrayOutputStream baosMsg = new ByteArrayOutputStream();
	private static Logger _rfaAppLogger          = LoggerFactory.getLogger("ReutersAppLogger");
	private static Logger _rfaMsgLogger          = LoggerFactory.getLogger("ReutersMsgLogger");
	private static DateTimeFormatter _dtfFrom    = DateTimeFormatter.ofPattern("dd MMM yyyy");
	private static DateTimeFormatter _dtfTo      = DateTimeFormatter.ofPattern("yyyyMMdd");
	
	private String _serviceName     = null;
	private String _originalSymbol  = null;
	private String _subscriptionId  = null;
	private String _tenor           = null;
	private String _matureDate      = null;
	
	private final IFxCalendarBizService fxCalendarBizService;
	private Hashtable<String, String> fwdPriceMasterCopy = null;
	
	public enum INTERESTED_FIELDS	{
    	BID, BID_1, BID_2, ASK, ASK_1, ASK_2, HIGH_1, LOW_1, MATUR_DATE
    };
	
	public ReutersRFAAdapterPriceReq(ReutersRFAAdapter mainApp, IReutersRFAMsgListener listener) {
		this._mainApp        = mainApp;
		this._msgListener    = listener;
		this._priceReqHandle = null;
		this.fxCalendarBizService = AppContext.getContext().getBean(IFxCalendarBizService.class);
		
		fwdPriceMasterCopy   = new Hashtable<>();
		for(INTERESTED_FIELDS field : INTERESTED_FIELDS.values()) {
           fwdPriceMasterCopy.put(field.toString(), "");
        }
	}
	
	public void sendPriceRequest(String symbol)	{
		OMMItemIntSpec rfaIntSpec = new OMMItemIntSpec();
		OMMPool pool              = _mainApp.get_ommPool();
		OMMMsg itemReqMsg         = pool.acquireMsg();
		
		itemReqMsg.setMsgType(OMMMsg.MsgType.REQUEST);
		itemReqMsg.setMsgModelType(RDMMsgTypes.MARKET_PRICE);
		itemReqMsg.setPriority((byte)1, 1);
		itemReqMsg.setIndicationFlags(OMMMsg.Indication.REFRESH);
		
		if(_mainApp.getLoginHandle() != null)
			itemReqMsg.setAssociatedMetaInfo(_mainApp.getLoginHandle());
		
		itemReqMsg.setAttribInfo(_serviceName, symbol, RDMInstrument.NameType.RIC);
		
		
		baosMsg.reset();
		PrintStream psPrice = new PrintStream(baosMsg);
		ReutersGenericOMMParser.parseMsg(itemReqMsg, psPrice);
		String priceMsg = new String(baosMsg.toByteArray(), StandardCharsets.UTF_8);
		_rfaMsgLogger.info("PRICE REQ. >> \n" + priceMsg);
		
		
		rfaIntSpec.setMsg(itemReqMsg);
		
		_priceReqHandle = _mainApp.get_ommConsumer().registerClient(_mainApp.get_eventQueue(), rfaIntSpec, this, null);
		pool.releaseMsg(itemReqMsg);
		_rfaAppLogger.debug("RFA Price PRICE REQ SENT For " + symbol + " @ " + System.currentTimeMillis());
	}
	
	public void closePriceRequest()	{
		if(_priceReqHandle != null)
			_mainApp.get_ommConsumer().unregisterClient(_priceReqHandle);
		
		_priceReqHandle = null;		
		_msgListener    = null;
		_mainApp        = null;
		
		_rfaAppLogger.info("RFA Price CLOSING PRICE HANDLE For " + get_originalSymbol() + get_tenor() + "...");
	}
	
	@Override
	public void processEvent(Event event) {
		if (event.getType() == Event.COMPLETION_EVENT)	{
            _rfaAppLogger.warn("RFA Price Received a COMPLETION_EVENT. >> " + event.getHandle());
            return;
        }

        if (event.getType() != Event.OMM_ITEM_EVENT)	{
            _rfaAppLogger.warn("RFA Price Received an unsupported Event type. >> " + event.getType());
            return;
        }
        
        
        _rfaAppLogger.info("RFA Price Received Response for " + get_subscriptionId() + " @ " + System.currentTimeMillis());
        
        OMMItemEvent ie = (OMMItemEvent)event;
        OMMMsg respMsg  = ie.getMsg();
        
        
        baosMsg.reset();        
        PrintStream psPrice = new PrintStream(baosMsg);        
                
        //ReutersGenericOMMParser.parse(respMsg);
        ReutersGenericOMMParser.parseMsg(respMsg, psPrice);
        String priceResp = new String(baosMsg.toByteArray(), StandardCharsets.UTF_8);
        _rfaMsgLogger.info("PRICE RESP. >> \n" + priceResp);
                
        
        if(((respMsg.getMsgType() == OMMMsg.MsgType.REFRESH_RESP) || (respMsg.getMsgType() == OMMMsg.MsgType.UPDATE_RESP))
        		&& ((respMsg.getDataType() != OMMTypes.NO_DATA)))	{
        	
        	final long receiveTime = System.currentTimeMillis();
        	OMMData respData       = respMsg.getPayload();
        	
        	Hashtable<String, String> hashFwdPrice = ReutersGenericOMMParser.parseAggregate(respData); 
            if(!hashFwdPrice.isEmpty())	{
            	
            	for(INTERESTED_FIELDS field : INTERESTED_FIELDS.values()) {
            		if(hashFwdPrice.containsKey(field.name()))
            			fwdPriceMasterCopy.put(field.name(), hashFwdPrice.get(field.name()));
            	}
            	
            	try	{            	
	            	//_matureDate = null;
	            	//_matureDate = _dtfTo.format(_dtfFrom.parse(fwdPriceMasterCopy.get(INTERESTED_FIELDS.MATUR_DATE.name())));
	            		            	
	            	String[] nmSplit1 = _tenor.split("[^\\d]");
					String[] nmSplit2 = _tenor.split("[\\d]");
					Long periodValue  = Long.valueOf(nmSplit1.length > 0 ? nmSplit1[0] : "-1");
					String periodCode = nmSplit2.length > 0 ? nmSplit2[nmSplit2.length - 1] : "";
					
					LocalDate tradeDate = LocalDate.now();
					LocalDate valueDate = fxCalendarBizService.getForwardValueDate(_originalSymbol, tradeDate, periodCode, periodValue, PricingConventionConstants.INTERBANK);
					_matureDate         = ChronologyUtil.getDateString(valueDate);
            	}
            	catch(DateTimeParseException pExp)	{
            		_matureDate = fwdPriceMasterCopy.get(INTERESTED_FIELDS.MATUR_DATE.name());
            		_rfaAppLogger.error("ReutersRFAAdapterPriceReq ParseException @ processEvent", pExp);
            	}
            	
            	Tenor.Builder tenor = Tenor.newBuilder();
            	tenor.setAskSwapPoints(fwdPriceMasterCopy.get(INTERESTED_FIELDS.ASK.name()))
            	     .setBidSwapPoints(fwdPriceMasterCopy.get(INTERESTED_FIELDS.BID.name()))
            	     .setName(_tenor)
            	     .setActualDate(_matureDate);       
            	
            	ForwardCurve.Builder fwdPrice = ForwardCurve.newBuilder();
            	fwdPrice.addTenors(tenor)
            	        .setSymbol(_originalSymbol)
            	        .getLatencyBuilder().setFaReceiveTimestamp(receiveTime);
            	
            	
            	_rfaMsgLogger.info(TextFormat.shortDebugString(fwdPrice.build()));
            	if(_msgListener != null)
            		_msgListener.onFwdRICMessage(_subscriptionId, fwdPrice);
            }
        }
	}

	public String get_serviceName() {
		return(_serviceName);
	}
	public void set_serviceName(String _serviceName) {
		this._serviceName = _serviceName;
	}

	public String get_originalSymbol() {
		return(_originalSymbol);
	}
	public void set_originalSymbol(String _originalSymbol) {
		this._originalSymbol = _originalSymbol;
	}

	public String get_subscriptionId() {
		return(_subscriptionId);
	}
	public void set_subscriptionId(String _subscriptionId) {
		this._subscriptionId = _subscriptionId;
	}

	public String get_tenor() {
		return(_tenor);
	}
	public void set_tenor(String _tenor) {
		this._tenor = _tenor;
	}	
}