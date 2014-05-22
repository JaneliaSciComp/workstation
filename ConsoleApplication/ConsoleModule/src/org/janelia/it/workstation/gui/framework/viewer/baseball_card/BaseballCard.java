package org.janelia.it.workstation.gui.framework.viewer.baseball_card;

import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
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
    private static final String DEFAULT_IMAGE_PATH = "/nobackup/jacs/jacsData/filestore/nerna/Alignment/819/554/1874649241884819554/align/Aligned63xScale_signal.png"; // Get Stacy's black watermark.
    public static final int IMAGE_WIDTH = 100;
    public static final int IMAGE_HEIGHT = 100;
    private Entity entity;
    private org.janelia.it.workstation.gui.framework.viewer.DynamicImagePanel dynamicImagePanel;
    private JPanel textDetailsPanel;
    private Logger logger = LoggerFactory.getLogger( BaseballCard.class );

    public BaseballCard() {
        textDetailsPanel = new ToolTipRelayPanel();
        ToolTipManager.sharedInstance().registerComponent(textDetailsPanel);
        textDetailsPanel.setLayout(new BorderLayout());
        textDetailsPanel.setOpaque( true );
    }

    public BaseballCard( Entity entity ) {
        this();
        loadEntity( entity );
    }

    public void setBackground( Color color ) {
        textDetailsPanel.setBackground( color );
    }

    @Override
    public String toString() {
        if ( entity == null ) {
            return null;
        }
        else {
            return entity.getName();
        }
    }

    public void loadEntity( final Entity entity ) {
        textDetailsPanel.removeAll();
        this.entity = entity;

        SimpleWorker annotationsLoadingWorker = new SimpleWorker() {
            List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();

            @Override
            protected void doStuff() throws Exception {
                for(Entity entityAnnot : org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getAnnotationsForEntity(entity.getId())) {
                    OntologyAnnotation annotation = new OntologyAnnotation();
                    annotation.init(entityAnnot);
                    if(null!=annotation.getTargetEntityId())
                        annotations.add(annotation);
                }

            }

            @Override
            protected void hadSuccess() {
                // Want to get the annotations, and the entity name.
                textDetailsPanel.setLayout(new BorderLayout());
                JPanel entityNamePanel = new JPanel();
                entityNamePanel.setLayout( new FlowLayout( FlowLayout.LEADING ) );

                String entityNameDesignation = entity.getName();
                if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                    try {
                        Entity entityParent = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_SAMPLE);
                        if ( entityParent == null ) {
                            entityNameDesignation = "Mock-Parent / " + entityNameDesignation;
                        }
                        else {
                            entityNameDesignation = entityParent.getName() + " / " + entityNameDesignation;
                        }
                    } catch ( Exception ex ) {
                        logger.error( "Exception when fetching parent sample of fragment " + entityNameDesignation );
                    }
                }
                JLabel entityNameLabel = makeLabelWithTip(entityNameDesignation, "Entity: " + entity.getId(), true );
                entityNamePanel.add( new JLabel( "Name" ) );
                entityNamePanel.add( entityNameLabel );
                textDetailsPanel.add( entityNamePanel, BorderLayout.NORTH );

                JPanel annotationPanel = new ToolTipRelayPanel();
                annotationPanel.setLayout( new FlowLayout( FlowLayout.LEADING) );
                JLabel subheaderLabel = new JLabel("Annotations");
                subheaderLabel.setFont( subheaderLabel.getFont().deriveFont( subheaderLabel.getFont().getSize() + 1 ));
                annotationPanel.add( subheaderLabel );
                if ( annotations != null && annotations.size() > 0 ) {
                    for ( OntologyAnnotation annotation: annotations ) {
                        annotationPanel.add(
                                makeLabelWithTip(
                                        getLabelText( annotation.getKeyString(), annotation.getValueString() ),
                                        getTooltipText( annotation.getKeyString(), annotation.getValueString() ),
                                        false
                                )
                        );
                    }
                }

                textDetailsPanel.add( annotationPanel, BorderLayout.CENTER );
                textDetailsPanel.revalidate();

            }

            @Override
            protected void hadError(Throwable error) {
                logger.error("Problem loading annotations");
                error.printStackTrace();
            }

            /** Need to ensure that some meaningful value appears on label. Many annotations have key, no value. */
            private String getLabelText( String key, String value ) {
                String labelText = value;
                if ( StringUtils.isEmpty(labelText) ) {
                    labelText = key;
                }
                return labelText;
            }

            /** Ensure tool tip does not contain dangling colons. */
            private String getTooltipText( String keyString, String valueString ) {
                String toolTipText;
                if ( StringUtils.isEmpty(valueString) ) {
                    toolTipText = keyString;
                }
                else {
                    toolTipText = keyString + ": " + valueString;
                }
                return toolTipText;
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

    public org.janelia.it.workstation.gui.framework.viewer.DynamicImagePanel getDynamicImagePanel() {
        return dynamicImagePanel;
    }

    private JLabel makeLabelWithTip( String labelVal, String toolTipText, boolean raised ) {
        JLabel rtnVal = new JLabel( labelVal );
        rtnVal.setOpaque( true );
        rtnVal.setToolTipText( toolTipText );
        rtnVal.setBorder( new BevelBorder( raised ? BevelBorder.RAISED : BevelBorder.LOWERED ) );
        return rtnVal;
    }

    private org.janelia.it.workstation.gui.framework.viewer.DynamicImagePanel getDynamicImagePanel(String imageFilePath) {
        final org.janelia.it.workstation.gui.framework.viewer.DynamicImagePanel rtnVal = new org.janelia.it.workstation.gui.framework.viewer.DynamicImagePanel(
                imageFilePath, org.janelia.it.workstation.gui.framework.viewer.ImagesPanel.MAX_IMAGE_WIDTH
        ) {
            protected void syncToViewerState() {
            }

            public String toString() {
                return getToolTipText();
            }
        };

        rtnVal.rescaleImage( IMAGE_WIDTH );
        rtnVal.setPreferredSize(new Dimension( IMAGE_WIDTH, IMAGE_HEIGHT ));

        rtnVal.setViewable(true, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (rtnVal.getMaxSizeImage() != null && dynamicImagePanel.getImage() != null) {
                    double w = rtnVal.getImage().getIconWidth();
                    double h = rtnVal.getImage().getIconHeight();
                    dynamicImagePanel.setDisplaySize((int) h);
                }
                return null;
            }

        });
        rtnVal.setToolTipText( entity.getName() );
        return rtnVal;
    }

    private class ToolTipRelayPanel extends JPanel {
        public ToolTipRelayPanel() {
            setOpaque(true);
        }

        @Override
        public String getToolTipText() {
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html><ul>");
            getToolTipText(tooltip, this);
            tooltip.append("</ul></html>");
            return tooltip.toString();
        }

        @Override
        public void setBackground( Color color ) {
            List<JComponent> tree = new ArrayList<JComponent>();
            getAffectedDescendants(tree, this);
            for ( JComponent c: tree ) {
                c.setBackground( color );
            }
            super.setBackground( color );
        }

        @Override
        public void setForeground( Color color ) {
            List<JComponent> tree = new ArrayList<JComponent>();
            getAffectedDescendants( tree, this );
            for ( JComponent c: tree ) {
                c.setForeground(color);
            }
        }

        @Override
        public String toString() {
            String rtnVal = null;
            for ( int i = 0; i < getComponentCount(); i++ ) {
                Component c = getComponent(i);
                if ( c instanceof JLabel ) {
                    rtnVal = ((JLabel)c).getText();
                }
            }
            return rtnVal;
        }

        private void getAffectedDescendants( List<JComponent> components, JComponent j ) {
            for ( Component c: j.getComponents() ) {
                if ( c instanceof JComponent ) {
                    JComponent jc = (JComponent) c;
                    components.add(jc);
                    getAffectedDescendants( components, jc );
                }
            }
        }
        private void getToolTipText( StringBuilder tooltip, JPanel startingComponent ) {
            for ( Component c: startingComponent.getComponents() ) {
                if ( c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    String toolTipText = label.getToolTipText();
                    if ( toolTipText != null  &&  toolTipText.trim().length() > 0  &&
                         (! tooltip.toString().contains( toolTipText )) ) {
                        tooltip.append("<li><b>")
                                .append(toolTipText)
                                .append("</b></li>");
                    }
                }
                else if ( c instanceof JPanel  &&  (c != startingComponent) ) {
                    getToolTipText(tooltip, (JPanel) c);
                }
            }
        }

    }
}
