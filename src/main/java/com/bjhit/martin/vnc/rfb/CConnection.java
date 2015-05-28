package com.bjhit.martin.vnc.rfb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.exception.AppIOException;
import com.bjhit.martin.vnc.exception.AuthFailureException;
import com.bjhit.martin.vnc.io.BaseInputStream;
import com.bjhit.martin.vnc.io.CMsgReaderV3;
import com.bjhit.martin.vnc.io.CMsgWriterV3;
import com.bjhit.martin.vnc.io.OutStream;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:57:19
 * @version 1.0
 */
public abstract class CConnection extends ClientMessageHandler {
	private ConnectionInfo connInfo;
	public static final int RFBSTATE_UNINITIALISED = 0;
	public static final int RFBSTATE_PROTOCOL_VERSION = 1;
	public static final int RFBSTATE_SECURITY_TYPES = 2;
	public static final int RFBSTATE_SECURITY = 3;
	public static final int RFBSTATE_SECURITY_RESULT = 4;
	public static final int RFBSTATE_INITIALISATION = 5;
	public static final int RFBSTATE_NORMAL = 6;
	public static final int RFBSTATE_INVALID = 7;
	BaseInputStream is;
	OutStream os;
	CMsgReaderV3 reader_;
	CMsgWriterV3 writer_;
	boolean shared;
	public CSecurity csecurity;
	public SecurityClient security;
	public static final int maxSecTypes = 8;
	int nSecTypes;
	int[] secTypes;
	int state_ = 0;
	String serverName;
	int serverPort;
	boolean useProtocol3_3;
	boolean clientSecTypeOrder;
	static LogWriter vlog = new LogWriter("CConnection");

	public ConnectionInfo getConnInfo() {
		return this.connInfo;
	}

	public void setConnInfo(ConnectionInfo connInfo) {
		this.connInfo = connInfo;
	}

	public CConnection() {
		this.csecurity = null;
		this.is = null;
		this.os = null;
		this.reader_ = null;
		this.writer_ = null;
		this.shared = false;
		this.state_ = 0;
		this.useProtocol3_3 = false;
		this.security = new SecurityClient();
	}

	public void deleteReaderAndWriter() {
		this.reader_ = null;
		this.writer_ = null;
	}

	public final void initialiseProtocol() {
		this.state_ = 1;
	}

	public void processMsg() {
		switch (this.state_) {
		case RFBSTATE_PROTOCOL_VERSION:
			processVersionMsg();
			break;
		case RFBSTATE_SECURITY_TYPES:
			processSecurityTypesMsg();
			break;
		case RFBSTATE_SECURITY:
			processSecurityMsg();
			break;
		case RFBSTATE_SECURITY_RESULT:
			processSecurityResultMsg();
			break;
		case RFBSTATE_INITIALISATION:
			processInitMsg();
			break;
		case RFBSTATE_NORMAL:
			this.reader_.readMsg();
			break;
		case RFBSTATE_UNINITIALISED:
			throw new RuntimeException("CConnection.processMsg: not initialised yet?");
		default:
			throw new RuntimeException("CConnection.processMsg: invalid state");
		}
	}

	private void processVersionMsg() {
		vlog.debug("reading protocol version");
		if (!(this.cp.readVersion(this.is))) {
			this.state_ = RFBSTATE_INVALID;
			throw new RuntimeException("reading version failed: not an RFB server?");
		}
		if (!(this.cp.done)) {
			return;
		}
		vlog.info("Server supports RFB protocol version " + this.cp.majorVersion + "." + this.cp.minorVersion);

		if (this.cp.beforeVersion(3, 3)) {
			String msg = "Server gave unsupported RFB protocol version " + this.cp.majorVersion + "." + this.cp.minorVersion;
			vlog.error(msg);
			this.state_ = RFBSTATE_INVALID;
			throw new RuntimeException(msg);
		}
		if ((this.useProtocol3_3) || (this.cp.beforeVersion(3, 7)))
			this.cp.setVersion(3, 3);
		else if (this.cp.afterVersion(3, 8)) {
			this.cp.setVersion(3, 8);
		}

		this.cp.writeVersion(this.os);
		this.state_ = RFBSTATE_SECURITY_TYPES;

		vlog.info("Using RFB protocol version " + this.cp.majorVersion + "." + this.cp.minorVersion);
	}

	private void processSecurityTypesMsg() {
		vlog.debug("processing security types message");

		int secType = 0;

		List secTypes = new ArrayList();
		secTypes = Security.GetEnabledSecTypes();

		if (this.cp.isVersion(3, 3)) {
			secType = this.is.readU32();
			if (secType == 0) {
				throwConnFailedException();
			} else if ((secType == 1) || (secType == 2)) {
				for (Iterator i = secTypes.iterator(); i.hasNext();) {
					int refType = ((Integer) i.next()).intValue();
					if (refType == secType) {
						secType = refType;
						break;
					}
				}

				if (!(secTypes.contains(Integer.valueOf(secType))))
					secType = 0;
			} else {
				vlog.error("Unknown 3.3 security type " + secType);
				throw new RuntimeException("Unknown 3.3 security type");
			}

		} else {
			int nServerSecTypes = this.is.readU8();
			if (nServerSecTypes == 0) {
				throwConnFailedException();
			}

			for (int i = 0; i < nServerSecTypes; ++i) {
				int serverSecType = this.is.readU8();
				vlog.debug("Server offers security type " + Security.secTypeName(serverSecType) + "(" + serverSecType + ")");

				if (secType == 0) {
					for (Iterator j = secTypes.iterator(); j.hasNext();) {
						int refType = ((Integer) j.next()).intValue();
						if (refType == serverSecType) {
							secType = refType;
							break;
						}
					}
				}

			}

			if (secType != 0) {
				this.os.writeByte(secType);
				vlog.debug("Choosing security type " + Security.secTypeName(secType) + "(" + secType + ")");
			}
		}

		if (secType == 0) {
			this.state_ = RFBSTATE_INVALID;
			vlog.error("No matching security types");
			throw new RuntimeException("No matching security types");
		}

		this.state_ = RFBSTATE_SECURITY;
		this.csecurity = this.security.GetCSecurity(secType);
		processSecurityMsg();
	}

	private void processSecurityMsg() {
		vlog.debug("processing security message");
		if (this.csecurity.processMsg(this)) {
			this.state_ = RFBSTATE_SECURITY_RESULT;
			processSecurityResultMsg();
		}
	}

	private void processSecurityResultMsg() {
		vlog.debug("processing security result message");
		int result;
		if ((this.cp.beforeVersion(3, 8)) && (this.csecurity.getType() == 1)) {
			result = 0;
		} else {
			// if (!(this.is.checkNoWait(1)))
			// return;
			result = this.is.readU32();
		}
		switch (result) {
		case 0:
			securityCompleted();
			return;
		case 1:
			vlog.debug("auth failed");
			break;
		case 2:
			vlog.debug("auth failed - too many tries");
			break;
		default:
			throw new RuntimeException("Unknown security result from server");
		}
		String reason;
		if (this.cp.beforeVersion(3, 8))
			reason = "Authentication failure";
		else
			reason = this.is.readString();
		this.state_ = RFBSTATE_INVALID;
		throw new AuthFailureException(reason);
	}

	private void processInitMsg() {
		vlog.debug("reading server initialisation");
		this.reader_.readServerInit();
	}

	private void throwConnFailedException() {
		this.state_ = RFBSTATE_INVALID;

		String reason = this.is.readString();
		throw new AppIOException(reason);
	}

	private void securityCompleted() {
		state_ = RFBSTATE_INITIALISATION;
		reader_ = new CMsgReaderV3(this, is);
		writer_ = new CMsgWriterV3(cp, os);
		vlog.debug("Authentication success!");
		authSuccess();
		writer_.writeClientInit(shared);
	}

	public final void setServerName(String name) {
		this.serverName = name;
	}

	public final void setStreams(BaseInputStream is_, OutStream os_) {
		this.is = is_;
		this.os = os_;
	}

	public final void setShared(boolean s) {
		this.shared = s;
	}

	public final void setProtocol3_3(boolean s) {
		this.useProtocol3_3 = s;
	}

	public void setServerPort(int port) {
		this.serverPort = port;
	}

	public void initSecTypes() {
		this.nSecTypes = 0;
	}

	public void authSuccess() {
	}

	public void serverInit() {
		this.state_ = RFBSTATE_NORMAL;
		vlog.debug("initialisation done");
	}

	public void setClientSecTypeOrder(boolean csto) {
		this.clientSecTypeOrder = csto;
	}

	public CMsgReaderV3 reader() {
		return this.reader_;
	}

	public CMsgWriterV3 writer() {
		return this.writer_;
	}

	public BaseInputStream getInStream() {
		return this.is;
	}

	public OutStream getOutStream() {
		return this.os;
	}

	public String getServerName() {
		return this.serverName;
	}

	public int getServerPort() {
		return this.serverPort;
	}

	public int state() {
		return this.state_;
	}

	protected final void setState(int s) {
		this.state_ = s;
	}

	public void fence(int flags, int len, byte[] data) {
		super.fence(flags, len, data);

		if ((flags & 0x80000000) != 0) {
			return;
		}

		flags = 0;

		synchronized (this) {
			writer().writeFence(flags, len, data);
		}
	}

	private void throwAuthFailureException() {
		vlog.debug("state=" + state() + ", ver=" + this.cp.majorVersion + "." + this.cp.minorVersion);
		String reason;
		if ((state() == RFBSTATE_SECURITY_RESULT) && (!(this.cp.beforeVersion(3, 8))))
			reason = this.is.readString();
		else {
			reason = "Authentication failure";
		}
		this.state_ = RFBSTATE_INVALID;
		vlog.error(reason);
		throw new AuthFailureException(reason);
	}
}