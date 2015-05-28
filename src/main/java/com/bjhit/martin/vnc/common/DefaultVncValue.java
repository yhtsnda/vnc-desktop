package com.bjhit.martin.vnc.common;

import com.bjhit.martin.vnc.client.AliasParameter;
import com.bjhit.martin.vnc.client.BoolParameter;
import com.bjhit.martin.vnc.client.IntParameter;
import com.bjhit.martin.vnc.rfb.StringParameter;

/**
 * @description
 * @project com.bjhit.vnc.vmconsole
 * @author guanxianchun
 * @Create 2015-1-21 上午9:03:04
 * @version 1.0
 */
public class DefaultVncValue {
	public BoolParameter useLocalCursor = new BoolParameter("UseLocalCursor", "Render the mouse cursor locally", true);
	public BoolParameter sendLocalUsername = new BoolParameter("SendLocalUsername", "Send the local username for SecurityTypes " + "such as Plain rather than prompting", true);
	public StringParameter passwordFile = new StringParameter("PasswordFile", "Password file for VNC authentication", "");
	public AliasParameter passwd = new AliasParameter("passwd", "Alias for PasswordFile", passwordFile);
	public BoolParameter autoSelect = new BoolParameter("AutoSelect", "Auto select pixel format and encoding", true);
	public BoolParameter fullColour = new BoolParameter("FullColour", "Use full colour - otherwise 6-bit colour is " + "used until AutoSelect decides the link is " + "fast enough", true);
	public AliasParameter fullColourAlias = new AliasParameter("FullColor", "Alias for FullColour", fullColour);
	public IntParameter lowColourLevel = new IntParameter("LowColorLevel", "Color level to use on slow connections. " + "0 = Very Low (8 colors), 1 = Low (64 colors), " + "2 = Medium (256 colors)", 2);
	public AliasParameter lowColourLevelAlias = new AliasParameter("LowColourLevel", "Alias for LowColorLevel", lowColourLevel);
	public StringParameter preferredEncoding = new StringParameter("PreferredEncoding", "Preferred encoding to use (Tight, ZRLE, " + "hextile or raw) - implies AutoSelect=0", "ZRLE");
	public BoolParameter viewOnly = new BoolParameter("ViewOnly", "Don't send any mouse or keyboard events to " + "the server", false);
	public BoolParameter shared = new BoolParameter("Shared", "Don't disconnect other viewers upon " + "connection - share the desktop instead", false);
	public BoolParameter fullScreen = new BoolParameter("FullScreen", "Full Screen Mode", false);
	public BoolParameter acceptClipboard = new BoolParameter("AcceptClipboard", "Accept clipboard changes from the server", true);
	public BoolParameter sendClipboard = new BoolParameter("SendClipboard", "Send clipboard changes to the server", true);
	public StringParameter menuKey = new StringParameter("MenuKey", "The key which brings up the popup menu", "F8");
	public StringParameter desktopSize = new StringParameter("DesktopSize", "Reconfigure desktop size on the server on " + "connect (if possible)", "");
	public BoolParameter listenMode = new BoolParameter("listen", "Listen for connections from VNC servers", false);
	public StringParameter scalingFactor = new StringParameter("ScalingFactor",
			"Reduce or enlarge the remote desktop image. " + "The value is interpreted as a scaling factor " + "in percent. If the parameter is set to " + "\"Auto\", then automatic scaling is "
					+ "performed. Auto-scaling tries to choose a " + "scaling factor in such a way that the whole " + "remote desktop will fit on the local screen. "
					+ "If the parameter is set to \"FixedRatio\", " + "then automatic scaling is performed, but the " + "original aspect ratio is preserved.", "100");
	public BoolParameter alwaysShowServerDialog = new BoolParameter("AlwaysShowServerDialog", "Always show the server dialog even if a server " + "has been specified in an applet parameter or on "
			+ "the command line", false);
	public StringParameter vncServerName = new StringParameter("Server", "The VNC server <host>[:<dpyNum>] or " + "<host>::<port>", null);
	public IntParameter vncServerPort = new IntParameter("Port", "The VNC server's port number, assuming it is on " + "the host from which the applet was downloaded", 0);
	public BoolParameter acceptBell = new BoolParameter("AcceptBell", "Produce a system beep when requested to by the server.", true);
	public StringParameter via = new StringParameter("via", "Automatically create an encrypted TCP tunnel to " + "machine gateway, then use that tunnel to connect "
			+ "to a VNC server running on host. By default, " + "this option invokes SSH local port forwarding and " + "assumes that the SSH client binary is located at "
			+ "/usr/bin/ssh. Note that when using the -via " + "option, the host machine name should be specified " + "from the point of view of the gateway machine. "
			+ "For example, \"localhost\" denotes the gateway, " + "not the machine on which vncviewer was launched. " + "See the System Properties section below for "
			+ "information on configuring the -via option.", null);

	public StringParameter tunnelMode = new StringParameter("tunnel", "Automatically create an encrypted TCP tunnel to " + "remote gateway, then use that tunnel to connect "
			+ "to the specified VNC server port on the remote " + "host. See the System Properties section below " + "for information on configuring the -tunnel option.", null);

	public BoolParameter customCompressLevel = new BoolParameter("CustomCompressLevel", "Use custom compression level. " + "Default if CompressLevel is specified.", false);
	public IntParameter compressLevel = new IntParameter("CompressLevel", "Use specified compression level " + "0 = Low, 6 = High", 1);
	public BoolParameter noJpeg = new BoolParameter("NoJPEG", "Disable lossy JPEG compression in Tight encoding.", false);
	public IntParameter qualityLevel = new IntParameter("QualityLevel", "JPEG quality level. " + "0 = Low, 9 = High", 8);

	public StringParameter config = new StringParameter("config", "Specifies a configuration file to load.", null);

}
