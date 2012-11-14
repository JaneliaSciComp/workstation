package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.apache.log4j.Logger;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Created by IntelliJ IDEA.
 * User: fosterl
 * Date: 11/13/12
 * Time: 12:58 PM
 * 
 * This class acts as a mediator between other classes in this package, and the entity layers.
 */
public class EntityFilenameFetcher {
    private Logger logger = Logger.getLogger( EntityFilenameFetcher.class );
    public enum FilenameType { IMAGE_3d }
    public String fetchFilename( Entity entity, FilenameType type ) {
        if ( type == FilenameType.IMAGE_3d ) {
            String imageFilePath = EntityUtils.getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
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
}
