package com.tts.web.controller;


import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;

@Controller
@RequestMapping(value = "/spread")
public class SpreadController {
	
	@Inject
	PriceSubscriptionRegistry priceSubscriptionRegister;

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public String showIndex() {
    	String name = GlobalAppConfig.getSpreadConfig().getName();
        return name;
    }
    
    @RequestMapping(value = "/update/{spreadName}", method = RequestMethod.GET)
    @ResponseBody
    public String updateSpreadConfig(@PathVariable("spreadName") String spreadName) {
    	GlobalAppConfig.switchSpreadConfig(spreadName);
		return "SUCCESS";
    	
    }
    

}
