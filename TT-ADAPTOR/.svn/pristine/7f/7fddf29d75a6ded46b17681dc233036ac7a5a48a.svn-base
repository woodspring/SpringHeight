package com.tts.mlp.rate.provider.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tts.mlp.app.GlobalAppConfig;
import com.tts.vo.NumberVo;

import quickfix.field.MDEntryType;

public class Instrument
{
	
	private static final int RWALK_MAX = 4;
	
	private static final int RWALK_MIN = -4;
		
	
	private String symbol;
	
	private String increment = "0.0001";
	
	private int precision;
	
	private int pointValue;
	
	private List<Tick> askTicks = new ArrayList<Tick>();
	
	private List<Tick> bidTicks = new ArrayList<Tick>();

	/**
	 * constructor for Instrument
	 * 
	 * @param symbol
	 */
	public Instrument(String symbol)
	{
		super();
		setSymbol(symbol);
	}
	
	public Instrument(Instrument instrument)
	{
		super();
		this.symbol = instrument.symbol;
		this.increment = instrument.increment;
		this.precision = instrument.precision;
		this.pointValue = instrument.pointValue;
		
		for ( int i = 0; i < instrument.askTicks.size(); i++ ) {
			askTicks.add(instrument.askTicks.get(i).deepClone());
		}
		
		for ( int i = 0; i < instrument.bidTicks.size(); i++ ) {
			bidTicks.add(instrument.bidTicks.get(i).deepClone());
		}
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
	 * @param symbol the symbol to set
	 */
	public void setSymbol(String symbol)
	{
		this.symbol = symbol;
	}

	/**
	 * @return the precision
	 */
	public int getPrecision() {
		return precision;
	}

	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(int precision) {
		this.precision = precision;
	}
	
	
	public int getPointValue() {
		return pointValue;
	}

	public void setPointValue(int pointValue) {
		this.pointValue = pointValue;
	}



	/**
	 * @return the increment
	 */
	public String getIncrement() {
		return increment;
	}

	/**
	 * @param increment the increment to set
	 */
	public void setIncrement(String increment) {
		this.increment = increment;
	}

	/**
	 * @return the askTicks
	 */
	public List<Tick> getAskTicks()
	{
		return askTicks;
	}

	/**
	 * @param askTicks the askTicks to set
	 */
	public void setAskTicks(List<Tick> askTicks)
	{
		this.askTicks = askTicks;
	}

	/**
	 * @param tick
	 * @return
	 */
	public boolean addAskTick(Tick tick)
	{
		if (tick != null)
		{
			if (!askTicks.contains(tick))
			{
				return askTicks.add(tick);
			}
			else
			{
				askTicks.set(askTicks.indexOf(tick), tick);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param tick
	 * @return
	 */
	public boolean removeAskTick(Tick tick)
	{
		if (tick != null)
		{
			return askTicks.remove(tick);
		}
		return false;
	}
	
	/**
	 * @return the bidTicks
	 */
	public List<Tick> getBidTicks()
	{
		return bidTicks;
	}

	/**
	 * @param bidTicks the bidTicks to set
	 */
	public void setBidTicks(List<Tick> bidTicks)
	{
		this.bidTicks = bidTicks;
	}
	
	/**
	 * @param tick
	 * @return
	 */
	public boolean addBidTick(Tick tick)
	{
		if (tick != null)
		{
			if (!bidTicks.contains(tick))
			{
				return bidTicks.add(tick);
			}
			else
			{
				bidTicks.set(bidTicks.indexOf(tick), tick);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param tick
	 * @return
	 */
	public boolean removeBidTick(Tick tick)
	{
		if (tick != null)
		{
			return bidTicks.remove(tick);
		}
		return false;
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
	 * @param entryType
	 * @param ticks
	 */
	public void setTicksByEntryType(char entryType, List<Tick> ticks)
	{
		switch (entryType)
		{
			case MDEntryType.BID:				
				setBidTicks(ticks);
				break;

			case MDEntryType.OFFER:
				setAskTicks(ticks);
				break;
				
			default:
				break;
		}
	}
	
	/**
	 * 
	 * @param entryType
	 * @param tick
	 * @return
	 */
	public boolean addTickByEntryType(char entryType, Tick tick)
	{
		switch (entryType)
		{
			case MDEntryType.BID:				
				return addBidTick(tick);

			case MDEntryType.OFFER:
				return addAskTick(tick);
				
			default:
				return false;
		}
	}
	
	/**
	 * 
	 * @param entryType
	 * @param tick
	 * @return
	 */
	public boolean removeTickByEntryType(char entryType, Tick tick)
	{
		switch (entryType)
		{
			case MDEntryType.BID:				
				return removeBidTick(tick);

			case MDEntryType.OFFER:
				return removeAskTick(tick);
				
			default:
				return false;
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
	
	/**
	 * 
	 */
	public void randomWalk() {
		long randomwalk =  RWALK_MIN + (long)(Math.random() * ((RWALK_MAX - RWALK_MIN) + 1));
		NumberVo value = (new NumberVo(increment, increment.length() - increment.indexOf("."))).multiply(randomwalk);

		
		//long deltaSpread =  RWALK_MIN + (long)(Math.random() * ((RWALK_MAX - RWALK_MIN) + 1));
		NumberVo spreadValue = (new NumberVo(increment, increment.length() - increment.indexOf("."))).multiply(randomwalk).multiply(GlobalAppConfig.getSpreadConfig().getSpread()).absolute();

		for (int i = 0; i < askTicks.size(); i++) {	
			NumberVo bidValue = (new NumberVo(Double.toString(bidTicks.get(i).getPrice()), getPrecision()).plus(value).minus(spreadValue));
			if (!bidValue.isNegative()) {
				bidTicks.get(i).setPrice(Double.valueOf(bidValue.toPercisionString()));
			}
			
			NumberVo askValue = (new NumberVo(Double.toString(askTicks.get(i).getPrice()), getPrecision()).plus(value).plus(spreadValue));
			if (!askValue.isNegative()) {
				askTicks.get(i).setPrice(Double.valueOf(askValue.toPercisionString()));
			}
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


}
