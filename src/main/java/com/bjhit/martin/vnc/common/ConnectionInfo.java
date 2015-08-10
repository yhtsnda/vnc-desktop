package com.bjhit.martin.vnc.common;

import com.bjhit.martin.vnc.util.StringUtil;


/**
 * @description
 * @project vmconsole
 * @author guanxianchun
 * @Create 2014-12-29 上午10:59:09
 * @version 1.0
 */
public class ConnectionInfo {
	
	private String vmId;
	
	private String host;
	
	private int port;
	
	private String proxyHost;
	
	private int proxyPort;
	
	private String userName = "";
	
	private String password;
	
	private String encodePassword;
	
	private String proxyUser = "bjhit";
	
	private String proxyPassword = "bjhit2014$*";
	
	private int connectCount = 1;
	
	private ConnectType connType;
	
	public ConnectionInfo() {
		
	}
	
	public ConnectionInfo(String vmId,String host,int port,String password,ConnectType connectType) {
		this.vmId = vmId;
		this.host = host;
		this.port = port;
		this.password = password;
		this.connType = connectType;
	}
	
	public ConnectionInfo(String vmId,String host,int port,String password,String proxyHost,int proxyPort,ConnectType connectType) {
		this(vmId,host, port,password,connectType);
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}
	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}
	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	
	/**
	 * @return the proxyHost
	 */
	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 * @param proxyHost the proxyHost to set
	 */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/**
	 * @return the proxyPort
	 */
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * @param proxyPort the proxyPort to set
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the encodePassword
	 */
	public String getEncodePassword() {
		return encodePassword;
	}

	/**
	 * @param encodePassword the encodePassword to set
	 */
	public void setEncodePassword(String encodePassword) {
		this.encodePassword = encodePassword;
	}

	@Override
	public int hashCode() {
		int result = 0;
		if (StringUtil.isEmpty(vmId)) {
			for(int i=0;i<host.length();i++){
				result = 31*result+host.charAt(i);
			}
			result = 31*result+port;
		}else {
			for(int i=0;i<vmId.length();i++){
				result = 31*result+vmId.charAt(i);
			}
		}
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConnectionInfo) {
			ConnectionInfo info = (ConnectionInfo) obj;
			if (StringUtil.equal(host, info.getHost()) && port == info.getPort() && StringUtil.equal(vmId, info.getVmId())) {
				return true;
			}else {
				return false;
			}
		}else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		if (StringUtil.isEmpty(vmId)) {
			return "{\"host\":\""+host+",\"port\":\""+port+"\",\"proxyHost\":\""+proxyHost+"\",\"proxyPort\":\""+proxyPort+"\"}";
		}else {
			return "{\"vmId\":\""+vmId+"\"host\":\""+host+",\"port\":\""+port+"\",\"proxyHost\":\""+proxyHost+"\",\"proxyPort\":\""+proxyPort+"\"}";
		}
		
	}

	/**
	 * @return the vmId
	 */
	public String getVmId() {
		return vmId;
	}

	/**
	 * @param vmId the vmId to set
	 */
	public void setVmId(String vmId) {
		this.vmId = vmId;
	}

	/**
	 * @return the proxyUser
	 */
	public String getProxyUser() {
		return proxyUser;
	}

	/**
	 * @param proxyUser the proxyUser to set
	 */
	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	/**
	 * @return the proxyPassword
	 */
	public String getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * @param proxyPassword the proxyPassword to set
	 */
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public int getConnectCount() {
		return connectCount;
	}

	public void setConnectCount(int connectCount) {
		this.connectCount = connectCount;
	}

	public ConnectType getConnType() {
		return connType;
	}

	public void setConnType(ConnectType connType) {
		this.connType = connType;
	}
	
	
}

