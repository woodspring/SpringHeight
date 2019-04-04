package com.tts.web.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tts.marketclient.app.TtsTradAirClientApp;

@Controller
@RequestMapping(value = "/")
public class IndexController {
	
	@Inject
	TtsTradAirClientApp app;

    @RequestMapping(value = "/subscribe", method = RequestMethod.GET)
    @ResponseBody
    public String subscribe(@RequestParam("symbol") String symbol,@RequestParam("requestId") String requestId) {
    	app.onNewPriceSubscriptionRquest(requestId,symbol );
        return "OK";
    }

    
    @RequestMapping(value = "/unsubscribe", method = RequestMethod.GET)
    @ResponseBody
    public String unsubscribe(@RequestParam("symbol") String symbol,@RequestParam("requestId") String requestId) {
    	app.onUnsubscribe(symbol, requestId);
        return "OK";

    }
    
    @RequestMapping("/execute")
    @ResponseBody
    public String execute(@RequestParam("transId") String transId, @RequestParam("symbol") String symbol, @RequestParam("notionalCurrency") String notionalCurrency, @RequestParam("amount") String amount, @RequestParam("side") String side, @RequestParam("ordPrice") String ordPrice, @RequestParam("timeInForce") String timeInForceStr) {
    	app.onNewExecutionRequest(transId, symbol, notionalCurrency, amount, side, ordPrice, timeInForceStr);
    	return "OK";
    }
}
