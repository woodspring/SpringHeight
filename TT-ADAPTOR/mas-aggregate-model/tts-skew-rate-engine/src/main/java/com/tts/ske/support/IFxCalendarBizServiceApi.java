package com.tts.ske.support;

import com.tts.message.system.RolloverStruct.RolloverNotification;

public interface IFxCalendarBizServiceApi {

	public String getForwardValueDate(String symbol, String tenor);

	public String getCurrentBusinessDay(String symbol);
	
	public void onRolloverEvent(RolloverNotification rolloverNotification);
	
}
