package com.bjhit.martin.vnc.record;

import java.awt.image.BufferedImage;

/**
 * @description
 * @project vmconsole
 * @author guanxianchun
 * @Create 2014-12-24 上午9:44:52
 * @version 1.0
 */
public class FrameInfo {
	
	private long frameTime;
	
	private BufferedImage image;
	/**
	 * 构造器
	 * @param image
	 * @param consumeTime
	 */
	public FrameInfo(BufferedImage image, long consumeTime) {
		this.image = image;
		this.frameTime = consumeTime;
	}
	/**
	 * @return the time
	 */
	public long getTime() {
		return frameTime;
	}
	/**
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.frameTime = time;
	}
	/**
	 * @return the image
	 */
	public BufferedImage getImage() {
		return image;
	}
	/**
	 * @param image the image to set
	 */
	public void setImage(BufferedImage image) {
		this.image = image;
	}
	
}

