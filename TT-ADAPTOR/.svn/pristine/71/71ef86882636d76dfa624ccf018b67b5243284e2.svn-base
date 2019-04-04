package com.tts.mlp.data.provider.vo;

public class TickEntry 
{

	private String price;
	
	private long quantity;
	
	/**
	 * constructor for Tick
	 * 
	 * @param price
	 * @param quantity
	 * @param level
	 */
	public TickEntry(String price, long quantity)
	{
		super();
		setPrice(price);
		setQuantity(quantity);
	}
	
	public TickEntry(TickEntry tick) {
		this.price = tick.price;
		this.quantity = tick.quantity;
	}
	
	public TickEntry deepClone() {
		return new TickEntry(this);
	}
		
	/**
	 * @return the price
	 */
	public String getPrice()
	{
		return price;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(String price)
	{
		this.price = price;
	}

	/**
	 * @return the quantity
	 */
	public long getQuantity()
	{
		return quantity;
	}

	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(long quantity)
	{
		this.quantity = quantity;
	}

}
