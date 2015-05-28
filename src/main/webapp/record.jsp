<?xml version="1.0" encoding="UTF-8" ?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Record</title>
<SCRIPT type="text/javascript"
	src="http://code.jquery.com/jquery-2.1.3.min.js"></SCRIPT>

<SCRIPT type="text/javascript">
	function processRecord(object) {
		var connInfo ='{host:"192.168.1.110",port:6001,encodePassword:"wCVfAXGTaLw="}';
		var cmd = $(object).val();
		if(cmd == "Record"){
			cmd = "record";
		}else{
			cmd ="stop";
		}
		var url = "<%=request.getContextPath()%>/recordServlet?cmd=" + cmd;
		$.post(url, {
			data : connInfo
		}, function(data) {
			var dataObj=eval("("+data+")");
			var success =dataObj.success;
			if(success){
				if(cmd=="stop"){
					$(object).val("Record");
				}else{
					$(object).val("Recording");
				}
			}else{
				alert(dataObj.error);
			}
		});
	}
</SCRIPT>
</head>
<body>
	<DIV>
		<INPUT type="button" id="record" value="Record"
			onclick="processRecord(this)" />
	</DIV>
</body>
</html>
