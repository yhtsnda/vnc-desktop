/* Copyright (C) 2012 Brian P. Hinz
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package com.bjhit.martin.vnc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.exception.AppIOException;
import com.bjhit.martin.vnc.util.StringUtil;

public class SocketDescriptor implements FileDescriptor {

	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private ConnectionInfo connInfo;
	private static LogWriter log = new LogWriter("SocketDescriptor");
	
	public SocketDescriptor(ConnectionInfo connInfo) {
		this.connInfo = connInfo;
	}

	public int read(byte[] buf, int off, int length) throws AppIOException {
		try {
			int count = this.in.read(buf, off, length);

			return count;
		} catch (IOException e) {
			throw new AppIOException(e);
		}
	}

	public void write(byte[] buf, int off, int length) throws AppIOException {
		try {
			out.write(buf, off, length);
		} catch (IOException e) {
			throw new AppIOException(e);
		}
	}

	public void close() throws AppIOException {
		try {
			this.in.close();
			this.out.close();
			this.socket.close();
		} catch (IOException e) {
			throw new AppIOException(e);
		}
	}

	public boolean connet() {
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
			return false;
		}
		return true;
	}

	private Proxy initProxy() {
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

	public Socket socket() {
		return this.socket;
	}

}
