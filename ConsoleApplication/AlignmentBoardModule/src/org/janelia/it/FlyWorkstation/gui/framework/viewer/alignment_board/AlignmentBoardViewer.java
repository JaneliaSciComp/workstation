package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.LayersPanel;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.AlignmentBoardEntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.AlignmentBoardControllable;
import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Callable;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.FlyWorkstation.gui.viewer3d.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.events.AlignmentBoardOpenEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.FlyWorkstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.AlignmentBoardPanel;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows alignment board relevant entities in 3D.
 */
@Deprecated
public class AlignmentBoardViewer extends Viewer implements AlignmentBoardControllable {

    public static final String SAMPLER_PANEL_NAME = "GpuSampler";

    @SuppressWarnings("unused")
    private AlignmentBoardPanel alignmentBoardPanel;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    private ShutdownListener shutdownListener;

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);
        alignmentBoardPanel = new AlignmentBoardPanel();

        setLayout(new BorderLayout());
        add( alignmentBoardPanel, BorderLayout.CENTER );
        ModelMgr.getModelMgr().registerOnEventBus(this);
        
        setTransferHandler( new AlignmentBoardEntityTransferHandler( this ) );

        // Saveback settings.
        shutdownListener = new ShutdownListener();
        SessionMgr.getSessionMgr().addSessionModelListener( shutdownListener );
    }

    @Override
    public void clear() {
        logger.info("Clearing the a-board.");
        alignmentBoardPanel.clear();
    }

    @Override
    public void showLoadingIndicator() {
        alignmentBoardPanel.showLoadingIndicator();
    }

    /** These getters/setters are required to subclass Viewer, but unused, here. */
    @Override
    public void loadEntity(RootedEntity rootedEntity) {}
    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {}

	public RootedEntity getContextRootedEntity() {
        LayersPanel layersPanel = AlignmentBoardMgr.getInstance().getLayersPanel();
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
        alignmentBoardPanel.close();
    }

    @Override
    public void refresh() {
        alignmentBoardPanel.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleBoardOpened(AlignmentBoardOpenEvent event) {
        alignmentBoardPanel.handleBoardOpened(event);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleItemChanged(AlignmentBoardItemChangeEvent event) {
        alignmentBoardPanel.handleItemChanged(event);
    }

    //---------------------------------------IMPLEMENTATION of AlignmentBoardControllable
    @Override
    public void clearDisplay() {
        alignmentBoardPanel.clearDisplay();
    }

    /**
     * Callback from loader threads to control loading information.
     *
     * @param signalTexture for the signal
     * @param maskTexture for the mask
     */
    @Override
    public void loadVolume( TextureDataI signalTexture, TextureDataI maskTexture ) {
        alignmentBoardPanel.loadVolume( signalTexture, maskTexture );
    }

    @Override
    public void displayReady() {
        alignmentBoardPanel.displayReady();
    }

    /**
     * Note: this is happening in the AWT Event Thread.  Callback indicating end of the load, so data can be pushed.
     *
     * @param successful Error or not?
     * @param loadFiles Files were loaded = T, display level mod, only = F
     * @param error any exception thrown during op, or null.
     */
    @Override
    public void loadCompletion( boolean successful, boolean loadFiles, Throwable error ) {
        alignmentBoardPanel.loadCompletion( successful, loadFiles, error );
    }

    @Override
    public void renderModCompletion() {
        alignmentBoardPanel.renderModCompletion();
    }

    //---------------------------------------HELPERS
    private void serializeInWorker() {
        alignmentBoardPanel.serializeInWorker();
    }

    private class ShutdownListener implements SessionModelListener {

        @Override
        public void browserAdded(BrowserModel browserModel) {
        }

        @Override
        public void browserRemoved(BrowserModel browserModel) {
        }

        @Override
        public void sessionWillExit() {
            serializeInWorker();
        }

        @Override
        public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
        }

    }

}
