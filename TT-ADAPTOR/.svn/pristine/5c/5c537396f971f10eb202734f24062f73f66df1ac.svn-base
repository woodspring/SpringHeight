package com.tts.mas;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.tts.fix.support.IFixListener;
import com.tts.fix.support.IMkQfixApp;
import com.tts.mas.app.MarketQFixApp;
import com.tts.mas.app.controller.DataFlowController;
import com.tts.mas.config.MasConfiguration;
import com.tts.mas.config.MasReutersConfiguration;
import com.tts.mas.qfx.impl.QuickfixEngineAcceptorContainer;
import com.tts.mas.qfx.impl.QuickfixEngineInitiatorContainer;
import com.tts.mas.vo.LogControlVo;
import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.eas.request.SubscriptionStruct.PriceSubscriptionRequest;
import com.tts.message.trade.PostTradeMessage.ExecutionReportInfo;
import com.tts.monitor.agent.MonitorAgentFactory;
import com.tts.monitor.agent.api.IMonitorAgent;
import com.tts.monitor.agent.util.MonitorConstant;
import com.tts.plugin.adapter.api.IFixIntegrationPluginSpi;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.api.dialect.IRequestDialectHelper.QuoteSide;
import com.tts.plugin.adapter.api.route.IQfixRoutingAgent;
import com.tts.plugin.adapter.api.setting.IFixSetting;
//import com.tts.plugin.adapter.impl.ykb.dialect.YkbResponseDialectHelper;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.vo.TenorVo;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.UnsupportedMessageType;

public class TtsMarketAdapterTestMain {
	private final static Logger logger = LoggerFactory.getLogger(TtsMarketAdapterTestMain.class);
	private static final IMonitorAgent monitorAgent = MonitorAgentFactory.getMonitorAgent(logger);
	
	public static class SysOutFixListener implements IFixListener {

		private final static PrintStream OUT = System.out;
		
		@Override
		public void onMessage(quickfix.Message message, IQfixRoutingAgent routingAgent)
				throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
			OUT.println(message);
		}

		@Override
		public void onFixSessionLogoff() {
			
		}
		
		@Override
		public void onFixSessionLogon() {
			// TODO Auto-generated method stub
		}		
	}
	
	public static class ExecReportSysOutFixListener implements IFixListener {

		private final static PrintStream OUT = System.out;
		//YkbResponseDialectHelper dialect = new YkbResponseDialectHelper();
		
		@Override
		public void onMessage(quickfix.Message message, IQfixRoutingAgent routingAgent)
				throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
			OUT.println(message);
			ExecutionReportInfo.Builder e = ExecutionReportInfo.newBuilder();
			//dialect.convertAndUpdate((quickfix.fix44.ExecutionReport) message, e);
			System.out.println(TextFormat.shortDebugString(e));
			logger.debug(TextFormat.shortDebugString(e));
		}

		@Override
		public void onFixSessionLogoff() {
			
		}
		
		@Override
		public void onFixSessionLogon() {
			// TODO Auto-generated method stub
		}
		
	}
	public static class TradableSysOutFixListener implements IFixListener {

		private final static PrintStream OUT = System.out;
		private final AtomicInteger count = new AtomicInteger(0);
		private final IMkQfixApp qfixApp;

				
		public TradableSysOutFixListener(IMkQfixApp qfixApp) {
			super();
			this.qfixApp = qfixApp;
		}

		@Override
		public void onMessage(quickfix.Message message, IQfixRoutingAgent routingAgent)
				throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
			OUT.println(message);

			int newcount = count.incrementAndGet();
			if ( newcount == 10 && message instanceof quickfix.fix44.MarketDataSnapshotFullRefresh) {
				quickfix.fix44.MarketDataSnapshotFullRefresh response = (quickfix.fix44.MarketDataSnapshotFullRefresh) message;
				String symbol = response.getSymbol().getValue();
				String currency = symbol.substring(0, 3);
							
				qfixApp.sendTradeExecRequest("", 
						"" , 
						"1000000", 
						currency, 
						symbol, 
						"", 
						"", 
						null,
						QuoteSide.BUY, 
						Long.toString(Calendar.getInstance().getTimeInMillis()), 
						"", 
						AppType.FIXTRADEADAPTER,  
						Long.toString(System.currentTimeMillis()), 
						qfixApp.getExecutionReportListener(), 
						"");
			}
		}

		@Override
		public void onFixSessionLogoff() {
			
		}
		
		@Override
		public void onFixSessionLogon() {
			// TODO Auto-generated method stub
		}
	}
		
	public static void main(String[] args) {
		int quoteDuration = 60;
		boolean notionalOnCcy2 = false;
		DataFlowController dataFlowController = null;
		String func = "TtsMarketAdapterTestMain.main";
		AnnotationConfigApplicationContext ctx = null;
		try {
			ctx = new AnnotationConfigApplicationContext();
			
			ctx.register(MasConfiguration.class);
			if ( "REUTERS".equals(System.getenv("MARKET_FWDADAPTER"))) {
				ctx.register(MasReutersConfiguration.class);
			}			ctx.refresh();
			ctx.registerShutdownHook();
			
			IFixIntegrationPluginSpi integrationPlugin = ctx.getBean(IFixIntegrationPluginSpi.class);
			IFixSetting fixSetting = ctx.getBean(IFixSetting.class);
			List<quickfix.SessionSettings> qfixSessionSettings = fixSetting.getQuickfixSessionSettings();
			LogControlVo logControl = new LogControlVo(true);
			MarketQFixApp qfixApp = new MarketQFixApp(fixSetting, integrationPlugin, logControl);
			
			for (quickfix.SessionSettings qfixSessionSetting: qfixSessionSettings ) {
				Properties p = qfixSessionSetting.getDefaultProperties();
				logger.debug("Loading : " + qfixSessionSetting);
				if ( "acceptor".equals(p.get("ConnectionType"))) {		
					QuickfixEngineAcceptorContainer container = new QuickfixEngineAcceptorContainer(
							qfixSessionSetting, 
							qfixApp,
							logControl);
		
					container.start();
				} else {
					QuickfixEngineInitiatorContainer container = new QuickfixEngineInitiatorContainer(
							qfixSessionSetting,
							qfixApp,
							logControl);
					container.logon();
					
					Thread.sleep(30 * ChronologyUtil.MILLIS_IN_SECOND);

				}
			
			}	
			Thread.sleep(60 * ChronologyUtil.MILLIS_IN_SECOND);
			
			String appType = null;
			String symbol_str = null;
			String tenor = TenorVo.NOTATION_SPOT;
			String size_str = null;
			
			if ( args.length >= 1) {
				appType = args[0];
			}
			
			if ( args.length >= 2) {
				symbol_str = args[1];
			}
			
			if ( args.length >= 3) {
				tenor = args[2];
				if ( tenor.equals("-") || tenor.equals("na") )  {
					tenor =  TenorVo.NOTATION_SPOT;
				}
			}
			
			if ( args.length >= 4) {
				size_str = args[3];
				if ( size_str.equals("-") || size_str.equals("na") )  {
					size_str = null;
				}
			}

			if ( args.length >= 5) {
				try {
					quoteDuration = Integer.parseInt(args[4]);
				} catch (Exception e) {
					quoteDuration = 60;
				}
			}
			if ( args.length >= 6) {
				notionalOnCcy2 = Boolean.parseBoolean(args[5]);
			}
			String[] symbols = null;
			if ( symbol_str.indexOf(",") > 0 ) {
				symbols = symbol_str.split(",");
			} else {
				symbols = new String[] { symbol_str };
			}
			String[] sizes = null;
			if ( size_str != null && size_str.indexOf(",") > 0 ) {
				sizes = size_str.split(",");
			} else {
				sizes = new String[] { size_str };
			}
			
			IMsgReceiverFactory msgReceiverFactory = ctx.getBean(IMsgReceiverFactory.class);
			IMsgReceiver msgReceiver = msgReceiverFactory.getMsgReceiver(false, false, false);
			msgReceiver.setTopic("_TTS.CTRL.EVENT.SUBSCRIBE.MD");
			msgReceiver.setListener(new OnDemandMDListener(qfixApp));
			msgReceiver.init();
			qfixApp.setExecutionReportListener(new ExecReportSysOutFixListener());
			logger.info("Submitting " + appType  + " request(s), symbol: " + symbol_str + ", tenor:" + tenor + ", size: " + size_str + " quoteDuration: " + quoteDuration);
			if ( appType.equalsIgnoreCase("rfs")) {
				for (String symbol: symbols) {
					for ( String size: sizes) {
						qfixApp.sendRfsRequest(Long.parseLong(size), symbol, notionalOnCcy2? symbol.substring(3,  6) : symbol.substring(0 ,3), tenor, "19700101", QuoteSide.BOTH, quoteDuration, new SysOutFixListener(), AppType.SPOTADAPTER);
					}
				}
			}
			if ( appType.equalsIgnoreCase("esp")) {
				for (String s: symbols) {
					qfixApp.sendEspRequest(s, tenor, "19700101", new TradableSysOutFixListener(qfixApp), AppType.SPOTADAPTER);
				}
			}
			
			while ( true) {
				Thread.sleep(Long.MAX_VALUE);
			}

		} catch (InterruptedException e) {
			
		} catch (Exception e) {
			monitorAgent.logError(func, MonitorConstant.ERROR_UNKNOWN_SYSTEM_ERR, "Main Exception " + e.getMessage(), e);
			e.printStackTrace();
		} finally { 
			if ( dataFlowController != null ) {
				try {
					dataFlowController.destroy();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (ctx != null) {
				ctx.close();
			}
		}
	}
	
	private static class OnDemandMDListener implements IMsgListener {
		private final IMkQfixApp qfixApp;

		public OnDemandMDListener(IMkQfixApp qfixApp) {
			this.qfixApp = qfixApp;
		}

		@Override
		public void onMessage(TtMsg arg0, IMsgSessionInfo arg1, IMsgProperties arg2) {
			try {
				PriceSubscriptionRequest request = PriceSubscriptionRequest.parseFrom(arg0.getParameters());
				logger.info(TextFormat.shortDebugString(request));
				String symbol = request.getQuoteParam().getCurrencyPair();
				qfixApp.sendEspRequest(symbol, TenorVo.NOTATION_SPOT, "19700101", new SysOutFixListener(), AppType.SPOTADAPTER);

			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
	}

}
