package com.tts.mde.spot.vo;

import java.util.Arrays;
import java.util.List;

public class AdapterSubscriptionVo {

	private final List<MdSubscriptionVo> mdSubscriptions;
	
	private AdapterSubscriptionVo(MdSubscriptionVo mdSubscription) {
		super();
		this.mdSubscriptions = Arrays.asList(new MdSubscriptionVo[] { mdSubscription });
	}

	public List<MdSubscriptionVo> getMdSubscriptions() {
		return mdSubscriptions;
	}
		
}
