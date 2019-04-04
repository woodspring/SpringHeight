package com.tts.mde.plugin.ykb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.tts.mde.algo.IMDAggAlgoProvider;
import com.tts.mde.plugin.IMDEmbeddedAdapterFactory;
import com.tts.mde.plugin.IMdePlugin;
import com.tts.mde.support.config.Adapter;

public class TtsYkbFwdcPlugin implements IMdePlugin {
	
	public final static String ADAPTER_NM = "HARMONI_SWAP_PTS_ADAPTER";
	private static final HashSet<String> SET_OF_ADAPTERS = new HashSet<String>(Arrays.asList(new String[] { ADAPTER_NM }));

	public final YkbMDEmbeddedAdapterFactory factory = new YkbMDEmbeddedAdapterFactory();

	@Override
	public Set<String> getMDEmbeddedAdapterNames() {
		return SET_OF_ADAPTERS;
	}

	@Override
	public IMDEmbeddedAdapterFactory getMDEmbeddedAdapterFactory(Adapter adapterNm) {
		return factory;
	}

	@Override
	public Set<String> getMDAggAlgoNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMDAggAlgoProvider getMDAggAlgoProvider() {
		// TODO Auto-generated method stub
		return null;
	}

}
