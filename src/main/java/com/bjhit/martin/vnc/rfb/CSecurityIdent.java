/* Copyright (C) 2011 Brian P. Hinz
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
import com.bjhit.martin.vnc.client.UserPasswdGetter;
import com.bjhit.martin.vnc.common.LogWriter;
import com.bjhit.martin.vnc.io.OutStream;

public class CSecurityIdent extends CSecurity {

  public CSecurityIdent() { }

  public boolean processMsg(CConnection cc) {
    OutStream os = cc.getOutStream();

    StringBuffer username = new StringBuffer();

    CConn.upg.getUserPasswd(username, null);

    // Return the response to the server
    try {
      byte[] utf8str = username.toString().getBytes("UTF8");
      byte[] buff = new byte[4+utf8str.length];
      buff[0] = (byte) (username.length() >> 24 & 0xff);
      buff[1] = (byte) (username.length() >> 16 & 0xff);
      buff[2] = (byte) (username.length() >> 8 & 0xff);
      buff[3] = (byte) (username.length() & 0xff);
      System.arraycopy(username, 0, buff, 4, utf8str.length);
      os.writeBuffer(buff);
    } catch(java.io.UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return true;
  }

  public int getType() { return Security.secTypeIdent; }

  java.net.Socket sock;
  UserPasswdGetter upg;

  static LogWriter vlog = new LogWriter("Ident");
  public String description() { return "No Encryption"; }

}
