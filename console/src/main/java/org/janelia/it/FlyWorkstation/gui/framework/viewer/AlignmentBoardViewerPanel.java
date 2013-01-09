package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:30 PM
 *
 * This panel holds all alignment board viewers.
 * @todo remove this class prior to next release.
 * @deprecated this was only for making multiple tabs of alignment boards.
 */
public class AlignmentBoardViewerPanel extends JPanel {
    private static final AlignmentBoardViewerPanel s_abvp = new AlignmentBoardViewerPanel();
    public static AlignmentBoardViewerPanel getSingletonInstance() {
        return s_abvp;
    }

    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewerPanel.class);

    private ViewerPane alignmentBoardViewerPane = new ViewerPane(
            null, EntitySelectionModel.CATEGORY_ALIGNMENT_BOARD_VIEW, false
    );
    private JTabbedPane tabbedPane;

    /**
     * This is being setup for singleton access, initially.
     */
    private AlignmentBoardViewerPanel() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        this.setLayout(new BorderLayout());
        this.add( tabbedPane, BorderLayout.CENTER );
    }

    //-------------------------PUBLIC METHODS

    /**
     * Call this with the rooted entity whose entity is an alignment board.
     *
     * @param rootedEntity getEntity returns an alignment board.
     */
    public void addViewer(RootedEntity rootedEntity) {
        addViewer(rootedEntity, rootedEntity.getEntity());
    }

    /**
     * This will add a new tab to the tabbed-pane, to display all the stuff attached to the alignment board entity.
     * The rooted entity should be the one for which "alignmentBoardEntity==rootedEntity.getEntity()" is true.
     *
     * A rooted entity is required for use with all Viewer subclasses, and the alignemnt board viewer is such.
     *
     * @param rootedEntity getEntity returns the alignment board below.
     * @param alignmentBoardEntity its children may/may-not have 3d images to load.
     */
    public void addViewer(RootedEntity rootedEntity, Entity alignmentBoardEntity) {
        if (alignmentBoardEntity == null) {
            return;
        }
        else if (!EntityConstants.TYPE_ALIGNMENT_BOARD.equals(alignmentBoardEntity.getEntityType().getName()) ) {
            logger.warn(
                    "Entity of type {} given when adding alignment viewer tab. Expected {}. No viewer created",
                    alignmentBoardEntity.getEntityType(),
                    EntityConstants.TYPE_ALIGNMENT_BOARD
            );
            return;
        }
        else {
            // Make the viewer.
            AlignmentBoardViewer viewer = new AlignmentBoardViewer(alignmentBoardViewerPane);
            viewer.setAlignmentBoard(rootedEntity, alignmentBoardEntity);
            tabbedPane.add(alignmentBoardEntity.getName(), viewer);
            tabbedPane.setSelectedComponent(viewer);
        }
    }
}
