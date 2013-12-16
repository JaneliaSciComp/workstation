package org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityDetailsPanel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.DynamicImagePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ImagesPanel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/5/13
 * Time: 10:58 AM
 *
 * Holds info representing an entity for baseball card display.
 */
public class BaseballCard {
    // images/Aligned63xScale_signal.png"
    private static final String DEFAULT_IMAGE_PATH = "/archive/scicomp/jacsData/filestore/nerna/Alignment/819/554/1874649241884819554/align/Aligned63xScale_signal.png"; // Get Stacy's black watermark.
    public static final int IMAGE_WIDTH = 100;
    public static final int IMAGE_HEIGHT = 100;
    private Entity entity;
    private EntityDetailsPanel entityDetailsPanel;
    private DynamicImagePanel dynamicImagePanel;
    private Logger logger = LoggerFactory.getLogger( BaseballCard.class );

    public BaseballCard() {
    }

    public BaseballCard( Entity entity ) {
        this();
        loadEntity( entity );
    }

    public void loadEntity( Entity entity ) {
        if (entityDetailsPanel == null) {
            entityDetailsPanel = new EntityDetailsPanel();
        }
        entityDetailsPanel.loadEntity( entity );
        this.entity = entity;

        String imagePath = EntityUtils.getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if ( imagePath == null ) {
            imagePath = DEFAULT_IMAGE_PATH;
        }
        dynamicImagePanel = getDynamicImagePanel(imagePath);
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
        return getDynamicImagePanel( imageFile.getAbsolutePath() );
    }

    private DynamicImagePanel getDynamicImagePanel(String imageFilePath) {
        final DynamicImagePanel rtnVal = new DynamicImagePanel(
                imageFilePath, ImagesPanel.MAX_IMAGE_WIDTH
        ) {
            protected void syncToViewerState() {
            }
        };

        rtnVal.rescaleImage( IMAGE_WIDTH );
        rtnVal.setPreferredSize(new Dimension( IMAGE_WIDTH, IMAGE_HEIGHT ));

        rtnVal.setViewable( true, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (rtnVal.getMaxSizeImage()!=null && dynamicImagePanel.getImage()!=null) {
                    double w = rtnVal.getImage().getIconWidth();
                    double h = rtnVal.getImage().getIconHeight();
                    dynamicImagePanel.setDisplaySize( (int)h );
                }
                return null;
            }

        });
        return rtnVal;
    }

}
