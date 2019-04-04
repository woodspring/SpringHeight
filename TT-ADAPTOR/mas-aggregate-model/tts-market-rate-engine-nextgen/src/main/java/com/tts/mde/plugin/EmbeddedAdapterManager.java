package com.tts.mde.plugin;

import com.tts.mde.service.IMDEmbeddedAdapter;
import com.tts.mde.support.ISchedulingWorker;
import com.tts.mde.vo.ISessionInfo;
import com.tts.util.plugin.AbstractPlugInManager;

public class EmbeddedAdapterManager extends AbstractPlugInManager<IMDEmbeddedAdapterFactoryPlugin> {
	private ISchedulingWorker schedulingWorker;
	private ISessionInfo sessionInfo;
	
	public EmbeddedAdapterManager() {
		super(IMDEmbeddedAdapterFactoryPlugin.class);
	}

	public void setRunnableWorker(ISchedulingWorker schedulingWorker) {
		this.schedulingWorker = schedulingWorker;
	}
	
	public void setSessionInfo(ISessionInfo sessionInfo) {
		this.sessionInfo = sessionInfo;
	}
	
	public IMDEmbeddedAdapter getMDEmbeddedAdapter(String adapterNm) {
		for (IMDEmbeddedAdapterFactoryPlugin plugin: loader  )  {
			plugin.setRunnableWorker(schedulingWorker);
			plugin.setSessionInfo(sessionInfo);
			IMDEmbeddedAdapter adapter = plugin.getMDEmbeddedAdapterByAdapterSourceName(adapterNm);
			if ( adapter != null ) {
				return adapter;
			}
		}
		return null;
	}
	
}
