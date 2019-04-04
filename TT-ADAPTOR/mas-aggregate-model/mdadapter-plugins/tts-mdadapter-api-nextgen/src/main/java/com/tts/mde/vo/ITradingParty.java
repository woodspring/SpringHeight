package com.tts.mde.vo;

import com.tts.vo.CounterPartyVo;

public interface ITradingParty {
	CounterPartyVo getFxSPOTCounterParty();
	CounterPartyVo getFxOutrightCounterParty();
	CounterPartyVo getFxSWAPCounterParty();
}