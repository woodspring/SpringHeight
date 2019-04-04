package com.tts.mlp.rate.provider.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.GlobalAppConfig.SpreadConfig;

import quickfix.field.MDEntryType;

public class Instrument
{

	private static String increment = "0.00001";
	
	private final String symbol;
		
	private final int precision;
	
	private final int pointValue;
		
	private long[] lqLevels;
	
	private double tobMidPrice;

	private List<Tick> bidTicks = new ArrayList<Tick>();
	private List<Tick> askTicks = new ArrayList<Tick>();

	public Instrument(String symbol, int precision, int pointValue) {
		super();
		this.symbol = symbol;
		this.precision = precision;
		this.pointValue = pointValue;
	}


	public Instrument(Instrument instrument)
	{
		super();
		this.symbol = instrument.symbol;
		this.precision = instrument.precision;
		this.pointValue = instrument.pointValue;
		
		this.tobMidPrice = instrument.tobMidPrice;
		this.lqLevels = Arrays.copyOf(instrument.lqLevels, instrument.lqLevels.length);

	}
	
	public Instrument deepClone() {
		return new Instrument(this);
	}
	
	/**
	 * @return the symbol
	 */
	public String getSymbol()
	{
		return symbol;
	}

	/**
	 * @return the precision
	 */
	public int getPrecision() {
		return precision;
	}
	
	public int getPointValue() {
		return pointValue;
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
	public List<Tick> getAskTicks()
	{
		return askTicks;
	}

		
	/**
	 * @return the bidTicks
	 */
	public List<Tick> getBidTicks()
	{
		return bidTicks;
	}

	/**
	 * @param entryType
	 * @return
	 */
	public List<Tick> getTicksByEntryType(char entryType)
	{
		switch (entryType)
		{
			case MDEntryType.BID:				
				return getBidTicks();

			case MDEntryType.OFFER:				
				return getAskTicks();
				
			default:
				return null;
		}
	}
	
	/**
	 * 
	 */
	public void sortBuyTicks()
	{
		Collections.sort(askTicks, new TickComparator());
	}
	
	/**
	 * 
	 */
	public void sortSellTicks()
	{
		Collections.sort(bidTicks, new TickComparator());
	}
	
	
	
	public long[] getLqLevels() {
		return lqLevels;
	}


	public void setLqLevels(long[] lqLevels) {
		this.lqLevels = lqLevels;
	}

	
	public double getTobMidPrice() {
		return tobMidPrice;
	}


	public void setTobMidPrice(double tobMidPrice) {
		this.tobMidPrice = tobMidPrice;
	}


	/**
	 * 
	 */
	public void randomWalk() {
		double[] deltaSpreadFactors = new double[] { 0.7d, 0.8d, 0.9d, 1.0d, 1.1d, 1.2d, 1.3d};
		SpreadConfig spreadConfig = GlobalAppConfig.getSpreadConfig();
		int idx = (int) (Math.random() * deltaSpreadFactors.length);
		
		BigDecimal spread = spreadConfig.getSpread().multiply(BigDecimal.valueOf(deltaSpreadFactors[idx]));
		BigDecimal incrementBd = new BigDecimal(increment);
		long randomwalk =  spreadConfig.getRwalkMin() + (long)(Math.random() * ((spreadConfig.getRwalkMax() - spreadConfig.getRwalkMin()) + 1));
		tobMidPrice= BigDecimal.valueOf(tobMidPrice).add(BigDecimal.valueOf( ((double) randomwalk/Math.pow(10, precision)))).doubleValue();
		BigDecimal bid = BigDecimal.valueOf(tobMidPrice).subtract(spread);
		BigDecimal ask = BigDecimal.valueOf(tobMidPrice).add(spread);
		for ( int i = 0; i < lqLevels.length; i++) {
			Tick bidTick = new Tick(bid.doubleValue(), lqLevels[i], i+1);
			Tick askTick = new Tick(ask.doubleValue(), lqLevels[i], i+1);
			bid = bid.subtract(incrementBd);
			ask = ask.add(incrementBd);
			bidTicks.add(bidTick);
			askTicks.add(askTick);
		}
	}
	
	/**
	 * 
	 */
	private class TickComparator implements Comparator<Tick>
	{
		@Override
		public int compare(Tick o1, Tick o2)
		{
			return o1.getLevel().compareTo(o2.getLevel());
		}
	}
	
	public static void main(String[] args) {

	}


}
