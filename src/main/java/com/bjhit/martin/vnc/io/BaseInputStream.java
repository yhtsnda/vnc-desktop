package com.bjhit.martin.vnc.io;

import java.io.UnsupportedEncodingException;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:27:54
 * @version 1.0
 */
public abstract class BaseInputStream {
	public static int maxStringLength = 65535;
	protected byte[] b;
	protected int ptr;
	protected int end;

	public int check(int itemSize, int nItems, boolean wait) {
		if (this.ptr + itemSize * nItems > this.end) {
			if (this.ptr + itemSize > this.end)
				return overrun(itemSize, nItems, wait);
			nItems = (this.end - this.ptr) / itemSize;
		}
		return nItems;
	}

	public int check(int itemSize, int nItems) {
		return check(itemSize, nItems, true);
	}

	public int check(int itemSize) {
		return check(itemSize, 1);
	}

	public final boolean checkNoWait(int length) {
		return (check(length, 1, false) != 0);
	}

	public final int readS8() {
		check(1);
		return this.b[(this.ptr++)];
	}

	public final int readS16() {
		check(2);
		int b0 = this.b[(this.ptr++)];
		int b1 = this.b[(this.ptr++)] & 0xFF;
		return (b0 << 8 | b1);
	}

	public final int readS32() {
		check(4);
		int b0 = this.b[(this.ptr++)];
		int b1 = this.b[(this.ptr++)] & 0xFF;
		int b2 = this.b[(this.ptr++)] & 0xFF;
		int b3 = this.b[(this.ptr++)] & 0xFF;
		return (b0 << 24 | b1 << 16 | b2 << 8 | b3);
	}

	public final int readU8() {
		return (readS8() & 0xFF);
	}

	public final int readU16() {
		return (readS16() & 0xFFFF);
	}

	public final int readU32() {
		return (readS32() & 0xFFFFFFFF);
	}

	public final String readString() {
		int len = readU32();

		byte[] str = new byte[len];
		readBytes(str, 0, len);
		String utf8string = new String();
		try {
			utf8string = new String(str, "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return utf8string;
	}

	public final void skip(int bytes) {
		while (bytes > 0) {
			int n = check(1, bytes);
			this.ptr += n;
			bytes -= n;
		}
	}

	public void readBytes(byte[] data, int dataPtr, int length) {
		int dataEnd = dataPtr + length;
		while (dataPtr < dataEnd) {
			int n = check(1, dataEnd - dataPtr);
			System.arraycopy(this.b, this.ptr, data, dataPtr, n);
			this.ptr += n;
			dataPtr += n;
		}
	}

	public final int readOpaque8() {
		return readU8();
	}

	public final int readOpaque16() {
		return readU16();
	}

	public final int readOpaque32() {
		return readU32();
	}

	public final int readOpaque24A() {
		check(3);
		int b0 = this.b[(this.ptr++)];
		int b1 = this.b[(this.ptr++)];
		int b2 = this.b[(this.ptr++)];
		return (b0 << 24 | b1 << 16 | b2 << 8);
	}

	public final int readOpaque24B() {
		check(3);
		int b0 = this.b[(this.ptr++)];
		int b1 = this.b[(this.ptr++)];
		int b2 = this.b[(this.ptr++)];
		return (b0 << 16 | b1 << 8 | b2);
	}

	public final int readPixel(int bytesPerPixel, boolean bigEndian) {
		byte[] pix = new byte[4];
		readBytes(pix, 0, bytesPerPixel);

		if (bigEndian) {
			return (0xFF000000 | (pix[0] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | pix[2] & 0xFF);
		}
		return (0xFF000000 | (pix[2] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | pix[0] & 0xFF);
	}

	public final void readPixels(int[] buf, int length, int bytesPerPixel, boolean bigEndian) {
		int npixels = length * bytesPerPixel;
		byte[] pixels = new byte[npixels];
		readBytes(pixels, 0, npixels);
		for (int i = 0; i < length; ++i) {
			byte[] pix = new byte[4];
			System.arraycopy(pixels, i * bytesPerPixel, pix, 0, bytesPerPixel);
			if (bigEndian)
				buf[i] = (0xFF000000 | (pix[0] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | pix[2] & 0xFF);
			else
				buf[i] = (0xFF000000 | (pix[2] & 0xFF) << 16 | (pix[1] & 0xFF) << 8 | pix[0] & 0xFF);
		}
	}

	public final int readCompactLength() {
		int b = readU8();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = readU8();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = readU8();
				result |= (b & 0xFF) << 14;
			}
		}
		return result;
	}

	public abstract int pos();

	public boolean bytesAvailable() {
		return (this.end != this.ptr);
	}

	public final byte[] getbuf() {
		return this.b;
	}

	public final int getptr() {
		return this.ptr;
	}

	public final int getend() {
		return this.end;
	}

	public final void setptr(int p) {
		this.ptr = p;
	}

	protected abstract int overrun(int paramInt1, int paramInt2, boolean paramBoolean);
}