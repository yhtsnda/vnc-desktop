/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2011 Pierre Ossman for Cendio AB
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


public class FdOutStream extends OutStream {

	public FdOutStream(FileDescriptor fd_, int timeoutms_) {
		fd = fd_;
		timeoutms = timeoutms_;
	}

	public FdOutStream(FileDescriptor fd_) {
		fd = fd_;
		timeoutms = 0;
	}
	
	public void setTimeout(int timeoutms_) {
		timeoutms = timeoutms_;
	}

	public void setBlocking(boolean blocking_) {
		blocking = blocking_;
	}

	public FileDescriptor getFd() {
		return fd;
	}

	public void setFd(FileDescriptor fd_) {
		fd = fd_;
	}

	protected FileDescriptor fd;
	protected boolean blocking;
	protected int timeoutms;
	byte[] buff = new byte[5];
	@Override
	public synchronized void writeByte(int value) {
		buff[0] = (byte) value;
		fd.write(buff, 0, 1);
	}


	@Override
	public synchronized void writeBuffer(byte[] buf, int off, int length) {
		fd.write(buf, off, length);
	}


	@Override
	public synchronized void writeBuffer(byte[] buff) {
		fd.write(buff, 0, buff.length);
	}
}
