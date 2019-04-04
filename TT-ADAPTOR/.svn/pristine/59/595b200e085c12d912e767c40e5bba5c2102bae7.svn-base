package com.tts.mlp.data.provider.vo;

import java.util.ArrayList;
import java.util.List;

public class InstrumentRateVo
{
	
	private static String increment = "0.00001";

	protected transient InstrumentDefinitionVo instrumentDefinition;
	protected String tobMidPrice;

	private List<TickEntry> bidTicks = new ArrayList<TickEntry>();
	private List<TickEntry> askTicks = new ArrayList<TickEntry>();

	public InstrumentRateVo(InstrumentDefinitionVo def) {
		super();
		this.instrumentDefinition = def;
	}


	public InstrumentRateVo(InstrumentRateVo instrument)
	{
		super();
		this.instrumentDefinition = instrument.instrumentDefinition;
		this.tobMidPrice = instrument.tobMidPrice;

	}
	
	public InstrumentRateVo deepClone() {
		return new InstrumentRateVo(this);
	}
	
	/**
	 * @return the symbol
	 */
	public String getSymbol()
	{
		return instrumentDefinition.getSymbol();
	}

	/**
	 * @return the precision
	 */
	public int getPrecision() {
		return instrumentDefinition.getSpotPrecision();
	}
	
	public int getPointValue() {
		return instrumentDefinition.getPointValue();
	}

	/**
	 * @return the increment
	 */
	public String getIncrement() {
		return increment;
	}

	/**
	 * @return the askTicks
	 */
	public List<TickEntry> getAskTicks()
	{
		return askTicks;
	}

		
	/**
	 * @return the bidTicks
	 */
	public List<TickEntry> getBidTicks()
	{
		return bidTicks;
	}
	
		
	public String getTobMidPrice() {
		return tobMidPrice;
	}


	public void setTobMidPrice(String tobMidPrice) {
		this.tobMidPrice = tobMidPrice;
	}


	public static void main(String[] args) {

	}


}
