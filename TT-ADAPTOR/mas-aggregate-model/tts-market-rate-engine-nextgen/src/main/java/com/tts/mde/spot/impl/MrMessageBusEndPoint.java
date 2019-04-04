package com.tts.mde.spot.impl;

import com.google.protobuf.Message;
import com.tts.mde.spot.IMrEndpoint;
import com.tts.mde.support.IPublishingEndpoint;
import com.tts.message.market.FullBookStruct.FullBook;

public class MrMessageBusEndPoint implements IMrEndpoint {
	
	private final IPublishingEndpoint publishingEndpoint;
	private final String topicVWAP;
	private final String topicRAW;
	private final String topicOut;
	private final String topicConsolidated;
	
	
	public MrMessageBusEndPoint(String symbol, String qualifierNm, IPublishingEndpoint publishingEndpoint) {
		this.publishingEndpoint = publishingEndpoint;
		this.topicRAW = String.format("TTS.MD.FX.MR.SPOT.%s.%s.RAW", symbol, qualifierNm);
		this.topicVWAP = String.format("TTS.MD.FX.MR.SPOT.%s", symbol);
		this.topicOut = null;
		this.topicConsolidated = String.format("TTS.MD.FX.MR.SPOT.%s.%s.CONSOLIDATE", symbol, qualifierNm);
	}
	
	public MrMessageBusEndPoint(String topicOut, IPublishingEndpoint publishingEndpoint) {
		this.publishingEndpoint = publishingEndpoint;
		this.topicOut = topicOut;
		this.topicRAW = null;
		this.topicVWAP = null;
		this.topicConsolidated = null;
	}
	
	@Override
	public void publish(OutboundType outT,
			MrSubscriptionProperties properties, Message.Builder message) {
		if ( message instanceof FullBook.Builder) {
			((FullBook.Builder) message).setQuoteRefId(properties.getHandlerId());
		}
		Message m = message.build();
		if ( topicOut != null) {
			publishingEndpoint.publish(topicOut, m);
		} else if ( outT == OutboundType.RAW) {
			publishingEndpoint.publish(topicRAW, m);
		} else if ( outT == OutboundType.VWAP) {
			publishingEndpoint.publish(topicVWAP, m);
		} else if ( outT == OutboundType.CONSOLIDATED) {
			publishingEndpoint.publish(topicConsolidated, m);
		}
	}

}
