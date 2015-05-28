package com.bjhit.martin.vnc.decode;

import com.bjhit.martin.vnc.io.CMsgReader;
import com.bjhit.martin.vnc.rfb.ClientMessageHandler;
import com.bjhit.martin.vnc.rfb.Encodings;
import com.bjhit.martin.vnc.rfb.Rect;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-11 下午1:16:58
 * @version 1.0
 */
public abstract class Decoder {
	
	abstract public void readRect(Rect r, ClientMessageHandler handler);

	static public boolean supported(int encoding) {
		/*
		 * return encoding <= Encodings.encodingMax && createFns[encoding];
		 */
		return (encoding == Encodings.encodingRaw || encoding == Encodings.encodingRRE || encoding == Encodings.encodingHextile || encoding == Encodings.encodingTight || encoding == Encodings.encodingZRLE);
	}

	static public Decoder createDecoder(int encoding, CMsgReader reader) {
		/*
		 * if (encoding <= Encodings.encodingMax && createFns[encoding]) return
		 * (createFns[encoding])(reader); return 0;
		 */
		switch (encoding) {
		case Encodings.encodingRaw:
			return new RawDecoder(reader);
		case Encodings.encodingRRE:
			return new RREDecoder(reader);
		case Encodings.encodingHextile:
			return new HextileDecoder(reader);
		case Encodings.encodingTight:
			return new TightDecoder(reader);
		case Encodings.encodingZRLE:
			return new ZRLEDecoder(reader);
		}
		return null;
	}
}

