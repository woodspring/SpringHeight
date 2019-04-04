package com.tts.plugin.adapter.impl.base.setting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.setting.IFixSetting;

import quickfix.SessionID;

public abstract class AbstractFixSetting implements IFixSetting {

	protected final Map<String, String> userNameMap;
	protected final Map<String, String> passwordMap;
	protected final Map<String, String> onBehalfOfNameMap;

	public AbstractFixSetting(FixApplicationProperties fixApplicationProperties) {
		Map<String, String> _onBehalfOfMap = new HashMap<String, String>(); 
		Map<String, String> _userNameMap = new HashMap<String, String>(); 
		Map<String, String> _passwordMap = new HashMap<String, String>(); 

		for (Object key: fixApplicationProperties.keySet() ) {
			String stringKey = (String) key;
			if ( stringKey.startsWith(IFixSetting.SETTING__PREFIX__SESSION) ) {
				if ( stringKey.contains(IFixSetting.SETTING__USERNAME)) {
					String sessionIDName = stringKey.replace(IFixSetting.SETTING__PREFIX__SESSION, "").replace(IFixSetting.SETTING__USERNAME, "");
					_userNameMap.put(sessionIDName, fixApplicationProperties.getProperty(stringKey));
				}
				if ( stringKey.contains(IFixSetting.SETTING__PASSWORD)) {
					String sessionIDName = stringKey.replace(IFixSetting.SETTING__PREFIX__SESSION, "").replace(IFixSetting.SETTING__PASSWORD, "");
					_passwordMap.put(sessionIDName, fixApplicationProperties.getProperty(stringKey));
				}
				if ( stringKey.contains(IFixSetting.SETTING__ON_BEHALF_OF)) {
					String sessionIDName = stringKey.replace(IFixSetting.SETTING__PREFIX__SESSION, "").replace(IFixSetting.SETTING__ON_BEHALF_OF, "");
					_onBehalfOfMap.put(sessionIDName, fixApplicationProperties.getProperty(stringKey));
				}
			}
		}	
		
		this.userNameMap = Collections.unmodifiableMap(_userNameMap);
		this.passwordMap = Collections.unmodifiableMap(_passwordMap);
		this.onBehalfOfNameMap = Collections.unmodifiableMap(_onBehalfOfMap);
	}

	@Override
	public String getOnBehalfOfName(SessionID arg0) {
		return onBehalfOfNameMap.get(arg0.toString());
	}

	@Override
	public String getPassword(SessionID arg0) {
		return passwordMap.get(arg0.toString());
	}

	@Override
	public String getUserName(SessionID arg0) {
		return userNameMap.get(arg0.toString());
	}

}