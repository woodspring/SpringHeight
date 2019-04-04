package com.tts.mde.support.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.tts.mde.support.IMasGlobolSequenceProvider;
import com.tts.util.AppUtils;
import com.tts.util.constant.SysProperty.DefaultValue;

public class MasGlobalSequenceProvider implements IMasGlobolSequenceProvider {
	
	private final AtomicLong sequence; 

	public MasGlobalSequenceProvider() {
		long seqPrefix = AppUtils.getSequencePrefix();
		sequence = new AtomicLong(seqPrefix + DefaultValue.STARTING_SEQUENCE);
	}
	
	@Override
	public long getNewSequence() {
		return sequence.getAndIncrement();
	}
	
	@Override
	public long getCurrentSequence() {
		return sequence.get();
	}
}
