package com.tts.plugin.adapter.impl.cibc.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.impl.base.setting.AbstractFixSetting;

import quickfix.SessionID;
import quickfix.SessionSettings;

public class CibcFixSettingImpl extends AbstractFixSetting {
	
	@SuppressWarnings("unused")
	private final FixApplicationProperties fixApplicationProperties;
	
	private final SessionSettings fixSetting;

	private final List<SessionType> expectedSessions;
	
	private final String mktPriceSession;

	public CibcFixSettingImpl(quickfix.SessionSettings fixSetting, FixApplicationProperties fixApplicationProperties) {
		super(fixApplicationProperties);
		Set<SessionType> expectedSessions = new HashSet<SessionType>();
		String _mktPriceSession = null;
		Iterator<SessionID> sessionIterator = fixSetting.sectionIterator();
		while (sessionIterator.hasNext()) {
			SessionID session = sessionIterator.next();
			String sessionStr = session.toString();
			if ( sessionStr.indexOf("RFS") > 0 ) {
				expectedSessions.add(SessionType.BANK_RFS);
			} else if (sessionStr.indexOf("ORD") > 0 ){ 
				expectedSessions.add(SessionType.MARKET_ORDER);
			} else if( sessionStr.indexOf("_ESP") > 0 
					|| sessionStr.indexOf("_MKD") > 0
					|| sessionStr.indexOf("_MD") > 0) {
				expectedSessions.add(SessionType.MARKET_PRICE);
				_mktPriceSession = sessionStr;
			}		
		}
		
		this.mktPriceSession = _mktPriceSession;
		this.fixSetting = fixSetting;
		this.fixApplicationProperties = fixApplicationProperties;
		this.expectedSessions = Collections.unmodifiableList(new ArrayList<SessionType>(expectedSessions));
	}

	@Override
	public List<SessionType> getExpectedSessions() {
		LoggerFactory.getLogger(this.getClass()).debug("Expected FIX sessions are " + expectedSessions.toString());
		return expectedSessions;
	}
	

	@Override
	public quickfix.SessionSettings getQuickfixSessionSetting() {
		return fixSetting;
	}

	@Override
	public String getOnBehalfOfName(SessionID arg0) {
		String rte =  super.getOnBehalfOfName(arg0);
		if ( rte == null ) {
			return onBehalfOfNameMap.get("DEFAULT");
		}
		return rte;
	}

	@Override
	public String getMarketPriceSessionId() {
		return(this.mktPriceSession);
	}
}
