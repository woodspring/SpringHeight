package com.tts.plugin.adapter.impl.base.setting;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.tts.fix.support.FixApplicationProperties;
import com.tts.plugin.adapter.api.setting.IFixSetting;
import com.tts.plugin.adapter.api.setting.IFixSettingBuilder;

import quickfix.ConfigError;

public class DefaultFixSettingBuilderImpl implements IFixSettingBuilder {

	private volatile FixApplicationProperties fixApplicationProperties;
	private volatile quickfix.SessionSettings fixSessionSettings;
	
	public DefaultFixSettingBuilderImpl() {
		super();
	}

	@Override
	public IFixSetting build() {
		return new DefaultFixSetting(getFixSessionSettings(), fixApplicationProperties);
	}

	@Override
	public void setFixApplicationProperties(FixApplicationProperties arg0) {
		this.fixApplicationProperties = arg0;
		
	}
	
	protected quickfix.SessionSettings getFixSessionSettings() {
		if (fixSessionSettings == null   ) {
			quickfix.SessionSettings _fixSessionSettings = getAllFixSessionSettings().get(0);
			this.fixSessionSettings = _fixSessionSettings;
		}
		return this.fixSessionSettings;
	}

	protected FixApplicationProperties getFixApplicationProperties() {
		return fixApplicationProperties;
	}
	
	protected List<quickfix.SessionSettings> getAllFixSessionSettings() {
		Set<Entry<Object, Object>> kvpairs  = fixApplicationProperties.entrySet();
		List<quickfix.SessionSettings> qfixSettingList = new LinkedList<quickfix.SessionSettings>();
		TreeMap<String, String> fixSettingMap = new TreeMap<String, String>();
		for (Entry<Object, Object> kvpair: kvpairs) {
			String k = (String) kvpair.getKey();
			if ( k.startsWith(IFixSetting.SETTING__FIX_SESSIONS_SETTING_PATH__DEFAULT)) {
				String v = (String) kvpair.getValue();
				fixSettingMap.put(k, v);
			}
		}
		
		for ( String key: fixSettingMap.keySet()) {
			quickfix.SessionSettings _fixSessionSettings = loadQfixSettingFile(fixSettingMap.get(key));;
			
			if ( _fixSessionSettings != null ) {
				qfixSettingList.add(_fixSessionSettings);
			}
		}

		return qfixSettingList;
	}

	private quickfix.SessionSettings loadQfixSettingFile(String classpathLocation) {
		quickfix.SessionSettings _fixSessionSettings = null;
		InputStream is = null;
		try {
			is = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(classpathLocation);
			_fixSessionSettings = new quickfix.SessionSettings(is);
		} catch (ConfigError e) {
			e.printStackTrace();
		} finally {
			if ( is != null ) {
				try {
					is.close();
				} catch (IOException e) {
					
				}
			}
		}
		return _fixSessionSettings;
	}

}