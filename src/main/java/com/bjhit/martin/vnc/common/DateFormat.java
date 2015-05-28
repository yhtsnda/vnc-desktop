package com.bjhit.martin.vnc.common;

public enum DateFormat {
	YYYYMMDD_HHMMSS("yyyyMMddHHmmss"),YYYYMMDD("yyyy-MM-dd"),YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss");
	private DateFormat(String format) {
		this.format = format;
	}
	private String format;
	public String getFormat() {
		return format.toString();
	}
}
