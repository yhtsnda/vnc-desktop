package com.bjhit.martin.vnc.decode;

import com.bjhit.martin.vnc.io.BaseInputStream;
import com.bjhit.martin.vnc.io.CMsgReader;
import com.bjhit.martin.vnc.rfb.ClientMessageHandler;
import com.bjhit.martin.vnc.rfb.Rect;

public class HextileDecoder extends Decoder {
	CMsgReader reader;

	public HextileDecoder(CMsgReader reader_) {
		this.reader = reader_;
	}

	public void readRect(Rect r, ClientMessageHandler handler) {
		BaseInputStream is = this.reader.getInStream();
		int bytesPerPixel = handler.cp.pf().bpp / 8;
		boolean bigEndian = handler.cp.pf().bigEndian;

		int[] buf = this.reader.getImageBuf(1024);

		Rect t = new Rect();
		int bg = 0;
		int fg = 0;

		for (t.tl.y = r.tl.y; t.tl.y < r.br.y; t.tl.y += 16) {
			t.br.y = Math.min(r.br.y, t.tl.y + 16);

			for (t.tl.x = r.tl.x; t.tl.x < r.br.x; t.tl.x += 16) {
				t.br.x = Math.min(r.br.x, t.tl.x + 16);

				int tileType = is.readU8();

				if ((tileType & 0x1) != 0) {
					is.readPixels(buf, t.area(), bytesPerPixel, bigEndian);
					handler.imageRect(t, buf);
				} else {
					if ((tileType & 0x2) != 0) {
						bg = is.readPixel(bytesPerPixel, bigEndian);
					}
					int len = t.area();
					int ptr = 0;
					for (; len-- > 0; buf[(ptr++)] = bg)
						;
					if ((tileType & 0x4) != 0) {
						fg = is.readPixel(bytesPerPixel, bigEndian);
					}
					if ((tileType & 0x8) != 0) {
						int nSubrects = is.readU8();

						for (int i = 0; i < nSubrects; ++i) {
							if ((tileType & 0x10) != 0) {
								fg = is.readPixel(bytesPerPixel, bigEndian);
							}
							int xy = is.readU8();
							int wh = is.readU8();

							int x = xy >> 4 & 0xF;
							int y = xy & 0xF;
							int w = (wh >> 4 & 0xF) + 1;
							int h = (wh & 0xF) + 1;
							ptr = y * t.width() + x;
							int rowAdd = t.width() - w;
							while (h-- > 0) {
								len = w;
								for (; len-- > 0; buf[(ptr++)] = fg)
									;
								ptr += rowAdd;
							}
						}
					}
					handler.imageRect(t, buf);
				}
			}
		}
	}
}