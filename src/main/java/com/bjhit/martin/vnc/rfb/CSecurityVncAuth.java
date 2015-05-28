/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
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
import com.bjhit.martin.vnc.exception.BaseException;
import com.bjhit.martin.vnc.io.BaseInputStream;
import com.bjhit.martin.vnc.io.OutStream;
import com.bjhit.martin.vnc.util.Base64Util;
import com.bjhit.martin.vnc.util.DescryptUtil;
import com.bjhit.martin.vnc.util.StringUtil;

public class CSecurityVncAuth extends CSecurity {

	public CSecurityVncAuth() {
	}

	private static final int vncAuthChallengeSize = 16;

	public boolean processMsg(CConnection cc) {
		BaseInputStream is = cc.getInStream();
		OutStream os = cc.getOutStream();

		// Read the challenge & obtain the user's password
		byte[] challenge = new byte[vncAuthChallengeSize];
		is.readBytes(challenge, 0, vncAuthChallengeSize);
		StringBuffer passwd = new StringBuffer();
		if (cc.getConnInfo() == null || StringUtil.isEmpty(cc.getConnInfo().getEncodePassword())) {
			CConn.upg.getUserPasswd(null, passwd);
		}else {
			if (!StringUtil.isEmpty(cc.getConnInfo().getEncodePassword())) {
				try {
					cc.getConnInfo().setPassword(new String(DescryptUtil.decrypt(Base64Util.decode(cc.getConnInfo().getEncodePassword()), DescryptUtil.DEFAULT_KEY), "utf-8"));
				}  catch (Exception e) {
					throw new BaseException("DECE001", "decode exception", e);
				}
			}
			passwd.append(cc.getConnInfo().getPassword());
		}
		// Calculate the correct response
		byte[] key = new byte[8];
		int pwdLen = passwd.length();
		byte[] utf8str = new byte[pwdLen];
		try {
			utf8str = passwd.toString().getBytes("UTF8");
		} catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < 8; i++)
			key[i] = i < pwdLen ? utf8str[i] : 0;
		DesCipher des = new DesCipher(key);
		for (int j = 0; j < vncAuthChallengeSize; j += 8)
			des.encrypt(challenge, j, challenge, j);

		// Return the response to the server
		os.writeBuffer(challenge, 0, vncAuthChallengeSize);
		return true;
	}

	public int getType() {
		return Security.secTypeVncAuth;
	}

	public String description() {
		return "No Encryption";
	}

	static LogWriter vlog = new LogWriter("VncAuth");
}
