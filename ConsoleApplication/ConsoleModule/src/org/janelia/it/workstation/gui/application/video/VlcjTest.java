package org.janelia.it.workstation.gui.application.video;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

import com.sun.jna.NativeLibrary;

/**
 * Base class for tests.
 * <p>
 * This makes it a lot easier to switch vlc versions or vlc install directories
 * without having to change system properties on a lot of IDE application run-
 * configurations.
 * <p>
 * Explicitly setting a search path forces JNA to search that path
 * <em>first</em>.
 * <p>
 * The search path should be the directory that contains libvlc.so and
 * libvlccore.so.
 * <p>
 * If you do not explicitly set the search path, the system search path will be
 * used.
 * <p>
 * You can also set the log level here.
 */
public abstract class VlcjTest {

	/**
	 * Log level, used only if the -Dvlcj.log= system property has not already
	 * been set.
	 */
	private static final String VLCJ_LOG_LEVEL = "INFO";

	/**
	 * Change this to point to your own vlc installation, or comment out the
	 * code if you want to use your system default installation.
	 * <p>
	 * This is a bit more explicit than using the -Djna.library.path= system
	 * property.
	 */
	private static final String NATIVE_LIBRARY_SEARCH_PATH = "/Applications/VLC.app/Contents/MacOS/lib";

	/**
	 * Static initialisation.
	 */
	static {
		if (null == System.getProperty("vlcj.log")) {
			System.setProperty("vlcj.log", VLCJ_LOG_LEVEL);
		}

		// Safely try to initialise LibX11 to reduce the opportunity for native
		// crashes - this will silently throw an Error on Windows (and maybe
		// MacOS) that can safely be ignored
		LibXUtil.initialise();

		if (null != NATIVE_LIBRARY_SEARCH_PATH) {
			// Logger.info("Explicitly adding JNA native library search path: '{}'",
			// NATIVE_LIBRARY_SEARCH_PATH);
			NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), NATIVE_LIBRARY_SEARCH_PATH);
		}
	}

	/**
	 * Set the standard look and feel.
	 */
	protected static final void setLookAndFeel() {
		String lookAndFeelClassName = null;
		LookAndFeelInfo[] lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
		for (LookAndFeelInfo lookAndFeel : lookAndFeelInfos) {
			if ("Nimbus".equals(lookAndFeel.getName())) {
				lookAndFeelClassName = lookAndFeel.getClassName();
			}
		}
		if (lookAndFeelClassName == null) {
			lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
		}
		try {
			UIManager.setLookAndFeel(lookAndFeelClassName);
		} catch (Exception e) {
			// Silently fail, it doesn't matter
		}
	}
}
