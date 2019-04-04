package com.tts.mde.spot;

import com.google.protobuf.Message.Builder;
import com.tts.mde.spot.impl.MrSubscriptionProperties;

public interface IMrEndpoint {
	
	enum OutboundType {
		RAW("RAW"),
		VWAP("VWAP"),
		CONSOLIDATED("CONSOLIDATED");
		
		private final String s;
		
		private OutboundType(final String s) {
			this.s = s;
		}
		
		public static OutboundType fromString(String s) {
			for ( OutboundType o : values() ) {
				if ( o.s.startsWith(s)) {
					return o;
				}
			}
			return null;
		}
		
		public String toString() {
			return s;
		}
	}

	void publish(OutboundType outT, MrSubscriptionProperties properties, Builder message);
}
