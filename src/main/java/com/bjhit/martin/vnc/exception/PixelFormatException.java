package com.bjhit.martin.vnc.exception;
/**
 * @description
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-7 下午2:09:04
 * @version 1.0
 */
@SuppressWarnings("serial")
public class PixelFormatException extends BaseException{

	public PixelFormatException(String messeage) {
		super("PFE001", "Pixel Format Excepption", messeage);
	}

}

