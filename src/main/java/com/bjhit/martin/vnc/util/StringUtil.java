package com.bjhit.martin.vnc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description
 * @project com.bjhit.martian.common
 * @author guanxianchun
 * @Create 2014-11-25 下午3:13:14
 * @version 1.0
 */
public class StringUtil {
	
	private static final String ipRegex = "^(([0-2]*[0-9]+[0-9]+)\\.([0-2]*[0-9]*[0-9]+)\\.([0-2]*[0-9]*[0-9]+)\\.([0-2]*[0-9]*[0-9]+))$";
	
	/**
	 * 判断二字符串相等
	 * @param first
	 * @param secondary
	 * @return
	 */
	public static boolean equal(String first,String secondary) {
		if (first != null) {
			return first.equals(secondary);
		}
		if(secondary != null){
			return secondary.equals(first);
		}
		return true;
	}
	
	public static boolean equalsIgnoreCase(String first,String secondary) {
		if (first != null) {
			return first.equalsIgnoreCase(secondary);
		}
		if(secondary != null){
			return secondary.equalsIgnoreCase(first);
		}
		return true;
	}
	/**
	 * 判断字符串是否为空
	 * @param dist
	 * @return
	 */
	public static boolean isEmpty(String dist) {
		return dist ==null?true:"".equals(dist);
	}
	/**
	 * 判断source是否包括sub字符串
	 * @param source
	 * @param sub
	 * @return
	 */
	public static boolean contains(String source,String sub) {
		if (isEmpty(source)) {
			return false;
		}
		return source.contains(sub);
	}
	/**
	 * 判断source是否包含sub字符，且不区分大小写
	 * @param source
	 * @param sub
	 * @return
	 */
	public static boolean containsIgnoreCase(String source,String sub) {
		if (isEmpty(source) || isEmpty(sub)) {
			return false;
		}
		return contains(source.toLowerCase(), sub.toLowerCase());
	}
	
	public static boolean isIpAddress(String ip) {
		if (isEmpty(ip)) {
			return false;
		}
		Pattern pattern = Pattern.compile(ipRegex);
		Matcher matcher = pattern.matcher(ip);
		return matcher.find();
	}
	
}

