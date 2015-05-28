/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2011-2012 Brian P.Hinz
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

package com.bjhit.martin.vnc.common;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.IndexColorModel;
import java.nio.ByteOrder;

import com.bjhit.martin.vnc.client.CConn;
import com.bjhit.martin.vnc.client.DesktopWindow;
import com.bjhit.martin.vnc.rfb.PixelBuffer;
import com.bjhit.martin.vnc.rfb.PixelFormat;

abstract public class PlatformPixelBuffer extends PixelBuffer {
	protected static Toolkit tk = Toolkit.getDefaultToolkit();
	protected Image image;
	int nColours;
	byte[] reds;
	byte[] greens;
	byte[] blues;
	CConn cc;
	DesktopWindow desktop;
	static LogWriter vlog = new LogWriter("PlatformPixelBuffer");

	public PlatformPixelBuffer(int w, int h, CConn cc_, DesktopWindow desktop_) {
		this.cc = cc_;
		this.desktop = desktop_;
		PixelFormat nativePF = getNativePF();
		if (nativePF.depth > this.cc.serverPF.depth)
			setPF(this.cc.serverPF);
		else {
			setPF(nativePF);
		}
		resize(w, h);
	}

	public abstract void resize(int paramInt1, int paramInt2);

	public PixelFormat getNativePF() {
		this.cm = tk.getColorModel();
		PixelFormat pf;
		if (this.cm.getColorSpace().getType() == 5) {
			int depth = (this.cm.getPixelSize() > 24) ? 24 : this.cm.getPixelSize();
			int bpp = (depth > 16) ? 32 : (depth > 8) ? 16 : 8;
			ByteOrder byteOrder = ByteOrder.nativeOrder();
			boolean bigEndian = byteOrder == ByteOrder.BIG_ENDIAN;
			boolean trueColour = depth > 8;
			int redShift = this.cm.getComponentSize()[0] + this.cm.getComponentSize()[1];
			int greenShift = this.cm.getComponentSize()[0];
			int blueShift = 0;
			pf = new PixelFormat(bpp, depth, bigEndian, trueColour, (depth > 8) ? 255 : 0, (depth > 8) ? 255 : 0, (depth > 8) ? 255 : 0, (depth > 8) ? redShift : 0, (depth > 8) ? greenShift : 0,
					(depth > 8) ? blueShift : 0);
		} else {
			pf = new PixelFormat(8, 8, false, false, 7, 7, 3, 0, 3, 6);
		}
		vlog.debug("Native pixel format is " + pf.print());
		return pf;
	}

	public abstract void imageRect(int paramInt1, int paramInt2, int paramInt3, int paramInt4, Object paramObject);

	public void setColourMapEntries(int firstColour, int nColours_, int[] rgbs) {
		this.nColours = nColours_;
		this.reds = new byte[this.nColours];
		this.blues = new byte[this.nColours];
		this.greens = new byte[this.nColours];
		for (int i = 0; i < this.nColours; ++i) {
			this.reds[(firstColour + i)] = (byte) (rgbs[(i * 3)] >> 8);
			this.greens[(firstColour + i)] = (byte) (rgbs[(i * 3 + 1)] >> 8);
			this.blues[(firstColour + i)] = (byte) (rgbs[(i * 3 + 2)] >> 8);
		}
	}

	public void updateColourMap() {
		this.cm = new IndexColorModel(8, this.nColours, this.reds, this.greens, this.blues);
	}

	public abstract Image getImage();
}
