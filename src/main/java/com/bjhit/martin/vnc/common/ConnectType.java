package com.bjhit.martin.vnc.common;

import com.bjhit.martin.vnc.util.StringUtil;

public enum ConnectType {
	PURE_VNC("pure_vnc"),XEN_VNC("xen_vnc");
	
	private String type;
	
	private ConnectType(String type) {
		this.type = type;
	}
	
	public String geType() {
		return type;
	}
	
	public static ConnectType getType(String type) {
		if (StringUtil.equalsIgnoreCase(type, PURE_VNC.geType())) {
			return PURE_VNC;
		}else if (StringUtil.equalsIgnoreCase(type, XEN_VNC.geType())) {
			return XEN_VNC;
		}
		return null;
	}
	
	public String value(){
		return this.type;
	}
}
