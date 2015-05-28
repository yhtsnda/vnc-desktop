package com.bjhit.martin.vnc.util;
/**
 * @description
 * @project vmconsole
 * @author guanxianchun
 * @Create 2014-12-29 上午10:02:00
 * @version 1.0
 */
public class ScreenPropertyUtil {
	
	/**
	 * 得到当前用户目录
	 * @return
	 */
	public static String getUserHomeDictory() {
		return System.getProperty("user.home");
	}
	
	/**
	 * 得到当前用户目录
	 * @return
	 */
	public static String getRunningDictory() {
		return System.getProperty("user.dir");
	}
	/**
	 * 像素压缩比
	 * @return
	 */
	public static float getDefaultCompQuality() {
		return 0.6f;
	}

}

