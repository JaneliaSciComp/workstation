package org.janelia.it.FlyWorkstation.gui.framework.viewer.baseball_card;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.BaseballCardPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.LayersPanel;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/9/13
 * Time: 4:20 PM
 *
 * This is a viewer to support baseball card panel.
 */
public class BaseballCardViewer extends Viewer {
    private Logger logger = LoggerFactory.getLogger( BaseballCardViewer.class );
    private boolean loadingInProgress;
    private BaseballCardPanel baseballCardPanel;

    public BaseballCardViewer( ViewerPane pane )  {
        super( pane );
    }

    @Override
    public RootedEntity getContextRootedEntity() {
        LayersPanel layersPanel = SessionMgr.getBrowser().getLayersPanel();
        if ( layersPanel == null ) {
            return null;
        }
        AlignmentBoardContext alignmentBoardContext = layersPanel.getAlignmentBoardContext();
        if ( alignmentBoardContext == null ) {
            return null;
        };
        return alignmentBoardContext.getInternalRootedEntity();
    }

    @Override
    public void clear() {
        logger.info("Clearing the cards.");
    }

    @Override
    public void showLoadingIndicator() {
        setLoading(true);
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    //------------------------------------------------IMPLEMENTS Viewer
    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        loadEntity(rootedEntity, null);
    }


    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {

    }

    @Override
    public List<RootedEntity> getRootedEntities() {
        return null;
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        return null;
    }

    @Override
    public RootedEntity getRootedEntityById(String uniqueId) {
        return null;
    }

    @Override
    public void close() {
        logger.info("Closing");
        loadingInProgress = false;
    }

    @Override
    public void refresh() {
        logger.info("Refresh called.");

        showLoadingIndicator();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private synchronized void setLoading( boolean loadingState ) {
        this.loadingInProgress = loadingState;
    }

    private synchronized boolean isLoading() {
        return loadingInProgress;
    }


}
