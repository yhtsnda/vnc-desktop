/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
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

//
// PixelBuffer - note that this code is only written for the 8, 16, and 32 bpp cases at the
// moment.
//

package com.bjhit.martin.vnc.rfb;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;

import com.bjhit.martin.vnc.exception.PixelFormatException;

public class PixelBuffer {

	public PixelBuffer() {
		setPF(new PixelFormat());
	}

	public void setPF(PixelFormat pf) {
		if (!(pf.bpp == 32) && !(pf.bpp == 16) && !(pf.bpp == 8))
			throw new PixelFormatException("Internal error: bpp must be 8, 16, or 32 in PixelBuffer (" + pf.bpp + ")");
		format = pf;
		switch (pf.depth) {
		case 3:
			// Fall-through to depth 8
		case 6:
			// Fall-through to depth 8
		case 8:
			if (!pf.trueColour) {
				if (cm == null)
					cm = new IndexColorModel(8, 256, new byte[256], new byte[256], new byte[256]);
				break;
			}
			int rmask = pf.redMax << pf.redShift;
			int gmask = pf.greenMax << pf.greenShift;
			int bmask = pf.blueMax << pf.blueShift;
			cm = new DirectColorModel(8, rmask, gmask, bmask);
			break;
		case 16:
			cm = new DirectColorModel(32, 0xF800, 0x07C0, 0x003E);
			break;
		case 24:
			cm = new DirectColorModel(32, (0xff << 16), (0xff << 8), 0xff);
			break;
		case 32:
			cm = new DirectColorModel(32, (0xff << pf.redShift), (0xff << pf.greenShift), (0xff << pf.blueShift));
			break;
		default:
			throw new PixelFormatException("Unsupported color depth (" + pf.depth + ")");
		}
	}

	public PixelFormat getPF() {
		return format;
	}

	public final int width() {
		return width_;
	}

	public final int height() {
		return height_;
	}

	public final int area() {
		return width_ * height_;
	}

	public void fillRect(int x, int y, int w, int h, int pix) {
		for (int ry = y; ry < y + h; ry++)
			for (int rx = x; rx < x + w; rx++)
				data[ry * width_ + rx] = pix;
	}

	public void imageRect(int x, int y, int w, int h, int[] pix) {
		for (int j = 0; j < h; j++)
			System.arraycopy(pix, (w * j), data, width_ * (y + j) + x, w);
	}

	public void copyRect(int x, int y, int w, int h, int srcX, int srcY) {
		int dest = (width_ * y) + x;
		int src = (width_ * srcY) + srcX;
		int inc = width_;

		if (y > srcY) {
			src += (h - 1) * inc;
			dest += (h - 1) * inc;
			inc = -inc;
		}
		int destEnd = dest + h * inc;

		while (dest != destEnd) {
			System.arraycopy(data, src, data, dest, w);
			src += inc;
			dest += inc;
		}
	}

	public void maskRect(int x, int y, int w, int h, int[] pix, byte[] mask) {
		int maskBytesPerRow = (w + 7) / 8;

		for (int j = 0; j < h; j++) {
			int cy = y + j;

			if (cy < 0 || cy >= height_)
				continue;

			for (int i = 0; i < w; i++) {
				int cx = x + i;

				if (cx < 0 || cx >= width_)
					continue;

				int byte_ = j * maskBytesPerRow + i / 8;
				int bit = 7 - i % 8;

				if ((mask[byte_] & (1 << bit)) != 0)
					data[cy * width_ + cx] = pix[j * w + i];
			}
		}
	}

	public int[] data;
	public ColorModel cm;

	protected PixelFormat format;
	protected int width_, height_;
}
