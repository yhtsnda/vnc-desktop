package com.bjhit.martin.vnc.record;

import com.bjhit.martin.vnc.client.CConn;
import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.util.ScreenPropertyUtil;

/**
 * @description
 * @project vmconsole
 * @author guanxianchun
 * @Create 2014-12-29 上午11:06:42
 * @version 1.0
 */
public class RecordThread extends Thread {
	// 登录变量
	private ConnectionInfo connectionInfo;
	private CConn conn;
	private boolean stopped = false;

	public RecordThread(ConnectionInfo connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

	public void run() {
		try {
			while (!stopped && !(conn.isShuttingDown()))
				conn.processMsg();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (conn != null) {
				conn.stoppedRecord();
				conn.close();
			}
		}
	}

	public void initParams() throws Exception {
		conn = new CConn(connectionInfo);
		conn.setControl(false);
		if (!RecordTimer.checkSecurity(false)) {
			throw new RuntimeException("can not create file permission");
		}
		conn.startRecord(ScreenPropertyUtil.getRunningDictory());
	}

	public void stoppedRecord() {
		try {
			if (conn == null) {
				return;
			}
			stopped = true;
			conn.stoppedRecord();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		stoppedRecord();
	}

	@Override
	public void destroy() {
		stoppedRecord();
	}
}
