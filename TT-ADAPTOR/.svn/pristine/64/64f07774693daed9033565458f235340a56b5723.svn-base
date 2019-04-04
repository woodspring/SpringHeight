package com.tts.web.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;

import quickfix.SessionID;

@Controller
@RequestMapping(value = "/")
public class IndexController {
	
	@Inject
	PriceSubscriptionRegistry priceSubscriptionRegister;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public String showIndex() {
    	List<SessionID> sessions = priceSubscriptionRegister.listSessions();
    	Gson gson = new Gson();
        return gson.toJson(sessions);
    }
    @RequestMapping(value = "/subs", method = RequestMethod.GET)
    @ResponseBody
    public String showSubscriptions() {
    	List<SubscriptionRequestVo> subs = priceSubscriptionRegister.getSubscriptionRequests();
    	Gson gson = new Gson();
        return gson.toJson(subs);
    }
    
    @RequestMapping(value = "/set", method = RequestMethod.GET)
    @ResponseBody
    public String setIndicative(@RequestParam("symbol") String symbol,@RequestParam("indicative") String indicative) {
        return priceSubscriptionRegister.setIndicativeStatus(symbol, Boolean.parseBoolean(indicative.toUpperCase()));
    }
    
    @RequestMapping("/")
    protected String redirect() 
    {
        return "redirect:index.html";
    }

    @RequestMapping("/rates_pause")
    @ResponseBody
    public String pauseRates(@RequestParam("enable") String enable) {
    	boolean enableB = Boolean.parseBoolean(enable);
    	GlobalAppConfig.setRatePause(enableB);
    	return "OK";
    }
    
    @RequestMapping("/rates_freeze")
    @ResponseBody
    public String freezeRates(@RequestParam("enable") String enable) {
    	boolean enableB = Boolean.parseBoolean(enable);
    	GlobalAppConfig.setRateFreezed(enableB);
    	return "OK";
    }
    
    @RequestMapping("/orderTimeCheck")
    @ResponseBody
    public String orderTimeCheck(@RequestParam("enable") String enable) {
    	boolean enableB = Boolean.parseBoolean(enable);
    	GlobalAppConfig.setOrderTimeCheck(enableB);
    	return "OK";
    }
    
    @RequestMapping("/crazyPriceStructure")
    @ResponseBody
    public String crazyPriceStructure(@RequestParam("enable") String enable) {
    	boolean enableB = Boolean.parseBoolean(enable);
    	GlobalAppConfig.setCrazyPriceStructure(enableB);
    	return "OK";
    }
    
    @RequestMapping("/doubleSizeUp")
    @ResponseBody
    public String doubleSizeUp(@RequestParam("bidEnable") String bidEnable, @RequestParam("offerEnable") String offerEnable) {
    	boolean bidEnableB = Boolean.parseBoolean(bidEnable);
    	boolean offerEnableB = Boolean.parseBoolean(offerEnable);

    	GlobalAppConfig.setDoubleSizeUp(bidEnableB, offerEnableB);
    	return "OK";
    }
    
    @RequestMapping("/setFillPriceMustBeDiffThanOrdPrice")
    @ResponseBody
    public String setFillPriceMustBeDiffThanOrdPrice(@RequestParam("enable") String enable) {
    	boolean enableB = Boolean.parseBoolean(enable);
    	GlobalAppConfig.setFillPriceMustBeDiffThanOrdPrice(enableB);
    	return "OK";
    }
}
