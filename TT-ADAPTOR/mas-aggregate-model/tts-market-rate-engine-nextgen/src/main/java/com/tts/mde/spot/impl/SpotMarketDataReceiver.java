package com.tts.mde.spot.impl;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.util.AppContext;

public class SpotMarketDataReceiver implements IMsgListener {
	
	private volatile IMsgListener spotDataListener;

	private final IMsgReceiver msgReceiver;
	
	public SpotMarketDataReceiver() {
		super();
		IMsgReceiverFactory factory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		IMsgReceiver msgReceiver = factory.getMsgReceiver(true, false, false);
		msgReceiver.setListener(this);
		msgReceiver.setTopic("TTS.MD.FX.FA.>");
		msgReceiver.init();
		this.msgReceiver = msgReceiver;
	}

	@Override
	public void onMessage(TtMsg arg0, IMsgSessionInfo arg1, IMsgProperties arg2) {
		if ( spotDataListener != null ) {
			spotDataListener.onMessage(arg0, arg1, arg2);
		}
	}

	public void setSpotDataListener(IMsgListener spotDataListener) {
		this.spotDataListener = spotDataListener;
	}
	
	public void destroy() {
		msgReceiver.destroy();
	}

}
