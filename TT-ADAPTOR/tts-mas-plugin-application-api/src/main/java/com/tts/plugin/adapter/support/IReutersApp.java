package com.tts.plugin.adapter.support;

import com.tts.fix.support.ISupportiveApp;


public interface IReutersApp extends ISupportiveApp {
	
	/**
	 * Sends Price Streaming Request to Reuters.
	 * @param symbol		SYMBOL for which Streaming Request is send.
	 * @param tenor			Tenor line ON,TN
	 * @param listener		Object interested in getting streaming response.
	 * @return				Subscription ID. NULL if the request failed.
	 */
	public String subscribeToFwdRIC(String symbol, String tenor, IReutersRFAMsgListener listener);

	/**
	 * Un-Subscribe to Specific SYMBOL based on subscriptionId
	 * @param subscriptionId
	 */
	public void unsubscribeFromFwdRIC(String subscriptionId);
	
	/**
	 * Un-Subscribe ALL SYMBOL that are currently Subscribed
	 */
	public void unsubscribeFromFwdRIC();
	
	/**
	 * Initialize & Start the Reuters Application.
	 */
	public void beginReutersApp();
	
	/**
	 * End & Cleanup Reuters Application.
	 * This Method will un-subscribe ALL SYMBOLS.
	 */
	public void endReutersApp();
	
	/**
	 * Check if Adapter is Currently logged-in to Reuters Data Provider Server
	 * @return		TRUE if successfully logged-in else FALSE.
	 */
	public boolean isReutersAppLoggedIn();
	
	/**
	 * Gets the MAX time interval that a price message is considered valid.
	 * If the CurrentTime - LastReceivedTime is above this value, a re-subscription is initiated.
	 * @return MAX Time Interval in MINUTES.
	 */
	public int getMaxAllowedMsgTimeInterval();
}
