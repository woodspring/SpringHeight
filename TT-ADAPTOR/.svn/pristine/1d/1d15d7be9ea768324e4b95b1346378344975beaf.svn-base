package com.tts.mde.spot.impl;

import java.util.Arrays;
import java.util.List;

import com.tts.mde.spot.IMrEndpoint;

public class MrSubscriptionProperties {

	private final String symbol;
	private final long[] size;
	private final List<IMrEndpoint.OutboundType> interestedOutboundTypes;
	private final String handlerId;
	
	public MrSubscriptionProperties(String symbol, String handlerId, long[] size, List<IMrEndpoint.OutboundType> interestedOutboundTypes) {
		super();
		this.size = size;
		this.symbol = symbol;
		this.interestedOutboundTypes = interestedOutboundTypes;
		this.handlerId = handlerId;
	}

	public long[] getSize() {
		return size;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public String getHandlerId() {
		return handlerId;
	}

	public List<IMrEndpoint.OutboundType> getInterestedOutboundTypes() {
		return interestedOutboundTypes;
	}

	public MrSubscriptionProperties deepClone() {
		long[] size = Arrays.copyOf(this.size, this.size.length);
		return new MrSubscriptionProperties(this.symbol, this.handlerId, size, this.interestedOutboundTypes);
	}
	
	public Builder toBuilder() {
		Builder b = new Builder();
		b.setInterestedOutboundTypes(interestedOutboundTypes);
		b.setSize(Arrays.copyOf(this.size, this.size.length));
		b.setSymbol(symbol);
		b.setHandlerId(handlerId);
		return b;
	}
	
	public static class Builder  {
		private String symbol;
		private long[] size;
		private List<IMrEndpoint.OutboundType> interestedOutboundTypes;
		private String handlerId;
		
		public String getSymbol() {
			return symbol;
		}
		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}
		public long[] getSize() {
			return size;
		}
		public void setSize(long[] size) {
			this.size = size;
		}
		
		public String getHandlerId() {
			return handlerId;
		}
		public void setHandlerId(String handlerId) {
			this.handlerId = handlerId;
		}
		public List<IMrEndpoint.OutboundType> getInterestedOutboundTypes() {
			return interestedOutboundTypes;
		}
		public void setInterestedOutboundTypes(List<IMrEndpoint.OutboundType> interestedOutboundTypes) {
			this.interestedOutboundTypes = interestedOutboundTypes;
		}
		
		public MrSubscriptionProperties build() {
			return new MrSubscriptionProperties(symbol, handlerId, size, interestedOutboundTypes);
		}

	}
}
