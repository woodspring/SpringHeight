package com.tts.adapter.api;

import com.tts.plugin.adapter.api.dialect.vo.QuoteVo;
import com.tts.plugin.adapter.impl.base.app.QuoteStackRepo;

public class QuoteStackRepoTest {

	
	public static void main(String[] args) {
		QuoteStackRepo repo = new QuoteStackRepo();
		
		QuoteVo a = new QuoteVo();
		a.setQuoteId("DEF");
		
		QuoteVo b = new QuoteVo();
		b.setQuoteId("EFG");
		
		QuoteVo c = new QuoteVo();
		c.setQuoteId("FGH");
		
		QuoteVo d = new QuoteVo();
		d.setQuoteId("GHI");
		
		QuoteVo e = new QuoteVo();
		e.setQuoteId("HIJ");
		
		repo.addQuote("ABC", a );
		repo.addQuote("ABC", b );
		repo.addQuote("ABC", c );
		repo.addQuote("ABC", d );
		repo.addQuote("ABC", e );
		
		QuoteVo q = repo.findQuote("ABC", "DEF");
		
		System.out.println(q.getQuoteId());
		
	}
}
