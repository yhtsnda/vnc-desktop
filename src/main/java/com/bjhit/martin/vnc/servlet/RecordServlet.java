/**
 * 
 */
package com.bjhit.martin.vnc.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.bjhit.martin.vnc.common.ConnectType;
import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.record.RecordThread;
import com.bjhit.martin.vnc.util.DescryptUtil;
import com.bjhit.martin.vnc.util.StringUtil;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-21 下午5:07:46
 * @version 1.0
 */
/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-21 下午5:07:46
 * @version 1.0
 */
@SuppressWarnings("serial")
@WebServlet("/recordServlet")
public class RecordServlet extends HttpServlet {
	private static final Map<ConnectionInfo, RecordThread> recordHosts = new HashMap<ConnectionInfo, RecordThread>();
	private final String START_RECORD = "record";
	private final String STOP_RECORD = "stop";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String cmd = request.getParameter("cmd");
		String dataStr = request.getParameter("data");
		JSONObject jsonObject = JSONObject.fromObject(dataStr);
		ConnectionInfo connectionInfo = initConnection(jsonObject);
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		if (StringUtil.equal(cmd, START_RECORD)) {
			if (!recordHosts.containsKey(connectionInfo)) {
				RecordThread thread = new RecordThread(connectionInfo);
				try {
					thread.initParams();
					thread.start();
					synchronized (recordHosts) {
						recordHosts.put(connectionInfo, thread);
					}
					out.println("{\"success\":true}");
				} catch (Exception e) {
					out.println("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
				}
			}else{
				ConnectionInfo temp = getConnectInfoFromPool(connectionInfo);
				if (temp != null) {
					temp.setConnectCount(temp.getConnectCount()+1);
				}
				out.println("{\"success\":true}");
			}
		} else if (StringUtil.equal(cmd, STOP_RECORD)) {
			RecordThread thread;
			if(recordHosts.containsKey(connectionInfo)){
				ConnectionInfo temp = getConnectInfoFromPool(connectionInfo);
				if (temp != null && temp.getConnectCount()>1) {
					temp.setConnectCount(temp.getConnectCount()-1);
				}else if(temp !=null && temp.getConnectCount() == 1){
					synchronized (recordHosts) {
						thread = recordHosts.remove(connectionInfo);
					}
					if (thread != null) {
						thread.stoppedRecord();
					}
				}
			}
			out.println("{\"success\":true}");
		}
		out.flush();
		out.close();
	}

	private ConnectionInfo getConnectInfoFromPool(ConnectionInfo connectionInfo) {
		Iterator<ConnectionInfo> iterator = recordHosts.keySet().iterator();
		ConnectionInfo temp = null;
		while (iterator.hasNext()) {
			temp = iterator.next();
			if (connectionInfo.equals(temp)) {
				break;
			}
		}
		return temp;
	}

	private ConnectionInfo initConnection(JSONObject jsonObject) {
		ConnectionInfo info = new ConnectionInfo();
		info.setHost(jsonObject.getString("host"));
		info.setPort(jsonObject.getInt("port"));
		if (jsonObject.containsKey("encodePassword")) {
			info.setEncodePassword(jsonObject.getString("encodePassword"));
			try {
				info.setPassword(DescryptUtil.decode(info.getEncodePassword()));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (jsonObject.containsKey("proxyHost")) {
			info.setProxyHost(jsonObject.getString("proxyHost"));
		}
		if (jsonObject.containsKey("proxyPort")) {
			info.setProxyPort(jsonObject.getInt("proxyPort"));
		}
		if (jsonObject.containsKey("vmName")) {
			info.setVmId(jsonObject.getString("vmName"));
		}
		if (jsonObject.containsKey("connectType")) {
			ConnectType type = ConnectType.getType(jsonObject.getString("connectType").trim());
			info.setConnType(type);
		}
		if (jsonObject.containsKey("userName")) {
			info.setUserName(jsonObject.getString("userName"));
		}
		return info;
	}

	@Override
	public void destroy() {
		Iterator<ConnectionInfo> iterator = recordHosts.keySet().iterator();
		ConnectionInfo info = null;
		RecordThread thread = null;
		while (iterator.hasNext()) {
			info = iterator.next();
			thread = recordHosts.get(info);
			thread.stoppedRecord();
			iterator.remove();
		}
	}

	protected String getRecordHostJson() {
		StringBuilder result = new StringBuilder();
		result.append("{vms:[");
		synchronized (recordHosts) {
			if (recordHosts.isEmpty()) {
				result.append("]}");
			} else {
				Iterator<ConnectionInfo> iterator = recordHosts.keySet().iterator();
				while (iterator.hasNext()) {
					ConnectionInfo connectionInfo = (ConnectionInfo) iterator.next();
					result.append(connectionInfo.toString() + ",");
				}
				result.delete(result.length() - 1, result.length());
				result.append("]}");
			}
		}
		return result.toString();
	}

}
