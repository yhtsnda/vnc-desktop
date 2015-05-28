package com.bjhit.martin.vnc.record;

import java.util.LinkedList;

import com.bjhit.martin.vnc.common.LogWriter;

/**
 * @description
 * @project vmconsole
 * @author guanxianchun
 * @Create 2014-12-24 上午9:43:38
 * @version 1.0
 */
public class FrameCache {
	static LogWriter log = new LogWriter("FrameCache");
	
	private static LinkedList<FrameInfo> frameInfos = new LinkedList<FrameInfo>();
	
	public synchronized void addFrame(FrameInfo info) {
		frameInfos.addLast(info);
	}
	
	public synchronized FrameInfo removeFirst() {
		if (frameInfos.size() == 0) {
			return null;
		}
		log.debug("frameCache size:"+frameInfos.size());
		return frameInfos.removeFirst();
	}
}

