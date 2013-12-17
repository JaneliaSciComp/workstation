package org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.DynamicImagePanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ImagesPanel;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    private DynamicImagePanel dynamicImagePanel;
    private JPanel textDetailsPanel;
    private Logger logger = LoggerFactory.getLogger( BaseballCard.class );

    public BaseballCard() {
        textDetailsPanel = new JPanel();
        textDetailsPanel.setLayout( new BorderLayout( ) );
    }

    public BaseballCard( Entity entity ) {
        this();
        loadEntity( entity );
    }

    public void loadEntity( final Entity entity ) {
        textDetailsPanel.removeAll();
        this.entity = entity;

        SimpleWorker annotationsLoadingWorker = new SimpleWorker() {
            List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();

            @Override
            protected void doStuff() throws Exception {
                for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntity(entity.getId())) {
                    OntologyAnnotation annotation = new OntologyAnnotation();
                    annotation.init(entityAnnot);
                    if(null!=annotation.getTargetEntityId())
                        annotations.add(annotation);
                }

            }

            @Override
            protected void hadSuccess() {
                // Want to get the annotations, and the entity name.
                int rows = 1 + annotations.size(); // First is for the entity name.
                textDetailsPanel.setLayout( new GridLayout( rows, 2 ) );
                textDetailsPanel.add( new JLabel("Entity") );
                textDetailsPanel.add( new JLabel( entity.getName() ) );

                for ( OntologyAnnotation annotation: annotations ) {
                    textDetailsPanel.add( new JLabel( annotation.getKeyString() ) );
                    textDetailsPanel.add( new JLabel( annotation.getValueString() ) );
                }
            }

            @Override
            protected void hadError(Throwable error) {
                logger.error("Problem loading annotations");
                error.printStackTrace();
            }
        };
        annotationsLoadingWorker.execute();

        String imagePath = EntityUtils.getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        if ( imagePath == null ) {
            imagePath = DEFAULT_IMAGE_PATH;
        }
        dynamicImagePanel = getDynamicImagePanel(imagePath);
    }

    public Entity getEntity() {
        return entity;
    }

    public JPanel getEntityDetailsPanel() {
        return textDetailsPanel;
    }

    public DynamicImagePanel getDynamicImagePanel() {
        return dynamicImagePanel;
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

//    private class DynamicValueTable extends DynamicTable {
//        private Entity entity;
//        public DynamicValueTable( Entity entity ) {
//            this.entity = entity;
//        }
//
//        @Override
//        public Object getValue(Object userObject, DynamicColumn column) {
//            return null;
//        }
//    }

}
