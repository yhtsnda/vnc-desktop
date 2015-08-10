package com.bjhit.martin.vnc.io;

import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.decode.Decoder;
import com.bjhit.martin.vnc.exception.BaseException;
import com.bjhit.martin.vnc.exception.NumberExceedException;
import com.bjhit.martin.vnc.rfb.ClientMessageHandler;
import com.bjhit.martin.vnc.rfb.Point;
import com.bjhit.martin.vnc.rfb.Rect;
import com.bjhit.martin.vnc.util.UnicodeUtil;


/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:38:50
 * @version 1.0
 */

public abstract class CMsgReader {
	public int imageBufIdealSize;
	protected ClientMessageHandler handler;
	protected BaseInputStream is;
	protected Decoder[] decoders;
	protected int[] imageBuf;
	protected int imageBufSize;
	static LogWriter vlog = new LogWriter("CMsgReader");

	protected CMsgReader(ClientMessageHandler handler_, BaseInputStream is_) {
		this.imageBufIdealSize = 0;
		this.handler = handler_;
		this.is = is_;
		this.imageBuf = null;
		this.imageBufSize = 0;
		this.decoders = new Decoder[256];
	}

	protected void readSetColourMapEntries() {
		is.skip(1);
		int firstColour = is.readU16();
		int nColours = is.readU16();
		int[] rgbs = new int[nColours * 3];
		for (int i = 0; i < nColours * 3; ++i)
			rgbs[i] = is.readU16();
		handler.setColourMapEntries(firstColour, nColours, rgbs);
	}

	protected void readBell() {
		handler.bell();
	}

	protected void readServerCutText() {
		is.skip(3);
		int len = is.readU32();
		if (len > 262144) {
			is.skip(len);
			vlog.error("cut text too long (" + len + " bytes) - ignoring");
			return;
		}
		byte[] buf = new byte[len];
		is.readBytes(buf, 0, len);
		String str = new String();
		try {
			str = new String(buf, "GBK");
			if (UnicodeUtil.firstIsUnicode(str)) {
				str = UnicodeUtil.decodeUnicode(str);
			}
		} catch (Exception e) {
			vlog.error("server cut text decode error:"+e.getMessage());
			return;
		}
		vlog.info("text:"+str+"   len="+len);
		handler.serverCutText(str, len);
	}

	protected void readFramebufferUpdateStart() {
		handler.framebufferUpdateStart();
	}

	protected void readFramebufferUpdateEnd() {
		handler.framebufferUpdateEnd(true);
	}

	protected void readRect(Rect r, int encoding) {
		if ((r.br.x > handler.cp.width) || (r.br.y > handler.cp.height)) {
			vlog.error("Rect too big: " + r.width() + "x" + r.height() + " at " + r.tl.x + "," + r.tl.y + " exceeds " + handler.cp.width + "x" + handler.cp.height);
			throw new NumberExceedException("Rect too big");
		}

		if (r.is_empty()) {
			vlog.debug("Ignoring zero size rect");
		}
		handler.beginRect(r, encoding);

		if (encoding == 1) {
			readCopyRect(r);
		} else {
			if (decoders[encoding] == null) {
				decoders[encoding] = Decoder.createDecoder(encoding, this);
				if (decoders[encoding] == null) {
					vlog.error("Unknown rect encoding " + encoding);
					throw new BaseException("ECODE001", "ecoding exception", "Unknown rect encoding");
				}
			}
			decoders[encoding].readRect(r, handler);
		}

		handler.endRect(r, encoding);
	}

	protected void readCopyRect(Rect r) {
		int srcX = is.readU16();
		int srcY = is.readU16();
		handler.copyRect(r, srcX, srcY);
	}

	protected void readSetCursor(int width, int height, Point hotspot) {
		int data_len = width * height;
		int mask_len = (width + 7) / 8 * height;
		int[] data = new int[data_len];
		byte[] mask = new byte[mask_len];

		is.readPixels(data, data_len, handler.cp.pf().bpp / 8, handler.cp.pf().bigEndian);
		is.readBytes(mask, 0, mask_len);

		handler.setCursor(width, height, hotspot, data, mask);
	}

	public int[] getImageBuf(int required) {
		return getImageBuf(required, 0, 0);
	}

	public int[] getImageBuf(int required, int requested, int nPixels) {
		int requiredBytes = required;
		int requestedBytes = requested;
		int size = requestedBytes;
		if (size > imageBufIdealSize)
			size = imageBufIdealSize;

		if (size < requiredBytes) {
			size = requiredBytes;
		}
		if (imageBufSize < size) {
			imageBufSize = size;
			imageBuf = new int[imageBufSize];
		}
		if (nPixels != 0)
			nPixels = imageBufSize / handler.cp.pf().bpp / 8;
		return imageBuf;
	}

	public final int bpp() {
		return handler.cp.pf().bpp;
	}

	public abstract void readServerInit();

	public abstract void readMsg();

	public BaseInputStream getInStream() {
		return is;
	}
}