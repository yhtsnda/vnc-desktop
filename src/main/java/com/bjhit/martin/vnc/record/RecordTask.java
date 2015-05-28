package com.bjhit.martin.vnc.record;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.TimerTask;

import com.bjhit.martin.vnc.common.ConnectionInfo;
import com.bjhit.martin.vnc.common.DateFormat;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.common.VideoConstant;
import com.bjhit.martin.vnc.util.DateFormatUtil;
import com.bjhit.martin.vnc.util.WaterMarkUtil;
import com.bjhit.video.api.ImageConvertVideo;

/**
 * 
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-22 上午9:17:23
 * @version 1.0
 */
public class RecordTask extends TimerTask {
	static LogWriter log = new LogWriter("RecordTask");
	private FrameCache frameCache;
	// private AVIOutputStream out = null;
	private int width;
	private int height;
	private BufferedImage newImage;
	private BufferedImage image;
	private Graphics2D g;
	private File aviFile;
	private static final long FILE_MAX_SIZE = 900l << 10 << 10;// 文件最大为900M
	private Object syncObject = new Object();
	private String fileDictory;
	private ConnectionInfo connectionInfo;
	private Font timeFont = new Font("宋体", Font.BOLD, 25);
	private BufferedImage waterPicture;
	private ImageConvertVideo encoder;
	
	public RecordTask(FrameCache frameCache, String fileDictory, ConnectionInfo connectionInfo) throws IOException {
		this.frameCache = frameCache;
		this.fileDictory = fileDictory;
		this.connectionInfo = connectionInfo;
		initVedio();
	}
	String date;
	private void setImageWaterMark(FrameInfo info) {
		
		if (info != null) {
			image = info.getImage();
		}
		if (image == null) {
			return;
		}
		if (width != image.getWidth() || height != image.getHeight()) {
			width = image.getWidth();
			height = image.getHeight();
			newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		g = (Graphics2D) newImage.getGraphics();
		g.drawImage(image, 0, 0, width, height, null);
		date = DateFormatUtil.getDateTime(DateFormat.YYYY_MM_DD_HH_MM_SS)+"   ";
		WaterMarkUtil.addWaterText(newImage, timeFont, Color.RED, 1.0f, WaterMarkUtil.Position.RightTop, date);
		if (waterPicture !=null) {
			WaterMarkUtil.addWaterPicture(newImage, waterPicture, 0.6f, WaterMarkUtil.Position.RightBottom);
		}
	}

	

	public void initVedio() throws IOException {
		File dictory = new File(fileDictory + System.getProperty("file.separator") + "recFiles");
		if (!dictory.exists()) {
			dictory.mkdir();
		}
		String fileName = connectionInfo.getHost() + "_" + connectionInfo.getPort() + "_" + DateFormatUtil.getDateTime(DateFormat.YYYYMMDD_HHMMSS) + ".mp4";
		aviFile = new File(dictory.getAbsolutePath(), fileName);
		log.info("save avi file path:" + aviFile.getAbsolutePath());
		encoder = new ImageConvertVideo(aviFile, VideoConstant.TIME_SCALE);
		try {
			waterPicture = WaterMarkUtil.loadImageFromResource("waterPicture.png");
		} catch (Exception e) {
			log.error("unload water image :"+e.getMessage());
		}
	}

	public void flushData() {
		try {
			synchronized (syncObject) {
				FrameInfo info;
				while ((info = frameCache.removeFirst()) != null) {
					setImageWaterMark(info);
					encodeImage(info.getTime());
				}
				if (encoder != null) {
					encoder.finish();
					encoder = null;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getFilePath() {
		if (aviFile != null) {
			return aviFile.getAbsolutePath();
		}
		return "";
	}

	@Override
	public void run() {
		FrameInfo info = frameCache.removeFirst();
		if (info == null || info.getImage() == null) {
			return;
		}
		setImageWaterMark(info);
		if (aviFile.length() > FILE_MAX_SIZE) {
			flushData();
			try {
				initVedio();
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}
		try {
			synchronized (syncObject) {
				encodeImage(info.getTime());
			}
		} catch (IOException ie) {
			log.error(ie.getMessage());
		}
	}
	
	private void encodeImage(long scaleTime) throws IOException {
		if (newImage != null && encoder != null) {
			encoder.encodeImage(newImage, VideoConstant.TIME_SCALE,1);
		}
	}
	
	@Override
	public boolean cancel() {
		boolean cancel = super.cancel();
		flushData();
		return cancel;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		flushData();
	}
	
}
