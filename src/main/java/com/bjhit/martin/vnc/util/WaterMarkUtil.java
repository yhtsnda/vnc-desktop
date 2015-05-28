package com.bjhit.martin.vnc.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.bjhit.martin.vnc.record.RecordTask;
import com.bjhit.martin.vnc.rfb.Point;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-2-28 上午8:50:48
 * @version 1.0
 */
public class WaterMarkUtil {
	
	public static int BorderWidth = 12; 
	
	/**
	 * 添加文字水印
	 * 
	 * @param image
	 *            图片
	 * @param point
	 *            坐标
	 * @param font
	 *            字体
	 * @param fontColor
	 *            字体颜色
	 * @param waterText
	 *            水印文字
	 */
	public static void addWaterText(BufferedImage image, Point point, Font font, Color fontColor,Float alpha, String waterText) {
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setFont(font);
		g.setColor(fontColor);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g.drawString(waterText, point.x, point.y);
		g.dispose();
	}

	/**
	 * 添加文字水印
	 * 
	 * @param image
	 *            图片
	 * @param font
	 *            字体
	 * @param fontColor
	 *            字体颜色
	 * @param position
	 *            方位
	 * @param waterText
	 *            水印文字
	 */
	public static void addWaterText(BufferedImage image, Font font, Color fontColor,Float alpha, Position position, String waterText) {
		Point point = getPoint(image.getWidth(), image.getHeight(), position, waterText, font);
		addWaterText(image, point, font, fontColor,alpha, waterText);
	}

	public static void addWaterPicture(BufferedImage image, BufferedImage waterPicture,Float alpha, Point point) {
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g.setBackground(Color.red);
		g.drawImage(waterPicture, point.x, point.y, null);
		g.dispose();
	}

	public static void addWaterPicture(BufferedImage image, BufferedImage waterPicture, Float alpha,Position position) {
		Point point = getPoint(image.getWidth(), image.getHeight(), position, waterPicture.getWidth(), waterPicture.getHeight());
		addWaterPicture(image, waterPicture,alpha, point);
	}

	protected static Point getPoint(int maxX, int maxY, Position position, int width, int height) {
		Point point = new Point();
		switch (position) {
		case LeftTop:
			point.y = BorderWidth;
			point.x = BorderWidth;
			break;
		case LeftBottom:
			point.y = maxY - height - BorderWidth;
			point.x = BorderWidth;
			break;
		case RightTop:
			point.y = BorderWidth;
			point.x = maxX - width - BorderWidth;
			break;
		case RightBottom:
			point.y = maxY -height - BorderWidth;
			point.x = maxX - width - BorderWidth;
			break;
		case West:
			point.y = maxY / 2;
			point.x = BorderWidth;
			break;
		case North:
			point.y = BorderWidth;
			point.x = (maxX - width) / 2;
			break;
		case East:
			point.y = maxY / 2;
			point.x = maxX - width - BorderWidth;
			break;
		case South:
			point.y = maxY - height - BorderWidth;
			point.x = (maxX - width) / 2;
			break;
		case Center:
			point.y = maxY / 2;
			point.x = (maxX - width) / 2;
			break;
		default:
			break;
		}
		return point;
	}

	/**
	 * 得到水印文字的x,y坐标点
	 * 
	 * @param maxX
	 * @param maxY
	 * @param position
	 * @param waterText
	 * @param font
	 * @return
	 */
	protected static Point getPoint(int maxX, int maxY, Position position, String waterText, Font font) {
		Point point = new Point();
		switch (position) {
		case LeftTop:
			point.y = BorderWidth+font.getSize();
			point.x = BorderWidth;
			break;
		case LeftBottom:
			point.y = maxY - font.getSize() - BorderWidth;
			point.x = BorderWidth;
			break;
		case RightTop:
			point.y = BorderWidth+font.getSize();
			point.x = maxX - getLength(waterText) * font.getSize() - BorderWidth;
			break;
		case RightBottom:
			point.y = maxY - font.getSize() - BorderWidth;
			point.x = maxX - getLength(waterText) * font.getSize() - BorderWidth;
			break;
		case West:
			point.y = maxY / 2;
			point.x = BorderWidth;
			break;
		case North:
			point.y = BorderWidth+font.getSize();
			point.x = (maxX - getLength(waterText) * font.getSize()) / 2;
			break;
		case East:
			point.y = maxY / 2;
			point.x = maxX - getLength(waterText) * font.getSize() - BorderWidth;
			break;
		case South:
			point.y = maxY - font.getSize() - BorderWidth;
			point.x = (maxX - getLength(waterText) * font.getSize()) / 2;
			break;
		case Center:
			point.y = maxY / 2;
			point.x = (maxX - getLength(waterText) * font.getSize()) / 2;
			break;
		default:
			break;
		}
		return point;
	}

	/**
	 * 计算字符串的像素长度
	 * 
	 * @param text
	 * @return
	 */
	public final static int getLength(String text) {
		int length = 0;
		for (int i = 0; i < text.length(); i++) {
			if (new String(text.charAt(i) + "").getBytes().length > 1) {
				length += 2;
			} else {
				length += 1;
			}
		}
		return length / 2+1;
	}

	public enum Position {
		LeftTop, LeftBottom, RightTop, RightBottom, West, East, South, North, Center;
	}

	public static BufferedImage loadImageFromResource(String fileName) throws IOException {
		InputStream in = RecordTask.class.getClassLoader().getResourceAsStream(fileName);
		if (in != null) {
			return ImageIO.read(in);
		}else {
			return null;
		}
	}
}
