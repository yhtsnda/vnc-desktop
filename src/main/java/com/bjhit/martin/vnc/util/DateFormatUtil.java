package com.bjhit.martin.vnc.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.bjhit.martin.vnc.common.DateFormat;

/**
 * @description
 * @project com.bjhit.martian.common
 * @author guanxianchun
 * @Create 2014-11-25 下午3:44:21
 * @version 1.0
 */
public class DateFormatUtil {
	
	public static String getDateTime(DateFormat dateFormat) {
		Date date=new Date();
 		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat.getFormat());
 		return formatter.format(date);
	}
	
}

