package org.janelia.it.FlyWorkstation.gui.util;

import java.io.File;

/**
 * Translate between paths to various mounted file resources.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PathTranslator {

	public static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
	public static final String JACS_DATA_PATH_LINUX = ConsoleProperties.getString("remote.defaultLinuxPath");
	public static final String JACS_DATA_MOUNT_MAC = ConsoleProperties.getString("remote.remoteMacMount");
	
    public static boolean isMounted() {
    	if (SystemInfo.isMac) {
    		File jacsData = new File(JACS_DATA_PATH_MAC);
    		return jacsData.canRead();
    	}
		File jacsData = new File(JACS_DATA_PATH_LINUX);
		return jacsData.canRead();    	
    }
    
    public static String convertImagePath(String filepath) {
    	if (SystemInfo.isMac) {
    		return filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_MAC);	
    	}
    	return filepath;
    }
    
}
