/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2012 Brian P. Hinz
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

import java.net.InetSocketAddress;

import com.bjhit.martin.vnc.common.ConnectionInfo;

public class TcpSocket extends Socket {
	private SocketDescriptor socket;
	private static boolean socketsInitialised = false;
	private boolean closeFd;

	public int getMyPort() {
		return 0;
	}

	public String getPeerAddress() {
		return null;
	}

	public String getPeerName() {
		return null;
	}

	public int getPeerPort() {
		return 0;
	}

	public String getPeerEndpoint() {
		return null;
	}

	public boolean sameMachine() {
		return false;
	}

	public static void initSockets() {
		if (socketsInitialised)
			return;
		socketsInitialised = true;
	}

	public TcpSocket(ConnectionInfo connInfo) throws Exception {
		this.closeFd = true;
		boolean result = false;

		initSockets();
		try {
			this.socket = new SocketDescriptor(connInfo);
			result = this.socket.connet();
		} catch (Exception e) {
			throw new RuntimeException("unable to create socket: " + e.toString());
		}
		if (!(result)) {
			throw new RuntimeException("unable connect to socket");
		}

		this.instream = new FdInStream(this.socket);
		this.outstream = new FdOutStream(this.socket);
		this.ownStreams = true;
	}

	public int getSockPort() {
		return ((InetSocketAddress) ((SocketDescriptor) getFd()).socket().getRemoteSocketAddress()).getPort();
	}

}


