/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2009-2011 Pierre Ossman <ossman@cendio.se> for Cendio AB
 * Copyright (C) 2011-2013 D. R. Commander.  All Rights Reserved.
 * Copyright (C) 2011-2013 Brian P. Hinz
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
// CConn
//
// Methods on CConn are called from both the GUI thread and the thread which
// processes incoming RFB messages ("the RFB thread").  This means we need to
// be careful with synchronization here.
//
// Any access to writer() must not only be synchronized, but we must also make
// sure that the connection is in RFBSTATE_NORMAL.  We are guaranteed this for
// any code called after serverInit() has been called.  Since the DesktopWindow
// isn't created until then, any methods called only from DesktopWindow can
// assume that we are in RFBSTATE_NORMAL.

package com.bjhit.martin.vnc.client;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.DefaultVncValue;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.io.MemInStream;
import com.bjhit.martin.vnc.io.Socket;
import com.bjhit.martin.vnc.io.TcpSocket;
import com.bjhit.martin.vnc.record.FrameCache;
import com.bjhit.martin.vnc.record.FrameInfo;
import com.bjhit.martin.vnc.record.RecordTask;
import com.bjhit.martin.vnc.record.RecordTimer;
import com.bjhit.martin.vnc.record.StopRecordThread;
import com.bjhit.martin.vnc.rfb.CConnection;
import com.bjhit.martin.vnc.rfb.Encodings;
import com.bjhit.martin.vnc.rfb.FenceTypes;
import com.bjhit.martin.vnc.rfb.PixelFormat;
import com.bjhit.martin.vnc.rfb.Point;
import com.bjhit.martin.vnc.rfb.Rect;
import com.bjhit.martin.vnc.rfb.Screen;
import com.bjhit.martin.vnc.rfb.ScreenSet;
import com.bjhit.martin.vnc.rfb.ScreenTypes;
import com.bjhit.martin.vnc.rfb.Security;
import com.bjhit.martin.vnc.rfb.UnicodeToKeysym;
import com.bjhit.martin.vnc.rfb.VncAuth;

public class CConn extends CConnection implements UserPasswdGetter, OptionsDialogCallback {

	static LogWriter vlog = new LogWriter("CConn");
	static final PixelFormat verylowColourPF = new PixelFormat(8, 3, false, true, 1, 1, 1, 2, 1, 0);
	static final PixelFormat lowColourPF = new PixelFormat(8, 6, false, true, 3, 3, 3, 4, 2, 0);
	static final PixelFormat mediumColourPF = new PixelFormat(8, 8, false, false, 7, 7, 3, 0, 3, 6);
	static final int KEY_LOC_SHIFT_R = 0;
	static final int KEY_LOC_SHIFT_L = 16;
	static final int SUPER_MASK = 1 << 15;

	private VncDesktop viewport;
	Socket sock;
	public static UserPasswdGetter upg;
	private boolean shuttingDown = false;
	int lowColourLevel;
	// public F8Menu menu;
	public OptionsDialog options;
	int buttonMask;
	private DesktopWindow desktop;
	public PixelFormat serverPF;
	private PixelFormat fullColourPF;
	private boolean pendingPFChange;
	private PixelFormat pendingPF;
	private int currentEncoding;
	private boolean formatChange;
	private boolean encodingChange;
	private boolean firstUpdate;
	private boolean pendingUpdate;
	private boolean continuousUpdates;
	private boolean forceNonincremental;
	private boolean supportsSyncFence;
	int modifiers;
	public int menuKeyCode;
	private boolean fullColour;
	private boolean autoSelect;
	public boolean fullScreen;

	private boolean control;

	private DefaultVncValue defaultVncValue;
	// 录屏
	private FrameCache frameCache = new FrameCache();
	private RecordTimer timer;
	private RecordTask recordTask;

	public final PixelFormat getPreferredPF() {
		return fullColourPF;
	}

	public CConn(ConnectionInfo connInfo) {
		setConnInfo(connInfo);
		defaultVncValue = new DefaultVncValue();
		pendingPFChange = false;
		currentEncoding = Encodings.encodingZRLE;
		fullColour = defaultVncValue.fullColour.getValue();
		lowColourLevel = defaultVncValue.lowColourLevel.getValue();
		autoSelect = defaultVncValue.autoSelect.getValue();
		formatChange = false;
		encodingChange = false;
		fullScreen = defaultVncValue.fullScreen.getValue();
		menuKeyCode = MenuKey.getMenuKeyCode();
		options = new OptionsDialog(this);
		options.initDialog();

		firstUpdate = true;
		pendingUpdate = false;
		continuousUpdates = false;
		forceNonincremental = true;
		supportsSyncFence = false;

		setShared(true);
		upg = this;
		// msg = this;

		String encStr = defaultVncValue.preferredEncoding.getValue();
		int encNum = Encodings.encodingNum(encStr);
		if (encNum != -1) {
			currentEncoding = encNum;
		}
		cp.supportsDesktopResize = true;
		cp.supportsExtendedDesktopSize = true;
		cp.supportsSetDesktopSize = true;
		cp.supportsClientRedirect = true;
		cp.supportsDesktopRename = true;
		cp.supportsLocalCursor = defaultVncValue.useLocalCursor.getValue();
		cp.customCompressLevel = defaultVncValue.customCompressLevel.getValue();
		cp.compressLevel = defaultVncValue.compressLevel.getValue();
		cp.noJpeg = defaultVncValue.noJpeg.getValue();
		cp.qualityLevel = defaultVncValue.qualityLevel.getValue();
		// initMenu();
		try {
			vlog.debug(connInfo.toString());
			sock = new TcpSocket(connInfo);
			vlog.debug(sock + "");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		vlog.info("connected to host " + connInfo.getHost() + " port " + connInfo.getPort());

		setServerName(connInfo.getHost());
		setStreams(sock.inStream(), sock.outStream());
		initialiseProtocol();
	}

	public void refreshFramebuffer() {
		forceNonincremental = true;

		if (supportsSyncFence)
			requestNewUpdate();
	}

	public boolean showMsgBox(int flags, String title, String text) {
		return true;
	}

	public final boolean getUserPasswd(StringBuffer user, StringBuffer passwd) {
		String title = "VNC Authentication [" + csecurity.description() + "]";
		String passwordFileStr = defaultVncValue.passwordFile.getValue();

		if ((user == null) && (passwordFileStr != "")) {
			InputStream fp = null;
			try {
				fp = new FileInputStream(passwordFileStr);
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Opening password file failed");
			}
			byte[] obfPwd = new byte[256];
			try {
				fp.read(obfPwd);
				fp.close();
			} catch (IOException e) {
				throw new RuntimeException("Failed to read VncPasswd file");
			}
			String PlainPasswd = VncAuth.unobfuscatePasswd(obfPwd);
			passwd.append(PlainPasswd);
			passwd.setLength(PlainPasswd.length());
			return true;
		}
		PasswdDialog dlg;
		if (user == null) {
			dlg = new PasswdDialog(title, user == null, passwd == null);
		} else {
			if ((passwd == null) && (defaultVncValue.sendLocalUsername.getValue())) {
				user.append((String) System.getProperties().get("user.name"));
				return true;
			}
			dlg = new PasswdDialog(title, defaultVncValue.sendLocalUsername.getValue(), passwd == null);
		}
		if (!(dlg.showDialog()))
			return false;
		if (user != null) {
			if (defaultVncValue.sendLocalUsername.getValue())
				user.append((String) System.getProperties().get("user.name"));
			else {
				user.append(dlg.userEntry.getText());
			}
		}
		if (passwd != null)
			passwd.append(new String(dlg.passwdEntry.getPassword()));
		return true;
	}

	public void serverInit() {
		super.serverInit();

		if ((cp.beforeVersion(3, 8)) && (autoSelect)) {
			fullColour = true;
		}
		serverPF = cp.pf();

		desktop = new DesktopWindow(cp.width, cp.height, serverPF, this);
		fullColourPF = desktop.getPreferredPF();

		formatChange = true;
		encodingChange = true;

		requestNewUpdate();

		assert (pendingPFChange);
		desktop.setServerPF(pendingPF);
		cp.setPF(pendingPF);
		pendingPFChange = false;

		recreateViewport();
	}

	public void setDesktopSize(int w, int h) {
		super.setDesktopSize(w, h);
		forceNonincremental = true;
		resizeFramebuffer();
	}

	public void setExtendedDesktopSize(int reason, int result, int w, int h, ScreenSet layout) {
		super.setExtendedDesktopSize(reason, result, w, h, layout);

		if ((reason == ScreenTypes.reasonClient) && (result != ScreenTypes.resultSuccess)) {
			vlog.error("SetDesktopSize failed: " + result);
			return;
		}

		resizeFramebuffer();
	}

	public void setName(String name) {
		super.setName(name);
	}

	public void framebufferUpdateStart() {
		pendingUpdate = false;
		requestNewUpdate();
	}

	private FrameInfo frameInfo;
	private long preTime;

	public void framebufferUpdateEnd(boolean imageIsUpdate) {
		desktop.updateWindow();
		if (imageIsUpdate || System.currentTimeMillis() - preTime >= 400) {
			preTime = System.currentTimeMillis();
			addImageForRecord();
		}
		if (firstUpdate) {
			if (cp.supportsFence) {
				writer().writeFence(FenceTypes.fenceFlagRequest | FenceTypes.fenceFlagSyncNext, 0, null);
			}
			if ((cp.supportsSetDesktopSize) && (defaultVncValue.desktopSize.getValue() != null) && (defaultVncValue.desktopSize.getValue().split("x").length == 2)) {
				int width = Integer.parseInt(defaultVncValue.desktopSize.getValue().split("x")[0]);
				int height = Integer.parseInt(defaultVncValue.desktopSize.getValue().split("x")[1]);
				ScreenSet layout = cp.screenLayout;
				if (layout.num_screens() == 0)
					layout.add_screen(new Screen());
				else if (layout.num_screens() != 1) {
					while (true) {
						Iterator<Screen> iter = layout.screens.iterator();
						Screen screen = iter.next();
						if (!(iter.hasNext())) {
							break;
						}
						layout.remove_screen(screen.id);
					}
				}

				Screen screen0 = (Screen) layout.screens.iterator().next();
				screen0.dimensions.tl.x = 0;
				screen0.dimensions.tl.y = 0;
				screen0.dimensions.br.x = width;
				screen0.dimensions.br.y = height;

				writer().writeSetDesktopSize(width, height, layout);
			}
			if (viewport != null) {// 有界面则设置一些按键为可用状态
				viewport.setRecordEnable(true);
				viewport.setCltAltDelEnable(control);
			}
			firstUpdate = false;
		}

		if (pendingPFChange) {
			desktop.setServerPF(pendingPF);
			cp.setPF(pendingPF);
			pendingPFChange = false;
		}

		if (autoSelect)
			autoSelectFormatAndEncoding();
	}

	public void setColourMapEntries(int firstColour, int nColours, int[] rgbs) {
		desktop.setColourMapEntries(firstColour, nColours, rgbs);
	}

	public void bell() {
		if (defaultVncValue.acceptBell.getValue())
			desktop.getToolkit().beep();
	}

	public void serverCutText(String str, int len) {
		SecurityManager sm = System.getSecurityManager();
		try {
			if (sm != null){
				sm.checkSystemClipboardAccess();
			}
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (cb != null) {
				StringSelection ss = new StringSelection(str);
				try {
					cb.setContents(ss, ss);
				} catch (Exception e) {
					vlog.debug(e.toString());
				}
			}
		} catch (SecurityException e) {
			vlog.error("Cannot access the system clipboard");
		}
	}

	public void beginRect(Rect r, int encoding) {
		sock.inStream().startTiming();
	}

	public void endRect(Rect r, int encoding) {
		sock.inStream().stopTiming();
	}

	public void fillRect(Rect r, int p) {
		desktop.fillRect(r.tl.x, r.tl.y, r.width(), r.height(), p);
	}

	public void imageRect(Rect r, Object p) {
		desktop.imageRect(r.tl.x, r.tl.y, r.width(), r.height(), p);
	}

	public void copyRect(Rect r, int sx, int sy) {
		desktop.copyRect(r.tl.x, r.tl.y, r.width(), r.height(), sx, sy);
	}

	public void setCursor(int width, int height, Point hotspot, int[] data, byte[] mask) {
		desktop.setCursor(width, height, hotspot, data, mask);
	}

	public void fence(int flags, int len, byte[] data) {
		cp.supportsFence = true;

		if ((flags & FenceTypes.fenceFlagRequest) != 0) {
			flags = flags & (FenceTypes.fenceFlagBlockBefore | FenceTypes.fenceFlagBlockAfter);
			writer().writeFence(flags, len, data);
			return;
		}

		if (len == 0) {
			if ((flags & FenceTypes.fenceFlagSyncNext) != 0) {
				supportsSyncFence = true;

				if (cp.supportsContinuousUpdates) {
					vlog.info("Enabling continuous updates");
					continuousUpdates = true;
					writer().writeEnableContinuousUpdates(true, 0, 0, cp.width, cp.height);
				}
			}
		} else {
			MemInStream memStream = new MemInStream(data, 0, len);
			PixelFormat pf = new PixelFormat();

			pf.read(memStream);

			desktop.setServerPF(pf);
			cp.setPF(pf);
		}
	}

	private void resizeFramebuffer() {
		if (desktop == null) {
			return;
		}
		if (continuousUpdates) {
			writer().writeEnableContinuousUpdates(true, 0, 0, cp.width, cp.height);
		}
		if ((cp.width == 0) && (cp.height == 0))
			return;
		if ((desktop.width() == cp.width) && (desktop.height() == cp.height)) {
			return;
		}
		desktop.resize();
		recreateViewport();
	}

	private void recreateViewport() {
		if (viewport == null) {
			return;
		}
		if (viewport != null) {
			viewport.setVisible(false);

		}
		viewport.setSize(desktop.getWidth(), desktop.getHeight());
		viewport.getScrollPane().getViewport().setView(desktop);
		reconfigureViewport();
		if ((cp.width > 0) && (cp.height > 0))
			viewport.setVisible(true);
		desktop.requestFocusInWindow();
	}

	private void reconfigureViewport() {
		boolean pack = true;
		Dimension dpySize = viewport.getToolkit().getScreenSize();
		desktop.setScaledSize();
		int w = desktop.scaledWidth;
		int h = desktop.scaledHeight;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		if (fullScreen) {
			return;
		}

		int wmDecorationWidth = viewport.getInsets().left + viewport.getInsets().right;
		int wmDecorationHeight = viewport.getInsets().top + viewport.getInsets().bottom;
		if (w + wmDecorationWidth >= dpySize.width) {
			w = dpySize.width - wmDecorationWidth;
			pack = false;
		}
		if (h + wmDecorationHeight >= dpySize.height) {
			h = dpySize.height - wmDecorationHeight;
			pack = false;
		}
		if (gd.isFullScreenSupported())
			gd.setFullScreenWindow(null);
	}

	private void autoSelectFormatAndEncoding() {
		long kbitsPerSecond = sock.inStream().kbitsPerSecond();
		long timeWaited = sock.inStream().timeWaited();
		boolean newFullColour = fullColour;
		int newQualityLevel = cp.qualityLevel;

		if (currentEncoding != Encodings.encodingZRLE) {
			currentEncoding = Encodings.encodingZRLE;
			encodingChange = true;
		}

		if ((kbitsPerSecond == 0) || (timeWaited < 100)) {
			return;
		}

		if (!(cp.noJpeg)) {
			if (kbitsPerSecond > 16000)
				newQualityLevel = 8;
			else {
				newQualityLevel = 6;
			}
			if (newQualityLevel != cp.qualityLevel) {
				vlog.info("Throughput " + kbitsPerSecond + " kbit/s - changing to quality " + newQualityLevel);
				cp.qualityLevel = newQualityLevel;
				defaultVncValue.qualityLevel.setParam(Integer.toString(newQualityLevel));
				encodingChange = true;
			}
		}

		if (cp.beforeVersion(3, 8)) {
			return;
		}

		newFullColour = kbitsPerSecond > 256;
		if (newFullColour != fullColour) {
			vlog.info("Throughput " + kbitsPerSecond + " kbit/s - full color is now " + ((newFullColour) ? "enabled" : "disabled"));
			fullColour = newFullColour;
			formatChange = true;
			forceNonincremental = true;
		}
	}

	private void requestNewUpdate() {
		if (formatChange) {
			assert ((!(pendingUpdate)) || (supportsSyncFence));
			PixelFormat pf;
			if (fullColour) {
				pf = fullColourPF;
			} else {
				if (lowColourLevel == 0) {
					pf = verylowColourPF;
				} else {
					if (lowColourLevel == 1)
						pf = lowColourPF;
					else {
						pf = mediumColourPF;
					}
				}
			}
			if (supportsSyncFence) {
				byte[] buff = pf.getPixelFormatByte();
				writer().writeFence(FenceTypes.fenceFlagRequest | FenceTypes.fenceFlagSyncNext, buff.length, buff);
			} else {
				pendingPFChange = true;
				pendingPF = pf;
			}

			String str = pf.print();
			vlog.info("Using pixel format " + str);
			writer().writeSetPixelFormat(pf);

			formatChange = false;
		}

		checkEncodings();
		synchronized (this) {//休息30毫秒后再请求帧
			try {
				this.wait(30);
			} catch (InterruptedException e) {
				vlog.error(e.getMessage());
			}
		}
		if ((forceNonincremental) || (!(continuousUpdates))) {
			pendingUpdate = true;
			writer().writeFramebufferUpdateRequest(new Rect(0, 0, cp.width, cp.height), !(forceNonincremental));
		}
		forceNonincremental = false;
	}

	public void close() {
		shuttingDown = true;
		try {
			if (sock != null) {
				sock.shutdown();
				sock.close();
			}
			vlog.info("close socket :" + getConnInfo().getHost() + " port " + getConnInfo().getPort());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void refresh() {
		writer().writeFramebufferUpdateRequest(new Rect(0, 0, cp.width, cp.height), false);
		pendingUpdate = true;
	}

	public void setOptions() {
		options.autoSelect.setSelected(autoSelect);
		options.fullColour.setSelected(fullColour);
		options.lowColour.setSelected((!(fullColour)) && (lowColourLevel == 1));
		options.mediumColour.setSelected((!(fullColour)) && (lowColourLevel == 2));
		options.tight.setSelected(currentEncoding == Encodings.encodingTight);
		options.zrle.setSelected(currentEncoding == Encodings.encodingZRLE);
		options.hextile.setSelected(currentEncoding == Encodings.encodingHextile);
		options.raw.setSelected(currentEncoding == Encodings.encodingRaw);

		options.customCompressLevel.setSelected(defaultVncValue.customCompressLevel.getValue());
		int digit = 0 + defaultVncValue.compressLevel.getValue();
		if ((digit >= 0) && (digit <= 9))
			options.compressLevel.setSelectedItem(Integer.valueOf(digit));
		else {
			options.compressLevel.setSelectedItem(Integer.valueOf(Integer.parseInt(defaultVncValue.compressLevel.getDefaultStr())));
		}
		options.noJpeg.setSelected(!(defaultVncValue.noJpeg.getValue()));
		digit = 0 + defaultVncValue.qualityLevel.getValue();
		if ((digit >= 0) && (digit <= 9))
			options.qualityLevel.setSelectedItem(Integer.valueOf(digit));
		else {
			options.qualityLevel.setSelectedItem(Integer.valueOf(Integer.parseInt(defaultVncValue.qualityLevel.getDefaultStr())));
		}

		options.viewOnly.setSelected(defaultVncValue.viewOnly.getValue());
		options.acceptClipboard.setSelected(defaultVncValue.acceptClipboard.getValue());
		options.sendClipboard.setSelected(defaultVncValue.sendClipboard.getValue());
		options.menuKey.setSelectedItem(KeyEvent.getKeyText(MenuKey.getMenuKeyCode()));
		options.sendLocalUsername.setSelected(defaultVncValue.sendLocalUsername.getValue());

		if (state() == RFBSTATE_NORMAL) {
			options.shared.setEnabled(false);
			options.secVeNCrypt.setEnabled(false);
			options.encNone.setEnabled(false);
			options.encTLS.setEnabled(false);
			options.encX509.setEnabled(false);
			options.ca.setEnabled(false);
			options.crl.setEnabled(false);
			options.secIdent.setEnabled(false);
			options.secNone.setEnabled(false);
			options.secVnc.setEnabled(false);
			options.secPlain.setEnabled(false);
			options.sendLocalUsername.setEnabled(false);
			options.cfLoadButton.setEnabled(false);
			options.cfSaveAsButton.setEnabled(true);
		} else {
			options.shared.setSelected(defaultVncValue.shared.getValue());
			options.sendLocalUsername.setSelected(defaultVncValue.sendLocalUsername.getValue());
			options.cfSaveAsButton.setEnabled(false);

			List<Integer> secTypes = new ArrayList<Integer>();
			secTypes = Security.GetEnabledSecTypes();
			for (Iterator<Integer> i = secTypes.iterator(); i.hasNext();) {
				switch (((Integer) i.next()).intValue()) {
				case 19:
					options.secVeNCrypt.setSelected(UserPreferences.getBool("viewer", "secVeNCrypt", true));
					break;
				case 1:
					options.encNone.setSelected(true);
					options.secNone.setSelected(UserPreferences.getBool("viewer", "secTypeNone", true));
					break;
				case 2:
					options.encNone.setSelected(true);
					options.secVnc.setSelected(UserPreferences.getBool("viewer", "secTypeVncAuth", true));
				}

			}

			if (options.secVeNCrypt.isSelected()) {
				List<Integer> secTypesExt = new ArrayList<Integer>();
				secTypesExt = Security.GetEnabledExtSecTypes();
				for (Iterator<Integer> iext = secTypesExt.iterator(); iext.hasNext();)
					switch (((Integer) iext.next()).intValue()) {
					case Security.secTypePlain:
						options.encNone.setSelected(UserPreferences.getBool("viewer", "encNone", true));
						options.secPlain.setSelected(UserPreferences.getBool("viewer", "secPlain", true));
						break;
					case Security.secTypeIdent:
						options.encNone.setSelected(UserPreferences.getBool("viewer", "encNone", true));
						options.secIdent.setSelected(UserPreferences.getBool("viewer", "secIdent", true));
						break;
					case Security.secTypeTLSNone:
						options.encTLS.setSelected(UserPreferences.getBool("viewer", "encTLS", true));
						options.secNone.setSelected(UserPreferences.getBool("viewer", "secNone", true));
						break;
					case Security.secTypeTLSVnc:
						options.encTLS.setSelected(UserPreferences.getBool("viewer", "encTLS", true));
						options.secVnc.setSelected(UserPreferences.getBool("viewer", "secVnc", true));
						break;
					case Security.secTypeTLSPlain:
						options.encTLS.setSelected(UserPreferences.getBool("viewer", "encTLS", true));
						options.secPlain.setSelected(UserPreferences.getBool("viewer", "secPlain", true));
						break;
					case Security.secTypeTLSIdent:
						options.encTLS.setSelected(UserPreferences.getBool("viewer", "encTLS", true));
						options.secIdent.setSelected(UserPreferences.getBool("viewer", "secIdent", true));
						break;
					case Security.secTypeX509None:
						options.encX509.setSelected(UserPreferences.getBool("viewer", "encX509", true));
						options.secNone.setSelected(UserPreferences.getBool("viewer", "secNone", true));
						break;
					case Security.secTypeX509Vnc:
						options.encX509.setSelected(UserPreferences.getBool("viewer", "encX509", true));
						options.secVnc.setSelected(UserPreferences.getBool("viewer", "secVnc", true));
						break;
					case Security.secTypeX509Plain:
						options.encX509.setSelected(UserPreferences.getBool("viewer", "encX509", true));
						options.secPlain.setSelected(UserPreferences.getBool("viewer", "secPlain", true));
						break;
					case Security.secTypeX509Ident:
						options.encX509.setSelected(UserPreferences.getBool("viewer", "encX509", true));
						options.secIdent.setSelected(UserPreferences.getBool("viewer", "secIdent", true));
						break;
					}
			}
			options.encNone.setEnabled(options.secVeNCrypt.isSelected());
			options.encTLS.setEnabled(options.secVeNCrypt.isSelected());
			options.encX509.setEnabled(options.secVeNCrypt.isSelected());
			options.ca.setEnabled(options.secVeNCrypt.isSelected());
			options.crl.setEnabled(options.secVeNCrypt.isSelected());
			options.secIdent.setEnabled(options.secVeNCrypt.isSelected());
			options.secPlain.setEnabled(options.secVeNCrypt.isSelected());
			options.sendLocalUsername.setEnabled((options.secPlain.isSelected()) || (options.secIdent.isSelected()));
		}

		options.fullScreen.setSelected(fullScreen);
		options.useLocalCursor.setSelected(defaultVncValue.useLocalCursor.getValue());
		options.acceptBell.setSelected(defaultVncValue.acceptBell.getValue());
		String scaleString = defaultVncValue.scalingFactor.getValue();
		if (scaleString.equalsIgnoreCase("Auto")) {
			options.scalingFactor.setSelectedItem("Auto");
		} else if (scaleString.equalsIgnoreCase("FixedRatio")) {
			options.scalingFactor.setSelectedItem("Fixed Aspect Ratio");
		} else {
			digit = Integer.parseInt(scaleString);
			if ((digit >= 1) && (digit <= 1000)) {
				options.scalingFactor.setSelectedItem(digit + "%");
			} else {
				digit = Integer.parseInt(defaultVncValue.scalingFactor.getDefaultStr());
				options.scalingFactor.setSelectedItem(digit + "%");
			}
			int scaleFactor = Integer.parseInt(scaleString.substring(0, scaleString.length()));
			if (desktop != null)
				desktop.setScaledSize();
		}
	}

	public void getOptions() {
		autoSelect = options.autoSelect.isSelected();
		if (fullColour != options.fullColour.isSelected()) {
			formatChange = true;
			forceNonincremental = true;
		}
		fullColour = options.fullColour.isSelected();
		if (!fullColour) {
			int newLowColourLevel = (options.lowColour.isSelected() ? 1 : 2);
			if (newLowColourLevel != lowColourLevel) {
				lowColourLevel = newLowColourLevel;
				formatChange = true;
				forceNonincremental = true;
			}
		}
		int newEncoding = (options.zrle.isSelected() ? Encodings.encodingZRLE : options.hextile.isSelected() ? Encodings.encodingHextile : options.tight.isSelected() ? Encodings.encodingTight
				: Encodings.encodingRaw);
		if (newEncoding != currentEncoding) {
			currentEncoding = newEncoding;
			encodingChange = true;
		}

		defaultVncValue.customCompressLevel.setParam(options.customCompressLevel.isSelected());
		if (cp.customCompressLevel != defaultVncValue.customCompressLevel.getValue()) {
			cp.customCompressLevel = defaultVncValue.customCompressLevel.getValue();
			encodingChange = true;
		}
		if (Integer.parseInt(options.compressLevel.getSelectedItem().toString()) >= 0 && Integer.parseInt(options.compressLevel.getSelectedItem().toString()) <= 9) {
			defaultVncValue.compressLevel.setParam(options.compressLevel.getSelectedItem().toString());
		} else {
			defaultVncValue.compressLevel.setParam(defaultVncValue.compressLevel.getDefaultStr());
		}
		if (cp.compressLevel != defaultVncValue.compressLevel.getValue()) {
			cp.compressLevel = defaultVncValue.compressLevel.getValue();
			encodingChange = true;
		}
		defaultVncValue.noJpeg.setParam(!options.noJpeg.isSelected());
		if (cp.noJpeg != defaultVncValue.noJpeg.getValue()) {
			cp.noJpeg = defaultVncValue.noJpeg.getValue();
			encodingChange = true;
		}
		defaultVncValue.qualityLevel.setParam(options.qualityLevel.getSelectedItem().toString());
		if (cp.qualityLevel != defaultVncValue.qualityLevel.getValue()) {
			cp.qualityLevel = defaultVncValue.qualityLevel.getValue();
			encodingChange = true;
		}
		defaultVncValue.sendLocalUsername.setParam(options.sendLocalUsername.isSelected());

		defaultVncValue.viewOnly.setParam(options.viewOnly.isSelected());
		defaultVncValue.acceptClipboard.setParam(options.acceptClipboard.isSelected());
		defaultVncValue.sendClipboard.setParam(options.sendClipboard.isSelected());
		defaultVncValue.acceptBell.setParam(options.acceptBell.isSelected());
		String scaleString = options.scalingFactor.getSelectedItem().toString();
		String oldScaleFactor = defaultVncValue.scalingFactor.getValue();
		if (scaleString.equalsIgnoreCase("Fixed Aspect Ratio")) {
			scaleString = new String("FixedRatio");
		} else if (scaleString.equalsIgnoreCase("Auto")) {
			scaleString = new String("Auto");
		} else {
			scaleString = scaleString.substring(0, scaleString.length() - 1);
		}
		if (!oldScaleFactor.equals(scaleString)) {
			defaultVncValue.scalingFactor.setParam(scaleString);
			if ((options.fullScreen.isSelected() == fullScreen) && (desktop != null))
				recreateViewport();
		}
		setShared(options.shared.isSelected());
		defaultVncValue.useLocalCursor.setParam(options.useLocalCursor.isSelected());
		if (cp.supportsLocalCursor != defaultVncValue.useLocalCursor.getValue()) {
			cp.supportsLocalCursor = defaultVncValue.useLocalCursor.getValue();
			encodingChange = true;
			if (desktop != null)
				desktop.resetLocalCursor();
		}

		checkEncodings();

		if (state() != RFBSTATE_NORMAL) {
			/* Process security types which don't use encryption */
			if (options.encNone.isSelected()) {
				if (options.secNone.isSelected())
					Security.EnableSecType(Security.secTypeNone);
				if (options.secVnc.isSelected())
					Security.EnableSecType(Security.secTypeVncAuth);
				if (options.secPlain.isSelected())
					Security.EnableSecType(Security.secTypePlain);
				if (options.secIdent.isSelected())
					Security.EnableSecType(Security.secTypeIdent);
			} else {
				Security.DisableSecType(Security.secTypeNone);
				Security.DisableSecType(Security.secTypeVncAuth);
				Security.DisableSecType(Security.secTypePlain);
				Security.DisableSecType(Security.secTypeIdent);
			}

			/* Process security types which use TLS encryption */
			if (options.encTLS.isSelected()) {
				if (options.secNone.isSelected())
					Security.EnableSecType(Security.secTypeTLSNone);
				if (options.secVnc.isSelected())
					Security.EnableSecType(Security.secTypeTLSVnc);
				if (options.secPlain.isSelected())
					Security.EnableSecType(Security.secTypeTLSPlain);
				if (options.secIdent.isSelected())
					Security.EnableSecType(Security.secTypeTLSIdent);
			} else {
				Security.DisableSecType(Security.secTypeTLSNone);
				Security.DisableSecType(Security.secTypeTLSVnc);
				Security.DisableSecType(Security.secTypeTLSPlain);
				Security.DisableSecType(Security.secTypeTLSIdent);
			}

			/* Process security types which use X509 encryption */
			if (options.encX509.isSelected()) {
				if (options.secNone.isSelected())
					Security.EnableSecType(Security.secTypeX509None);
				if (options.secVnc.isSelected())
					Security.EnableSecType(Security.secTypeX509Vnc);
				if (options.secPlain.isSelected())
					Security.EnableSecType(Security.secTypeX509Plain);
				if (options.secIdent.isSelected())
					Security.EnableSecType(Security.secTypeX509Ident);
			} else {
				Security.DisableSecType(Security.secTypeX509None);
				Security.DisableSecType(Security.secTypeX509Vnc);
				Security.DisableSecType(Security.secTypeX509Plain);
				Security.DisableSecType(Security.secTypeX509Ident);
			}

			/* Process *None security types */
			if (options.secNone.isSelected()) {
				if (options.encNone.isSelected())
					Security.EnableSecType(Security.secTypeNone);
				if (options.encTLS.isSelected())
					Security.EnableSecType(Security.secTypeTLSNone);
				if (options.encX509.isSelected())
					Security.EnableSecType(Security.secTypeX509None);
			} else {
				Security.DisableSecType(Security.secTypeNone);
				Security.DisableSecType(Security.secTypeTLSNone);
				Security.DisableSecType(Security.secTypeX509None);
			}

			/* Process *Vnc security types */
			if (options.secVnc.isSelected()) {
				if (options.encNone.isSelected())
					Security.EnableSecType(Security.secTypeVncAuth);
				if (options.encTLS.isSelected())
					Security.EnableSecType(Security.secTypeTLSVnc);
				if (options.encX509.isSelected())
					Security.EnableSecType(Security.secTypeX509Vnc);
			} else {
				Security.DisableSecType(Security.secTypeVncAuth);
				Security.DisableSecType(Security.secTypeTLSVnc);
				Security.DisableSecType(Security.secTypeX509Vnc);
			}

			/* Process *Plain security types */
			if (options.secPlain.isSelected()) {
				if (options.encNone.isSelected())
					Security.EnableSecType(Security.secTypePlain);
				if (options.encTLS.isSelected())
					Security.EnableSecType(Security.secTypeTLSPlain);
				if (options.encX509.isSelected())
					Security.EnableSecType(Security.secTypeX509Plain);
			} else {
				Security.DisableSecType(Security.secTypePlain);
				Security.DisableSecType(Security.secTypeTLSPlain);
				Security.DisableSecType(Security.secTypeX509Plain);
			}

			/* Process *Ident security types */
			if (options.secIdent.isSelected()) {
				if (options.encNone.isSelected())
					Security.EnableSecType(Security.secTypeIdent);
				if (options.encTLS.isSelected())
					Security.EnableSecType(Security.secTypeTLSIdent);
				if (options.encX509.isSelected())
					Security.EnableSecType(Security.secTypeX509Ident);
			} else {
				Security.DisableSecType(Security.secTypeIdent);
				Security.DisableSecType(Security.secTypeTLSIdent);
				Security.DisableSecType(Security.secTypeX509Ident);
			}
		}
		if (options.fullScreen.isSelected() ^ fullScreen)
			toggleFullScreen();
	}

	public void toggleFullScreen() {
		fullScreen = (!(fullScreen));
		// menu.fullScreen.setSelected(fullScreen);
		if (viewport != null)
			recreateViewport();
	}

	public void writeClientCutText(String str, int len) {
		if (state() != RFBSTATE_NORMAL || shuttingDown || !control)
			return;
		writer().writeClientCutText(str, len);
	}

	public void writeKeyEvent(int keysym, boolean down) {
		if (state() != RFBSTATE_NORMAL || shuttingDown || !control)
			return;
		writer().writeKeyEvent(keysym, down);
	}

	public void writeKeyEvent(KeyEvent ev, int keysym) {
		if (keysym < 0)
			return;
		String fmt = ev.paramString().replaceAll("%", "%%");
		vlog.debug(String.format(fmt.replaceAll(",", "%n       ")));
		// Windows sends an extra CTRL_L + ALT_R when AltGr is down that need to
		// be suppressed for keyTyped events. In Java 6
		// KeyEvent.isAltGraphDown()
		// is broken for keyPressed/keyReleased events.
		int ALTGR_MASK = ((Event.CTRL_MASK << KEY_LOC_SHIFT_L) | Event.ALT_MASK);
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows") && ((modifiers & ALTGR_MASK) != 0)) {
			writeKeyEvent(Keysyms.Control_L, false);
			writeKeyEvent(Keysyms.Alt_R, false);
			writeKeyEvent(keysym, true);
			writeKeyEvent(keysym, false);
			writeKeyEvent(Keysyms.Control_L, true);
			writeKeyEvent(Keysyms.Alt_R, true);
		} else {
			writeKeyEvent(keysym, true);
			writeKeyEvent(keysym, false);
		}
	}

	public void writeKeyEvent(KeyEvent ev) {
		int keysym = 0, keycode, key, location, locationShift;

		if (shuttingDown)
			return;

		boolean down = (ev.getID() == KeyEvent.KEY_PRESSED);

		keycode = ev.getKeyCode();
		if (keycode == KeyEvent.VK_UNDEFINED)
			return;
		key = ev.getKeyChar();
		location = ev.getKeyLocation();
		if (location == KeyEvent.KEY_LOCATION_RIGHT)
			locationShift = KEY_LOC_SHIFT_R;
		else
			locationShift = KEY_LOC_SHIFT_L;

		if (!ev.isActionKey()) {
			if (keycode >= KeyEvent.VK_0 && keycode <= KeyEvent.VK_9 && location == KeyEvent.KEY_LOCATION_NUMPAD)
				keysym = Keysyms.KP_0 + keycode - KeyEvent.VK_0;

			switch (keycode) {
			case KeyEvent.VK_BACK_SPACE:
				keysym = Keysyms.BackSpace;
				break;
			case KeyEvent.VK_TAB:
				keysym = Keysyms.Tab;
				break;
			case KeyEvent.VK_ENTER:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Enter;
				else
					keysym = Keysyms.Return;
				break;
			case KeyEvent.VK_ESCAPE:
				keysym = Keysyms.Escape;
				break;
			case KeyEvent.VK_NUMPAD0:
				keysym = Keysyms.KP_0;
				break;
			case KeyEvent.VK_NUMPAD1:
				keysym = Keysyms.KP_1;
				break;
			case KeyEvent.VK_NUMPAD2:
				keysym = Keysyms.KP_2;
				break;
			case KeyEvent.VK_NUMPAD3:
				keysym = Keysyms.KP_3;
				break;
			case KeyEvent.VK_NUMPAD4:
				keysym = Keysyms.KP_4;
				break;
			case KeyEvent.VK_NUMPAD5:
				keysym = Keysyms.KP_5;
				break;
			case KeyEvent.VK_NUMPAD6:
				keysym = Keysyms.KP_6;
				break;
			case KeyEvent.VK_NUMPAD7:
				keysym = Keysyms.KP_7;
				break;
			case KeyEvent.VK_NUMPAD8:
				keysym = Keysyms.KP_8;
				break;
			case KeyEvent.VK_NUMPAD9:
				keysym = Keysyms.KP_9;
				break;
			case KeyEvent.VK_DECIMAL:
				keysym = Keysyms.KP_Decimal;
				break;
			case KeyEvent.VK_ADD:
				keysym = Keysyms.KP_Add;
				break;
			case KeyEvent.VK_SUBTRACT:
				keysym = Keysyms.KP_Subtract;
				break;
			case KeyEvent.VK_MULTIPLY:
				keysym = Keysyms.KP_Multiply;
				break;
			case KeyEvent.VK_DIVIDE:
				keysym = Keysyms.KP_Divide;
				break;
			case KeyEvent.VK_DELETE:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Delete;
				else
					keysym = Keysyms.Delete;
				break;
			case KeyEvent.VK_CLEAR:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Begin;
				else
					keysym = Keysyms.Clear;
				break;
			case KeyEvent.VK_CONTROL:
				if (down)
					modifiers |= (Event.CTRL_MASK << locationShift);
				else
					modifiers &= ~(Event.CTRL_MASK << locationShift);
				if (location == KeyEvent.KEY_LOCATION_RIGHT)
					keysym = Keysyms.Control_R;
				else
					keysym = Keysyms.Control_L;
				break;
			case KeyEvent.VK_ALT:
				if (down)
					modifiers |= (Event.ALT_MASK << locationShift);
				else
					modifiers &= ~(Event.ALT_MASK << locationShift);
				if (location == KeyEvent.KEY_LOCATION_RIGHT)
					keysym = Keysyms.Alt_R;
				else
					keysym = Keysyms.Alt_L;
				break;
			case KeyEvent.VK_SHIFT:
				if (down)
					modifiers |= (Event.SHIFT_MASK << locationShift);
				else
					modifiers &= ~(Event.SHIFT_MASK << locationShift);
				if (location == KeyEvent.KEY_LOCATION_RIGHT)
					keysym = Keysyms.Shift_R;
				else
					keysym = Keysyms.Shift_L;
				break;
			case KeyEvent.VK_META:
				if (down)
					modifiers |= (Event.META_MASK << locationShift);
				else
					modifiers &= ~(Event.META_MASK << locationShift);
				if (location == KeyEvent.KEY_LOCATION_RIGHT)
					keysym = Keysyms.Meta_R;
				else
					keysym = Keysyms.Meta_L;
				break;
			default:
				if (ev.isControlDown()) {
					// For CTRL-<letter>, CTRL is sent separately, so just send
					// <letter>.
					if ((key >= 1 && key <= 26 && !ev.isShiftDown()) ||
					// CTRL-{, CTRL-|, CTRL-} also map to ASCII 96-127
							(key >= 27 && key <= 29 && ev.isShiftDown()))
						key += 96;
					// For CTRL-SHIFT-<letter>, send capital <letter> to emulate
					// behavior
					// of Linux. For CTRL-@, send @. For CTRL-_, send _. For
					// CTRL-^,
					// send ^.
					else if (key < 32)
						key += 64;
					// Windows and Mac sometimes return CHAR_UNDEFINED with
					// CTRL-SHIFT
					// combinations, so best we can do is send the key code if
					// it is
					// a valid ASCII symbol.
					else if (key == KeyEvent.CHAR_UNDEFINED && keycode >= 0 && keycode <= 127)
						key = keycode;
				}

				keysym = UnicodeToKeysym.translate(key);
				if (keysym == -1)
					return;
			}
		} else {
			// KEY_ACTION
			switch (keycode) {
			case KeyEvent.VK_HOME:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Home;
				else
					keysym = Keysyms.Home;
				break;
			case KeyEvent.VK_END:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_End;
				else
					keysym = Keysyms.End;
				break;
			case KeyEvent.VK_PAGE_UP:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Page_Up;
				else
					keysym = Keysyms.Page_Up;
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Page_Down;
				else
					keysym = Keysyms.Page_Down;
				break;
			case KeyEvent.VK_UP:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Up;
				else
					keysym = Keysyms.Up;
				break;
			case KeyEvent.VK_DOWN:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Down;
				else
					keysym = Keysyms.Down;
				break;
			case KeyEvent.VK_LEFT:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Left;
				else
					keysym = Keysyms.Left;
				break;
			case KeyEvent.VK_RIGHT:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Right;
				else
					keysym = Keysyms.Right;
				break;
			case KeyEvent.VK_BEGIN:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Begin;
				else
					keysym = Keysyms.Begin;
				break;
			case KeyEvent.VK_KP_LEFT:
				keysym = Keysyms.KP_Left;
				break;
			case KeyEvent.VK_KP_UP:
				keysym = Keysyms.KP_Up;
				break;
			case KeyEvent.VK_KP_RIGHT:
				keysym = Keysyms.KP_Right;
				break;
			case KeyEvent.VK_KP_DOWN:
				keysym = Keysyms.KP_Down;
				break;
			case KeyEvent.VK_F1:
				keysym = Keysyms.F1;
				break;
			case KeyEvent.VK_F2:
				keysym = Keysyms.F2;
				break;
			case KeyEvent.VK_F3:
				keysym = Keysyms.F3;
				break;
			case KeyEvent.VK_F4:
				keysym = Keysyms.F4;
				break;
			case KeyEvent.VK_F5:
				keysym = Keysyms.F5;
				break;
			case KeyEvent.VK_F6:
				keysym = Keysyms.F6;
				break;
			case KeyEvent.VK_F7:
				keysym = Keysyms.F7;
				break;
			case KeyEvent.VK_F8:
				keysym = Keysyms.F8;
				break;
			case KeyEvent.VK_F9:
				keysym = Keysyms.F9;
				break;
			case KeyEvent.VK_F10:
				keysym = Keysyms.F10;
				break;
			case KeyEvent.VK_F11:
				keysym = Keysyms.F11;
				break;
			case KeyEvent.VK_F12:
				keysym = Keysyms.F12;
				break;
			case KeyEvent.VK_F13:
				keysym = Keysyms.F13;
				break;
			case KeyEvent.VK_F14:
				keysym = Keysyms.F14;
				break;
			case KeyEvent.VK_F15:
				keysym = Keysyms.F15;
				break;
			case KeyEvent.VK_F16:
				keysym = Keysyms.F16;
				break;
			case KeyEvent.VK_F17:
				keysym = Keysyms.F17;
				break;
			case KeyEvent.VK_F18:
				keysym = Keysyms.F18;
				break;
			case KeyEvent.VK_F19:
				keysym = Keysyms.F19;
				break;
			case KeyEvent.VK_F20:
				keysym = Keysyms.F20;
				break;
			case KeyEvent.VK_F21:
				keysym = Keysyms.F21;
				break;
			case KeyEvent.VK_F22:
				keysym = Keysyms.F22;
				break;
			case KeyEvent.VK_F23:
				keysym = Keysyms.F23;
				break;
			case KeyEvent.VK_F24:
				keysym = Keysyms.F24;
				break;
			case KeyEvent.VK_PRINTSCREEN:
				keysym = Keysyms.Print;
				break;
			case KeyEvent.VK_SCROLL_LOCK:
				keysym = Keysyms.Scroll_Lock;
				break;
			case KeyEvent.VK_CAPS_LOCK:
				keysym = Keysyms.Caps_Lock;
				break;
			case KeyEvent.VK_NUM_LOCK:
				keysym = Keysyms.Num_Lock;
				break;
			case KeyEvent.VK_PAUSE:
				if (ev.isControlDown())
					keysym = Keysyms.Break;
				else
					keysym = Keysyms.Pause;
				break;
			case KeyEvent.VK_INSERT:
				if (location == KeyEvent.KEY_LOCATION_NUMPAD)
					keysym = Keysyms.KP_Insert;
				else
					keysym = Keysyms.Insert;
				break;
			// case KeyEvent.VK_FINAL: keysym = Keysyms.?; break;
			// case KeyEvent.VK_CONVERT: keysym = Keysyms.?; break;
			// case KeyEvent.VK_NONCONVERT: keysym = Keysyms.?; break;
			// case KeyEvent.VK_ACCEPT: keysym = Keysyms.?; break;
			// case KeyEvent.VK_MODECHANGE: keysym = Keysyms.Mode_switch?;
			// break;
			// case KeyEvent.VK_KANA: keysym = Keysyms.Kana_shift?; break;
			case KeyEvent.VK_KANJI:
				keysym = Keysyms.Kanji;
				break;
			// case KeyEvent.VK_ALPHANUMERIC: keysym = Keysyms.Eisu_Shift?;
			// break;
			case KeyEvent.VK_KATAKANA:
				keysym = Keysyms.Katakana;
				break;
			case KeyEvent.VK_HIRAGANA:
				keysym = Keysyms.Hiragana;
				break;
			// case KeyEvent.VK_FULL_WIDTH: keysym = Keysyms.?; break;
			// case KeyEvent.VK_HALF_WIDTH: keysym = Keysyms.?; break;
			// case KeyEvent.VK_ROMAN_CHARACTERS: keysym = Keysyms.?; break;
			// case KeyEvent.VK_ALL_CANDIDATES: keysym =
			// Keysyms.MultipleCandidate?; break;
			case KeyEvent.VK_PREVIOUS_CANDIDATE:
				keysym = Keysyms.PreviousCandidate;
				break;
			case KeyEvent.VK_CODE_INPUT:
				keysym = Keysyms.Codeinput;
				break;
			// case KeyEvent.VK_JAPANESE_KATAKANA: keysym = Keysyms.?; break;
			// case KeyEvent.VK_JAPANESE_HIRAGANA: keysym = Keysyms.?; break;
			case KeyEvent.VK_JAPANESE_ROMAN:
				keysym = Keysyms.Romaji;
				break;
			case KeyEvent.VK_KANA_LOCK:
				keysym = Keysyms.Kana_Lock;
				break;
			// case KeyEvent.VK_INPUT_METHOD_ON_OFF: keysym = Keysyms.?; break;

			case KeyEvent.VK_AGAIN:
				keysym = Keysyms.Redo;
				break;
			case KeyEvent.VK_UNDO:
				keysym = Keysyms.Undo;
				break;
			// case KeyEvent.VK_COPY: keysym = Keysyms.?; break;
			// case KeyEvent.VK_PASTE: keysym = Keysyms.?; break;
			// case KeyEvent.VK_CUT: keysym = Keysyms.?; break;
			case KeyEvent.VK_FIND:
				keysym = Keysyms.Find;
				break;
			// case KeyEvent.VK_PROPS: keysym = Keysyms.?; break;
			case KeyEvent.VK_STOP:
				keysym = Keysyms.Cancel;
				break;
			case KeyEvent.VK_HELP:
				keysym = Keysyms.Help;
				break;
			case KeyEvent.VK_WINDOWS:
				if (down)
					modifiers |= SUPER_MASK;
				else
					modifiers &= ~SUPER_MASK;
				keysym = Keysyms.Super_L;
				break;
			case KeyEvent.VK_CONTEXT_MENU:
				keysym = Keysyms.Menu;
				break;
			default:
				return;
			}
		}

		if (keysym > 0) {
			String fmt = ev.paramString().replaceAll("%", "%%");
			vlog.debug(String.format(fmt.replaceAll(",", "%n       ")));

			writeKeyEvent(keysym, down);
		}
	}

	public void writePointerEvent(MouseEvent ev) {
		if (state() != RFBSTATE_NORMAL || shuttingDown || !control)
			return;

		switch (ev.getID()) {
		case MouseEvent.MOUSE_PRESSED:
			buttonMask = 1;
			if ((ev.getModifiers() & KeyEvent.ALT_MASK) != 0)
				buttonMask = 2;
			if ((ev.getModifiers() & KeyEvent.META_MASK) != 0)
				buttonMask = 4;
			break;
		case MouseEvent.MOUSE_RELEASED:
			buttonMask = 0;
			break;
		}

		if (cp.width != desktop.scaledWidth || cp.height != desktop.scaledHeight) {
			int sx = (desktop.scaleWidthRatio == 1.00) ? ev.getX() : (int) Math.floor(ev.getX() / desktop.scaleWidthRatio);
			int sy = (desktop.scaleHeightRatio == 1.00) ? ev.getY() : (int) Math.floor(ev.getY() / desktop.scaleHeightRatio);
			ev.translatePoint(sx - ev.getX(), sy - ev.getY());
		}

		writer().writePointerEvent(new Point(ev.getX(), ev.getY()), buttonMask);
	}

	public void writeWheelEvent(MouseWheelEvent ev) {
		if (state() != RFBSTATE_NORMAL || shuttingDown || !control)
			return;
		int x, y;
		int clicks = ev.getWheelRotation();
		if (clicks < 0) {
			buttonMask = 8;
		} else {
			buttonMask = 16;
		}
		for (int i = 0; i < Math.abs(clicks); i++) {
			x = ev.getX();
			y = ev.getY();
			writer().writePointerEvent(new Point(x, y), buttonMask);
			buttonMask = 0;
			writer().writePointerEvent(new Point(x, y), buttonMask);
		}
	}

	public synchronized void releaseModifiers() {
		if ((modifiers & Event.SHIFT_MASK) == Event.SHIFT_MASK)
			writeKeyEvent(Keysyms.Shift_R, false);
		if (((modifiers >> KEY_LOC_SHIFT_L) & Event.SHIFT_MASK) == Event.SHIFT_MASK)
			writeKeyEvent(Keysyms.Shift_L, false);
		if ((modifiers & Event.CTRL_MASK) == Event.CTRL_MASK)
			writeKeyEvent(Keysyms.Control_R, false);
		if (((modifiers >> KEY_LOC_SHIFT_L) & Event.CTRL_MASK) == Event.CTRL_MASK)
			writeKeyEvent(Keysyms.Control_L, false);
		if ((modifiers & Event.ALT_MASK) == Event.ALT_MASK)
			writeKeyEvent(Keysyms.Alt_R, false);
		if (((modifiers >> KEY_LOC_SHIFT_L) & Event.ALT_MASK) == Event.ALT_MASK)
			writeKeyEvent(Keysyms.Alt_L, false);
		if ((modifiers & Event.META_MASK) == Event.META_MASK)
			writeKeyEvent(Keysyms.Meta_R, false);
		if (((modifiers >> KEY_LOC_SHIFT_L) & Event.META_MASK) == Event.META_MASK)
			writeKeyEvent(Keysyms.Meta_L, false);
		if ((modifiers & SUPER_MASK) == SUPER_MASK)
			writeKeyEvent(Keysyms.Super_L, false);
		modifiers = 0;
	}

	private void checkEncodings() {
		if ((encodingChange) && (writer() != null)) {
			vlog.info("Requesting " + Encodings.encodingName(currentEncoding) + " encoding");
			writer().writeSetEncodings(currentEncoding, true);
			encodingChange = false;
		}
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public void setShuttingDown(boolean shuttingDown) {
		this.shuttingDown = shuttingDown;
	}

	public boolean isControl() {
		return control;
	}

	public void setControl(boolean control) {
		this.control = control;
	}

	public void registerViewport(VncDesktop viewport) {
		this.viewport = viewport;
	}

	public void unregisterViewport(VncDesktop viewport) {
		if (this.viewport == viewport) {
			this.viewport = null;
		}
	}

	/**
	 * @return the defaultVncValue
	 */
	public DefaultVncValue getDefaultVncValue() {
		return defaultVncValue;
	}

	boolean isRecording = false;

	public void startRecord(String filePath) throws IOException {
		if (!RecordTimer.checkSecurity(true)) {
			throw new IOException("can not create file permission");
		}
		frameCache = new FrameCache();
		recordTask = new RecordTask(frameCache, filePath, getConnInfo());
		if (desktop != null) {
			frameCache.addFrame(new FrameInfo((BufferedImage) desktop.getImage(), 200));
		}
		timer = new RecordTimer(false);
		timer.schedule(recordTask, 0, 150);
		preTime = System.currentTimeMillis();
//		if (viewport != null) {
//			viewport.getRecord().setText(RecordState.REC_START);
//			JOptionPane.showMessageDialog(null, "save avi file to path:" + recordTask.getFilePath());
//		}
		isRecording = true;
	}

	private void addImageForRecord() {
		if (isRecording && desktop.getImage() instanceof BufferedImage) {
			frameInfo = new FrameInfo((BufferedImage) desktop.getImage(), System.currentTimeMillis()-preTime);
			frameCache.addFrame(frameInfo);
		}
	}

	public void stoppedRecord() {
		try {
			if (isRecording) {
				isRecording = false;
				if (recordTask != null) {
					new Thread(new StopRecordThread(recordTask)).start();
				}
//				if (viewport != null) {
//					viewport.getRecord().setText(RecordState.REC_STOPPING);
//				}
			}

		} catch (Exception e) {
			vlog.error(e.getMessage());
		} finally {
//			if (viewport != null) {
//				viewport.getRecord().setText(RecordState.REC_INIT);
//			}
		}
	}

	/**
	 * @return the viewport
	 */
	public VncDesktop getViewport() {
		return viewport;
	}

	/**
	 * @return the desktop
	 */
	public DesktopWindow getDesktop() {
		return desktop;
	}
	
	@Override
	public void writeKey(int keyCode) {
		writeKeyEvent(keyCode, true);
		writeKeyEvent(keyCode, false);
	}
}
