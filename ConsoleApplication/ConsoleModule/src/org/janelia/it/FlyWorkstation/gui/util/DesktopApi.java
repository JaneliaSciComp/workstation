package org.janelia.it.FlyWorkstation.gui.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A desktop API that supports more Linux desktop managers.
 * 
 * Adapted from MightPork's class here:
 * http://stackoverflow.com/questions/18004150
 * /desktop-api-is-not-supported-on-the-current-platform
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DesktopApi {
	
    private static final Logger log = LoggerFactory.getLogger(DesktopApi.class);
	
	public static boolean browse(File file) {
		if (openSystemSpecific(file, true)) return true;
		if (browseDesktop(file.toURI())) return true;
		return false;
	}

	public static boolean open(File file) {
		if (openSystemSpecific(file, false)) return true;
		if (openDesktop(file)) return true;
		return false;
	}

	private static boolean openSystemSpecific(File file, boolean reveal) {

		String path = file.getAbsolutePath();
		if (SystemInfo.isLinux) {
			if (reveal && file.isFile()) {
		        path = file.getParentFile().getAbsolutePath();
			}
			if (runCommand("kde-open", "%s", path)) return true;
			if (runCommand("gnome-open", "%s", path)) return true;
			if (runCommand("xdg-open", "%s", path)) return true;
		}
		else if (SystemInfo.isMac) {
            if (reveal && file.isFile()) {
            	if (runCommand("open", "-R %s", path)) return true;
            }
            else {
            	if (runCommand("open", "%s", path)) return true;
            }
		}
		else if (SystemInfo.isWindows) {
            if (reveal && file.isFile()) {
            	if (runCommand("explorer", "/select,%s", path)) return true;
            }
            else {
            	if (runCommand("explorer", "/e,%s", path)) return true;
            }
		}
		else {
			throw new RuntimeException("Desktop interaction is not supported with this operating system: "+SystemInfo.OS_NAME);
		}

		return false;
	}

	private static boolean browseDesktop(URI uri) {

		log.debug("Trying to use Desktop.getDesktop().browse() with {}", uri);
		try {
			if (!Desktop.isDesktopSupported()) {
				log.warn("Platform is not supported.");
				return false;
			}

			if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				log.warn("Browse action is not supported.");
				return false;
			}

			Desktop.getDesktop().browse(uri);

			return true;
		} catch (Throwable t) {
			log.error("Error using desktop browse.", t);
			return false;
		}
	}

	private static boolean openDesktop(File file) {

		log.debug("Trying to use Desktop.getDesktop().open() with "
				+ file.toString());
		try {
			if (!Desktop.isDesktopSupported()) {
				log.warn("Platform is not supported.");
				return false;
			}

			if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				log.warn("Open action is not supported.");
				return false;
			}

			Desktop.getDesktop().open(file);

			return true;
		} catch (Throwable t) {
			log.error("Error using desktop open.", t);
			return false;
		}
	}

	private static boolean runCommand(String command, String args, String file) {

		String[] parts = prepareCommand(command, args, file);
		log.debug("Trying to exec: {}", StringUtils.getCommaDelimited(parts));

		try {
			Process p = Runtime.getRuntime().exec(parts);
			if (p == null)
				return false;

			try {
				int returnValue = p.exitValue();
				if (returnValue == 0) {
					log.warn("Process ended immediately.");
					return false;
				} else {
					log.warn("Process crashed.");
					return false;
				}
			} catch (IllegalThreadStateException itse) {
				log.trace("Process is running.");
				return true;
			}
		} catch (IOException e) {
			log.error("Error running command.", e);
			return false;
		}
	}

	private static String[] prepareCommand(String command, String args, String file) {

		List<String> parts = new ArrayList<String>();
		parts.add(command);

		if (args != null) {
			for (String s : args.split(" ")) {
				s = String.format(s, file); 
				parts.add(s.trim());
			}
		}

		return parts.toArray(new String[parts.size()]);
	}

}