package com.tts.web.controller;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tts.mlp.data.provider.IMarketRawDataProvider;
import com.tts.mlp.data.provider.MarketDataProvider;
import com.tts.mlp.data.provider.MarketRateBookProvider;
import com.tts.mlp.data.provider.vo.InstrumentRateVo;
import com.tts.mlp.data.provider.vo.InstrumentSwapPointsVo;
import com.tts.mlp.data.provider.vo.MarketRawDataVo;
import com.tts.mlp.data.provider.vo.SwapPointEntityVo;
import com.tts.util.AppContext;


@Controller
@RequestMapping(value = "/")
public class IndexController {

    
    @RequestMapping("/")
    protected String redirect() 
    {
        return "redirect:index.html";
    }

    @RequestMapping("/getSpotData/{symbol}")
    @ResponseBody
    public InstrumentRateVo getSpotData(@PathVariable("symbol") String symbol) {
    	MarketRateBookProvider p = AppContext.getContext().getBean(MarketRateBookProvider.class);
    	return p.getInstrumentRateBook(symbol);
    }
    
    @RequestMapping("/getSwapPoints/{symbol}")
    @ResponseBody
    public InstrumentSwapPointsVo getSwapPoints(@PathVariable("symbol") String symbol) {
    	MarketDataProvider p = AppContext.getContext().getBean(MarketDataProvider.class);
    	MarketRawDataVo rawSpot = p.getSpotData(symbol);
    	List<MarketRawDataVo> rawFwds = p.getFwdData(symbol);
    	List<SwapPointEntityVo> outList = new ArrayList<>();
    	for ( MarketRawDataVo rawFwd : rawFwds ) {
    		SwapPointEntityVo swap = new SwapPointEntityVo();
    		swap.setSymbol(	rawFwd.getName() );
    		swap.setTenorNm(rawFwd.getName2());
    		swap.setBidSwapPoint(rawFwd.getBid());
    		swap.setAskSwapPoint(rawFwd.getAsk());
    		swap.setBidSpotRefRate(rawSpot.getBid());
    		swap.setAskSpotRefRate(rawSpot.getBid());
    		outList.add(swap);
    	}
    	return new InstrumentSwapPointsVo(outList);
    }
    
    @RequestMapping("/getDataRefreshTimestamp")
    @ResponseBody
    public long getDataRefreshTimestamp() {
    	IMarketRawDataProvider p = AppContext.getContext().getBean(IMarketRawDataProvider.class);
    	return p.getDataRefreshTimestamp();
    } 
    
    
    @RequestMapping("/refresh")
    @ResponseBody
    public long refresh() {
    	long rte = -1;
    	IMarketRawDataProvider p = AppContext.getContext().getBean(IMarketRawDataProvider.class);
    	try {
			p.refresh();
			rte = p.getDataRefreshTimestamp();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	return rte;
    } 
}
