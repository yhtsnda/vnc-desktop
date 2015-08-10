package com.bjhit.martin.vnc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.Socket;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.exception.AppIOException;

public abstract class VncSession {

	protected Socket socket = null;
	protected InputStream in = null;
	protected OutputStream out = null;
	protected ConnectionInfo connInfo = null;

	public abstract boolean initConnection() throws Exception;

	public abstract Proxy initProxy();

	/**
	 * 
	 * @param buf
	 * @param off
	 * @param length
	 * @return
	 * @throws AppIOException
	 */
	public int read(byte[] buf, int off, int length) throws AppIOException {
		try {
			int count = in.read(buf, off, length);
			return count;
		} catch (IOException e) {
			throw new AppIOException(e);
		}
	}

	/**
	 * 
	 * @param buf
	 * @param off
	 * @param length
	 * @throws AppIOException
	 */
	public void write(byte[] buf, int off, int length) throws AppIOException {
		try {
			out.write(buf, off, length);
		} catch (IOException e) {
			throw new AppIOException(e);
		}
	}

	/**
	 * 
	 * @throws AppIOException
	 */
	public abstract void close() throws AppIOException;
}
