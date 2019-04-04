package com.tts.fixapi.impl;

import com.tts.fixapi.type.IFIXAcceptorIntegrationPlugin;
import com.tts.fixapi.type.IFIXAcceptorMessageBuilder;
import com.tts.fixapi.type.IFIXAcceptorRoutingAgent;


public class FIXAcceptorDefaultIntegrationPlugin implements IFIXAcceptorIntegrationPlugin {

	@Override
	public IFIXAcceptorMessageBuilder getFIXMessageBuilder() {
		return(new FIXAcceptorDefaultMessageBuilder());
	}

	@Override
	public IFIXAcceptorRoutingAgent getFIXRoutingAgent() {
		return(new FIXAcceptorDefaultRoutingAgent());
	}
}
