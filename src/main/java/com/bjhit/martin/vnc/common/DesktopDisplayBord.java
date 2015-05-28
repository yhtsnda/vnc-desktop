package com.bjhit.martin.vnc.common;

import com.bjhit.martin.vnc.rfb.PixelFormat;
import com.bjhit.martin.vnc.rfb.Point;
import com.bjhit.martin.vnc.rfb.Rect;

/**
 * @description 
 * @project com.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-9 上午11:08:03
 * @version 1.0
 */
public interface DesktopDisplayBord {
	public void setColourMapEntries(int firstColour, int nColours, int[] rgbs);
	public void fillRect(Rect rect, int pixel);
	public void imageRect(Rect rect, Object obj);
	public void copyRect(Rect r, int srcx, int srcy);
	public void setCursor(int width, int height, Point hotspot, int[] data, byte[] mask);
	public void setPixelFormat(PixelFormat pixelFormat);
	public PixelFormat getPixelFormat();
	public void setPlatformPixelBuffer(PlatformPixelBuffer pixelBuffer);
	public PlatformPixelBuffer getPixelBuffer();
	public void setSize(int width, int height);
}

