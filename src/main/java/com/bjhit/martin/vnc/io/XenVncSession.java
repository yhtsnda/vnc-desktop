package com.bjhit.martin.vnc.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.xmlrpc.XmlRpcException;

import com.bjhit.martin.vnc.common.ConnectType;
import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.exception.AppIOException;
import com.bjhit.martin.vnc.util.StringUtil;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Console;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.SessionAuthenticationFailed;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VM.Record;

public class XenVncSession extends VncSession {

	private boolean usessl;
	private static final Pattern END_PATTERN = Pattern.compile("^\r\n$");
	private static final Pattern HEADER_PATTERN = Pattern
			.compile("^([A-Z_a-z0-9-]+):\\s*(.*)\r\n$");
	private static final Pattern HTTP_PATTERN = Pattern
			.compile("^HTTP/\\d+\\.\\d+ (\\d*) (.*)\r\n$");
	private static LogWriter log = new LogWriter("XenVncSession");

	private String path = "";
	private URL uri;
    
	public XenVncSession(ConnectionInfo connInfo) {
		this.connInfo = connInfo;
		this.command = "CONNECT";
		this.session = null;
	}

	@Override
	public boolean initConnection() throws Exception {
		initXenConnection();
		initVmPathAndAuth();
		initSocket();
		initSocketProperties();
		try {
			if (usessl) {
				initSSLIO();
			} else {
				initIO();
			}
		} catch (Exception e) {
			log.error("init connection error:"+e.getMessage());
			close();
			return false;
		}
		return true;
	}
	private void initIO() throws IOException {
		in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}

	/**
	 * 设置socket的TCP连接延迟和发送缓冲区大小
	 */
	private void initSocketProperties() {
		try {
			if (socket != null) {
				socket.setTcpNoDelay(true);
				socket.setSendBufferSize(65535);
				socket.setSoTimeout(1000*60*60*3);
			}
		} catch (SocketException e) {
			log.error("set socket properties error:"+e.getMessage());
		}
	}
	
	private void initVmPathAndAuth() throws Exception {
		Set<VM> vms = VM.getByNameLabel(conn, connInfo.getVmId());
		if (vms.isEmpty()) {
			log.error("can not find vm:"+connInfo.getVmId());
			throw new AppIOException("can not find vm:"+connInfo.getVmId());
		}
		VM vm = vms.iterator().next();
		Iterator<Console> iterator = vm.getRecord(conn).consoles.iterator();
		Console console = iterator.next();
		String pathtemp = console.getLocation(conn);
		session = conn.getSessionReference();
		uri = new URL(pathtemp);
		path = uri.getPath().concat("?").concat(uri.getQuery());
		usessl = "https".equals(uri.getProtocol());
	}
	Connection conn;
	
	private void initXenConnection() throws MalformedURLException, BadServerResponse, SessionAuthenticationFailed, XenAPIException, XmlRpcException {
		Connection tmpConn = new Connection(new URL("http://"+connInfo.getHost())); 
		Session.loginWithPassword(tmpConn, connInfo.getUserName(),connInfo.getPassword(), APIVersion.latest().toString());
		conn = tmpConn;
	}
	/**
	 * 初始化socket
	 * @throws KeyManagementException
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void initSocket() throws KeyManagementException, UnknownHostException, IOException {
		if (!(StringUtil.isEmpty(this.connInfo.getProxyHost())))
			initSocket(true);
		else {
			initSocket(false);
		}
	}
	/**
	 * 初始化socket
	 * @param proxy
	 * @throws IOException
	 * @throws KeyManagementException
	 * @throws UnknownHostException
	 */
	private void initSocket(boolean proxy) throws IOException, KeyManagementException, UnknownHostException {
		if (usessl) {
			javax.net.ssl.SSLContext context = SSLHelper.getInstance().getSSLContext();
			try {
				context.init(null, trustAllCerts, new SecureRandom());
				SocketFactory factory = context.getSocketFactory();
				if (proxy) {
					socket = (SSLSocket) factory.createSocket(uri.getHost(), connInfo.getPort(), InetAddress.getByName(connInfo.getProxyHost()), connInfo.getProxyPort());
				}else {
					socket = (SSLSocket) factory.createSocket(uri.getHost(), connInfo.getPort());
				}
				/* ssl.setSSLParameters(context.getDefaultSSLParameters()); */
			} catch (IOException e) {
				log.error("IOException: " + e.getMessage());
				throw e;
			} catch (KeyManagementException e) {
				log.error("KeyManagementException: " + e.getMessage());
				throw e;
			}
		} else {
			if (proxy) {
				socket = new Socket(initProxy());
			}else {
				socket = new Socket(connInfo.getHost(), connInfo.getPort());
			}
		}
	}

	@Override
	public Proxy initProxy() {
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(this.connInfo.getProxyHost(), this.connInfo.getProxyPort()));
		Authenticator auth = new Authenticator() {
			String pwd;
			private PasswordAuthentication pa;
			protected PasswordAuthentication getPasswordAuthentication() {
				return this.pa;
			}
		};
		Authenticator.setDefault(auth);
		return proxy;
	}

	private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	} };
	
	
	private final Map<String, String> responseHeaders = new HashMap<String, String>();
	/**
	 * 初始化ssl连接IO
	 * @throws IOException
	 */
	private void initSSLIO() throws IOException {
		String[] headers = makeHeaders();
		try {
			out = socket.getOutputStream();
			for (String header : headers) {
				out.write(header.getBytes());
				out.write("\r\n".getBytes());
			}
			out.flush();
			in = socket.getInputStream();
			while (true) {
				String line = readline(in);
				Matcher m = END_PATTERN.matcher(line);
				if (m.matches()) {
					return;
				}

				m = HEADER_PATTERN.matcher(line);
				if (m.matches()) {
					responseHeaders.put(m.group(1), m.group(2));
					continue;
				}

				m = HTTP_PATTERN.matcher(line);
				if (m.matches()) {
					String status_code = m.group(1);
					String reason_phrase = m.group(2);
					if (!"200".equals(status_code)) {
						throw new IOException("HTTP status " + status_code + " " + reason_phrase);
					}
				} else {
					throw new IOException("Unknown HTTP line " + line);
				}
			}
		} catch (IOException exn) {
			socket.close();
			throw exn;
		} catch (RuntimeException exn) {
			socket.close();
			throw exn;
		}
	}
	private final String command;
	private String session;
	private String[] makeHeaders() {
		String[] headers = { String.format("%s %s HTTP/1.0", command, path),
				String.format("Host: %s", "http://"+connInfo.getHost()), String.format("Cookie: session_id=%s", session),
				"" };
		return headers;
	}
	
	private static String readline(InputStream ic) throws IOException {
		String result = "";
		while (true) {
			try {
				int c = ic.read();
				if (c == -1) {
					return result;
				}
				result = result + (char) c;
				if (c == 0x0a /* LF */) {
					return result;
				}
			} catch (IOException e) {
				ic.close();
				throw e;
			}
		}
	}
	
	@Override
	public void close() throws AppIOException {
		try {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			throw new AppIOException(e);
		}finally{
			if (conn != null) {
				try {
					Session.logout(conn);
				} catch (Exception e) {
					log.error(e.getMessage());
				} 
			}
		}
	}
}
