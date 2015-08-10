package com.bjhit.martin.vnc.io;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.exception.AppIOException;
import com.bjhit.martin.vnc.util.StringUtil;

public class PureVncSession extends VncSession {
	
	private static LogWriter log = new LogWriter("PureVncSession");
	
	public PureVncSession(ConnectionInfo connectionInfo) {
		this.connInfo = connectionInfo;
	}
	
	@Override
	public boolean initConnection() throws IOException {
		try {
			if (!(StringUtil.isEmpty(this.connInfo.getProxyHost())))
				this.socket = new Socket(initProxy());
			else {
				this.socket = new Socket();
			}
			this.socket.connect(new InetSocketAddress(this.connInfo.getHost(), this.connInfo.getPort()));

			this.in = this.socket.getInputStream();
			this.out = this.socket.getOutputStream();
		} catch (IOException e) {
			log.debug(e.getMessage());
			throw e;
		}
		return true;
	}

	public Proxy initProxy() {
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(this.connInfo.getProxyHost(), this.connInfo.getProxyPort()));

		Authenticator auth = new Authenticator() {
			String pwd;
			private PasswordAuthentication pa;

			protected PasswordAuthentication getPasswordAuthentication() {
				return this.pa;
			}
		};
		Authenticator.setDefault(auth);
		return proxy;
	}
	
	@Override
	public void close() throws AppIOException {
		try {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			throw new AppIOException(e);
		}
	}
}
