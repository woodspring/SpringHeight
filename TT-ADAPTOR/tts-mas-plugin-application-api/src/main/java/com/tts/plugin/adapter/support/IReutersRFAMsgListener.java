package com.tts.plugin.adapter.support;

import com.tts.message.market.ForwardCurveStruct.ForwardCurve;

public interface IReutersRFAMsgListener {
	
	public void onFwdRICMessage(String SubscriptionID,  ForwardCurve.Builder fwdPrice);
}
