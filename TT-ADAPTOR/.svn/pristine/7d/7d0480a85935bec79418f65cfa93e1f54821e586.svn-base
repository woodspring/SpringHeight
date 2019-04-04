package com.tts.mde.plugin;

import com.tts.mde.fwdc.TickTradeRestBasedForwardCurveMarketDataHandler;
import com.tts.mde.service.IMDEmbeddedAdapter;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.vo.ISessionInfo;

public class TtsDemoMDEmbeddedAdapterFactory implements IMDEmbeddedAdapterFactoryPlugin {
	private ISchedulingWorker schedulingWorker;
	private ISessionInfo sessionInfo;

	@Override
	public void setRunnableWorker(ISchedulingWorker schedulingWorker) {
		this.schedulingWorker = schedulingWorker;
		
	}
	
	@Override
	public void setSessionInfo(ISessionInfo sessionInfo) {
		this.sessionInfo = sessionInfo;
		
	}

	@Override
	public IMDEmbeddedAdapter getMDEmbeddedAdapterByAdapterSourceName(String adapterNm) {
		if ( "TTS_SWAP_PTS_ADAPTER".equals(adapterNm)) {
			return new TickTradeRestBasedForwardCurveMarketDataHandler(sessionInfo, schedulingWorker);
		}
		return null;
	}



}
