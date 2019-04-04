package com.tts.mas.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.tts.fix.support.IFixListener;
import com.tts.util.AppUtils;

public class RequestIdRegistry {

	private final AtomicLong seq = new AtomicLong(AppUtils.getSequencePrefix());
	private final ConcurrentHashMap<String, IFixListener> map = new ConcurrentHashMap<String, IFixListener>();

	public String register(IFixListener listener, Function<Long, String> f) {
		long current = seq.getAndIncrement();
		String requestId = f.apply(current);

		map.put(requestId, listener);
		return requestId;
	}
	
	public void unregister(String requestId) {
		map.remove(requestId);
	}
	public IFixListener getListener(String requestId) {
		return map.get(requestId);
	}
}
 