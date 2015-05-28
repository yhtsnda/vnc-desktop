package com.bjhit.martin.vnc.decode;

import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.io.CMsgReader;
import com.bjhit.martin.vnc.rfb.ClientMessageHandler;
import com.bjhit.martin.vnc.rfb.Rect;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:18:37
 * @version 1.0
 */

public class RawDecoder extends Decoder {
	CMsgReader reader;
	static LogWriter vlog = new LogWriter("RawDecoder");

	public RawDecoder(CMsgReader reader_) {
		this.reader = reader_;
	}

	public void readRect(Rect r, ClientMessageHandler handler) {
		int x = r.tl.x;
		int y = r.tl.y;
		int w = r.width();
		int h = r.height();
		int[] imageBuf = new int[w * h];
		int nPixels = imageBuf.length;
		int bytesPerRow = w * this.reader.bpp() / 8;
		while (h > 0) {
			int nRows = nPixels / w;
			if (nRows > h)
				nRows = h;
			this.reader.getInStream().readPixels(imageBuf, nPixels, this.reader.bpp() / 8, handler.cp.pf().bigEndian);
			handler.imageRect(new Rect(x, y, x + w, y + nRows), imageBuf);
			h -= nRows;
			y += nRows;
		}
	}
}
