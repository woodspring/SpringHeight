package com.tts.plugin.adapter.impl.base.vo;

public class LatencyVo {

	private final long startTimeMillis;
	private long endTimeMillis;
	
	public LatencyVo() {
		startTimeMillis = System.currentTimeMillis();
		endTimeMillis = 0l;
	}
		
	public void setEndTimeMillis() { endTimeMillis = System.currentTimeMillis(); }
	
	@Override
	public String toString() {
		long diff = endTimeMillis - startTimeMillis;
		return String.format("%d,%d,%d", startTimeMillis, diff, endTimeMillis);
	}
}
