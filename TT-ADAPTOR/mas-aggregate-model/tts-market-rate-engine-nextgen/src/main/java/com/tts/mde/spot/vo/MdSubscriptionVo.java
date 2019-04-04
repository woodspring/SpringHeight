package com.tts.mde.spot.vo;

import com.tts.message.constant.Constants;

public class MdSubscriptionVo {

	private final MdSubscriptionType mdSubscriptionType;
	private final String adapter;
	private final String source;
	private final String symbol;
	private final double[] quantities1;
	private final double[] quantities2;
	private final String valueDate1;
	private final String valueDate2;
	private final String tenor1;
	private final String tenor2;
	private final String product;
	private final long quoteValidInterval;
	
	
	public MdSubscriptionVo(MdSubscriptionType mdSubscriptionType, String adapter, String source, String symbol, long quoteValidInterval) {
		super();
		this.mdSubscriptionType = mdSubscriptionType;
		this.symbol = symbol;
		this.adapter = adapter;
		this.source = source;
		this.quantities1 = null;
		this.quantities2 = null;
		this.valueDate1 = null;
		this.valueDate2 = null;
		this.tenor1 = null;
		this.tenor2 = null;
		this.product = Constants.ProductType.FXSPOT;
		this.quoteValidInterval = quoteValidInterval;
	}
	
	public MdSubscriptionVo(MdSubscriptionType mdSubscriptionType, String adapter, String source, String symbol, long quoteValidInterval, double[] rungs) {
		super();
		this.quoteValidInterval = quoteValidInterval;
		this.mdSubscriptionType = mdSubscriptionType;
		this.symbol = symbol;
		this.adapter = adapter;
		this.source = source;
		this.quantities1 = rungs;
		this.quantities2 = null;
		this.valueDate1 = null;
		this.valueDate2 = null;
		this.tenor1 = null;
		this.tenor2 = null;
		this.product = Constants.ProductType.FXSPOT;
	}
	public MdSubscriptionType getMdSubscriptionType() {
		return mdSubscriptionType;
	}
	public String getAdapter() {
		return adapter;
	}
	public String getSource() {
		return source;
	}
	public double[] getQuantities1() {
		return quantities1;
	}
	public double[] getQuantities2() {
		return quantities2;
	}
	public String getValueDate1() {
		return valueDate1;
	}
	public String getValueDate2() {
		return valueDate2;
	}
	public String getTenor1() {
		return tenor1;
	}
	public String getTenor2() {
		return tenor2;
	}
	public String getProduct() {
		return product;
	}
	public String getSymbol() {
		return symbol;
	}
	public long getQuoteValidInterval() {
		return quoteValidInterval;
	}

	public String getIdentifer() {
		StringBuilder sb = new StringBuilder(adapter);
		sb.append(mdSubscriptionType.getValue());
		sb.append(this.source);
		if ( mdSubscriptionType != MdSubscriptionType.ESP && quantities1.length > 0) {
			for ( double d: quantities1) {
				sb.append(d);
			}
		}
		return sb.toString();
	}
}
