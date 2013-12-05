package org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.DynamicImagePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ImagesPanel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/5/13
 * Time: 10:58 AM
 *
 * Holds info representing an entity for baseball card display.
 */
public class BaseballCard {
    private static final File DEFAULT_IMAGE_FILE = new File("images/cart_medium.png"); // Get Stacy's black watermark.
    private Entity entity;
    private EntityDetailsPanel entityDetailsPanel;
    private DynamicImagePanel dynamicImagePanel;
    private DynamicImagePanel defaultDynamicImagePanel;
    private Logger logger = LoggerFactory.getLogger( BaseballCard.class );

    public BaseballCard() {
        entityDetailsPanel = new EntityDetailsPanel();
        defaultDynamicImagePanel = getDynamicImagePanel( DEFAULT_IMAGE_FILE );
        dynamicImagePanel = defaultDynamicImagePanel;
    }

    public BaseballCard( Entity entity ) {
        this();
        loadEntity( entity );
    }

    public void loadEntity( Entity entity ) {
        this.entity = entity;
        String imagePath = EntityUtils.getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);

        if ( imagePath != null ) {
            logger.info("No image path for {}:{}" , entity.getName(), entity.getId());
            final File imageFile = SessionMgr.getCachedFile(imagePath, false);
            dynamicImagePanel = getDynamicImagePanel(imageFile);
        }
        else {
            dynamicImagePanel = defaultDynamicImagePanel;
        }

    }

    public Entity getEntity() {
        return entity;
    }

    public EntityDetailsPanel getEntityDetailsPanel() {
        return entityDetailsPanel;
    }

    public DynamicImagePanel getDynamicImagePanel() {
        return dynamicImagePanel;
    }

    private DynamicImagePanel getDynamicImagePanel(final File imageFile) {
        return new DynamicImagePanel(
                imageFile.getAbsolutePath(), ImagesPanel.MAX_IMAGE_WIDTH
        ) {
            protected void syncToViewerState() {
            }
        };
    }

}
