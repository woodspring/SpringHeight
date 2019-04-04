package com.tts.reuters.adapter; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.message.market.ForwardCurveStruct.Tenor;
import com.tts.message.system.admin.AdapterStruct;
import com.tts.message.system.admin.AdapterStruct.ChangeAdapterSessionStatus;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.plugin.adapter.support.IReutersRFAMsgListener;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.service.biz.calendar.IFxCalendarBizService;
import com.tts.util.AppContext;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.constant.PricingConventionConstants;


public class ReutersMockAdapter implements IReutersApp, IMsgListener {

	private static final String MAINTENANCE_TS_NAME = "<Maintenance Trading Session>";
	private static final String REUTERS_NAME        = "REUTERS_ADAPTER_FWD";
	private static final String CSV_SPLITTER        = ",";
	private static final SimpleDateFormat sdf       = new SimpleDateFormat("YYYYMMddHHmmss");
	private static final String SESSION_TO_LOGOFF   = IQfixRoutingAgent.SESSION_TO_LOGOFF;
	private static final String SESSION_TO_LOGON    = IQfixRoutingAgent.SESSION_TO_LOGON;
	
	private static Logger _rfaLogger = LoggerFactory.getLogger("ReutersAppLogger");
	private static AtomicLong _longSubId;
	
	private Map<String, String> _hashSymDB = null;
	private static Map<String, ReutersPriceDistributer> _hashPriceReqRef;
	
	private boolean _successLogin = false;
	private String _currentTradingSessionName;
	private String _currentStatus;
	
	public ReutersMockAdapter() {
		_hashSymDB = new ConcurrentHashMap<>();
		
		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		IMsgReceiver msgReceiver1 = msgReceiverFactory.getMsgReceiver(false, false);
		msgReceiver1.setListener(this);
		msgReceiver1.setTopic("TTS.CTRL.EVENT.ADAPTER_STATUS_UPDATE.>");
				
		_successLogin =  false;
		readCSVFile();
		_rfaLogger.info("<<< ReutersMockAdapter >>>");
	}
	
	private void readCSVFile()	{
		InputStream csvFile = Thread.currentThread().getContextClassLoader().getResourceAsStream("app-resources/fc.csv");
        String line         = "";
        
        try(BufferedReader br = new BufferedReader(new InputStreamReader(csvFile))) {
        	while ((line = br.readLine()) != null) {
        		String[] symbData = line.split(CSV_SPLITTER);
        		
        		if(symbData.length > 0)	{
        			symbData[0] = symbData[0].replaceFirst("<", "")
        									 .substring(0, symbData[0].indexOf(">") - 1);
        		}
        		
        		if(symbData[0].endsWith("ON=") || symbData[0].endsWith("TN="))	{
        			_hashSymDB.put(symbData[0], symbData[1] + "," + symbData[2]);
        		}
            }
        } 
        catch(IOException exp) {
            exp.printStackTrace();
        }
	}
	
	public void initReutersMockAdapterApp()	{
		_hashPriceReqRef = new ConcurrentHashMap<>();
		_longSubId       = new AtomicLong(Long.parseLong(sdf.format(Calendar.getInstance().getTime())));
		_successLogin    = true;
	}
	
	public void doApplicationCleanup(int exitValue)	{
		Set<String> refKeys = _hashPriceReqRef.keySet();
		for(String subId : refKeys)	{
			unsubscribeFromFwdRIC(subId);
		}
		
		_hashPriceReqRef.clear();
		
		_hashPriceReqRef = null;
		_longSubId       = null;
	}
	
	@Override
	public String subscribeToFwdRIC(String symbol, String tenor, IReutersRFAMsgListener listener) {
		String reqSymbol = symbol + tenor;
		StringBuffer subscriptionId = new StringBuffer();
		
		if(!_hashSymDB.containsKey(reqSymbol + "="))
			return(null);
		
		subscriptionId.append(symbol)
        			  .append(tenor.trim())
        			  .append("=");
        			 				
		String initPrice = _hashSymDB.get(subscriptionId.toString());
		if((initPrice ==  null) || (initPrice.trim().length() <= 0))
			return(null);
				
		subscriptionId.append(String.valueOf(_longSubId.incrementAndGet()));
		ReutersPriceDistributer objDistributer = new ReutersPriceDistributer(this, listener, initPrice, AppContext.getContext().getBean(IFxCalendarBizService.class));
		objDistributer.setSymbol(symbol);
		objDistributer.set_Tenor(tenor);
		objDistributer.setSubId(subscriptionId.toString());
		
		Thread tPriceDistributer = new Thread(objDistributer);
		tPriceDistributer.start();
		
		_hashPriceReqRef.put(subscriptionId.toString(), objDistributer);
		return(subscriptionId.toString());
	}
		
	@Override
	public void switchTradingSession(String tradingSessionName) {
		String fromTradingSession = _currentTradingSessionName;
		boolean switchToMaintenance = MAINTENANCE_TS_NAME.equals(tradingSessionName);
		boolean switchFromMaintence = MAINTENANCE_TS_NAME.equals(fromTradingSession);
		
		if((fromTradingSession == null) || (switchFromMaintence && !switchToMaintenance)) {
			initReutersMockAdapterApp();
		}
		if(switchToMaintenance && !switchFromMaintence)	{
			doApplicationCleanup(0);
		}
		
		_currentTradingSessionName = tradingSessionName;
	}

	@Override
	public String getName() {
		return REUTERS_NAME;
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
	public void unsubscribeFromFwdRIC(String subscriptionId) {
		ReutersPriceDistributer rfaPriceReq = null;
		
		rfaPriceReq = _hashPriceReqRef.remove(subscriptionId);
		if(rfaPriceReq != null)
			rfaPriceReq.setSubscribePrice(false, subscriptionId);
	}

	@Override
	public void unsubscribeFromFwdRIC() {
		Set<String> refKeys = _hashPriceReqRef.keySet();
		for(String subId : refKeys)	{
			unsubscribeFromFwdRIC(subId);
		}		
		_hashPriceReqRef.clear();
	}
	
	@Override
	public void onMessage(TtMsg msg, IMsgSessionInfo sessionInfo, IMsgProperties properties) {
		String eventName = properties.getSendTopic();
		
		if(eventName.contains(REUTERS_NAME))	{
			try	{
				ChangeAdapterSessionStatus changeStatus = null;
				changeStatus = ChangeAdapterSessionStatus.parseFrom(msg.getParameters());
				
				if(REUTERS_NAME.equals(changeStatus.getSessionName()) && _currentStatus.equals(changeStatus.getOriginalStatus().toString()))	{
					if(changeStatus.getNewStatus().equals(AdapterStruct.Status.ACTIVE))
						initReutersMockAdapterApp();
					
					if(changeStatus.getNewStatus().equals(AdapterStruct.Status.INACTIVE))
						doApplicationCleanup(0);
				}
			}
			catch(InvalidProtocolBufferException ipbExp)	{
				ipbExp.printStackTrace();
			}
		}		
	}

	@Override
	public void beginReutersApp() {
		initReutersMockAdapterApp();
	}

	@Override
	public void endReutersApp() {
		doApplicationCleanup(0);
	}

	@Override
	public boolean isReutersAppLoggedIn() {
		return(_successLogin);
	}

	@Override
	public int getMaxAllowedMsgTimeInterval() {
		return(30);
	}
}

class ReutersPriceDistributer implements Runnable	{
	private ReutersMockAdapter _mainApp         = null;
	private IReutersRFAMsgListener _msgListener = null;
	private boolean subscribePrice = false;
	
	private static Logger _rfaLogger = LoggerFactory.getLogger("ReutersMsgLogger");
	private final IFxCalendarBizService fxCalendarBizService;
	
	private String symbol = null;
	private String _tenor = null;
	private String subId  = null;
	
	private final double BID_PRICE;
	private final double ASK_PRICE;
	
	private long THREAD_SLEEP = 0L;
	
	public ReutersPriceDistributer(ReutersMockAdapter mainApp, IReutersRFAMsgListener listener, String initPrice, IFxCalendarBizService fxCalendarBizService)	{
		this._mainApp = mainApp;
		this._msgListener = listener;
		this.fxCalendarBizService = fxCalendarBizService;
		
		String[] temp  = initPrice.split(",");
		BID_PRICE      = Double.valueOf(temp[0]);
		ASK_PRICE      = Double.valueOf(temp[1]);	
		subscribePrice = true;
		THREAD_SLEEP   = (long) (((15000 - 10000) * Math.random()) + 10000);
	}

	@Override
	public void run() {
		Random rNum = new Random();
		DecimalFormat dfPrice = new DecimalFormat("0.000");
		double dPrcChange = 0.0;
		double dBidPrice  = 0.0;
		double dAskPrice  = 0.0;
		
		while(subscribePrice) {
			dPrcChange = (rNum.nextInt(10) / 100.0);
			dBidPrice  = BID_PRICE + dPrcChange;
			dAskPrice  = ASK_PRICE + dPrcChange;
			
			try	{
				String[] nmSplit1 = _tenor.split("[^\\d]");
				String[] nmSplit2 = _tenor.split("[\\d]");
				Long periodValue  = Long.valueOf(nmSplit1.length > 0 ? nmSplit1[0] : "-1");
				String periodCode = nmSplit2.length > 0 ? nmSplit2[nmSplit2.length - 1] : "";
				
				LocalDate tradeDate = LocalDate.now();
				LocalDate valueDate = fxCalendarBizService.getForwardValueDate(symbol, tradeDate, periodCode, periodValue, PricingConventionConstants.INTERBANK);
								
				Tenor.Builder tenor = Tenor.newBuilder();
            	tenor.setAskSwapPoints(dfPrice.format(dAskPrice))
            	     .setBidSwapPoints(dfPrice.format(dBidPrice))
            	     .setName(_tenor)
            	     .setActualDate(ChronologyUtil.getDateString(valueDate));
            	
            	ForwardCurve.Builder fwdPrice = ForwardCurve.newBuilder();
            	fwdPrice.addTenors(tenor)
            	        .setSymbol(symbol)
            	        .getLatencyBuilder().setFaReceiveTimestamp(System.currentTimeMillis());
            	
            	_rfaLogger.info(TextFormat.shortDebugString(fwdPrice.build()));
            	if(_msgListener != null)
            		_msgListener.onFwdRICMessage(subId, fwdPrice);
            	
				Thread.sleep(THREAD_SLEEP);
            }
			catch(InterruptedException iExp)	{
				iExp.printStackTrace();
			}
		}
		
		System.out.println("Unsubscribe... SubId: " + subId + ", SYM: " + symbol + ", TENOR: " + _tenor);
	}

	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String get_Tenor() {
		return _tenor;
	}
	public void set_Tenor(String tenor) {
		this._tenor = tenor;
	}

	public String getSubId() {
		return subId;
	}
	public void setSubId(String subId) {
		this.subId = subId;
	}

	public void setSubscribePrice(boolean subscribePrice) {
		this.subscribePrice = subscribePrice;
	}
	public void setSubscribePrice(boolean subscribePrice, String subscriptionId) {
		if((subscriptionId != null) && (this.subId.equalsIgnoreCase(subscriptionId)))
			this.subscribePrice = subscribePrice;
	}
}