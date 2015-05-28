package com.bjhit.martin.vnc.exception;

/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午1:02:37
 * @version 1.0
 */

@SuppressWarnings("serial")
public class BaseException extends RuntimeException {
	
	private String code;
	
	private String description;

	public BaseException(String code, String descrition, String messeage) {
		super(messeage);
		this.code = code;
		this.description = descrition;
	}

	public BaseException(String code, String descrition, Throwable throwable) {
		super(throwable.getMessage(),throwable);
		this.code = code;
		this.description = descrition;
	}

	@Override
	public String toString() {
		return description+"("+code+") "+getMessage();
	}
}
