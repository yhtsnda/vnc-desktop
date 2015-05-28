
package com.bjhit.martin.vnc.io;

import com.bjhit.martin.vnc.exception.NumberExceedException;
import com.bjhit.martin.vnc.exception.StreamEndException;
/**
 * 
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-22 上午9:17:03
 * @version 1.0
 */
public class FdInStream extends BaseInputStream {
	static final int DEFAULT_BUF_SIZE = 8192;
	static final int minBulkSize = 1024;
	private FileDescriptor fd;
	boolean closeWhenDone;
	protected int timeoutms;
	private int offset;
	private int bufSize;
	protected boolean timing;
	protected long timeWaitedIn100us;
	protected long timedKbits;

	public FdInStream(FileDescriptor fd_, int timeoutms_, int bufSize_, boolean closeWhenDone_) {
		this.fd = fd_;
		this.closeWhenDone = closeWhenDone_;
		this.timeoutms = timeoutms_;

		this.timing = false;
		this.timeWaitedIn100us = 5L;
		this.timedKbits = 0L;
		this.bufSize = ((bufSize_ > 0) ? bufSize_ : 8192);
		this.b = new byte[this.bufSize];
		this.ptr = (this.end = this.offset = 0);
	}

	public FdInStream(FileDescriptor fd_) {
		this(fd_, -1, 0, false);
	}

	public FdInStream(FileDescriptor fd_, int bufSize_) {
		this.fd = fd_;
		this.timeoutms = 0;

		this.timing = false;
		this.timeWaitedIn100us = 5L;
		this.timedKbits = 0L;
		this.bufSize = ((bufSize_ > 0) ? bufSize_ : 8192);
		this.b = new byte[this.bufSize];
		this.ptr = (this.end = this.offset = 0);
	}

	public final void readBytes(byte[] data, int dataPtr, int length) {
		if (length < 1024) {
			super.readBytes(data, dataPtr, length);
			return;
		}

		int n = this.end - this.ptr;
		if (n > length) {
			n = length;
		}
		System.arraycopy(this.b, this.ptr, data, dataPtr, n);
		dataPtr += n;
		length -= n;
		this.ptr += n;

		while (length > 0) {
			n = readWithTimeoutOrCallback(data, dataPtr, length);
			dataPtr += n;
			length -= n;
			this.offset += n;
		}
	}

	public void setTimeout(int timeoutms_) {
		this.timeoutms = timeoutms_;
	}

	public final int pos() {
		return (this.offset + this.ptr);
	}

	public final void startTiming() {
		this.timing = true;

		if (this.timeWaitedIn100us > 10000L) {
			this.timedKbits = (this.timedKbits * 10000L / this.timeWaitedIn100us);
			this.timeWaitedIn100us = 10000L;
		}
	}

	public final void stopTiming() {
		this.timing = false;
		if (this.timeWaitedIn100us < this.timedKbits / 2L)
			this.timeWaitedIn100us = (this.timedKbits / 2L);
	}

	public final long kbitsPerSecond() {
		return (this.timedKbits * 10000L / this.timeWaitedIn100us);
	}

	public final long timeWaited() {
		return this.timeWaitedIn100us;
	}

	protected int overrun(int itemSize, int nItems, boolean wait) {
		if (itemSize > this.bufSize)
			throw new NumberExceedException("FdInStream overrun: max itemSize exceeded:" + this.bufSize);
		if (this.end - this.ptr != 0) {
			System.arraycopy(this.b, this.ptr, this.b, 0, this.end - this.ptr);
		}
		this.offset += this.ptr;
		this.end -= this.ptr;
		this.ptr = 0;

		while (this.end < itemSize) {
			int bytes_to_read = this.bufSize - this.end;
			if (!(this.timing)) {
				bytes_to_read = Math.min(bytes_to_read, Math.max(itemSize * nItems, 8));
			}
			int n = readWithTimeoutOrCallback(this.b, this.end, bytes_to_read, wait);
			if (n == 0)
				return 0;
			this.end += n;
		}

		if (itemSize * nItems > this.end - this.ptr) {
			nItems = (this.end - this.ptr) / itemSize;
		}
		return nItems;
	}

	protected int readWithTimeoutOrCallback(byte[] buf, int bufPtr, int len, boolean wait) {
		long before = 0L;
		if (this.timing) {
			before = System.nanoTime();
		}
		int n = 0;
		while (true) {
			try {
				n = this.fd.read(buf, bufPtr, len);
			} catch (Exception localException) {
				
			}
			if (n < 0)
				continue;
			if (n > 0)
				break;
			if (!(wait)) {
				return 0;
			}

		}

		if (n == 0) {
			throw new StreamEndException("Inputstream is end");
		}
		if (this.timing) {
			long after = System.nanoTime();
			long newTimeWaited = (after - before) / 100000L;
			int newKbits = n * 8 / 1000;

			if (newTimeWaited > newKbits * 1000)
				newTimeWaited = newKbits * 1000;
			else if (newTimeWaited < newKbits / 4) {
				newTimeWaited = newKbits / 4;
			}

			this.timeWaitedIn100us += newTimeWaited;
			this.timedKbits += newKbits;
		}

		return n;
	}

	private int readWithTimeoutOrCallback(byte[] buf, int bufPtr, int len) {
		return readWithTimeoutOrCallback(buf, bufPtr, len, true);
	}

	public FileDescriptor getFd() {
		return this.fd;
	}

	public void setFd(FileDescriptor fd_) {
		this.fd = fd_;
	}
}
