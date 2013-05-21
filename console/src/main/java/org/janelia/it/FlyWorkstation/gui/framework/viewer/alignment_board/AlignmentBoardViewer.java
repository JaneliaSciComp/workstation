package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityWrapperTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.CompletionListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.ControlsListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.ABContextDataSource;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.VolumeWritebackHandler;
import org.janelia.it.FlyWorkstation.model.domain.*;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.*;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.ArtifactType;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.Channels;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.Size;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows alignment board relevant entities in 3D.
 */
public class AlignmentBoardViewer extends Viewer implements AlignmentBoardControllable {

    private static final Logger log = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    private Entity alignmentBoard;
    private RootedEntity albRootedEntity;

    private Mip3d mip3d;
    private RenderablesLoadWorker loadWorker;
    private JPanel wrapperPanel;

    private ModelMgrObserver modelMgrObserver;
    private RenderMappingI renderMapping;
    private CropCoordSet cropCoordSet;
    private BrainGlow brainGlow;
    private AlignmentBoardControlsDialog settings;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    private boolean loadingInProgress = false;
    private boolean outstandingLoadRequest = false;

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);

        renderMapping = new ConfigurableColorMapping();
        cropCoordSet = CropCoordSet.getDefaultCropCoordSet();
        setLayout(new BorderLayout());
        ModelMgr.getModelMgr().registerOnEventBus(this);
        
        setTransferHandler(new EntityWrapperTransferHandler() {
            @Override
            public JComponent getDropTargetComponent() {
                return AlignmentBoardViewer.this;
            }
        });
    }

    @Override
    public void clear() {
        clearObserver();
    }

    @Override
    public void showLoadingIndicator() {
        setLoading( true );
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        revalidate();
        repaint();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity) {
        Entity newEntity = rootedEntity.getEntity();
        if ( ! newEntity.equals( alignmentBoard ) ) {
            // Stop any existing load, to free up the A-board.
            if (loadWorker != null) {
                loadWorker.disregard();
                loadWorker.cancel( true );
            }

            deleteAll();
        }
        alignmentBoard = newEntity;

        setTransferHandler( new ABTransferHandler( alignmentBoard ) );
        refresh();

        // Listen for further changes, so can refresh again later.
        establishObserver();
    }

    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {
        loadEntity(rootedEntity);
        try {
            if ( success != null )
                success.call();
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
        }
    }

    @Override
    public List<RootedEntity> getRootedEntities() {
        return Arrays.asList( albRootedEntity );
    }

    @Override
    public List<RootedEntity> getSelectedEntities() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public RootedEntity getRootedEntityById(String uniqueId) {
        return albRootedEntity;
    }

    @Override
    public Entity getEntityById(String id) {
        return alignmentBoard;
    }

    @Override
    public void close() {
        logger.info("Closing");
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
        deleteAll();
    }

    @Override
    public void refresh() {
        logger.info("Refresh called.");

        if (alignmentBoard != null) {
            showLoadingIndicator();

            if ( mip3d == null ) {
                mip3d = createMip3d();
            }

            mip3d.refresh();
        }
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    @Subscribe
    public void handleBoardOpened(AlignmentBoardOpenEvent event) {
        
        AlignmentBoardContext abContext = event.getAlignmentBoardContext();
        this.getViewerPane().setTitle( "Alignment Board: " + abContext.getInternalEntity().getName() );
        printAlignmentBoardContext(abContext);

        // The true update!
        this.updateBoard( abContext );
    }

    @Subscribe 
    public void handleItemChanged(AlignmentBoardItemChangeEvent event) {

        AlignmentBoardContext abContext = event.getAlignmentBoardContext();
        
        printItemChanged(event.getAlignedItem(), event.getChangeType().toString());
        printAlignmentBoardContext(abContext);
        
        if ( event.getChangeType().equals( AlignmentBoardItemChangeEvent.ChangeType.VisibilityChange )  ||
             event.getChangeType().equals( AlignmentBoardItemChangeEvent.ChangeType.ColorChange ) ) {

            // Changing the render mapping values.
            if ( settings != null )
                this.updateRendering( abContext );

        }
        else {
            this.updateBoard( abContext );
        }
    }

    //---------------------------------------IMPLEMENTATION of AlignmentBoardControllable
    @Override
    public void clearDisplay() {
        mip3d.clear();
    }

    /**
     * Callback from loader threads to control loading information.
     *
     * @param signalTexture for the signal
     * @param maskTexture for the mask
     */
    @Override
    public void loadVolume( TextureDataI signalTexture, TextureDataI maskTexture ) {

        if ( ! mip3d.setVolume(
                signalTexture,
                maskTexture,
                renderMapping,
                cropCoordSet,
                (float) AlignmentBoardControlsDialog.DEFAULT_GAMMA
        ) ) {
            logger.error( "Failed to load volume to mip3d." );
        }
        else {
            settings.setVolumeMaxima(signalTexture.getSx(), signalTexture.getSy(), signalTexture.getSz());

        }

    }

    @Override
    public void displayReady() {
        mip3d.refresh();

        // Strip any "show-loading" off the viewer.
        removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        add( wrapperPanel, BorderLayout.CENTER );

    }

    @Override
    public void loadCompletion( boolean successful, boolean loadFiles, Throwable error ) {
        if ( successful ) {
            revalidate();
            repaint();

            if ( loadFiles ) {
                mip3d.refresh();
            }
            else {
                mip3d.refreshRendering();
            }

        }
        else {
            removeAll();
            revalidate();
            repaint();
            SessionMgr.getSessionMgr().handleException(error);
        }

        // Here, deal with synchronization of multiple incoming requests.
        if ( isOutstandingLoadRequest() ) {
            // AT THIS POINT:
            //   We are still in a loading request that is just finishing.  The user made additional requests
            //   by making changes to the Alignment Board model itself, while the current request was being honored.
            //   We start up another update to cover those new requests.

            // Invoke another load process.
            AlignmentBoardContext abContext = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();

            // There are no more outstanding load requests.  If by chance, any came in during the short time
            // required to initiate this one, they should be covered in the "current" update.
            setOutstandingLoadRequest( false );

            // Now launch the update-to-service-outstanding, which will be time-consuming
            setLoading( false );
            updateBoard( abContext );

        }
        else {
            // No outstanding request.  Just turn off the currently-loading state.
            setLoading( false );
        }

    }

    //---------------------------------------HELPERS
    private void printAlignmentBoardContext(AlignmentBoardContext abContext) {
        if ( log.isDebugEnabled() ) {
            log.debug("Alignment board: "+abContext.getName());
            log.debug("* Alignment space: "+abContext.getAlignmentContext().getAlignmentSpaceName());
            log.debug("* Optical resolution: "+abContext.getAlignmentContext().getOpticalResolution());
            log.debug("* Pixel resolution: "+abContext.getAlignmentContext().getPixelResolution());

            for(AlignedItem alignedItem : abContext.getAlignedItems()) {

                EntityWrapper itemEntity = alignedItem.getItemWrapper();

                if ( itemEntity instanceof Sample  &&  alignedItem.isVisible() ) {

                    Sample sample = (Sample)itemEntity;

                    log.debug("  Sample: "+sample.getName());
                    log.debug("  * 3d image: "+sample.get3dImageFilepath());
                    log.debug("  * fast 3d image: "+sample.getFast3dImageFilepath());

                    if (sample.getChildren()==null) {
                        log.warn("  Sample children not loaded");
                    }
                    if (sample.getNeuronSet()==null) {
                        log.warn("  Sample neurons not loaded");
                    }

                    MaskedVolume vol = sample.getMaskedVolume();
                    if (vol!=null) {
                        log.debug("    original separation volumes:");
                        log.debug("    * reference vol: "+vol.getReferenceVolumePath());
                        log.debug("    * signal vol: "+vol.getSignalVolumePath());
                        log.debug("    * signal label: "+vol.getSignalLabelPath());

                        log.debug("    fast load 8-bit volumes:");
                        log.debug("    * fast signal: "+vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, Size.Full, Channels.All, true));
                        log.debug("    * fast label: "+vol.getFastVolumePath(ArtifactType.ConsolidatedLabel, Size.Full, Channels.All, true));
                        log.debug("    * fast reference: "+vol.getFastVolumePath(ArtifactType.Reference, Size.Full, Channels.All, true));

                        log.debug("    subsampled volumes:");
                        for(Size size : Size.values()) {
                            log.debug("    * "+size+"/signal: "+vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, size, Channels.All, true));
                            log.debug("    * "+size+"/label: "+vol.getFastVolumePath(ArtifactType.ConsolidatedLabel, size, Channels.All, true));
                            log.debug("    * "+size+"/reference: "+vol.getFastVolumePath(ArtifactType.Reference, size, Channels.All, true));
                        }

                        log.debug("    mpeg4 volumes:");
                        for(Size size : Size.values()) {
                            for(Channels channels : Channels.values()) {
                                log.debug("    * "+size+"/"+channels+" signal: "+vol.getFastVolumePath(ArtifactType.ConsolidatedSignal, size, channels, false));
                            }
                            log.debug("    * "+size+"/reference: "+vol.getFastVolumePath(ArtifactType.Reference, size, Channels.All, false));
                        }

                        log.debug("  metadata files:");
                        for(Size size : Size.values()) {
                            log.debug("  * signal metadata: "+vol.getFastMetadataPath(ArtifactType.ConsolidatedSignal, size));
                            log.debug("  * reference metadata: "+vol.getFastMetadataPath(ArtifactType.Reference, size));
                        }
                    }

                    log.debug("  neurons:");
                    for(AlignedItem neuronAlignedItem : alignedItem.getAlignedItems()) {
                        EntityWrapper neuronItemEntity = neuronAlignedItem.getItemWrapper();
                        if (neuronItemEntity instanceof Neuron) {
                            Neuron neuron = (Neuron)neuronItemEntity;
                            log.debug("    "+neuron.getName()+" (visible="+neuronAlignedItem.isVisible()+", maskIndex="+neuron.getMaskIndex()+")");
                            log.debug("    * mask: "+neuron.getMask3dImageFilepath());
                            log.debug("    * chan: "+neuron.getChan3dImageFilepath());
                        }
                    }

                }
                else if ( itemEntity instanceof CompartmentSet && alignedItem.isVisible() ) {
                    log.debug( itemEntity.getName() + ": compartment set" );
                }
                else {
                    log.warn("No knowledge of entities of type: "+itemEntity.getType());
                }

            }
        }

    }

    private void printItemChanged(AlignedItem alignedItem, String changeType) {
        log.debug("Alignment board item changed");
        log.debug("* Change Type: {}", changeType);
        log.debug("* Item Alias: {}", alignedItem.getName());
        log.debug("* Item Name: {}", alignedItem.getItemWrapper().getName());
        log.debug("* Item Visibility: {}", alignedItem.isVisible());
        log.debug("* Item Color: {} (hex={})", alignedItem.getColor(), alignedItem.getColorHex() );
    }

    private void establishObserver() {
        modelMgrObserver = new ModelMgrListener( this, alignmentBoard );
        ModelMgr.getModelMgr().addModelMgrObserver(modelMgrObserver);
    }

    private void deleteAll() {
        clearObserver();
        if (loadWorker != null) {
            loadWorker.disregard();
        }
        alignmentBoard = null;
        albRootedEntity = null;
        removeAll();
        mip3d = null;
    }

    private void clearObserver() {
        if ( modelMgrObserver != null ) {
            ModelMgr.getModelMgr().removeModelMgrObserver(modelMgrObserver);
        }
    }

    private synchronized void setLoading( boolean loadingState ) {
        this.loadingInProgress = loadingState;
    }

    private synchronized boolean isLoading() {
        return loadingInProgress;
    }

    private boolean isOutstandingLoadRequest() {
        return outstandingLoadRequest;
    }

    private synchronized void setOutstandingLoadRequest(boolean outstandingLoadRequest) {
        this.outstandingLoadRequest = outstandingLoadRequest;
    }

    /**
     * This is called when the board data has been updated.
     */
    private void updateBoard( AlignmentBoardContext context ) {
        logger.info("Update-board called.");
        try {
            // TEMP
            //if ( brainGlow != null ) {
            //    brainGlow.isRunning = false;
            //} // TEMP

            if (context != null ) {
                if ( isLoading() ) {
                    setOutstandingLoadRequest( true );
                }
                else {
                    showLoadingIndicator();

                    if ( mip3d == null ) {
                        mip3d = createMip3d();
                        wrapperPanel = createWrapperPanel( mip3d );
                    }

                    mip3d.refresh();

                    // Here, should load volumes, for all the different items given.
                    loadWorker = new RenderablesLoadWorker(
                            new ABContextDataSource( context ), renderMapping, this, settings.getAlignmentBoardSettings()
                    );
                    loadWorker.execute();
                }

            }

            // TEMP
            //brainGlow = new BrainGlow();
            //brainGlow.start();  // TEMP

        } catch ( Throwable th ) {
            SessionMgr.getSessionMgr().handleException( th );
        }

    }

    /**
     * Build out the Mip3D object for rendering all.  Make listeners on it so the viewer changes its data
     * as needed.
     */
    private Mip3d createMip3d() {
        Mip3d rtnVal = new Mip3d();
        settings = new AlignmentBoardControlsDialog( rtnVal, cropCoordSet );
        settings.setDownSampleRate( AlignmentBoardControlsDialog.DEFAULT_DOWNSAMPLE_RATE );
        settings.addSettingsListener(
                new AlignmentBoardControlsListener( rtnVal, renderMapping, this )
        );

        rtnVal.addMenuAction( settings.getLaunchAction() );
        return rtnVal;
    }

    private JPanel createWrapperPanel( Mip3d mip3d ) {
        JPanel rtnVal = new JPanel();
        rtnVal.setLayout( new BorderLayout() );
        rtnVal.add( mip3d, BorderLayout.CENTER );

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new GridBagLayout() );
        JButton launchSettingsButton = new JButton();
        launchSettingsButton.setFocusable( false );
        launchSettingsButton.setRequestFocusEnabled(false);
        launchSettingsButton.setSelected(false);
        launchSettingsButton.setFont(launchSettingsButton.getFont().deriveFont(9.0f));
        launchSettingsButton.setAction(settings.getLaunchAction());
        buttonPanel.setBorder(null);

        GridBagConstraints btnConstraints = new GridBagConstraints(
                0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(1,1,1,1), 0, 0
        );

        buttonPanel.add( launchSettingsButton, btnConstraints );
        rtnVal.add( buttonPanel, BorderLayout.NORTH );
        return rtnVal;
    }

    /**
     * This is called when the board visibility or coloring has been change.
     */
    private void updateRendering( AlignmentBoardContext context ) {
        logger.info("Update-rendering called.");

        try {
            if (context != null) {
                // Here, simply make the rendering change.
                loadWorker = new RenderablesLoadWorker(
                        new ABContextDataSource(context), renderMapping, this, settings.getAlignmentBoardSettings()
                );
                loadWorker.setLoadFilesFlag( Boolean.FALSE );
                loadWorker.execute();

            }
        } catch ( Throwable th ) {
            SessionMgr.getSessionMgr().handleException( th );
        }

    }

    //------------------------------Inner Classes
    public static class AlignmentBoardControlsListener implements ControlsListener {
        private Mip3d mip3d;
        private AlignmentBoardViewer viewer;
        private RenderMappingI renderMapping;
        public AlignmentBoardControlsListener(Mip3d mip3d, RenderMappingI renderMapping, AlignmentBoardViewer viewer) {
            this.mip3d = mip3d;
            this.viewer = viewer;
            this.renderMapping = renderMapping;
        }
        @Override
        public void setBrightness(double brightness) {
            mip3d.setGamma( (float)brightness );
        }

        @Override
        public void updateSettings() {
            AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
            viewer.updateBoard(context);
        }

        @Override
        public void setSelectedCoords( CropCoordSet cropCoordSet ) {
            if ( cropCoordSet.getCurrentCoordinates() != null  ||  cropCoordSet.getAcceptedCoordinates().size() > 0 ) {
                mip3d.setCropCoords( cropCoordSet );

                AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
                viewer.updateRendering( context );
            }
        }

        @Override
        public void exportSelection(
                Collection<float[]> absoluteCropCoords,
                CompletionListener completionListener,
                ControlsListener.ExportMethod method ) {
            VolumeWritebackHandler writebackHandler = new VolumeWritebackHandler(
                    renderMapping, absoluteCropCoords, completionListener, mip3d
            );
            writebackHandler.writeBackVolumeSelection(method);
        }

        @Override
        public void setCropBlackout( boolean blackout ) {
            mip3d.setCropOutLevel( blackout ? 0.0f : Mip3d.DEFAULT_CROPOUT );
        }
    }

    /** Listens for changes to the child-set of the heard-entity. */
    public static class ModelMgrListener extends ModelMgrAdapter {
        private Entity heardEntity;
        private AlignmentBoardViewer viewer;
        ModelMgrListener( AlignmentBoardViewer viewer, Entity e ) {
            heardEntity = e;
            this.viewer = viewer;
        }

        @Override
        public void entityChildrenChanged(long entityId) {
            if (heardEntity.getId() == entityId) {
                viewer.refresh();
            }
        }
    }

    /** An experiment in animating the view.  If ever used, should be moved elsewhere. */
    public class BrainGlow extends Thread {
        private float gamma = 0.0f;
        public boolean isRunning = true;

        public BrainGlow() {
            super.start();
        }

        @Override
        public void run() {
            while ( isRunning ) {
                long curTime = System.currentTimeMillis() % 50L;
                if ( curTime > 25 ) {
                    curTime = 50 - curTime;
                }
                gamma = 0.75f + (curTime / 100.0f);
                mip3d.setGamma( gamma );
                try {
                    Thread.sleep( 60 );
                } catch ( Exception ex ) {
                    break;
                }
            }
        }
    }

}
