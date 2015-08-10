package com.bjhit.martin.vnc.io;

import com.bjhit.martin.vnc.common.ConnectionInfo;

public class ConnectionFactory {
	
	public static VncSession getConnection(ConnectionInfo info) {
		switch (info.getConnType()) {
		case PURE_VNC:
			return new PureVncSession(info);
		case XEN_VNC:
			return new XenVncSession(info);
		default:
			break;
		}
		return null;
	}
}
