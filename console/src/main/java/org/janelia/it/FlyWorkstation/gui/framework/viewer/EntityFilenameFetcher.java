package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.apache.log4j.Logger;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Created by IntelliJ IDEA.
 * User: fosterl
 * Date: 11/13/12
 * Time: 12:58 PM
 * 
 * This class acts as a mediator to find a specific type of filename associated with the given entity.
 */
public class EntityFilenameFetcher {
    private Logger logger = Logger.getLogger( EntityFilenameFetcher.class );
    public enum FilenameType { IMAGE_3d }
    public String fetchFilename( Entity entity, FilenameType type ) {
        if ( entity == null ) {
            logger.info("Null entity passed in, for type " + type);
            return null;
        }
        if ( type == FilenameType.IMAGE_3d ) {

            ensureEntityLoaded( entity );
            for (EntityData ed: entity.getEntityData()) {
                ensureEntityLoaded(ed.getChildEntity());
            }
            String imageFilePath = EntityUtils.getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);

            logger.debug( entity.getId() );
            logger.debug( entity.getName() );
            logger.debug( imageFilePath );

            if ( imageFilePath != null ) {
                imageFilePath = PathTranslator.convertPath( imageFilePath );
                logger.debug( "The 3D image is at " + imageFilePath);
            }
            return imageFilePath;
        }
        else {
            // In future, other types will be added, and must be handled.
            throw new IllegalArgumentException( "File type " + type + " not yet implemented." );
        }
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
                logger.error("Entity load cancelled.");
            }
        }
    }
}
