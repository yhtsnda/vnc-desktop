package com.bjhit.martin.vnc.exception;
/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午4:39:02
 * @version 1.0
 */
@SuppressWarnings("serial")
public class AuthFailureException extends BaseException{

	public AuthFailureException(String messeage) {
		super("AFE001", "auth failed Exception", messeage);
	}
	
}

