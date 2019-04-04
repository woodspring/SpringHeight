package com.tts.mde.vo;

import java.util.Properties;

public class MarketDataProviderVo implements IMDProvider {

	protected final boolean isRFSenabled;
	protected final boolean isESPenabled;
	protected boolean isEmbedded;

	protected final int internalAssignedProviderId;
	protected final String adapterNm;
	protected final String sourceNm;
	protected final Properties providerProperties = new Properties();
	
	private long defaultMdValidIntervalInMilli = -1L;
	
	public MarketDataProviderVo(int internalAssignedProviderId, String adapterNm, String sourceNm, boolean isRFSenabled, boolean isESPenabled) {
		super();
		this.internalAssignedProviderId = internalAssignedProviderId;
		this.adapterNm = adapterNm;
		this.sourceNm = sourceNm;
		this.isRFSenabled = isRFSenabled;
		this.isESPenabled = isESPenabled;
	}

	@Override
	public String getAdapterNm() {
		return adapterNm;
	}

	@Override
	public String getSourceNm() {
		return sourceNm;
	}

	@Override
	public boolean isRFSenabled() {
		return isRFSenabled;
	}

	@Override
	public boolean isESPenabled() {
		return isESPenabled;
	}

	public Properties getProviderProperties() {
		return providerProperties;
	}

	public boolean isEmbedded() {
		return isEmbedded;
	}

	public void setEmbedded(boolean isEmbedded) {
		this.isEmbedded = isEmbedded;
	}

	public int getInternalAssignedProviderId() {
		return internalAssignedProviderId;
	}
	
	public long getDefaultMdValidIntervalInMilli() {
		return defaultMdValidIntervalInMilli;
	}

	public void setDefaultMdValidIntervalInMilli(long defaultMdValidIntervalInMilli) {
		this.defaultMdValidIntervalInMilli = defaultMdValidIntervalInMilli;
	}

	public String toString() {
		return adapterNm + '/' + sourceNm;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((adapterNm == null) ? 0 : adapterNm.hashCode());
		result = prime * result + ((sourceNm == null) ? 0 : sourceNm.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MarketDataProviderVo other = (MarketDataProviderVo) obj;
		if (adapterNm == null) {
			if (other.adapterNm != null)
				return false;
		} else if (!adapterNm.equals(other.adapterNm))
			return false;
		if (sourceNm == null) {
			if (other.sourceNm != null)
				return false;
		} else if (!sourceNm.equals(other.sourceNm))
			return false;
		return true;
	}
	
	
}