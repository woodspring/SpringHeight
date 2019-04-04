package com.tts.plugin.adapter.impl.ykb.setting;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.util.AppContext;

import quickfix.SessionID;
import quickfix.SessionSettings;

public class YkbFixSettingImpl implements IFixSetting {
	
	private final List<quickfix.SessionSettings> fixSessionSettings;
	private final SessionSettings fixSetting;
	private final Map<String, String> userNameMap;
	private final Map<String, String> passwordMap;
	private final String defaultUserName;
	private final String defaultPassword;
	private final List<SessionType> expectedSessions;
	private String mktPriceSession = null;
	
	public YkbFixSettingImpl(List<quickfix.SessionSettings> fixSessionSettings, SessionSettings fixSetting) {
		FixApplicationProperties p = AppContext.getContext().getBean("applicationProperties", FixApplicationProperties.class);
		Map<String, String> _userNameMap = new HashMap<String, String>(); 
		Map<String, String> _passwordMap = new HashMap<String, String>(); 

		for (Object key: p.keySet() ) {
			String stringKey = (String) key;
			if ( stringKey.startsWith(IFixSetting.SETTING__PREFIX__SESSION) ) {
				if ( stringKey.contains(IFixSetting.SETTING__USERNAME)) {
					String sessionIDName = stringKey.replace(IFixSetting.SETTING__PREFIX__SESSION, "").replace(IFixSetting.SETTING__USERNAME, "");
					_userNameMap.put(sessionIDName, p.getProperty(stringKey));
				}
				if ( stringKey.contains(IFixSetting.SETTING__PASSWORD)) {
					String sessionIDName = stringKey.replace(IFixSetting.SETTING__PREFIX__SESSION, "").replace(IFixSetting.SETTING__PASSWORD, "");
					_passwordMap.put(sessionIDName, p.getProperty(stringKey));
				}
			}
		}	
		this.defaultUserName = p.getProperty("fix.username");
		this.defaultPassword = p.getProperty("fix.password");
		this.fixSetting = fixSetting;
		this.fixSessionSettings = fixSessionSettings;
		this.userNameMap = Collections.unmodifiableMap(_userNameMap);
		this.passwordMap = Collections.unmodifiableMap(_passwordMap);
		this.expectedSessions = Collections.unmodifiableList(Arrays.asList(new SessionType[] {SessionType.MARKET_PRICE}));
	}

	@Override
	public String getUserName(SessionID sessionID) {
		LoggerFactory.getLogger(YkbFixSettingImpl.class).debug(sessionID.toString() + " " + userNameMap.toString());
		if ( userNameMap.get(sessionID) != null) {
			return userNameMap.get(sessionID);
		}
		return defaultUserName;
	}

	@Override
	public String getPassword(SessionID sessionID) {
		if ( passwordMap.get(sessionID) != null) {
			return passwordMap.get(sessionID);
		}
		return defaultPassword;
	}

	@Override
	public String getOnBehalfOfName(SessionID sessionID) {
		return null;
	}

	@Override
	public List<SessionType> getExpectedSessions() {
		LoggerFactory.getLogger(this.getClass()).debug("Expected FIX sessions are " + expectedSessions.toString());
		return expectedSessions;
	}

	@Override
	public quickfix.SessionSettings getQuickfixSessionSetting() {
		return null;
	}

	@Override
	public List<SessionSettings> getQuickfixSessionSettings() {
		return fixSessionSettings;
	}

	@Override
	public String getMarketPriceSessionId() {
		Iterator<SessionID> sessionIterator = fixSetting.sectionIterator();
		while(sessionIterator.hasNext()) {
			SessionID session = sessionIterator.next();
			String sessionStr = session.toString();
			if(sessionStr.indexOf("STREAMING") > 0)
				mktPriceSession = sessionStr;	
		}
		
		return(mktPriceSession);
	}
}
