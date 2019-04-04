package com.tts.reuters.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.LogManager;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.reuters.rfa.common.Context;
import com.reuters.rfa.common.DispatchException;
import com.reuters.rfa.common.EventQueue;
import com.reuters.rfa.common.EventSource;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.dictionary.DictionaryException;
import com.reuters.rfa.omm.OMMEncoder;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.session.Session;
import com.reuters.rfa.session.omm.OMMConnectionIntSpec;
import com.reuters.rfa.session.omm.OMMConsumer;
import com.tts.fix.support.FixApplicationProperties;
import com.tts.fix.support.ISupportiveApp;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.system.admin.AdapterStruct;
import com.tts.message.system.admin.AdapterStruct.ChangeAdapterSessionStatus;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.plugin.adapter.support.IReutersRFAMsgListener;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.reuters.adapter.utils.ReutersGenericOMMParser;
import com.tts.reuters.adapter.utils.ric.FileBasedRicCodeProvider;
import com.tts.reuters.adapter.utils.ric.IRicCodeProvider;
import com.tts.util.AppContext;


public class ReutersRFAAdapter implements ISupportiveApp, IReutersApp, IMsgListener {
	
	private EventQueue _eventQueue;
    private Session _session;
    private OMMConsumer _ommConsumer;
    private OMMEncoder _ommEncoder;
    private OMMPool _ommPool;
    private ReutersRFAAdapterLogin _rfaLogin;
    private Handle _connIntSpecHandle;
    
    private static Logger _rfaAppLogger = LoggerFactory.getLogger("ReutersAppLogger");
    private static Logger _rfaUtilLogger;
    private static AtomicLong _longSubId;
    private static Hashtable<String, ReutersRFAAdapterPriceReq> _hashPriceReqRef;
    
    private static final IRicCodeProvider ricCodeProvider = new FileBasedRicCodeProvider();
    private static final String REUTERS_NAME             = "REUTERS_ADAPTER_FWD";
    private static final String SESSION_TO_LOGOFF        = IQfixRoutingAgent.SESSION_TO_LOGOFF;
    private static final String SESSION_TO_LOGON         = IQfixRoutingAgent.SESSION_TO_LOGON;
    
    
    private static final String DEFAULT_SESSION_NAME     = "YKB::ykbSession";
    private static final String DEFAULT_CONFIG_FILE_NAME = "ReutersRFAConfiguration.xml";
    private static final String DEFAULT_SESSION_USER     = "rdc_whitelabel_prod";
    private static final String DEFAULT_SERVICE_PROVIDER = "IDN_RDF";
    private static final String DEFAULT_CONFIG_LOG_RFA   = "ReutersLogging.properties";
    private static final int DEFAULT_MAX_PRICE_INTERVAL  = 60;
    
    private static final String MAINTENANCE_TS_NAME      = "<Maintenance Trading Session>";
	private static final String DEFAUlT_DICTIONARY_FILE  = "ReutersRDMFieldDictionary.dic";
	private static final String DEFAULT_ENUM_FILE        = "ReutersEnumType.def";
	
	private boolean _successLogin   = false;
    private boolean _successConfig  = true;
    private boolean _keepRunning    = true;
	
    private int _maxPriceInterval   = 0;
    private String _sessionName     = null;
    private String _configFileName  = null;
    private String _sessionUserName = null;
    private String _serviceProvider = null;
    private String _currentTradingSessionName;
    private String _currentStatus;
    
    public ReutersRFAAdapter()	{
		_rfaAppLogger.info("<<<>>>   RFA Adapter Initializating Started...   <<<>>>");
		
		reutersAdapterAppInitLogger();
//				
//		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
//		IMsgReceiver msgReceiver1 = msgReceiverFactory.getMsgReceiver(false, false);
//		msgReceiver1.setListener(this);
//		msgReceiver1.setTopic("TTS.CTRL.EVENT.ADAPTER_STATUS_UPDATE.>");
//				
		//	Read RFA Configuration XML
		_successConfig = readConfigurationFile();
		set_currentStatus(SESSION_TO_LOGOFF);
		
		_rfaAppLogger.info("<<< ReutersRFAAdapter >>>");
		_rfaAppLogger.info("RFA Adapter Initialization Completed...");
	}
	
	private void reutersAdapterAppInitLogger()	{
		try {
			InputStream ipStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("env-resources/rfa/" + DEFAULT_CONFIG_LOG_RFA);
			LogManager lmLog     = LogManager.getLogManager();
		
			lmLog.readConfiguration(ipStream);
		} 
		catch (SecurityException | IOException exp) {
			_rfaAppLogger.error("ReutersRFAAdapter SecurityException | IOException @ reutersAdapterAppInitLogger", exp);
		}
		
		
		_rfaUtilLogger = LoggerFactory.getLogger("com.reuters.rfa");
		_rfaAppLogger.info("RFA Adapter Logger Initialized Successfully...");
	}
	
	private boolean readConfigurationFile()	{
		boolean success            = false;
		InputStream ipStreamConfig = null;
		String fieldDictionaryFilename = null;
        String enumDictionaryFilename  = null;
        String maxPriceInterval = null;
		
        FixApplicationProperties fixAppProp = AppContext.getContext().getBean(FixApplicationProperties.class);
        
        _configFileName  = fixAppProp.getProperty("tr.rfa.config.file");
		_sessionName     = fixAppProp.getProperty("tr.rfa.session.name");
		_sessionUserName = fixAppProp.getProperty("tr.rfa.session.user");
		_serviceProvider = fixAppProp.getProperty("tr.rfa.service.provider");
		
		fieldDictionaryFilename = fixAppProp.getProperty("tr.rfa.dictionary.file");
		enumDictionaryFilename  = fixAppProp.getProperty("tr.rfa.enum.file");
		maxPriceInterval        = fixAppProp.getProperty("tr.rfa.max.price.interval");
		
		
		_configFileName  = ((_configFileName == null) || (_configFileName.trim().length() <= 0))? DEFAULT_CONFIG_FILE_NAME: _configFileName;
		_sessionName     = ((_sessionName == null) || (_sessionName.trim().length() <= 0))? DEFAULT_SESSION_NAME: _sessionName;
		_sessionUserName = ((_sessionUserName == null) || (_sessionUserName.trim().length() <= 0))? DEFAULT_SESSION_USER: _sessionUserName;
		_serviceProvider = ((_serviceProvider == null) || (_serviceProvider.trim().length() <= 0))? DEFAULT_SERVICE_PROVIDER: _serviceProvider;
				
		fieldDictionaryFilename = ((fieldDictionaryFilename == null) || (fieldDictionaryFilename.trim().length() <= 0))? DEFAUlT_DICTIONARY_FILE: fieldDictionaryFilename;
		enumDictionaryFilename  = ((enumDictionaryFilename == null) || (enumDictionaryFilename.trim().length() <= 0))? DEFAULT_ENUM_FILE: enumDictionaryFilename;
		try	{
			_maxPriceInterval = ((maxPriceInterval == null) || (maxPriceInterval.trim().length() <= 0) || (Integer.parseInt(maxPriceInterval) <= 0))
																								? DEFAULT_MAX_PRICE_INTERVAL: Integer.parseInt(maxPriceInterval);
		}
		catch(NumberFormatException nfExp)	{
			_maxPriceInterval = DEFAULT_MAX_PRICE_INTERVAL;
			_rfaAppLogger.error("ReutersRFAAdapter NumberFormatException @ readConfigurationFile", nfExp);
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("RFA Adapter CONFIGURATION >>> \n\t")
		  .append("_configFileName: ").append(_configFileName).append("\n\t")
		  .append("_sessionName: ").append(_sessionName).append("\n\t")
		  .append("_sessionUserName: ").append(_sessionUserName).append("\n\t")
		  .append("_serviceProvider: ").append(_serviceProvider).append("\n\t")
		  .append("fieldDictionaryFilename: ").append(fieldDictionaryFilename).append("\n\t")
		  .append("enumDictionaryFilename: ").append(enumDictionaryFilename);
		
		_rfaAppLogger.info(sb.toString());
		sb = null;
        		
		try	{
			ipStreamConfig = Thread.currentThread().getContextClassLoader().getResourceAsStream("env-resources/rfa/" + _configFileName);
			if(ipStreamConfig == null)
				throw new ReutersRFAAdapterExp("RFA Adapter Configuration File NOT FOUND in CLASS PATH.");
			
			Preferences.importPreferences(ipStreamConfig);			
			ipStreamConfig.close();
			success = true;
			_rfaAppLogger.info("RFA Adapter Properties loaded Successfully...");
		}
		catch(ReutersRFAAdapterExp ricExp)	{
			success = false;
			_rfaAppLogger.error("ReutersRFAAdapter ReutersRFAAdapterExp @ readConfigurationFile ", ricExp);
		}
		catch(InvalidPreferencesFormatException | IOException exp)	{
			success = false;
			_rfaAppLogger.error("ReutersRFAAdapter InvalidPreferencesFormatException | IOException @ readConfigurationFile", exp);
		}
		finally {
			ipStreamConfig = null;
		}
				
		//	Initialize the Application's Dictionary to Decode the Incoming Messages...
		try	{
			URL urlFieldDictionary = Thread.currentThread().getContextClassLoader().getResource("env-resources/rfa/" + fieldDictionaryFilename);
			URL urlEnumDictionary  = Thread.currentThread().getContextClassLoader().getResource("env-resources/rfa/" + enumDictionaryFilename);
			_rfaAppLogger.info("FieldDictionary: " + urlFieldDictionary.getPath() + ", EnumDictionary: " + urlEnumDictionary.getPath());
						
			ReutersGenericOMMParser.initializeDictionary(urlFieldDictionary.getPath(), urlEnumDictionary.getPath());
            success = true;
            _rfaAppLogger.info("RFA Adapter Application's Dictionary loaded Successfully...");
        }
		catch (DictionaryException dExp)	{
            success = false;
            _rfaAppLogger.error("ReutersRFAAdapter DictionaryException @ readConfigurationFile", dExp);
        }
		
		return(success);
	}
			
	public void initReutersAdapterApp()	{
		_rfaAppLogger.info("RFA Adapter Connecting to RFA Server......");
		
		SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMddHHmmss");
		_longSubId           = new AtomicLong(Long.parseLong(sdf.format(Calendar.getInstance().getTime())));
		_hashPriceReqRef     = new Hashtable<>();
		
		
		//	Check RFA Configuration Status
		if(!_successConfig)	{
			_rfaAppLogger.error("RFA Adapter App Initalization was NOT SUCCESS. CAN'T Connect to RFA Server.");
			return;
		}
		
		try	{
			// Initialize RFA Contex
			Context.initialize();
			
			// Acquire RFA Session			
			_session = Session.acquire(_sessionName);
			
			if(_session == null)	{
				throw new ReutersRFAAdapterExp("RFA Adapter Failed to Acquire SESSION.");
			}
			_rfaAppLogger.info("RFA Adapter SESSION CREATED. RFA Version: " + Context.getRFAVersionInfo().getProductVersion());
			
			// Create RFA Event Queue
			_eventQueue = EventQueue.create("rfaEventQueue");
			_ommPool    = OMMPool.create();
			_ommEncoder = _ommPool.acquireEncoder();
			
			_ommEncoder.initialize(OMMTypes.MSG, 5000);
			
			// Create RFA Event Source
			_ommConsumer = (OMMConsumer)_session.createEventSource(EventSource.OMM_CONSUMER, "rfaOMMConsumer", true);
			
			// Initialize Application Login Object
			_rfaLogin  = new ReutersRFAAdapterLogin(this);
			_rfaLogin.set_rfaUser(_sessionUserName);
			_rfaAppLogger.info("RFA Adapter LOGIN Object Created.");
			
			OMMConnectionIntSpec connIntSpec = new OMMConnectionIntSpec();
			_connIntSpecHandle = _ommConsumer.registerClient(_eventQueue, connIntSpec, _rfaLogin, null);
			
			
			_rfaLogin.sendLoginRequest();			
			Thread rfaEvnts = new Thread(new Runnable() {
									public void run() {
										dispatchAppEvents();
									}
								});
			rfaEvnts.setName("RFAEVNTS");
			rfaEvnts.start();
			
			//dispatchAppEvents();
		}
		catch(ReutersRFAAdapterExp raeExp )	{
			_rfaAppLogger.error("ReutersRFAAdapter ReutersRFAAdapterExp @ initReutersAdapterApp", raeExp);
			doApplicationCleanup(1);
		}
		catch (Exception exp) {
			_rfaAppLogger.error("ReutersRFAAdapter Exception @ initReutersAdapterApp", exp);
			doApplicationCleanup(1);
		}
	}
		
	private void dispatchAppEvents()	{
		_rfaAppLogger.info("RFA Adapter Starting Dispatch Events Thread...");
		
		while(_keepRunning)	{
            try	{
            	_eventQueue.dispatch(1000);
            }
            catch(DispatchException dExp)	{
            	_rfaAppLogger.error("ReutersRFAAdapter DispatchException @ dispatchAppEvents", dExp);
            }
            
            if(!_keepRunning)
            	_rfaAppLogger.warn("RFA Adapter EXITING Dispatch Events Thread.");
        }
	}
		
	public void doApplicationCleanup(int exitVal)	{
		doApplicationCleanup(exitVal, true);
    }
	
	public void doApplicationCleanup(int exitVal, boolean doLoginCleanup)	{
		_rfaAppLogger.info("RFA Adapter Begining Application Cleanup... exitVal: " + String.valueOf(exitVal) + ", doLoginCleanup: " + String.valueOf(doLoginCleanup));
		
		try	{
			//	Deactivate Event Q
			if(_eventQueue != null)		_eventQueue.deactivate();
			set_keepRunning(false);
			
			Set<String> refKeys = _hashPriceReqRef.keySet();
			_rfaAppLogger.debug("RFA Adapter Price Unsubscription List: " + refKeys.toString());
			
			for(String subId : refKeys)	{
				unsubscribeFromFwdRIC(subId);
			}
			
			if(_connIntSpecHandle != null) 	_ommConsumer.unregisterClient(_connIntSpecHandle);
			
			//	Unregister Login
			if((_rfaLogin != null) && (doLoginCleanup))		_rfaLogin.closeLoginRequest();
					
			//	Destroy Event Q
			if(_eventQueue != null)		_eventQueue.destroy();
			
			//	Destroy Event Source
			if(_ommConsumer != null)	_ommConsumer.destroy();
			
			//	Release Session
			if(_session != null) 		_session.release();
			
			// 	Uninitialize Contex
			Context.uninitialize();
			
			_hashPriceReqRef.clear();			
		}
		catch(Exception exp)	{
			_rfaAppLogger.error("ReutersRFAAdapter Exception @ doApplicationCleanup", exp);
		}
		finally {
			_session     = null;
			_eventQueue  = null;
			_ommConsumer = null;
			_ommEncoder  = null;
		    _ommPool     = null;
		    _rfaLogin    = null;
		    _longSubId   = null;
		    
		    _connIntSpecHandle = null;
		    _hashPriceReqRef   = null;
		    		    
		    set_keepRunning(false);
		    set_successLogin(false);
		    set_currentStatus(SESSION_TO_LOGOFF);
		}
		
		_rfaAppLogger.info("RFA Adapter Application Cleanup Completed...");
	}
	
	public EventQueue get_eventQueue() {
		return _eventQueue;
	}

	public OMMConsumer get_ommConsumer() {
		return _ommConsumer;
	}

	public OMMEncoder get_ommEncoder() {
		return _ommEncoder;
	}

	public OMMPool get_ommPool() {
		return _ommPool;
	}
	
	public Handle getLoginHandle()	{
        if(_rfaLogin != null)	{
            return(_rfaLogin.getLoginHandle());
        }

        return(null);
    }
	
	public boolean is_successLogin() {
		return(_successLogin);
	}
	public void set_successLogin(boolean _successLogin) {
		this._successLogin = _successLogin;
	}

	public boolean is_keepRunning() {
		return(_keepRunning);
	}
	public void set_keepRunning(boolean _keepRunning) {
		this._keepRunning = _keepRunning;
	}
	
	public String get_currentStatus() {
		return _currentStatus;
	}
	public void set_currentStatus(String _currentStatus) {
		this._currentStatus = _currentStatus;
		_rfaAppLogger.info("RFA Adapter CURRENT STATUS >> " + this._currentStatus);
	}
	
	public int get_maxPriceInterval() {
		return _maxPriceInterval;
	}
	public void set_maxPriceInterval(int _maxPriceInterval) {
		this._maxPriceInterval = _maxPriceInterval;
	}

	@Override
	public String getName() {
		return(REUTERS_NAME);
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
	public void switchTradingSession(String tradingSessionName) {
		_rfaAppLogger.info("RFA Adapter Received switchTradingSession. _currentTradingSessionName: " + _currentTradingSessionName + ", tradingSessionName: " + tradingSessionName);
		
		String fromTradingSession = _currentTradingSessionName;
		boolean switchToMaintenance = MAINTENANCE_TS_NAME.equals(tradingSessionName);
		boolean switchFromMaintence = MAINTENANCE_TS_NAME.equals(fromTradingSession);
		
		if((fromTradingSession == null) || (switchFromMaintence && !switchToMaintenance)) {
			initReutersAdapterApp();
		}
		if(switchToMaintenance && !switchFromMaintence)	{
			doApplicationCleanup(0);
		}
		
		_currentTradingSessionName = tradingSessionName;
		ricCodeProvider.reload();
		_rfaAppLogger.info("RFA Adapter Switching _currentTradingSessionName TO >> " + tradingSessionName);
	}

	@Override
	public synchronized String subscribeToFwdRIC(String symbol, String tenor, IReutersRFAMsgListener listener)	{
		StringBuffer subscriptionId = new StringBuffer();
		String formattedRICSymbol   = ricCodeProvider.getRicCode(symbol, tenor);
		final String CCY_USD        = "USD";
		
		_rfaAppLogger.debug("RFA Adapter Received Subscription REQ. SYMBOL: " + symbol + ", TENOR: " + tenor);
		if(!_successLogin)	{
			_rfaAppLogger.warn("RFA Adapter Not LOGGED in. Sending Subscription REQ for SYM: " + symbol + ", TENOR: " + tenor);
			//return(null);
		}
		
		try	{				
			ReutersRFAAdapterPriceReq rfaPriceReq = new ReutersRFAAdapterPriceReq(this, listener);
			
			rfaPriceReq.set_serviceName(_serviceProvider);
			rfaPriceReq.set_originalSymbol(symbol);
			rfaPriceReq.set_tenor(tenor);
			
			//	Thomson Reuters format do not require 'USD' in RIC
			String tempSymbol = symbol.toUpperCase()
					                  .replaceFirst(CCY_USD, "").trim();
			
			subscriptionId.append(tempSymbol)
			              .append(tenor.trim())
			              .append("=");
			
			subscriptionId.append(String.valueOf(_longSubId.incrementAndGet()));
			
			rfaPriceReq.set_subscriptionId(subscriptionId.toString());
			
			_rfaAppLogger.debug("RFA Adapter Sending Sub REQ. SYMBOL: " + formattedRICSymbol + ", SUBID: " + subscriptionId.toString());
			rfaPriceReq.sendPriceRequest(formattedRICSymbol);
			
			_hashPriceReqRef.put(subscriptionId.toString(), rfaPriceReq);
		}
		catch(Exception exp)	{
			_rfaAppLogger.error("ReutersRFAAdapter Exception @ subscribeToFwdRIC", exp);
		}
		
		return(subscriptionId.toString());
	}

	@Override
	public synchronized void unsubscribeFromFwdRIC(String subscriptionId) {
		ReutersRFAAdapterPriceReq rfaPriceReq = null;
		
		_rfaAppLogger.debug("RFA Adapter Received Un-Subscription REQ. subscriptionId: " + subscriptionId);
		rfaPriceReq = _hashPriceReqRef.remove(subscriptionId);
		if(rfaPriceReq != null)
			rfaPriceReq.closePriceRequest();
	}

	@Override
	public synchronized void unsubscribeFromFwdRIC() {
		_rfaAppLogger.debug("RFA Adapter Received Un-Subscription REQ for ALL SYMBOLS.");
		Set<String> refKeys = _hashPriceReqRef.keySet();
		HashSet<String> _refKeys = new HashSet<String>(refKeys);
		for(String subId : _refKeys)	{
			unsubscribeFromFwdRIC(subId);
		}		
		_hashPriceReqRef.clear();
	}
	
	@Override
	public void beginReutersApp() {
		initReutersAdapterApp();
	}

	@Override
	public void endReutersApp() {
		doApplicationCleanup(0);
	}
	
	@Override
	public boolean isReutersAppLoggedIn() {
		return(is_successLogin());
	}
	
	@Override
	public int getMaxAllowedMsgTimeInterval() {
		return(get_maxPriceInterval());
	}
	
	@Override
	public void onMessage(TtMsg msg, IMsgSessionInfo sessionInfo, IMsgProperties properties) {
		String eventName = properties.getSendTopic();
		
		_rfaAppLogger.info("RFA Adapter Executing onMessage with eventName: " + eventName);
		if(eventName.contains(REUTERS_NAME))	{
			try	{
				ChangeAdapterSessionStatus changeStatus = null;
				changeStatus = ChangeAdapterSessionStatus.parseFrom(msg.getParameters());
				
				_rfaAppLogger.info("RFA Adapter AdapterName: " + changeStatus.getSessionName() + ", OriginalStatus: " + changeStatus.getOriginalStatus() 
															+ ", NewStatus: " + changeStatus.getNewStatus());
				if(REUTERS_NAME.equals(changeStatus.getSessionName()) && _currentStatus.equals(changeStatus.getOriginalStatus().toString()))	{
					if(changeStatus.getNewStatus().equals(AdapterStruct.Status.ACTIVE))
						initReutersAdapterApp();
					
					if(changeStatus.getNewStatus().equals(AdapterStruct.Status.INACTIVE))
						doApplicationCleanup(0);
				}
			}
			catch(InvalidProtocolBufferException ipbExp)	{
				_rfaAppLogger.error("ReutersRFAAdapter InvalidProtocolBufferException @ onMessage", ipbExp);
			}
		}		
	}
}