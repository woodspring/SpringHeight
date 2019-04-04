package com.tts.web.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tts.mlp.app.ForwardCurveDataManager;
import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.app.price.data.IUpdatableMarketPriceProvider;
import com.tts.mlp.app.price.subscription.PriceSubscriptionRegistry;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.Tick;
import com.tts.util.AppContext;

@Controller
@RequestMapping(value = "/instrument")
public class InstrumentController {
	
	@Inject
	PriceSubscriptionRegistry priceSubscriptionRegister;
	
    @RequestMapping(value = "/{symbol}/setIndicative", method = RequestMethod.GET)
    @ResponseBody
    public String setIndicative(@PathVariable("symbol") String symbol, 
    		@RequestParam("indicative") String indicative) {
        return priceSubscriptionRegister.setIndicativeStatus(symbol, Boolean.parseBoolean(indicative.toUpperCase()));
    }
    
    @RequestMapping(value = "/{symbol}/getIndicative", method = RequestMethod.GET)
    @ResponseBody
    public String getIndicative(@PathVariable("symbol") String symbol) {
        return new Boolean(priceSubscriptionRegister.getIndicativeStatus(symbol)).toString();
    }
	
    @RequestMapping(value = "/{symbol}/setPriceStructure", method = RequestMethod.GET)
    @ResponseBody
    public String setPriceStructure(@PathVariable("symbol") String symbol, 
    		@RequestParam("defaultRate") String defaultRate, 
    		@RequestParam("structures") String structures) {
		String[] structure = structures.split(",");

    	IRandomMarketPriceProvider randomMarketPriceProvider = AppContext.getContext().getBean(IRandomMarketPriceProvider.class);
    	if ( randomMarketPriceProvider instanceof  IUpdatableMarketPriceProvider) {
    		IUpdatableMarketPriceProvider updatableMarketPriceProvider = (IUpdatableMarketPriceProvider) randomMarketPriceProvider;
    		updatableMarketPriceProvider.updateMarketLiquidityStructure(symbol, defaultRate, structure);
    		return getPriceStructure(symbol);
    	}
        return "NOT SUPPORTED";
    }
    
    @RequestMapping(value = "/{symbol}/setFwdPts", method = RequestMethod.GET)
    @ResponseBody
    public String setForwardPoints(@PathVariable("symbol") String symbol, 
    		@RequestParam("bidPts") String bidPts, 
    		@RequestParam("askPts") String askPts) {
		
    	String result = "";
    	try {
    		double[] forwardPts = {Double.valueOf(bidPts),Double.valueOf(bidPts)};
    		ForwardCurveDataManager.setSwapPoints(symbol, forwardPts);
    		result = "FORWARD POINTS SET";
    	} catch (NumberFormatException e) {
    		result = "INVALID FORWARD POINTS";
    	}
    	
        return result;
    }
    
    @RequestMapping(value = "/{symbol}/setSettleDate", method = RequestMethod.GET)
    @ResponseBody
    public String setSettleDate(@PathVariable("symbol") String symbol, 
    		@RequestParam("date") String date) {
    	GlobalAppConfig.setOverrideSettleDate(symbol, date);
    	if ( date == null || date.isEmpty() ) {
    		GlobalAppConfig.removeOverrideSettleDate(symbol);
    	}
        return "OK";
    }

    @RequestMapping(value = "/{symbol}/trading", method = RequestMethod.GET)
    @ResponseBody
    public String setTradingAllow(@PathVariable("symbol") String symbol, 
    		@RequestParam("allow") String allow) {
    	if ( "false".equalsIgnoreCase(allow)) {
    		GlobalAppConfig.setTradeReject(symbol, Boolean.TRUE.toString());
    	} else {
    		GlobalAppConfig.setTradeReject(symbol, Boolean.FALSE.toString());
    	}
        return "OK";
    }
    
    @RequestMapping(value = "/{symbol}/getPriceStructure", method = RequestMethod.GET)
    @ResponseBody 
    public String getPriceStructure(@PathVariable("symbol") String symbol) {
    	IRandomMarketPriceProvider randomMarketPriceProvider = AppContext.getContext().getBean(IRandomMarketPriceProvider.class);
    	Instrument instrument = randomMarketPriceProvider.getCurrentPrice(symbol);
    	StringBuilder sb = new StringBuilder();
    	sb.append(instrument.getSymbol()).append('\n');
    	List<Tick> askTicks = instrument.getAskTicks();
    	List<Tick> bidTicks = instrument.getBidTicks();
    	for ( int i = 0; i < askTicks.size(); i ++ ) {
    		sb.append(askTicks.get(i).getQuantity()).append(' ').append(bidTicks.get(i).getPrice()).append(' ').append(askTicks.get(i).getPrice()).append('\n');
    	}
		return sb.toString();
	}
}
