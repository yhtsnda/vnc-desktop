package com.bjhit.martin.vnc.record;

import java.util.Timer;

/**
 * @description
 * @project vmconsole
 * @author guanxianchun
 * @Create 2014-12-24 上午9:52:59
 * @version 1.0
 */
@SuppressWarnings("serial")
public class RecordTimer extends Timer {

	public RecordTimer(boolean isDeamon) {
		super(isDeamon);
	}
	
	public static boolean checkSecurity(Boolean checkUserHome) {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			try {
				if (checkUserHome) {
					security.checkPropertyAccess("user.home");
				}else {
					security.checkPropertyAccess("user.dir");
				}
				security.checkPropertyAccess("file.separator");
			} catch (SecurityException e) {
				System.out.println("SecurityManager restricts session recording.");
				return false;
			}
		}
		return true;
	}

}
