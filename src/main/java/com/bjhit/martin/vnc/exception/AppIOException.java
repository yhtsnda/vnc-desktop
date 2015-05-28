package com.bjhit.martin.vnc.exception;
/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午1:43:32
 * @version 1.0
 */
@SuppressWarnings("serial")
public class AppIOException extends BaseException{

	public AppIOException(Throwable throwable) {
		super("IOE001", "IO Exception",throwable);
	}

	public AppIOException(String message) {
		super("IOE001", "IO Exception",message);
	}
}

