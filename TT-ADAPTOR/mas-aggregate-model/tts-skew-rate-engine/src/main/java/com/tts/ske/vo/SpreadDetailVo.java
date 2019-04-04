package com.tts.ske.vo;

import com.tts.message.trade.TradeMessage.SpreadDetail;
import com.tts.vo.ISpreadDetail;

public class SpreadDetailVo implements ISpreadDetail {
	private String fwdSpreadPtPip;
	private String fwdSpreadPt;
	private String spotSpread;
	
	public String getFwdSpreadPtPip() {
		return fwdSpreadPtPip;
	}
	public void setFwdSpreadPtPip(String fwdSpreadPtPip) {
		this.fwdSpreadPtPip = fwdSpreadPtPip;
	}
	public String getFwdSpreadPt() {
		return fwdSpreadPt;
	}
	public void setFwdSpreadPt(String fwdSpreadPt) {
		this.fwdSpreadPt = fwdSpreadPt;
	}
	public String getSpotSpread() {
		return spotSpread;
	}
	public void setSpotSpread(String spotSpread) {
		this.spotSpread = spotSpread;
	}
	
	public static SpreadDetailVo fromMessage(SpreadDetail sd) {
		SpreadDetailVo vo = new SpreadDetailVo();
		if ( sd.hasFwdSpreadPtPip()  ) vo.setFwdSpreadPtPip(sd.getFwdSpreadPtPip());
		if ( sd.hasFwdSpreadPt() ) vo.setFwdSpreadPt(sd.getFwdSpreadPt());
		if ( sd.hasSpotSpread() ) vo.setSpotSpread(sd.getSpotSpread());
		return vo;
	}
	public SpreadDetail toMessage() {
		SpreadDetail.Builder t = SpreadDetail.newBuilder();
		if ( this.getFwdSpreadPtPip() != null ) t.setFwdSpreadPtPip(this.getFwdSpreadPtPip());
		if ( this.getFwdSpreadPt() != null ) t.setFwdSpreadPt(this.getFwdSpreadPt());
		if ( this.getSpotSpread() != null ) t.setSpotSpread(this.getSpotSpread());
		return t.build();
	}
	public SpreadDetailVo deepClone() {
		SpreadDetailVo t = new SpreadDetailVo();
		t.setFwdSpreadPtPip(this.getFwdSpreadPtPip());
		t.setFwdSpreadPt(this.getFwdSpreadPt());
		t.setSpotSpread(this.getSpotSpread());
		return t;
	}

}
