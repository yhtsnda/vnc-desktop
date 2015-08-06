<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
	<div style="width:80%;height:110%">
		<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" codebase="http://www.apple.com/qtactivex/qtplugin.cab" width="826" height="644">
	   		<!--  <param name="src" value="fileServerURL${filePath }">
	   		-->
	   		<param name="src" value="http://localhost:8080/vmconsole/video.mp4">
			<param name="autoplay" value="true">
			<param name="controller" value="true">
			<param name="preload" value="true">
			<!-- 
			<embed src="fileServerURL %>${filePath }" width="826" height="644" preload="true" autoplay="true" controller="true" pluginspage="http://www.apple.com/quicktime/"></embed>
			-->
			<embed src="http://localhost:8080/vmconsole/video.mp4" width="826" height="644" preload="true" autoplay="true" controller="true" pluginspage="http://www.apple.com/quicktime/"></embed>
	   </object>
	</div>
</body>
</html>