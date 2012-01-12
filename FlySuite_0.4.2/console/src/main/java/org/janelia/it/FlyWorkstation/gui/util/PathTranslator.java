package org.janelia.it.FlyWorkstation.gui.util;

import java.io.File;
import java.util.List;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

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
 
		File jacsData = new File(JACS_DATA_PATH_LINUX);
		if (jacsData.canRead()) return true;
		
    	jacsData = new File(JACS_DATA_PATH_MAC);
    	return jacsData.canRead();
    }
    
    /**
     * Converts the given path to the current platform. 
     * @param filepath
     * @return
     */
    public static String convertPath(String filepath) {
    	
    	// This is a little optimization for Macs that have /groups mounted. It's faster than using /Volumes. 
		File jacsData = new File(JACS_DATA_PATH_LINUX);
		if (jacsData.canRead()) return filepath;
    	
		if (SystemInfo.isMac) {
    		return filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_MAC);	
    	}
    	return filepath;
    }

    /**
     * Calls translatePathsToCurrentPlatform() for every entity in the given list.
     * 
     * @param entities
     * @return
     */
    public static List<Entity> translatePathsToCurrentPlatform(List<Entity> entities) {
    	for(Entity entity : entities) {
    		translatePathsToCurrentPlatform(entity);
    	}
    	return entities;
    }

    /**
     * Modify the given entity tree so that any file path attributes are appropriate for the current platform.
     * 
     * @param entity
     * @return
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
}
