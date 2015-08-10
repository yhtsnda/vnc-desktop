package com.bjhit.martin.vnc.util;

import java.io.UnsupportedEncodingException;

public class UnicodeUtil {
	/**
	 * Unicode解码
	 * 
	 * @param dataStr
	 *            unicode编码串
	 * @return
	 */
	public static String decodeUnicode(final String dataStr) {
		int start = 0;
		int end = 0;
		final StringBuffer buffer = new StringBuffer();
		String str = null;
		while (start > -1) {
			end = dataStr.indexOf("\\u", start + 2);
			String charStr = "";
			if (end == -1) {
				if (dataStr.length() > start + 6) {
					charStr = dataStr.substring(start + 2, start + 6);
					str = dataStr.substring(start + 6, dataStr.length());
				} else {
					charStr = dataStr.substring(start + 2, dataStr.length());
				}
			} else {
				if (end > start + 6) {
					charStr = dataStr.substring(start + 2, start + 6);
					str = dataStr.substring(start + 6, end);
				} else {
					charStr = dataStr.substring(start + 2, end);
				}

			}
			try {
				char letter = (char) Integer.parseInt(charStr, 16);
				buffer.append(new Character(letter).toString());
			} catch (NumberFormatException e) {
				buffer.append("\\u").append(charStr);
			}
			if (str != null) {
				buffer.append(str);
				str = null;
			}
			start = end;
		}
		return buffer.toString();
	}

	/**
	 * 判断是否为unicode编码串 注：这里只判断一个是否为unicode
	 * 
	 * @param source
	 * @return
	 */
	public static boolean firstIsUnicode(String source) {
		if (source.indexOf("\\u") == 0) {
			try {
				Integer.parseInt(source.substring(2, 6), 16);
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * 
	 * 中文转unicode
	 * 
	 * @param str
	 * 
	 * @return 反回unicode编码
	 */

	public static String GBK2Unicode(String text) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < text.length(); i++) {
			char chr1 = (char) text.charAt(i);
			if (!isChinese(chr1)) {
				result.append(chr1);
				continue;
			}
			result.append("\\u" + Integer.toHexString((int) chr1));
		}
		return result.toString();
	}

	/**
	 * 判断是否为中文字符
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isChinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		String str = "媒体:靠穷吃穷更可怕";
		String unicode = GBK2Unicode(str);
		byte[] datas = str.getBytes("UTF-8");
		System.out.println(datas.length);
		System.out.println(unicode.length());
		System.out.println(new String(unicode.getBytes(), "UTF-8"));
		byte[] encode =str.getBytes("UTF-8");
		for(byte bytte:unicode.getBytes()){
			System.out.print(bytte+",");
		}
		System.out.println("\r\n");
		for(byte bytte:encode){
			System.out.print(bytte+",");
		}
		
		System.out.println("\r\n");
		for(byte bytte:unicode.getBytes("utf-8")){
			System.out.print(bytte+",");
		}
		System.out.println();
		System.out.println(unicode.getBytes().length);
		System.out.println(decodeUnicode(unicode));
		
		try {
		    String s1 = "\u5e7f\u5c9b\u4e4b\u604b.mp3";
		   
		    byte[] converttoBytes = s1.getBytes("UTF-8");
		    System.out.println("\r\n");
			for(byte bytte:converttoBytes){
				System.out.print(bytte+",");
			}
			System.out.println("\r\n");
		    System.out.println("----"+converttoBytes.length);
		    String s2 = new String(converttoBytes, "UTF-8");
		    System.out.println(s2);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
}
