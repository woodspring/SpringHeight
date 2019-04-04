package com.tts.plugin.adapter.impl.cibc.setting;

import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.plugin.adapter.impl.base.setting.DefaultFixSettingBuilderImpl;

public class CibcFixSettingBuilderImpl extends DefaultFixSettingBuilderImpl {
	
	@Override
	public IFixSetting build() {
		return new CibcFixSettingImpl(getFixSessionSettings(), getFixApplicationProperties());
	}

}
