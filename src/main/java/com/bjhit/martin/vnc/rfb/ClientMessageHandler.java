/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2009-2011 Pierre Ossman for Cendio AB
 * Copyright (C) 2011 D. R. Commander.  All Rights Reserved.
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

//
// CMsgHandler
//

package com.bjhit.martin.vnc.rfb;

import com.bjhit.martin.vnc.common.LogWriter;

abstract public class ClientMessageHandler {
	public ConnParams cp;
	static LogWriter vlog = new LogWriter("CMsgHandler");

	public ClientMessageHandler() {
		this.cp = new ConnParams();
	}

	public void setDesktopSize(int width, int height) {
		this.cp.width = width;
		this.cp.height = height;
	}

	public void setExtendedDesktopSize(int reason, int result, int width, int height, ScreenSet layout) {
		this.cp.supportsSetDesktopSize = true;

		if ((reason == 1) && (result != 0)) {
			return;
		}
		if (!(layout.validate(width, height))) {
			vlog.error("Server sent us an invalid screen layout");
		}
		this.cp.width = width;
		this.cp.height = height;
		this.cp.screenLayout = layout;
	}

	public void setPixelFormat(PixelFormat pf) {
		this.cp.setPF(pf);
	}

	public void setName(String name) {
		this.cp.setName(name);
	}

	public void fence(int flags, int len, byte[] data) {
		this.cp.supportsFence = true;
	}

	public void endOfContinuousUpdates() {
		this.cp.supportsContinuousUpdates = true;
	}

	public void clientRedirect(int port, String host, String x509subject) {
	}

	public void setCursor(int width, int height, Point hotspot, int[] data, byte[] mask) {
	}

	public void serverInit() {
	}

	public void framebufferUpdateStart() {
	}

	public void framebufferUpdateEnd(boolean imageIsUpdate) {
	}

	public void beginRect(Rect r, int encoding) {
	}

	public void endRect(Rect r, int encoding) {
	}

	public void setColourMapEntries(int firstColour, int nColours, int[] rgbs) {
	}

	public void bell() {
	}

	public void serverCutText(String str, int len) {
	}

	public void fillRect(Rect r, int pix) {
	}

	public void imageRect(Rect r, Object pixels) {
	}

	public void copyRect(Rect r, int srcX, int srcY) {
	}

	public abstract PixelFormat getPreferredPF();

	public abstract void refreshFramebuffer();
}
