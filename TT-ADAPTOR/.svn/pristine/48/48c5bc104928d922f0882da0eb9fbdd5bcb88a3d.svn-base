package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.market.ForwardCurveStruct.ForwardCurve;
import com.tts.plugin.adapter.api.app.IApp.AppType;
import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.plugin.adapter.support.ISchedulingWorker;
import com.tts.plugin.adapter.support.vo.IndividualInfoVo;
import com.tts.plugin.adapter.support.vo.SessionInfo;
import com.tts.util.flag.IndicativeFlag.IndicativeReason;


public class RefreshRequesterReutersFwdPoints implements Runnable
{
	private static Logger _rfaLogger = LoggerFactory.getLogger("ReutersAppLogger");
	
	private final String[] _symbols;
	private final String[] _tenors;
	private IReutersApp objRFAAdapter;
	private final ISchedulingWorker worker;
	
	private final long REUTERS_ALLOWED_RATE_TIME_FRAME;
	
	private final Map<String, IndividualInfoVo> sessionMarketSetup;
	private final DefaultReutersFwdPointsAppImpl reutersFwdImpl;
	private volatile ScheduledFuture<?> scheduled = null;
	
	public RefreshRequesterReutersFwdPoints(SessionInfo sessionInfo, 
			String[] symbols, 
			String[] tenors,
			IReutersApp rfaAdapter, 
			ISchedulingWorker worker,
			DefaultReutersFwdPointsAppImpl reutersFwdImpl) {
		
		this._tenors  = tenors;
		this._symbols = symbols;
		this.objRFAAdapter = rfaAdapter;
		this.worker =  worker;
		this.reutersFwdImpl = reutersFwdImpl;
		
		this.REUTERS_ALLOWED_RATE_TIME_FRAME = TimeUnit.MINUTES.toMillis(rfaAdapter.getMaxAllowedMsgTimeInterval());
		
		this.sessionMarketSetup = sessionInfo.getMarketDataset().getMarketStructuresByType(AppType.FCADAPTER.getPublishingFormatType().toString());
	}
	
	public void init()	{
		if(_symbols.length > 0)	{
			scheduled = getWorker().scheduleAtFixedRate(this, 30L);
		}
	}
	
	public void destory()	{
		if(scheduled != null)	{
			scheduled.cancel(false);
		
			int attempt = 0;
			while(attempt < 3 && (!scheduled.isDone() || !scheduled.isCancelled())) {
				try {
					Thread.sleep(500L);
				} 
				catch(InterruptedException iexp) {	}
				
				attempt++;
			}
		}
		
		_rfaLogger.warn("RefreshRequesterReutersFwdPoints @ DESTORY. Unsubscribing from all PRICE REQ.");
		this.objRFAAdapter.unsubscribeFromFwdRIC();
	}
	
	@Override
	public void run() {
		ArrayList<String> symbolsReqRefresh = new ArrayList<>(_symbols.length);
				
		long currentTimeStamp = System.currentTimeMillis();
		for(String symbol : _symbols)	{
			
			IndividualInfoVo individualInfo = sessionMarketSetup.get(symbol);
			long receiveTimeStamp           = 0L;
			for(String tenor: _tenors)	{
				ForwardCurve.Builder fwdPrice = reutersFwdImpl.getLatests(symbol, tenor);
				if((fwdPrice != null) && (fwdPrice.hasLatency()) && (fwdPrice.getLatency().getFaReceiveTimestamp() > receiveTimeStamp))	{
					receiveTimeStamp = fwdPrice.getLatency().getFaReceiveTimestamp();
				}
				
				if((fwdPrice != null) && (fwdPrice.hasLatency()) && 
				   ((fwdPrice.getLatency().getFaReceiveTimestamp() <= 0L) || ((currentTimeStamp - fwdPrice.getLatency().getFaReceiveTimestamp()) >= REUTERS_ALLOWED_RATE_TIME_FRAME)))	{
					symbolsReqRefresh.add(symbol + "|" + tenor);
				}
			}
			
			if((currentTimeStamp - receiveTimeStamp) >= REUTERS_ALLOWED_RATE_TIME_FRAME)	{
				individualInfo.addIndicativeReason(IndicativeReason.MA_NoData);
				_rfaLogger.debug("NO DATA @ RefreshRequesterReutersFwdPoints for " + symbol + " ==>> " + currentTimeStamp + "--" + receiveTimeStamp + "--" + REUTERS_ALLOWED_RATE_TIME_FRAME);
			}
		}
		
		
		if(this.objRFAAdapter.isReutersAppLoggedIn()) {
			if(symbolsReqRefresh.size() > 0)	{
				cancelSubscriptionRequest(symbolsReqRefresh.toArray(new String[0]));
				submitSubscriptionRequest(symbolsReqRefresh.toArray(new String[0]));
			}
		}		
	}
	
	private void cancelSubscriptionRequest(String[] symbolTenorInfo)	{
		_rfaLogger.debug("RefreshRequesterReutersFwdPoints @ cancelSubscriptionRequest SYMBOLS: " + Arrays.toString(symbolTenorInfo));
		for(String symbolTenor: symbolTenorInfo)	{
			
			String[] temp =  symbolTenor.split("\\|");
			String subscriptionId = reutersFwdImpl.getSubscriptionID(temp[0], temp[1]);
			
			if(subscriptionId != null)
				objRFAAdapter.unsubscribeFromFwdRIC(subscriptionId);
		}
	}
	
	private void submitSubscriptionRequest(String[] symbolTenorInfo)	{
		_rfaLogger.debug("RefreshRequesterReutersFwdPoints @ submitSubscriptionRequest SYMBOLS: " + Arrays.toString(symbolTenorInfo));
		for(String symbolTenor: symbolTenorInfo)	{
			
			String[] temp =  symbolTenor.split("\\|");
			String subscriptionId = objRFAAdapter.subscribeToFwdRIC(temp[0], temp[1], reutersFwdImpl);
			reutersFwdImpl.updateNewSubscriptionID(temp[0], temp[1], subscriptionId);
			
			//final IndividualInfoVo individualInfo = sessionMarketSetup.get(temp[0]);
			//individualInfo.setLastRefresh(System.currentTimeMillis());
		}
	}
	
	private ISchedulingWorker getWorker() {
		return(this.worker);
	}
}
