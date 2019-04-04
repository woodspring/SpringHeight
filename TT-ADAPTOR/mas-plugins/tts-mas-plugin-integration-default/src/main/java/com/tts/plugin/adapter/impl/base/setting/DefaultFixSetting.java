package com.tts.plugin.adapter.impl.base.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.tts.fix.support.FixApplicationProperties;

import quickfix.SessionID;
import quickfix.SessionSettings;

public class DefaultFixSetting extends AbstractFixSetting {
	
	private final quickfix.SessionSettings fixSetting;
	private final List<SessionType> expectedSessions;
	private String mktPriceSession = null;

	public DefaultFixSetting(quickfix.SessionSettings fixSetting, FixApplicationProperties fixApplicationProperties) {
		super(fixApplicationProperties);
		Set<SessionType> expectedSessions = new HashSet<SessionType>();
		
		Iterator<SessionID> sessionIterator = fixSetting.sectionIterator();
		while (sessionIterator.hasNext()) {
			SessionID session = sessionIterator.next();
			String sessionStr = session.toString();
			if ( sessionStr.indexOf("RFS") > 0 ) {
				expectedSessions.add(SessionType.BANK_RFS);
			} else if (sessionStr.indexOf("ORD") > 0 ){ 
				expectedSessions.add(SessionType.MARKET_ORDER);
			} else if (sessionStr.indexOf("MKD") > 0 || sessionStr.indexOf("_ESP") > 0   ){ 
				expectedSessions.add(SessionType.MARKET_PRICE);
				this.mktPriceSession = sessionStr;
			}		
		}
		


		this.fixSetting = fixSetting;
		this.expectedSessions = Collections.unmodifiableList(new ArrayList<SessionType>(expectedSessions));

	}

	@Override
	public List<SessionType> getExpectedSessions() {
		return expectedSessions;
	}

	@Override
	public SessionSettings getQuickfixSessionSetting() {
		return fixSetting;
	}

	@Override
	public String getMarketPriceSessionId() {
		return(mktPriceSession);
	}
}
