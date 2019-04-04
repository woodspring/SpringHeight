package com.tts.mas.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tts.plugin.adapter.api.app.IApp.PublishingFormatType;
import com.tts.plugin.adapter.api.app.IPublishingApp;

public class InjectionConfig {

	private volatile List<IPublishingApp> pApps = Collections.emptyList();

	public List<IPublishingApp> getPublishingApps() {
		return pApps;
	}

	public void setPublishingApps(List<IPublishingApp> pApps) {
		List<IPublishingApp> _pApps = new ArrayList<IPublishingApp>(pApps);
		Collections.sort(_pApps, new AppPublishingOrderingSorter());
		this.pApps = Collections.unmodifiableList(_pApps);
	}
	
	private static class AppPublishingOrderingSorter implements Comparator<IPublishingApp> {

		@Override
		public int compare(IPublishingApp o1, IPublishingApp o2) {
			if ( o1.getPublishingFormatType().equals(PublishingFormatType.FxSpot)) {
				return Integer.MAX_VALUE;
			}
			if ( o2.getPublishingFormatType().equals(PublishingFormatType.FxSpot)) {
				return Integer.MIN_VALUE;
			}
			return o1.getName().compareTo(o2.getName());
		}
		
	}
}
