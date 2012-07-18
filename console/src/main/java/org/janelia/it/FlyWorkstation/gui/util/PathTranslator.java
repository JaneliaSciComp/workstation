package org.janelia.it.FlyWorkstation.gui.util;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.io.File;

/**
 * Translate between paths to various mounted file resources.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PathTranslator {

    private static final String JACS_DATA_PATH_MAC       = ConsoleProperties.getString("remote.defaultMacPath");
    private static final String JACS_DATA_PATH_LINUX     = ConsoleProperties.getString("remote.defaultLinuxPath");
    private static final String JACS_DATA_PATH_WINDOWS   = ConsoleProperties.getString("remote.defaultWindowsPath");
    private static final String JACS_DATA_MOUNT_MAC      = ConsoleProperties.getString("remote.remoteMacMount");
    private static final String JACS_DATA_MOUNT_WINDOWS  = ConsoleProperties.getString("remote.remoteWindowsMount");

    public static boolean isMounted() {

        File jacsData = new File(JACS_DATA_PATH_LINUX);
        if (jacsData.canRead()) return true;

        jacsData = new File(JACS_DATA_PATH_MAC);
        if (jacsData.canRead()) return true;

        jacsData = new File(JACS_DATA_PATH_WINDOWS);
        return jacsData.canRead();
    }

    /**
     * Converts the given path to the current platform. 
     * @param filepath original path to the item
     * @return returns the most appropriate path to the file based on mount type and OS
     */
    public static String convertPath(String filepath) {
        // This is a little optimization for Macs that have /groups mounted. It's faster than using /Volumes.
        // Makes the assumption that all paths passed by the system are in the context of Linux
        File jacsData = new File(JACS_DATA_PATH_LINUX);
        if (jacsData.canRead()) return filepath;

        //System.out.println("convertPath() called with filepath="+filepath);
        // General filter for nfs vs /Volumes
        if (SystemInfo.isMac) {
            //System.out.println("SystemInfo.isMac() true");
            String[] pathComponents=filepath.split("/");
            //System.out.println("After split");
            if (pathComponents!=null && pathComponents.length>2) {
                int offset=0;
                if (pathComponents[0].trim().length()==0) {
                    offset=1;
                }
                //System.out.println("pathComponents offset="+offset+" length="+pathComponents.length);
                String nfsStylePath="/" + pathComponents[offset] + "/" + pathComponents[1+offset] + "/" + pathComponents[2+offset];
                File nfsStyleFile=new File(nfsStylePath);
                if (nfsStyleFile.canRead()) {
                    //System.out.println("Returning nfs-style path="+filepath);
                    return filepath;
                } else {
                    String macStylePath="/Volumes/"+pathComponents[2+offset];
                    File macStyleFile=new File(macStylePath);
                    if (macStyleFile.canRead()) {
                        StringBuilder macFullPath=new StringBuilder(macStylePath);
                        for (int i=(3+offset);i<pathComponents.length;i++) {
                            macFullPath.append("/").append(pathComponents[i]);
                        }
                        //System.out.println("Returning mac-style path="+macFullPath.toString());
                        return macFullPath.toString();
                    } else {
                        //System.out.println("Cannot read mac-style path="+macStyleFile.getAbsolutePath());
                    }
                }
            }
        }

        if (SystemInfo.isWindows) {
            filepath = filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_WINDOWS);
            return filepath.replaceAll("/", "\\\\");
        }
        return filepath;
    }

    /**
     * Modify the given entity tree so that any file path attributes are appropriate for the current platform.
     */
    public static Entity translatePathsToCurrentPlatform(Entity entity) {

        for (EntityData entityData : entity.getEntityData()) {
            if (entityData.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_FILE_PATH)) {
                entityData.setValue(PathTranslator.convertPath(entityData.getValue()));
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

    public static String getMountHelpMessage() {
        String message = "";
        if (SystemInfo.isMac) {
            message = "The jacsData file share is not mounted. From Finder choose 'Go' and 'Connect to Server' " +
                    "then enter '"+JACS_DATA_MOUNT_MAC+"' and press 'Connect'.";
        }
        else if (SystemInfo.isLinux) {
            message = "The jacsData file share is not mounted as "+JACS_DATA_PATH_LINUX+". Please contact the Helpdesk.";
        }
        else if (SystemInfo.isWindows) {
            message = "The jacsData file share is not mounted as "+JACS_DATA_PATH_WINDOWS+". From Windows Explorer choose 'Tools' and 'Map Network Drive' "+
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
}
