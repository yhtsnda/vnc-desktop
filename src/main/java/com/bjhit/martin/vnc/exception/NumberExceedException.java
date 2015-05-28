package com.bjhit.martin.vnc.exception;
/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午1:37:26
 * @version 1.0
 */
@SuppressWarnings("serial")
public class NumberExceedException extends BaseException {

	public NumberExceedException(String messeage) {
		super("NE001", "value exceed max value", messeage);
	}

}

