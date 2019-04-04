package com.tts.plugin.adapter.impl.base;

import com.tts.plugin.adapter.api.IMasApplicationPluginSpi;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;
import com.tts.plugin.adapter.impl.base.app.DefaultAppFactoryImpl;

public class DefaultMasApplicationPlugin implements IMasApplicationPluginSpi {

	public static final String NAME__TTS_DEFAULT = "DEFAULT";

	@Override
	public String getName() {
		return NAME__TTS_DEFAULT;
	}


	@Override
	public IInterfacingAppFactory getInterfacingAppFactory() {
		return new DefaultAppFactoryImpl();
	}

}
