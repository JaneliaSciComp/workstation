package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fosterl
 * Date: 11/13/12
 * Time: 12:58 PM
 * 
 * This class acts as a mediator to find a specific type of filename associated with the given entity.
 */
public class EntityFilenameFetcher {
	
	private static final Logger log = LoggerFactory.getLogger(EntityFilenameFetcher.class);
	
    public enum FilenameType {IMAGE_FAST_3d, NEURON_FRAGMENT_3d, MASK_FILE}

    private static Map<String,String> entityTypeToFileTypeMapping;
    static {
        entityTypeToFileTypeMapping = new HashMap<String,String>();
        entityTypeToFileTypeMapping.put( EntityConstants.TYPE_NEURON_FRAGMENT, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE );
        entityTypeToFileTypeMapping.put( EntityConstants.TYPE_CURATED_NEURON, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE );
        entityTypeToFileTypeMapping.put( EntityConstants.TYPE_SAMPLE, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE );
        entityTypeToFileTypeMapping.put( EntityConstants.TYPE_IMAGE_3D, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE ); //todo change
    }

    private static Map<FilenameType,String> fetchTypeToFileType;
    static {
        fetchTypeToFileType = new HashMap<FilenameType,String>();
        fetchTypeToFileType.put( FilenameType.IMAGE_FAST_3d, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
        fetchTypeToFileType.put(FilenameType.NEURON_FRAGMENT_3d, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    }

    /**  Given you already know the image's role, call this with your entity. */
    public String fetchFilename(Entity entity, String imageRole) {
        ensureEntityLoaded( entity );
        for (EntityData ed: entity.getEntityData()) {
            ensureEntityLoaded(ed.getChildEntity());
        }

        String imageFilePath = null;
        if ( imageRole != null ) {
            imageFilePath = getFiletypeForEntityAndRole(entity, imageRole);
        }
        return imageFilePath;

    }

    public String fetchFilename(Entity entity, FilenameType fetcherFilenameType) {
        ensureEntityLoaded( entity );
        for (EntityData ed: entity.getEntityData()) {
            ensureEntityLoaded(ed.getChildEntity());
        }

        String imageRole = fetchTypeToFileType.get( fetcherFilenameType );
        String imageFilePath = null;
        if ( imageRole != null ) {
            imageFilePath = getFiletypeForEntityAndRole(entity, imageRole);
        }
        return imageFilePath;

    }

    public String fetchSignalFilename(Entity entity, FilenameType type) {
        if ( entity == null  ||  type == null ) {
            log.debug("Null entity or type passed in, for type " + type);
            return null;
        }
        return fetchFilename( entity, type );
    }

    public String getEntityConstantFileType(String entityType) {
        return entityTypeToFileTypeMapping.get( entityType );
    }

//    public String getEntityConstantFileType(FilenameType type) {
//        return fetchTypeToFileType.get( type );
//    }
//
//    public String getMaskEntityConstantFileType(String entityType) {
//        //        fetchTypeToFileType.put(FilenameType.MASK_FILE, ); //todo change
//        if ( entityType.equals(EntityConstants.TYPE_IMAGE_3D ) ) {   //??????????????????????????
//            return EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE;
//        }
//        else {
//            return null;
//        }
//    }

    private String getFiletypeForEntityAndRole(Entity entity, String imageRole) {
        String imageFilePath;
        imageFilePath = EntityUtils.getImageFilePath(entity, imageRole);
        log.debug( "For entity {}, got file {}", entity.getId(), imageFilePath );

        if ( imageFilePath != null ) {
            imageFilePath = PathTranslator.convertPath(imageFilePath);
            log.debug( "The 3D image is at " + imageFilePath);
        }
        return imageFilePath;
    }

    /** If this is not called, lazy-loaded entities won't have child entities available for searching, later. */
    private void ensureEntityLoaded( final Entity entity ) {
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (entity != null  &&  EntityUtils.isInitialized(entity)) {
                	ModelMgr.getModelMgr().loadLazyEntity(entity, false);
                }
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
        while ( ! worker.isDone() ) {
            if ( worker.isCancelled() ) {
                log.error("Entity load cancelled.");
            }
        }
    }
}
