package com.bjhit.martin.vnc.record;
/**
 * @description
 * @project bjhit-vmconsole
 * @author guanxianchun
 * @Create 2015-3-2 上午10:55:57
 * @version 1.0
 */
public class StopRecordThread implements Runnable {
	private RecordTask task;
	
	public StopRecordThread(RecordTask task) {
		this.task = task;
	}
	
	@Override
	public void run() {
		task.cancel();
	}
}

