package com.tts.ske.vo;

import com.tts.util.flag.IndicativeFlag;

public class SessionInfoVo {

	public volatile long globalIndicativeFlag = IndicativeFlag.TRADABLE;

	public long getGlobalIndicativeFlag() {
		return globalIndicativeFlag;
	}

	public void addGlobalIndicativeReason(IndicativeFlag.IndicativeReason reason) {
		this.globalIndicativeFlag = IndicativeFlag.setIndicative(globalIndicativeFlag, reason);
	}
	
	public void removeGlobalIndicativeReason(IndicativeFlag.IndicativeReason reason) {
		this.globalIndicativeFlag = IndicativeFlag.removeIndicative(globalIndicativeFlag, reason);
	}
	
}
