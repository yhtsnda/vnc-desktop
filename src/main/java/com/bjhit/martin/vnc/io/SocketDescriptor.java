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

import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.exception.AppIOException;

public class SocketDescriptor implements FileDescriptor {

	private VncSession vncSession;
	private static LogWriter log = new LogWriter("SocketDescriptor");
	
	public SocketDescriptor(VncSession vncSession) {
		this.vncSession = vncSession;
	}

	public int read(byte[] buf, int off, int length) throws AppIOException {
		try {
			int count = vncSession.read(buf, off, length);
			return count;
		} catch (AppIOException e) {
			throw e;
		}
	}

	public void write(byte[] buf, int off, int length) throws AppIOException {
		try {
			vncSession.write(buf, off, length);
		} catch (AppIOException e) {
			throw e;
		}
	}

	public void close() throws AppIOException {
		try {
			vncSession.close();
		} catch (AppIOException e) {
			throw e;
		}
	}

	public boolean connet() throws Exception {
		try {
			boolean success = vncSession.initConnection();
			return success;
		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}
	}

	public VncSession getVncSession() {
		return vncSession;
	}

	
}
