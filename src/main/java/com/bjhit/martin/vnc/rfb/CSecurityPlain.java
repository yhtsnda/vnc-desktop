/* Copyright (C) 2005 Martin Koegler
 * Copyright (C) 2010 TigerVNC Team
 * Copyright (C) 2011 Brian P. Hinz
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

package com.bjhit.martin.vnc.rfb;

import com.bjhit.martin.vnc.client.CConn;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.io.OutStream;

public class CSecurityPlain extends CSecurity {

	public CSecurityPlain() {
	}

	public boolean processMsg(CConnection cc) {
		OutStream os = cc.getOutStream();

		StringBuffer username = new StringBuffer();
		StringBuffer password = new StringBuffer();

		CConn.upg.getUserPasswd(username, password);

		// Return the response to the server
//		os.writeU32(username.length());
//		os.writeU32(password.length());
		byte[] utf8str;
		try {
			byte[] userNames = username.toString().getBytes("UTF8");
//			os.writeBytes(utf8str, 0, username.length());
			byte[] passwords = password.toString().getBytes("UTF8");
//			os.writeBytes(utf8str, 0, password.length());
			byte[] buff = new byte[8+userNames.length+passwords.length];
			buff[0] = (byte) (username.length() >> 24 & 0xff);
			buff[1] = (byte) (username.length() >> 16 & 0xff);
			buff[2] = (byte) (username.length() >> 8 & 0xff);
			buff[3] = (byte) (username.length() & 0xff);
			buff[4] = (byte) (password.length() >> 24 & 0xff);
			buff[5] = (byte) (password.length() >> 16 & 0xff);
			buff[6] = (byte) (password.length() >> 8 & 0xff);
			buff[7] = (byte) (password.length() & 0xff);
			System.arraycopy(userNames, 0, buff, 8, userNames.length);
			System.arraycopy(passwords, 0, buff, 8+userNames.length, passwords.length);
			os.writeBuffer(buff);
		} catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return true;
	}

	public int getType() {
		return Security.secTypePlain;
	}

	public String description() {
		return "ask for username and password";
	}

	static LogWriter vlog = new LogWriter("Plain");
}
