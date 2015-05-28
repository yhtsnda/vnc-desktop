/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2009-2011 Pierre Ossman for Cendio AB
 * Copyright (C) 2011 Brian P. Hinz
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

import java.util.Arrays;

import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.decode.Decoder;
import com.bjhit.martin.vnc.rfb.ConnParams;
import com.bjhit.martin.vnc.rfb.Encodings;
import com.bjhit.martin.vnc.rfb.MsgTypes;
import com.bjhit.martin.vnc.rfb.PixelFormat;
import com.bjhit.martin.vnc.rfb.Point;
import com.bjhit.martin.vnc.rfb.Rect;

abstract public class CMsgWriter {

	abstract public void writeClientInit(boolean shared);

	protected byte[] buff = new byte[8196];

	public void checkBuffSize(int size) {
		synchronized (buff) {
			if (buff.length < size) {
				buff = new byte[size];
			}
		}
	}

	synchronized public void writeSetPixelFormat(PixelFormat pf) {
		byte[] data = pf.getPixelFormatByte();
		checkBuffSize(data.length + 4);
		buff[0] = MsgTypes.msgTypeSetPixelFormat;
		System.arraycopy(data, 0, buff, 4, data.length);
		os.writeBuffer(buff, 0, data.length + 4);
	}

	synchronized public void writeSetEncodings(int nEncodings, int[] encodings) {
		checkBuffSize(4 + 4 * nEncodings);

		buff[0] = (byte) MsgTypes.msgTypeSetEncodings;
		buff[2] = (byte) ((nEncodings >> 8) & 0xff);
		buff[3] = (byte) (nEncodings & 0xff);
		for (int i = 0; i < nEncodings; i++) {
			buff[4 + 4 * i] = (byte) ((encodings[i] >> 24) & 0xff);
			buff[5 + 4 * i] = (byte) ((encodings[i] >> 16) & 0xff);
			buff[6 + 4 * i] = (byte) ((encodings[i] >> 8) & 0xff);
			buff[7 + 4 * i] = (byte) (encodings[i] & 0xff);
		}
		os.writeBuffer(buff, 0, 4 + 4 * nEncodings);
	}

	// Ask for encodings based on which decoders are supported. Assumes higher
	// encoding numbers are more desirable.

	synchronized public void writeSetEncodings(int preferredEncoding, boolean useCopyRect) {
		int nEncodings = 0;
		int[] encodings = new int[Encodings.encodingMax + 3];
		if (cp.supportsLocalCursor)
			encodings[nEncodings++] = Encodings.pseudoEncodingCursor;
		if (cp.supportsDesktopResize)
			encodings[nEncodings++] = Encodings.pseudoEncodingDesktopSize;
		if (cp.supportsExtendedDesktopSize)
			encodings[nEncodings++] = Encodings.pseudoEncodingExtendedDesktopSize;
		if (cp.supportsDesktopRename)
			encodings[nEncodings++] = Encodings.pseudoEncodingDesktopName;
		if (cp.supportsClientRedirect)
			encodings[nEncodings++] = Encodings.pseudoEncodingClientRedirect;

		encodings[nEncodings++] = Encodings.pseudoEncodingLastRect;
		encodings[nEncodings++] = Encodings.pseudoEncodingContinuousUpdates;
		encodings[nEncodings++] = Encodings.pseudoEncodingFence;

		if (Decoder.supported(preferredEncoding)) {
			encodings[nEncodings++] = preferredEncoding;
		}

		if (useCopyRect) {
			encodings[nEncodings++] = Encodings.encodingCopyRect;
		}

		/*
		 * Prefer encodings in this order:
		 * 
		 * Tight, ZRLE, Hextile, *
		 */

		if ((preferredEncoding != Encodings.encodingTight) && Decoder.supported(Encodings.encodingTight))
			encodings[nEncodings++] = Encodings.encodingTight;

		if ((preferredEncoding != Encodings.encodingZRLE) && Decoder.supported(Encodings.encodingZRLE))
			encodings[nEncodings++] = Encodings.encodingZRLE;

		if ((preferredEncoding != Encodings.encodingHextile) && Decoder.supported(Encodings.encodingHextile))
			encodings[nEncodings++] = Encodings.encodingHextile;

		// Remaining encodings
		for (int i = Encodings.encodingMax; i >= 0; i--) {
			switch (i) {
			case Encodings.encodingTight:
			case Encodings.encodingZRLE:
			case Encodings.encodingHextile:
				break;
			default:
				if ((i != preferredEncoding) && Decoder.supported(i))
					encodings[nEncodings++] = i;
			}
		}

		encodings[nEncodings++] = Encodings.pseudoEncodingLastRect;
		if (cp.customCompressLevel && cp.compressLevel >= 0 && cp.compressLevel <= 9)
			encodings[nEncodings++] = Encodings.pseudoEncodingCompressLevel0 + cp.compressLevel;
		if (!cp.noJpeg && cp.qualityLevel >= 0 && cp.qualityLevel <= 9)
			encodings[nEncodings++] = Encodings.pseudoEncodingQualityLevel0 + cp.qualityLevel;

		writeSetEncodings(nEncodings, encodings);
	}

	synchronized public void writeFramebufferUpdateRequest(Rect r, boolean incremental) {
		checkBuffSize(10);
		buff[0] = (byte) MsgTypes.msgTypeFramebufferUpdateRequest;
		buff[1] = (byte) ((incremental ? 1 : 0) & 0xff);
		buff[2] = (byte) (r.tl.x >> 8 & 0xff);
		buff[3] = (byte) (r.tl.x & 0xff);
		buff[4] = (byte) (r.tl.y >> 8 & 0xff);
		buff[5] = (byte) (r.tl.y & 0xff);
		buff[6] = (byte) (r.width() >> 8 & 0xff);
		buff[7] = (byte) (r.width() & 0xff);
		buff[8] = (byte) (r.height() >> 8 & 0xff);
		buff[9] = (byte) (r.height() & 0xff);
		os.writeBuffer(buff, 0, 10);
	}

	synchronized public void writeKeyEvent(int key, boolean down) {
		checkBuffSize(8);
		buff[0] = (byte) MsgTypes.msgTypeKeyEvent;
		buff[1] = (byte) ((down ? 1 : 0) & 0xff);
		buff[4] = (byte) (key >> 24 & 0xff);
		buff[5] = (byte) (key >> 16 & 0xff);
		buff[6] = (byte) (key >> 8 & 0xff);
		buff[7] = (byte) (key & 0xff);
		os.writeBuffer(buff, 0, 8);
	}

	synchronized public void writePointerEvent(Point pos, int buttonMask) {
		Point p = new Point(pos.x, pos.y);
		if (p.x < 0)
			p.x = 0;
		if (p.y < 0)
			p.y = 0;
		if (p.x >= cp.width)
			p.x = cp.width - 1;
		if (p.y >= cp.height)
			p.y = cp.height - 1;
		checkBuffSize(6);
		buff[0] = (byte) MsgTypes.msgTypePointerEvent;
		buff[1] = (byte) (buttonMask & 0xff);
		buff[2] = (byte) (p.x >> 8 & 0xff);
		buff[3] = (byte) (p.x & 0xff);
		buff[4] = (byte) (p.y >> 8 & 0xff);
		buff[5] = (byte) (p.y & 0xff);
		os.writeBuffer(buff, 0, 6);
	}

	synchronized public void writeClientCutText(String str, int len) {
		try {
			byte[] utf8str = str.getBytes("UTF8");
			checkBuffSize(utf8str.length + 8);
			buff[0] = MsgTypes.msgTypeClientCutText;
			buff[4] = (byte) (len >> 24 & 0xff);
			buff[5] = (byte) (len >> 16 & 0xff);
			buff[6] = (byte) (len >> 8 & 0xff);
			buff[7] = (byte) (len & 0xff);
			System.arraycopy(utf8str, 0, buff, 8, len);
			os.writeBuffer(buff, 0, len + 8);
		} catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	synchronized public void setOutStream(OutStream os_) {
		os = os_;
	}

	ConnParams getConnParams() {
		return cp;
	}

	OutStream getOutStream() {
		return os;
	}

	protected CMsgWriter(ConnParams cp_, OutStream os_) {
		cp = cp_;
		os = os_;
	}

	ConnParams cp;
	OutStream os;
	static LogWriter vlog = new LogWriter("CMsgWriter");
}
