package com.tts.mlp.rate.provider.vo;

public class Tick 
{

	
	private Double price;
	
	private Double quantity;
	
	private Integer level;

	/**
	 * constructor for Tick
	 * 
	 * @param price
	 * @param quantity
	 * @param level
	 */
	public Tick(double price, double quantity, int level)
	{
		super();
		setPrice(price);
		setQuantity(quantity);
		setLevel(level);
	}
	
	public Tick(Tick tick) {
		this.price = new Double(tick.getPrice());
		this.quantity = new Double(tick.getQuantity());
		this.level = new Integer(tick.getLevel());
	}
	
	public Tick deepClone() {
		return new Tick(this);
	}
		
	/**
	 * @return the price
	 */
	public Double getPrice()
	{
		return price;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(double price)
	{
		this.price = price;
	}

	/**
	 * @return the quantity
	 */
	public Double getQuantity()
	{
		return quantity;
	}

	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(double quantity)
	{
		this.quantity = quantity;
	}

	/**
	 * @return the level
	 */
	public Integer getLevel()
	{
		return level;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(int level)
	{
		this.level = level;
	}
}
