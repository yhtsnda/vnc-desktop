package com.bjhit.martin.vnc.decode;

import com.bjhit.martin.vnc.io.BaseInputStream;
import com.bjhit.martin.vnc.io.CMsgReader;
import com.bjhit.martin.vnc.rfb.ClientMessageHandler;
import com.bjhit.martin.vnc.rfb.Rect;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:19:10
 * @version 1.0
 */

public class RREDecoder extends Decoder {
	CMsgReader reader;

	public RREDecoder(CMsgReader reader_) {
		this.reader = reader_;
	}

	public void readRect(Rect r, ClientMessageHandler handler) {
		BaseInputStream is = this.reader.getInStream();
		int bytesPerPixel = handler.cp.pf().bpp / 8;
		boolean bigEndian = handler.cp.pf().bigEndian;
		int nSubrects = is.readU32();
		int bg = is.readPixel(bytesPerPixel, bigEndian);
		handler.fillRect(r, bg);

		for (int i = 0; i < nSubrects; ++i) {
			int pix = is.readPixel(bytesPerPixel, bigEndian);
			int x = is.readU16();
			int y = is.readU16();
			int w = is.readU16();
			int h = is.readU16();
			handler.fillRect(new Rect(x, y, w, h), pix);
		}
	}
}
