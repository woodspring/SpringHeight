package com.tts.mde.plugin;

import com.tts.mde.service.IMDEmbeddedAdapter;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.vo.ISessionInfo;

public interface IMDEmbeddedAdapterFactoryPlugin {

	void setRunnableWorker(ISchedulingWorker schedulingWorker) ;
	
	/**
	 * Create a new  instance of adapter
	 * 
	 * @param adapterNm
	 * @return  new  instance of adapter
	 */
	IMDEmbeddedAdapter getMDEmbeddedAdapterByAdapterSourceName(String adapterNm);

	void setSessionInfo(ISessionInfo sessionInfo);
	
	
}
