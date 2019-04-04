package com.tts.plugin.adapter.api.app;

import com.tts.message.TtMessageStruct.TtMsg;

/**
 * SubscribingApp is an event-triggered "apps". When an message in the platform arrives
 * at the desired topics, the "app" is expected to handle the request or status changes
 * 
 *
 */
public interface ISubscribingApp extends IExternalInterfacingApp {

	/** 
	 * action for "app" to implement for handling the event or status change
	 * 
	 * @param topic
	 * @param message
	 */
	public void onRequest(String topic, TtMsg message);
	
	/**
	 * provide the topics the "app" interested in
	 * 
	 * @return topics
	 */
	public String[] getDesiredTopics();
}
