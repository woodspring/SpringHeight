package com.tts.ske.vo;

public class TransactionLatencyVo {
	
	private long createTime;

	private volatile long sentTime = -1;
	
	private volatile long ackReceivedTime = -1;
	
	private volatile long confReceivedTime = -1;
	
	private volatile long notifyTime = -1;
	
	public TransactionLatencyVo() {
		this.createTime = System.currentTimeMillis();
	}
	
	public TransactionLatencyVo( long createTime) {
		this.createTime = createTime;
	}

	public long getSentTime() {
		return sentTime;
	}

	public void setSentTime(long sentTime) {
		this.sentTime = sentTime;
	}

	public long getAckReceivedTime() {
		return ackReceivedTime;
	}

	public void setAckReceivedTime(long ackReceivedTime) {
		this.ackReceivedTime = ackReceivedTime;
	}

	public long getConfReceivedTime() {
		return confReceivedTime;
	}

	public void setConfReceivedTime(long confReceivedTime) {
		this.confReceivedTime = confReceivedTime;
	}

	public long getNotifyTime() {
		return notifyTime;
	}

	public void setNotifyTime(long notifyTime) {
		this.notifyTime = notifyTime;
	}

	public Long getCreateTime() {
		return createTime;
	}
	
	public void copyFrom(TransactionLatencyVo tl) {
		this.createTime = tl.createTime;
		this.setAckReceivedTime(tl.getAckReceivedTime());
		this.setConfReceivedTime(tl.getConfReceivedTime());
		this.setSentTime(tl.getSentTime());
		this.setNotifyTime(tl.getNotifyTime());
	}
	
	
}
