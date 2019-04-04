package com.tts.mlp.data.provider.vo;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MarketRawDataVo {

	private RawDataType type;
	private String name;
	private String name2;
	private double bid;
	private double mid;
	private double ask;
	private String entryDate;
		
	public MarketRawDataVo(RawDataType type, String name, double bid, double mid, double ask) {
		super();
		this.type = type;
		this.name = name;
		this.bid = bid;
		this.mid = mid;
		this.ask = ask;
		this.name2 = null;
		this.entryDate = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now());
	}

	public MarketRawDataVo(RawDataType type, String name, String name2, double bid, double mid, double ask) {
		super();
		this.type = type;
		this.name = name;
		this.bid = bid;
		this.mid = mid;
		this.ask = ask;
		this.name2 = name2;
		this.entryDate = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now());
	}

	public  RawDataType getType() {
		return type;
	}



	public  void setType(RawDataType type) {
		this.type = type;
	}



	public  String getName() {
		return name;
	}



	public  void setName(String name) {
		this.name = name;
	}



	public  double getBid() {
		return bid;
	}



	public  void setBid(double bid) {
		this.bid = bid;
	}



	public  double getMid() {
		return mid;
	}



	public  void setMid(double mid) {
		this.mid = mid;
	}



	public  double getAsk() {
		return ask;
	}



	public  void setAsk(double ask) {
		this.ask = ask;
	}



	public String getEntryDate() {
		return entryDate;
	}

	public  String getName2() {
		return name2;
	}

	public  void setName2(String name2) {
		this.name2 = name2;
	}

	public enum RawDataType {
		SPOT_RATE,
		SWAP_POINTS
	}
		
	
}
