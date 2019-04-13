package org.janelia.workstation.core.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.ForbiddenEntity;
import org.janelia.workstation.core.api.ServiceMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translate between paths to various mounted file resources.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PathTranslator {

    private static final Logger log = LoggerFactory.getLogger(PathTranslator.class);

    public static String JACS_DATA_PATH_PROPERTY = "SessionMgr.JacsDataPathProperty";
    
    public static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
    public static final String JACS_DATA_PATH_NFS = ConsoleProperties.getString("remote.defaultLinuxDataPath");
    public static final String JACS_DEPLOYMENT_PATH_NFS = ConsoleProperties.getString("remote.defaultLinuxDeploymentPath");
    public static final String JACS_DATA_PATH_WINDOWS = ConsoleProperties.getString("remote.defaultWindowsPath");
    public static final String JACS_DATA_MOUNT_MAC = ConsoleProperties.getString("remote.remoteMacMount");
    public static final String JACS_DATA_MOUNT_WINDOWS = ConsoleProperties.getString("remote.remoteWindowsMount");

    public static String jacsDataPath;

    static {

        jacsDataPath = (String) FrameworkImplProvider.getModelProperty(JACS_DATA_PATH_PROPERTY);
        if (jacsDataPath == null) {
            File jacsData = new File(PathTranslator.JACS_DATA_PATH_NFS);
            if (jacsData.canRead()) {
                jacsDataPath = jacsData.getAbsolutePath();
            }
            else {
                jacsDataPath = PathTranslator.getOsSpecificRootPath();
            }
            FrameworkImplProvider.setModelProperty(JACS_DATA_PATH_PROPERTY, jacsDataPath);
        }

        log.info("Using JACS data path: " + jacsDataPath);
    }

    /**
     * Converts the given path to the current platform.
     *
     * @param filepath original path to the item
     * @return returns the most appropriate path to the file based on mount type and OS
     */
    public static String convertPath(String filepath) {

        if (!jacsDataPath.startsWith(JACS_DATA_PATH_NFS)) {
            filepath = filepath.replace(JACS_DATA_PATH_NFS, jacsDataPath);
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

        for (EntityData entityData : getAccessibleEntityDatas(entity)) {
            if (entityData.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_FILE_PATH)) {
                entityData.setValue(convertPath(entityData.getValue()));
            }
            else {
                Entity child = entityData.getChildEntity();
                if (child != null && Hibernate.isInitialized(child)) {
                    translatePathsToCurrentPlatform(child);
                }
            }
        }

        return entity;
    }

    /**
     * Modify the given entity tree so that any file path attributes are proxied by our local file service.
     */
    public static Entity translatePathsToProxy(Entity entity) {

        for (EntityData entityData : getAccessibleEntityDatas(entity)) {
            if (isTranslatePath(entityData)) {
                String path = entityData.getValue();
                try {
                    String url = getProxiedFileUrl(path).toString();
                    log.info("Returning filepath = "+url);
                    entityData.setValue(url);
                }
                catch (MalformedURLException e) {
                    log.error("Error translating path to proxy: " + path, e);
                }
            }
            else {
                Entity child = entityData.getChildEntity();
                if (child != null && Hibernate.isInitialized(child)) {
                    translatePathsToCurrentPlatform(child);
                }
            }
        }

        return entity;
    }
    
    private static boolean isTranslatePath(EntityData entityData) {
        String attrName = entityData.getEntityAttrName();
        return EntityConstants.ATTRIBUTE_FILE_PATH.equals(attrName)
                || EntityConstants.ATTRIBUTE_VISUALLY_LOSSLESS_IMAGE.equals(attrName);
    }

    private static String getOsSpecificRootPath() {
        if (SystemInfo.isMac) {
            return PathTranslator.JACS_DATA_PATH_MAC;
        }
        else if (SystemInfo.isLinux) {
            return PathTranslator.JACS_DATA_PATH_NFS;
        }
        else if (SystemInfo.isWindows) {
            return PathTranslator.JACS_DATA_PATH_WINDOWS;
        }
        return "";
    }
    
    // TODO: move this somewhere. It used to be in the FileProxyService before NG refactoring madness. 
    public static URL getProxiedFileUrl(String standardPath) throws MalformedURLException {
        return new URL("http://localhost:"+ServiceMgr.getServiceMgr().getWebServerPort()+"/webdav"+standardPath);
    }

    private static List<EntityData> getAccessibleEntityDatas(Entity entity) {
        List<EntityData> entityDatas = new ArrayList<EntityData>();
        for (EntityData ed : entity.getOrderedEntityData()) {
            Entity child = ed.getChildEntity();
            if (child!=null && child instanceof ForbiddenEntity) {
                continue;
            }
            entityDatas.add(ed);
        }
        return entityDatas;
    } 
}
