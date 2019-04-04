package com.tts.mde.spot.impl;

import java.util.Arrays;

import com.tts.mde.provider.IMDProviderStateListener;
import com.tts.mde.provider.MDProviderStateManager;
import com.tts.mde.spot.vo.MdSubscriptionType;
import com.tts.mde.spot.vo.MdSubscriptionVo;
import com.tts.mde.vo.RawLiquidityVo;

public class SelfManagedLiquidityPool extends SingleSrcLiquidityPool implements IMDProviderStateListener {

	private static final RawLiquidityVo[] NO_QUOTES = new RawLiquidityVo[0];
	private final LPSingleCcyPairSubscriptionManager manager;
	private final MDProviderStateManager mdProviderStateManager;

	public SelfManagedLiquidityPool(MdSubscriptionVo sub, LPSingleCcyPairSubscriptionManager manager,
			MDProviderStateManager mdProviderStateManager) {
		super(sub);
		this.mdProviderStateManager = mdProviderStateManager;
		this.manager = manager;
		mdProviderStateManager.registerListener(sub.getAdapter(), sub.getSource(),
				sub.getMdSubscriptionType() == MdSubscriptionType.ESP
						? IMDProviderStateListener.CONSTANT_SESSIONTYPE_ESP
						: IMDProviderStateListener.CONSTANT_SESSIONTYPE_RFS,
				this);
		if (mdProviderStateManager.isStatusEnabled(sub.getAdapter(), sub.getSource(),
				sub.getMdSubscriptionType() == MdSubscriptionType.ESP
						? IMDProviderStateListener.CONSTANT_SESSIONTYPE_ESP
						: IMDProviderStateListener.CONSTANT_SESSIONTYPE_RFS)) {
			manager.doSendSubscriptionAndRegister(sub.getAdapter(),
					Arrays.asList(new MdSubscriptionVo[] { getSubscription() }), this);
		}
	}

	@Override
	public void doWhenOnline(String adapterNm, String sourceNm) {
		manager.doSendSubscriptionAndRegister(adapterNm, Arrays.asList(new MdSubscriptionVo[] { getSubscription() }),
				this);
	}

	@Override
	public void doWhenOffline(String adapterNm, String sourceNm) {
		super.replaceAllQuotes(NO_QUOTES, NO_QUOTES);

	}

	public void destory() {
		mdProviderStateManager.unregisterListener(getSubscription().getAdapter(), getSubscription().getSource(),
				getSubscription().getMdSubscriptionType() == MdSubscriptionType.ESP
						? IMDProviderStateListener.CONSTANT_SESSIONTYPE_ESP
						: IMDProviderStateListener.CONSTANT_SESSIONTYPE_RFS,
				this);
	}

}
