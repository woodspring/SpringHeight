package com.tts.mlp.app.price.data.refresh;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.tts.util.collection.formatter.DoubleFormatter;

public class InstrumentRateVo
{
	private static String increment = "0.00001";
	
	private transient InstrumentDefinitionVo instrumentDefinition;
	private String tobMidPrice;

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


	/**
	 * 
	 */
	public void randomWalk(MarketRawDataVo raw) {
		long[] lqLevels = this.instrumentDefinition.getLqyStructure();
		long tobSize = lqLevels[0];
		getBidTicks().clear();
		getAskTicks().clear();
		this.tobMidPrice = DoubleFormatter.convertToString(raw.getMid(), this.instrumentDefinition.getSpotPrecision(), RoundingMode.UNNECESSARY);
		TickEntry bidTick = new TickEntry(DoubleFormatter.convertToString(raw.getBid(), this.instrumentDefinition.getSpotPrecision(), RoundingMode.FLOOR), tobSize);
		TickEntry askTick = new TickEntry(DoubleFormatter.convertToString(raw.getAsk(), this.instrumentDefinition.getSpotPrecision(), RoundingMode.CEILING), tobSize);
		getBidTicks().add(bidTick);
		getAskTicks().add(askTick);
		for ( int i = 1; i < lqLevels.length ; i++ ) {
			long lqLevel = lqLevels[i];
			double diff = (double) (lqLevel / 100000);
			double off = diff * Math.pow(10, -1 * this.instrumentDefinition.getSpotPrecision()) / 1.755;
			TickEntry newBidTick = new TickEntry(DoubleFormatter.convertToString(raw.getBid() - off, this.instrumentDefinition.getSpotPrecision(), RoundingMode.FLOOR), lqLevel);
			TickEntry newAskTick = new TickEntry(DoubleFormatter.convertToString(raw.getAsk() + off, this.instrumentDefinition.getSpotPrecision(), RoundingMode.CEILING), lqLevel);
			getBidTicks().add(newBidTick);
			getAskTicks().add(newAskTick);
		}
//		BigDecimal incrementBd = new BigDecimal(increment);
//		long randomwalk =  spreadConfig.getRwalkMin() + (long)(Math.random() * ((spreadConfig.getRwalkMax() - spreadConfig.getRwalkMin()) + 1));
//		tobMidPrice= BigDecimal.valueOf(tobMidPrice).add(BigDecimal.valueOf( ((double) randomwalk/Math.pow(10, this.instrumentDefinition.getSpotPrecision())))).doubleValue();
//		BigDecimal bid = BigDecimal.valueOf(tobMidPrice).subtract(spread);
//		BigDecimal ask = BigDecimal.valueOf(tobMidPrice).add(spread);
//		for ( int i = 0; i < lqLevels.length; i++) {
//			Tick bidTick = new Tick(bid.doubleValue(), lqLevels[i], i+1);
//			Tick askTick = new Tick(ask.doubleValue(), lqLevels[i], i+1);
//			bid = bid.subtract(incrementBd);
//			ask = ask.add(incrementBd);
//			bidTicks.add(bidTick);
//			askTicks.add(askTick);
//		}
	}

	public static void main(String[] args) {

	}


}
