package com.tts.plugin.adapter.impl.base;

import com.tts.plugin.adapter.api.IMasApplicationPluginSpi;
import com.tts.plugin.adapter.api.factory.IInterfacingAppFactory;

public class YkbMasApplicationPlugin implements IMasApplicationPluginSpi {

	public static final String NAME__YKB = "YKB";

	@Override
	public String getName() {
		return NAME__YKB;
	}

	@Override
	public IInterfacingAppFactory getInterfacingAppFactory() {
		return new YkbAppFactoryImpl();
	}

}
