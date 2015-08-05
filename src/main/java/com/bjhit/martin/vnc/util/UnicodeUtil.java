package com.bjhit.martin.vnc.util;

public class UnicodeUtil {
	/**
	 * Unicode解码
	 * @param dataStr unicode编码串
	 * @return
	 */
	public static String decodeUnicode(final String dataStr) {
		int start = 0;
		int end = 0;
		final StringBuffer buffer = new StringBuffer();
		String str =null;
		while (start > -1) {
			end = dataStr.indexOf("\\u", start + 2);
			String charStr = "";
			if (end == -1) {
				if (dataStr.length()>start+6) {
					charStr = dataStr.substring(start + 2, start+6);
					str = dataStr.substring(start+6,dataStr.length());
				}else {
					charStr = dataStr.substring(start + 2, dataStr.length());
				}
			} else {
				if (end>start+6) {
					charStr = dataStr.substring(start + 2, start+6);
					str = dataStr.substring(start+6,end);
				}else {
					charStr = dataStr.substring(start + 2, end);
				}
				
			}
			try {
				char letter = (char) Integer.parseInt(charStr, 16);
				buffer.append(new Character(letter).toString());
			} catch (NumberFormatException e) {
				buffer.append("\\u").append(charStr);
			}
			if (str !=null) {
				buffer.append(str);
				str = null;
			}
			start = end;
		}
		return buffer.toString();
	}
	/**
	 * 判断是否为unicode编码串
	 * 注：这里只判断一个是否为unicode
	 * @param source
	 * @return
	 */
	public static boolean firstIsUnicode(String source) {
		if (source.indexOf("\\u") == 0) {
			try {
				Integer.parseInt(source.substring(2,6), 16);
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
		return false;
	}
}
