package com.tts.mde.spot.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.util.AppUtils;

public class GlobalReqIdProvider {
	private final AtomicLong reqId;
	
	public GlobalReqIdProvider() {
		long prefix = AppUtils.getSequencePrefix();                  
		reqId = new AtomicLong(prefix + AppUtils.getNumOfRestart() * 1000000000L);
		
	}

	public String getReqId(List<MdSubscriptionVo> subs) {
		int numberOfInnerSubs = subs.size();
		long id = reqId.incrementAndGet();
		return Long.toString( id + numberOfInnerSubs * 100000000000L);
	}
}
