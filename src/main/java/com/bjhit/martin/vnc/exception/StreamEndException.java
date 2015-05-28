package com.bjhit.martin.vnc.exception;
/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午1:15:27
 * @version 1.0
 */
public class StreamEndException extends BaseException {
	
	public StreamEndException(String messeage) {
		super("SE001","End of Stream", messeage);
	}
	
	public StreamEndException(String code, String descrition, Throwable throwable) {
		super("SE001","End of Stream", throwable);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}

