package org.janelia.it.FlyWorkstation.gui.util;

import java.io.File;

import org.hibernate.Hibernate;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Translate between paths to various mounted file resources.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PathTranslator {

    public static final String JACS_DATA_PATH_MAC       = ConsoleProperties.getString("remote.defaultMacPath");
    public static final String JACS_DATA_PATH_LINUX     = ConsoleProperties.getString("remote.defaultLinuxPath");
    public static final String JACS_DATA_PATH_WINDOWS   = ConsoleProperties.getString("remote.defaultWindowsPath");
    public static final String JACS_DATA_MOUNT_MAC      = ConsoleProperties.getString("remote.remoteMacMount");
    public static final String JACS_DATA_MOUNT_WINDOWS  = ConsoleProperties.getString("remote.remoteWindowsMount");

    public static String jacsDataPath;
    
    
    public static void initFromModelProperties(SessionModel sessionModel) {
    	System.out.println("Initializing Path Translator");

    	jacsDataPath = (String)sessionModel.getModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY);
        if (jacsDataPath == null) {
        	File jacsData = new File(PathTranslator.JACS_DATA_PATH_LINUX);
            if (jacsData.canRead()) {
            	jacsDataPath = jacsData.getAbsolutePath();	
            }
            else {
            	jacsDataPath = PathTranslator.getOsSpecificRootPath();
            }
            sessionModel.setModelProperty(SessionMgr.JACS_DATA_PATH_PROPERTY, jacsDataPath);
        }
        
        System.out.println("    Using JACS data path: "+jacsDataPath);
    }
    
    /**
     * Converts the given path to the current platform. 
     * @param filepath original path to the item
     * @return returns the most appropriate path to the file based on mount type and OS
     */
    public static String convertPath(String filepath) {

        if (!jacsDataPath.startsWith(JACS_DATA_PATH_LINUX)) {
        	filepath = filepath.replace(JACS_DATA_PATH_LINUX, jacsDataPath);	
        }
        
        if (SystemInfo.isWindows) {
        	filepath = filepath.replaceAll("/", "\\\\");
        }
        
        return filepath;
    }

    /**
     * Modify the given entity tree so that any file path attributes are appropriate for the current platform.
     */
    public static Entity translatePathsToCurrentPlatform(Entity entity) {

        for (EntityData entityData : entity.getEntityData()) {
            if (entityData.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_FILE_PATH)) {
                entityData.setValue(convertPath(entityData.getValue()));
            }
            else {
                Entity child = entityData.getChildEntity();
                if (child!=null && Hibernate.isInitialized(child)) {
                    translatePathsToCurrentPlatform(child);
                }
            }
        }

        return entity;
    }

    
    public static boolean isMounted() {
        File jacsData = new File(jacsDataPath);
        return jacsData.canRead();
    }
    
    public static String getMountHelpMessage() {
        String message = "";
        if (SystemInfo.isMac) {
            message = "The jacsData file share is not mounted. From Finder choose 'Go' and 'Connect to Server' " +
                    "then enter '"+JACS_DATA_MOUNT_MAC+"' and press 'Connect'.";
        }
        else if (SystemInfo.isLinux) {
            message = "The jacsData file share is not mounted as "+jacsDataPath+". Please contact the Helpdesk.";
        }
        else if (SystemInfo.isWindows) {
            message = "The jacsData file share is not mounted as "+jacsDataPath+". From Windows Explorer choose 'Tools' and 'Map Network Drive' "+
                    "then choose 'Drive' Q and specify folder "+JACS_DATA_MOUNT_WINDOWS+" and press 'Finish'.";
        }
        return message;
    }

    public static String getOsSpecificRootPath() {
        if (SystemInfo.isMac) { return PathTranslator.JACS_DATA_PATH_MAC; }
        else if (SystemInfo.isLinux) { return PathTranslator.JACS_DATA_PATH_LINUX; }
        else if (SystemInfo.isWindows) {return PathTranslator.JACS_DATA_PATH_WINDOWS; }
        return "";
    }

	public static String getJacsDataPath() {
		return jacsDataPath;
	}
}
