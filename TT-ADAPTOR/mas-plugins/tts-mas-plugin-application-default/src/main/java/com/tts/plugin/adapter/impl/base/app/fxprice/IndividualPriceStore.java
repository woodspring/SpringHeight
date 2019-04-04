package com.tts.plugin.adapter.impl.base.app.fxprice;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.protobuf.Message;

public class IndividualPriceStore<TARGET_TYPE extends Message.Builder> {
	public static final int INDIVIDUAL_STORE_QUEUE_SIZE = 16;
	
	@SuppressWarnings("unused")
	private final boolean debug;
	
	private final Message.Builder[] store;
	
	private volatile long seq = 0;
	private volatile long completed = 0;
	
	public IndividualPriceStore() {
		this(false, INDIVIDUAL_STORE_QUEUE_SIZE);
	}
	
	public IndividualPriceStore(boolean debug) {
		this(debug, INDIVIDUAL_STORE_QUEUE_SIZE);
	}
	
	public IndividualPriceStore(boolean debug, int size) {
		this.debug = debug;
		this.store = new Message.Builder[size];
	}
	
	public void init() throws Exception {
		
	}
	
	public void setStoreInitializerFunction(Function<Message.Builder[], Message.Builder[]> f) {
		f.apply(store);
	}
	
	@SuppressWarnings("unchecked")
	public <U> TARGET_TYPE updateLatest(BiFunction<Message.Builder, U, Message.Builder> f, U obj) {
		TARGET_TYPE t = getLatest();
		t = (TARGET_TYPE) f.apply(t, obj);
		return t;		
	}
	
	@SuppressWarnings("unchecked")
	public <U> TARGET_TYPE updateNextSlot(BiFunction<Message.Builder, U, Message.Builder> f, U obj) {
		long currentSeq = seq++;
		int idx = (int ) (currentSeq % INDIVIDUAL_STORE_QUEUE_SIZE);

		TARGET_TYPE t = (TARGET_TYPE) store[ idx];
		t = (TARGET_TYPE) f.apply(t, obj);
		completed = currentSeq;

		return t;		
	}

	@SuppressWarnings("unchecked")
	public TARGET_TYPE getLatest() {
		int idx = (int ) (completed % INDIVIDUAL_STORE_QUEUE_SIZE);

		TARGET_TYPE t = (TARGET_TYPE) store[ idx];
		return t;		
	}
	
}
