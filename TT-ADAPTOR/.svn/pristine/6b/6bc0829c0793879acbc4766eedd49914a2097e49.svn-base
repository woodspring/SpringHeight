package com.tts.mde.plugin.ykb;

import com.tts.mde.fwdc.ykb.YkbWebServiceBasedForwardCurveMarketDataHandler;
import com.tts.mde.plugin.IMDEmbeddedAdapterFactory;
import com.tts.mde.service.IMDEmbeddedAdapter;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.vo.ISessionInfo;

public class YkbMDEmbeddedAdapterFactory implements IMDEmbeddedAdapterFactory {
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
		return new YkbWebServiceBasedForwardCurveMarketDataHandler(sessionInfo, schedulingWorker);
	}



}
