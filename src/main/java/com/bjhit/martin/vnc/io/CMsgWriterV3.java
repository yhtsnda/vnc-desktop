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

import java.util.Iterator;

import com.bjhit.martin.vnc.exception.NumberExceedException;
import com.bjhit.martin.vnc.rfb.ConnParams;
import com.bjhit.martin.vnc.rfb.FenceTypes;
import com.bjhit.martin.vnc.rfb.MsgTypes;
import com.bjhit.martin.vnc.rfb.Screen;
import com.bjhit.martin.vnc.rfb.ScreenSet;

public class CMsgWriterV3 extends CMsgWriter {

	public CMsgWriterV3(ConnParams cp_, OutStream os_) {
		super(cp_, os_);
	}

	synchronized public void writeClientInit(boolean shared) {
		os.writeByte(shared ? 1 : 0);
	}

	

	synchronized public void writeSetDesktopSize(int width, int height, ScreenSet layout) {
		if (!cp.supportsSetDesktopSize)
			throw new NumberExceedException("Server does not support SetDesktopSize");
		checkBuffSize(8 + 16 * layout.screens.size());
		buff = new byte[8 + 16 * layout.screens.size()];
		buff[0] = (byte) MsgTypes.msgTypeSetDesktopSize;
		buff[2] = (byte) (width >> 8 & 0xff);
		buff[3] = (byte) (width & 0xff);
		buff[4] = (byte) (height >> 8 & 0xff);
		buff[5] = (byte) (height & 0xff);
		buff[6] = (byte) (layout.num_screens() & 0xff);
		int count = 0;
		for (Iterator<Screen> iter = layout.screens.iterator(); iter.hasNext();) {
			Screen refScreen = (Screen) iter.next();
			buff[8 + count * 16] = (byte) (refScreen.id >> 24 & 0xff);
			buff[8 + count * 16 + 1] = (byte) (refScreen.id >> 16 & 0xff);
			buff[8 + count * 16 + 2] = (byte) (refScreen.id >> 8 & 0xff);
			buff[8 + count * 16 + 3] = (byte) (refScreen.id & 0xff);
			buff[8 + count * 16 + 4] = (byte) (refScreen.dimensions.tl.x >> 8 & 0xff);
			buff[8 + count * 16 + 5] = (byte) (refScreen.dimensions.tl.x & 0xff);
			buff[8 + count * 16 + 6] = (byte) (refScreen.dimensions.tl.y >> 8 & 0xff);
			buff[8 + count * 16 + 7] = (byte) (refScreen.dimensions.tl.y & 0xff);
			buff[8 + count * 16 + 8] = (byte) (refScreen.dimensions.width() >> 8 & 0xff);
			buff[8 + count * 16 + 9] = (byte) (refScreen.dimensions.width() & 0xff);
			buff[8 + count * 16 + 10] = (byte) (refScreen.dimensions.height() >> 8 & 0xff);
			buff[8 + count * 16 + 11] = (byte) (refScreen.dimensions.height() & 0xff);
			buff[8 + count * 16 + 12] = (byte) (refScreen.flags >> 24 & 0xff);
			buff[8 + count * 16 + 13] = (byte) (refScreen.flags >> 16 & 0xff);
			buff[8 + count * 16 + 14] = (byte) (refScreen.flags >> 8 & 0xff);
			buff[8 + count * 16 + 15] = (byte) (refScreen.flags & 0xff);
			count++;
		}
		os.writeBuffer(buff,0,8 + 16 * layout.screens.size());
	}

	synchronized public void writeFence(int flags, int len, byte[] data) {
		if (!cp.supportsFence)
			throw new RuntimeException("Server does not support fences");
		if (len > 64)
			throw new NumberExceedException("Too large fence payload");
		if ((flags & ~FenceTypes.fenceFlagsSupported) != 0)
			throw new RuntimeException("Unknown fence flags");
		checkBuffSize(9+len);
		buff = new byte[9+len];
		buff[0] = (byte) MsgTypes.msgTypeClientFence;
		buff[4] = (byte) (flags >> 24 & 0xff);
		buff[5] = (byte) (flags >> 16 & 0xff);
		buff[6] = (byte) (flags >> 8 & 0xff);
		buff[7] = (byte) (flags & 0xff);
		buff[8] = (byte) (len & 0xff);
		System.arraycopy(data, 0, buff, 9, len);
		os.writeBuffer(buff,0,9+len);
	}

	synchronized public void writeEnableContinuousUpdates(boolean enable, int x, int y, int w, int h) {
		if (!cp.supportsContinuousUpdates)
			throw new RuntimeException("Server does not support continuous updates");
		checkBuffSize(10);
		buff = new byte[10];
		buff[0] = (byte) MsgTypes.msgTypeEnableContinuousUpdates;
		buff[1] = (byte) ((enable ? 1 : 0) & 0xff);
		buff[2] = (byte) (x >> 16 & 0xff);
		buff[3] = (byte) (x >> 8 & 0xff);
		buff[4] = (byte) (y >> 16 & 0xff);
		buff[5] = (byte) (y & 0xff);
		buff[6] = (byte) (w >>16 & 0xff);
		buff[7] = (byte) (w & 0xff);
		buff[8] = (byte) (h >>16 & 0xff);
		buff[9] = (byte) (h & 0xff);
		os.writeBuffer(buff,0,10);
	}
}
