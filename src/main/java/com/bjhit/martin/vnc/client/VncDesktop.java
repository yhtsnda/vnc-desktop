/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2011-2013 Brian P. Hinz
 * Copyright (C) 2012 D. R. Commander.  All Rights Reserved.
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

package com.bjhit.martin.vnc.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.exception.AppIOException;
import com.bjhit.martin.vnc.util.StringUtil;

@SuppressWarnings("serial")
public class VncDesktop extends JApplet implements Runnable, WindowListener {
	private Thread viewportThread;
	private ConnectionInfo connectionInfo;
	private boolean showControls;
	private CConn cc;
	private boolean stopped = false;
	static LogWriter vlog = new LogWriter("VncDesktop");

	public static void setLookAndFeel() {
		try {
			String laf = System.getProperty("swing.defaultlaf");
			if (laf == null) {
				UIManager.LookAndFeelInfo[] installedLafs = UIManager.getInstalledLookAndFeels();
				for (int i = 0; i < installedLafs.length; ++i) {
					if (installedLafs[i].getName().equals("Nimbus"))
						laf = installedLafs[i].getClassName();
				}
				if (laf == null)
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			UIManager.setLookAndFeel(laf);
			UIManager.put("TitledBorder.titleColor", Color.blue);
			if (UIManager.getLookAndFeel().getName().equals("Metal")) {
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				Enumeration keys = UIManager.getDefaults().keys();
				while (keys.hasMoreElements()) {
					Object key = keys.nextElement();
					Object value = UIManager.get(key);
					if (value instanceof FontUIResource) {
						String name = ((FontUIResource) value).getName();
						int style = ((FontUIResource) value).getStyle();
						int size = ((FontUIResource) value).getSize() - 1;
						FontUIResource f = new FontUIResource(name, style, size);
						UIManager.put(key, f);
					}
				}
				return;
			}

			if (UIManager.getLookAndFeel().getName().equals("Nimbus")) {
				Font f = UIManager.getFont("TitledBorder.font");
				String name = f.getName();
				int style = f.getStyle();
				int size = f.getSize() - 2;
				FontUIResource r = new FontUIResource(name, style, size);
				UIManager.put("TitledBorder.font", r);
			}
		} catch (Exception e) {
			vlog.info(e.toString());
		}
	}

	public void init() {
		setLookAndFeel();
		setFocusable(false);
		UIManager.getDefaults().put("ScrollPane.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[0]));

		connectionInfo = new ConnectionInfo();
		readInitParameters();
		vlog.debug(connectionInfo.toString());
		initComponents();
		viewportThread = new Thread(this);
		viewportThread.start();
	}

	private GridBagLayout layout;
	private JButton options;
//	private JButton record;
	private JButton ctlAltDel;
	private GridBagConstraints gbc;
	private ButtonActionListener listener;
	private JScrollPane scrollPane;

	private void initComponents() {
		options = new JButton("Options");
//		record = new JButton(RecordState.REC_INIT);
		ctlAltDel = new JButton("Clt+Alt+Del");
		JPanel toolPanel = new JPanel();
		scrollPane = new JScrollPane();
		scrollPane.setBackground(Color.BLUE);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		layout = new GridBagLayout();
		gbc = new GridBagConstraints();// 定义一个GridBagConstraints，
		this.getContentPane().setLayout(layout);
		this.getContentPane().add(options);// 添加组件
//		this.getContentPane().add(record);
		this.getContentPane().add(ctlAltDel);
		this.getContentPane().add(toolPanel);
		this.getContentPane().add(scrollPane);
		// 是用来控制添加进的组件的显示位置
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(1, 1, 1, 1); // 组件彼此的间距
		// 是用来控制添加进的组件的显示位置
		// 该方法是为了设置如果组件所在的区域比组件本身要大时的显示情况
		// NONE：不调整组件大小。
		// HORIZONTAL：加宽组件，使它在水平方向上填满其显示区域，但是不改变高度。
		// VERTICAL：加高组件，使它在垂直方向上填满其显示区域，但是不改变宽度。
		// BOTH：使组件完全填满其显示区域。
		gbc.gridwidth = 1;// 该方法是设置组件水平所占用的格子数，如果为0，就说明该组件是该行的最后一个
		gbc.weightx = 0;// 该方法设置组件水平的拉伸幅度，如果为0就说明不拉伸，不为0就随着窗口增大进行拉伸，0到1之间
		gbc.weighty = 0;// 该方法设置组件垂直的拉伸幅度，如果为0就说明不拉伸，不为0就随着窗口增大进行拉伸，0到1之间
		layout.setConstraints(options, gbc);// 设置组件
//		gbc.gridwidth = 1;
//		gbc.weightx = 0;
//		gbc.weighty = 0;
//		layout.setConstraints(record, gbc);
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		layout.setConstraints(ctlAltDel, gbc);
		gbc.gridwidth = 0;// 该方法是设置组件水平所占用的格子数，如果为0，就说明该组件是该行的最后一个
		gbc.weightx = 1;// 不能为1，j4是占了4个格，并且可以横向拉伸，
		// 但是如果为1，后面行的列的格也会跟着拉伸,导致j7所在的列也可以拉伸
		// 所以应该是跟着j6进行拉伸
		gbc.weighty = 0;
		layout.setConstraints(toolPanel, gbc);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(1, 1, 0, 0); // 组件彼此的间距
		layout.setConstraints(scrollPane, gbc);
		layout.setConstraints(scrollPane, gbc);

		listener = new ButtonActionListener();
		options.addActionListener(listener);
//		record.addActionListener(listener);
		ctlAltDel.addActionListener(listener);
		options.setEnabled(showControls);
//		record.setEnabled(false);
		ctlAltDel.setEnabled(false);
		options.setVisible(true);
//		record.setVisible(true);
		ctlAltDel.setVisible(true);

	}

	private void readInitParameters() {
		connectionInfo.setHost(getParameter("host"));
		connectionInfo.setHost("172.19.106.242");
		if (StringUtil.isEmpty(connectionInfo.getHost())) {
			connectionInfo.setHost(getParameter(getCodeBase().getHost()));
			if (StringUtil.isEmpty(connectionInfo.getHost())) {
				throw new RuntimeException("HOST parameter not specified");
			}
		}
		connectionInfo.setPort(Integer.parseInt((StringUtil.isEmpty(getParameter("port"))) ? "0" : getParameter("port")));
		connectionInfo.setEncodePassword(getParameter("encodePassword"));
		connectionInfo.setProxyHost(getParameter("proxy"));
		String proxyport = getParameter("proxyPort");
		if (!(StringUtil.isEmpty(proxyport))) {
			connectionInfo.setProxyPort(Integer.parseInt(proxyport));
		}
		String isControl = getParameter("isControl");
		if (!(StringUtil.isEmpty(isControl))) {
			showControls = Boolean.valueOf(isControl).booleanValue();
		} else {
			showControls = false;
		}
//		connectionInfo.setVmId(getParameter("vmId"));
		connectionInfo.setEncodePassword("wCVfAXGTaLw=");
		connectionInfo.setPort(5901);
		showControls = true;
	}

	public void run() {
		try {
			vlog.debug("new connection.....");
			cc = new CConn(connectionInfo);
			cc.registerViewport(this);
			cc.setControl(showControls);
			while (!stopped && !(cc.isShuttingDown()))
				cc.processMsg();
		} catch (AppIOException e) {
			vlog.debug(e.getMessage());
		} catch (Exception e) {
			vlog.debug(e.getMessage());
			if (cc != null) {
				cc.close();
			}
			
		} finally {
			vlog.debug(cc+" finally");
			if (cc != null) {
				cc.unregisterViewport(this);
			}
		}
	}

	public void setChild(DesktopWindow child) {
		scrollPane.getViewport().setView(child);
	}

	public void setGeometry(int x, int y, int w, int h, boolean pack) {
		if (!(pack)) {
			setSize(w, h);
		}
		if (!(cc.fullScreen))
			setLocation(x, y);
	}

	public static Window getFullScreenWindow() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		Window fullScreenWindow = gd.getFullScreenWindow();
		return fullScreenWindow;
	}

	public static void setFullScreenWindow(Window fullScreenWindow) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		gd.setFullScreenWindow(fullScreenWindow);
	}

	public void destroy() {
		stopped = true;
		if (cc != null) {
			cc.unregisterViewport(this);
			cc.stoppedRecord();
			cc.close();
		}
	}

	/**
	 * @return the scrollPane
	 */
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	/**
	 * @return the options
	 */
	public JButton getOptions() {
		return options;
	}

	/**
	 * @return the record
	 */
//	public JButton getRecord() {
//		return record;
//	}

	/**
	 * @return the ctlAltDel
	 */
	public JButton getCtlAltDel() {
		return ctlAltDel;
	}

	class ButtonActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (cc.getDesktop() != null) {
				cc.getDesktop().requestFocus();
			}
//			if (e.getSource() == record) {
//				if (!RecordTimer.checkSecurity(true)) {
//					throw new RuntimeException("can not create file permission");
//				}
//				if (StringUtil.equalsIgnoreCase(record.getLabel(), RecordState.REC_INIT)) {
//					try {
//						cc.startRecord(ScreenPropertyUtil.getUserHomeDictory());
//					} catch (IOException e1) {
//						e1.printStackTrace();
//					}
//				} else if (StringUtil.equalsIgnoreCase(record.getLabel(), RecordState.REC_START)) {
//					cc.stoppedRecord();
//				}
//
//			} else 
				
				if (e.getSource() == ctlAltDel) {
				cc.writeKeyEvent(Keysyms.Control_L, true);
				cc.writeKeyEvent(Keysyms.Alt_L, true);
				cc.writeKeyEvent(Keysyms.Delete, true);
				cc.writeKeyEvent(Keysyms.Delete, false);
				cc.writeKeyEvent(Keysyms.Alt_L, false);
				cc.writeKeyEvent(Keysyms.Control_L, false);
			} else if (e.getSource() == options) {
				cc.options.showDialog(options.getParent().getParent());
			}
		}
	}

	public void setRecordEnable(boolean enabled) {
//		record.setEnabled(enabled);
	}

	public void setCltAltDelEnable(boolean enabled) {
		ctlAltDel.setEnabled(enabled);
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent e) {
		disconnect();
	}

	private void disconnect() {
		if (cc != null) {
			cc.options.dispose();
			cc.stoppedRecord();
			cc.close();
			cc = null;
		}
		getContentPane().removeAll();
		validate();
		viewportThread.stop();
	}

	@Override
	public void windowClosed(WindowEvent e) {
		disconnect();
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	
	public String getAppletSize() {
		return getSize().getWidth()+":"+getSize().getHeight();
	}
}
