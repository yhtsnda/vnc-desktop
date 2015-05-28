package com.bjhit.martin.vnc.decode;

import java.awt.Image;
import java.awt.Toolkit;

import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.io.BaseInputStream;
import com.bjhit.martin.vnc.io.CMsgReader;
import com.bjhit.martin.vnc.io.ZlibInStream;
import com.bjhit.martin.vnc.rfb.ClientMessageHandler;
import com.bjhit.martin.vnc.rfb.PixelFormat;
import com.bjhit.martin.vnc.rfb.Rect;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:19:38
 * @version 1.0
 */

public class TightDecoder extends Decoder {
	static final int TIGHT_MAX_WIDTH = 2048;
	static final int rfbTightExplicitFilter = 4;
	static final int rfbTightFill = 8;
	static final int rfbTightJpeg = 9;
	static final int rfbTightMaxSubencoding = 9;
	static final int rfbTightFilterCopy = 0;
	static final int rfbTightFilterPalette = 1;
	static final int rfbTightFilterGradient = 2;
	static final int rfbTightMinToCompress = 12;
	static final Toolkit tk = Toolkit.getDefaultToolkit();
	private CMsgReader reader;
	private ZlibInStream[] zis;
	private PixelFormat serverpf;
	private PixelFormat clientpf;
	static LogWriter vlog = new LogWriter("TightDecoder");

	public TightDecoder(CMsgReader reader_) {
		this.reader = reader_;
		this.zis = new ZlibInStream[4];
		for (int i = 0; i < 4; ++i)
			this.zis[i] = new ZlibInStream();
	}

	public void readRect(Rect r, ClientMessageHandler handler) {
		BaseInputStream is = this.reader.getInStream();
		boolean cutZeros = false;
		this.clientpf = handler.getPreferredPF();
		this.serverpf = handler.cp.pf();
		int bpp = this.serverpf.bpp;
		cutZeros = false;
		if ((bpp == 32) && (this.serverpf.is888())) {
			cutZeros = true;
		}

		int comp_ctl = is.readU8();

		boolean bigEndian = handler.cp.pf().bigEndian;

		for (int i = 0; i < 4; ++i) {
			if ((comp_ctl & 0x1) != 0) {
				this.zis[i].reset();
			}
			comp_ctl >>= 1;
		}

		if (comp_ctl == 8) {
			int[] pix = new int[1];
			if (cutZeros) {
				byte[] bytebuf = new byte[3];
				is.readBytes(bytebuf, 0, 3);
				this.serverpf.bufferFromRGB(pix, 0, bytebuf, 0, 1);
			} else {
				pix[0] = is.readPixel(this.serverpf.bpp / 8, this.serverpf.bigEndian);
			}
			handler.fillRect(r, pix[0]);
			return;
		}

		if (comp_ctl == 9) {
			DECOMPRESS_JPEG_RECT(r, is, handler);
			return;
		}

		if (comp_ctl > 9) {
			throw new RuntimeException("TightDecoder: bad subencoding value received");
		}

		int palSize = 0;
		int[] palette = new int[256];
		boolean useGradient = false;

		if ((comp_ctl & 0x4) != 0) {
			int filterId = is.readU8();

			switch (filterId) {
			case 1:
				palSize = is.readU8() + 1;

				if (cutZeros) {
					byte[] tightPalette = new byte[768];
					is.readBytes(tightPalette, 0, palSize * 3);
					this.serverpf.bufferFromRGB(palette, 0, tightPalette, 0, palSize);
				} else {
					is.readPixels(palette, palSize, this.serverpf.bpp / 8, this.serverpf.bigEndian);
				}
				break;
			case 2:
				useGradient = true;
				break;
			case 0:
				break;
			default:
				throw new RuntimeException("TightDecoder: unknown filter code recieved");
			}
		}

		int bppp = bpp;
		if (palSize != 0)
			bppp = (palSize <= 2) ? 1 : 8;
		else if (cutZeros) {
			bppp = 24;
		}

		int rowSize = (r.width() * bppp + 7) / 8;
		int dataSize = r.height() * rowSize;
		int streamId = -1;
		BaseInputStream input;
		if (dataSize < 12) {
			input = is;
		} else {
			int length = is.readCompactLength();
			streamId = comp_ctl & 0x3;
			this.zis[streamId].setUnderlying(is, length);
			input = this.zis[streamId];
		}

		byte[] netbuf = new byte[dataSize];
		input.readBytes(netbuf, 0, dataSize);

		int stride = r.width();
		int[] buf = this.reader.getImageBuf(r.area());

		if (palSize == 0) {
			if (useGradient) {
				if ((bpp == 32) && (cutZeros))
					FilterGradient24(netbuf, buf, stride, r);
				else
					FilterGradient(netbuf, buf, stride, r);
			} else {
				int h = r.height();
				int ptr = 0;
				int srcPtr = 0;
				int w = r.width();
				if (cutZeros) {
					this.serverpf.bufferFromRGB(buf, ptr, netbuf, srcPtr, w * h);
				} else {
					int pixelSize = (bpp >= 24) ? 3 : bpp / 8;
					while (h > 0) {
						for (int i = 0; i < w; ++i) {
							if (bpp == 8)
								buf[(ptr + i)] = (netbuf[(srcPtr + i)] & 0xFF);
							else {
								for (int j = pixelSize - 1; j >= 0; --j)
									buf[(ptr + i)] |= (netbuf[(srcPtr + i + j)] & 0xFF) << j * 8;
							}
						}
						ptr += stride;
						srcPtr += w * pixelSize;
						--h;
					}
				}
			}
		} else {
			int h = r.height();
			int w = r.width();
			int pad = stride - w;
			int ptr = 0;
			int srcPtr = 0;
			if (palSize <= 2) {
				while (h > 0) {
					for (int x = 0; x < w / 8; ++x) {
						int bits = netbuf[(srcPtr++)];
						for (int b = 7; b >= 0; --b) {
							buf[(ptr++)] = palette[(bits >> b & 0x1)];
						}
					}
					if (w % 8 != 0) {
						int bits = netbuf[(srcPtr++)];
						for (int b = 7; b >= 8 - (w % 8); --b) {
							buf[(ptr++)] = palette[(bits >> b & 0x1)];
						}
					}
					ptr += pad;
					--h;
				}
			} else {
				do {
					int endOfRow = ptr + w;
					while (ptr < endOfRow) {
						buf[(ptr++)] = palette[(netbuf[(srcPtr++)] & 0xFF)];
					}
					ptr += pad;
					--h;
				} while (h > 0);
			}

		}

		handler.imageRect(r, buf);

		if (streamId != -1)
			this.zis[streamId].reset();
	}

	private final void DECOMPRESS_JPEG_RECT(Rect r, BaseInputStream is, ClientMessageHandler handler) {
		int compressedLen = is.readCompactLength();
		if (compressedLen <= 0) {
			vlog.info("Incorrect data received from the server.");
		}

		byte[] netbuf = new byte[compressedLen];
		is.readBytes(netbuf, 0, compressedLen);

		Image jpeg = tk.createImage(netbuf);
		jpeg.setAccelerationPriority(1.0F);
		handler.imageRect(r, jpeg);
		jpeg.flush();
	}

	private final void FilterGradient24(byte[] netbuf, int[] buf, int stride, Rect r) {
		int x, y, c;
		byte[] prevRow = new byte[TIGHT_MAX_WIDTH * 3];
		byte[] thisRow = new byte[TIGHT_MAX_WIDTH * 3];
		byte[] pix = new byte[3];
		int[] est = new int[3];

		// Set up shortcut variables
		int rectHeight = r.height();
		int rectWidth = r.width();

		for (y = 0; y < rectHeight; y++) {
			/* First pixel in a row */
			for (c = 0; c < 3; c++) {
				pix[c] = (byte) (netbuf[y * rectWidth * 3 + c] + prevRow[c]);
				thisRow[c] = pix[c];
			}
			serverpf.bufferFromRGB(buf, y * stride, pix, 0, 1);

			/* Remaining pixels of a row */
			for (x = 1; x < rectWidth; x++) {
				for (c = 0; c < 3; c++) {
					est[c] = (int) (prevRow[x * 3 + c] + pix[c] - prevRow[(x - 1) * 3 + c]);
					if (est[c] > 0xFF) {
						est[c] = 0xFF;
					} else if (est[c] < 0) {
						est[c] = 0;
					}
					pix[c] = (byte) (netbuf[(y * rectWidth + x) * 3 + c] + est[c]);
					thisRow[x * 3 + c] = pix[c];
				}
				serverpf.bufferFromRGB(buf, y * stride + x, pix, 0, 1);
			}

			System.arraycopy(thisRow, 0, prevRow, 0, prevRow.length);
		}
	}

	private final void FilterGradient(byte[] netbuf, int[] buf, int stride, Rect r) {
		int x, y, c;
		byte[] prevRow = new byte[TIGHT_MAX_WIDTH];
		byte[] thisRow = new byte[TIGHT_MAX_WIDTH];
		byte[] pix = new byte[3];
		int[] est = new int[3];

		// Set up shortcut variables
		int rectHeight = r.height();
		int rectWidth = r.width();

		for (y = 0; y < rectHeight; y++) {
			/* First pixel in a row */
			// FIXME
			// serverpf.rgbFromBuffer(pix, 0, netbuf, y*rectWidth, 1, cm);
			for (c = 0; c < 3; c++)
				pix[c] += prevRow[c];

			System.arraycopy(pix, 0, thisRow, 0, pix.length);

			serverpf.bufferFromRGB(buf, y * stride, pix, 0, 1);

			/* Remaining pixels of a row */
			for (x = 1; x < rectWidth; x++) {
				for (c = 0; c < 3; c++) {
					est[c] = (int) (prevRow[x * 3 + c] + pix[c] - prevRow[(x - 1) * 3 + c]);
					if (est[c] > 0xff) {
						est[c] = 0xff;
					} else if (est[c] < 0) {
						est[c] = 0;
					}
				}

				// FIXME
				// serverpf.rgbFromBuffer(pix, 0, netbuf, y*rectWidth+x, 1, cm);
				for (c = 0; c < 3; c++)
					pix[c] += est[c];

				System.arraycopy(pix, 0, thisRow, x * 3, pix.length);

				serverpf.bufferFromRGB(buf, y * stride + x, pix, 0, 1);
			}

			System.arraycopy(thisRow, 0, prevRow, 0, prevRow.length);
		}
	}
}
