
<%
	String host = request.getParameter("host");
	String port = request.getParameter("port");
	String encodePassword = request.getParameter("encodePassword");

	String proxy = request.getParameter("proxy");
	String proxyPort = request.getParameter("proxyPort");
	String isControl = request.getParameter("isControl");
	if (proxy == null) {
		proxy = "";
	}
	if (proxyPort == null) {
		proxyPort = "";
	}
	if (isControl == null) {
		isControl = "";
	}
	if (encodePassword == null) {
		encodePassword = "";
	}
%>


<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title><%=host%></title>
</head>
<body>
	<div  style="overflow:auto;">
		<applet id="console" archive="bjhit.vncdesktop.jar,bjhit-video-1.0.jar" code="com.bjhit.martin.vnc.client.VncDesktop" codebase="applet" width="100%" height="100%">
			<param NAME="host" VALUE="<%=host%>" />
			<param NAME="encodePassword" VALUE="<%=encodePassword%> " />
			<param NAME="port" VALUE="<%=port%> " />
			<param NAME="proxy" VALUE="<%=proxy%> " />
			<param NAME="proxyPort" VALUE="<%=proxyPort%> " />
			<param NAME="isControl" VALUE="<%=isControl%> ">
		</applet>
	</div>
	
</body>
</html>