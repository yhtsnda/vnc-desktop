package com.bjhit.martin.vnc.exception;
/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午1:22:40
 * @version 1.0
 */
@SuppressWarnings("serial")
public class TimeOutException extends BaseException{

	public TimeOutException(String messeage) {
		super("TE001", "time out", messeage);
	}
	
}

