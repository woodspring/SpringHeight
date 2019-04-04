package com.tts.fixapi.core;

import quickfix.FixVersions;
import quickfix.SessionID;


public class FIXAcceptorSession {
	private final SessionID sessionId;
	private final FixVersion fixSessionVersion;
	private final Long accountId;
	private final String customerName;
	
	public FIXAcceptorSession(SessionID sessionId, FixVersion fixVersion, Long accountId, String customerName)	{
		this.sessionId  = sessionId;
		this.fixSessionVersion = fixVersion;
		this.accountId = accountId;
		this.customerName = customerName;
	}
	
	public String getBeginString()	{
		return(sessionId.getBeginString());
	}
	
	public String getFixSessionVersion()	{
		return(fixSessionVersion.getFixVersion());
	}
	
	public Long getAccountId()	{
		return(accountId);
	}
	
	public String getCustomerName()	{
		return(customerName);
	}
	
	public Class<?> getExecutionReportMessageVersion() throws ClassNotFoundException	{
		String className   = fixSessionVersion.getQuickFixBasePkg() + "ExecutionReport";
		Class<?> classType = Class.forName(className);
		
		return(classType);
	}
	
	public Class<?> getOrderCancelRejectMessageVersion() throws ClassNotFoundException	{
		String className   = fixSessionVersion.getQuickFixBasePkg() + "OrderCancelReject";
		Class<?> classType = Class.forName(className);
		
		return(classType);
	}
	
	public static FixVersion fromString(String beginStr)	{
		FixVersion defaultVersion = FixVersion.FIX50;
		
		for(FixVersion version: FixVersion.values())	{
			if(version.getBeginString().equals(beginStr))	{
				defaultVersion = version;
				break;
			}
		}			
		return(defaultVersion);
	}
	
	
	public enum FixVersion	{
		FIX44(FixVersions.BEGINSTRING_FIX44, "FIX 4.4",  "quickfix.fix44."),
		FIX50(FixVersions.BEGINSTRING_FIXT11, "FIX 5.0", "quickfix.fix50.");
		
		private String beginString;
		private String fixVersion;
		private String quickFixBasePkg;
		
		private FixVersion(String beginStr, String version,  String basePkg)	{
			this.beginString     = beginStr;
			this.fixVersion      = version;
			this.quickFixBasePkg = basePkg;
		}

		public String getBeginString() {
			return(beginString);
		}

		public String getQuickFixBasePkg() {
			return(quickFixBasePkg);
		}
		
		public String getFixVersion()	{
			return(fixVersion);
		}
		
		@Override
		public String toString()	{
			return(fixVersion);
		}
	}
}
