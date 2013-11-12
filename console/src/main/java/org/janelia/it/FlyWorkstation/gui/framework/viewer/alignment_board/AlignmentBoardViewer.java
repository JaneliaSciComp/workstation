package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.MultiTexVolumeBrickFactory;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.*;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.FileStats;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture.ABContextDataSource;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.VolumeWritebackHandler;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.ViewerPane;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardOpenEvent;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.ScaledMip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.model.domain.*;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.ArtifactType;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.Channels;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume.Size;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
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
    private static final String SETTINGS_LAUNCH_BTN_NAME = "AlignmentBoard::SettingsLaunchButton";
    private static final String COLOR_SAVE_BTN_NAME = "AlignmentBoard::ColorSaveButton";
    private static final String SEARCH_SAVE_BTN_NAME = "AlignmentBoard::SearchSaveButton";
    private static final String SCREEN_SHOT_BTN_NAME = "AlignmentBoard::ScreenShotButton";
    public static final String SAMPLER_PANEL_NAME = "GpuSampler";
    private static final Dimension GPU_FEEDBACK_PANEL_SIZE = new Dimension( 1, 1 );

    private Mip3d mip3d;
    private RenderablesLoadWorker loadWorker;
    private JPanel wrapperPanel;

    private RenderMappingI renderMapping;
    private MultiMaskTracker multiMaskTracker;
    @SuppressWarnings("unused")
    private BrainGlow brainGlow;
    private AlignmentBoardControlsPanel settingsPanel;
    private AlignmentBoardControlsDialog settingsDialog;
    private AlignmentBoardControls controls;
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardViewer.class);

    private boolean loadingInProgress = false;
    private boolean outstandingLoadRequest = false;

    private boolean renderingInProgress = false;
    private boolean outstandingRenderRequest = false;

    private boolean preExistingBoard = true;

    private boolean boardOpen = false;
    private Double cachedDownSampleGuess = null;
    private AlignmentBoardSettings settingsData;
    private ShutdownListener shutdownListener;
    private JToolBar toolbar;
    private ABContextDataSource dataSource;
    private FileStats fileStats;

    public AlignmentBoardViewer(ViewerPane viewerPane) {
        super(viewerPane);

        logger.info( "C'tor" );
        settingsData = new AlignmentBoardSettings();
        multiMaskTracker = new MultiMaskTracker();
        fileStats = new FileStats();
        renderMapping = new ConfigurableColorMapping( multiMaskTracker, fileStats );
        setLayout(new BorderLayout());
        ModelMgr.getModelMgr().registerOnEventBus(this);
        
        setTransferHandler( new AlignmentBoardEntityTransferHandler( this ) );

        // Saveback settings.
        shutdownListener = new ShutdownListener();
        SessionMgr.getSessionMgr().addSessionModelListener( shutdownListener );
    }

    @Override
    public void clear() {
        logger.info("Clearing the a-board.");
        tearDownToolbar();
    }

    @Override
    public void showLoadingIndicator() {
        setLoading(true);
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** These getters/setters are required to subclass Viewer, but unused, here. */
    @Override
    public void loadEntity(RootedEntity rootedEntity) {}
    @Override
    public void loadEntity(RootedEntity rootedEntity, Callable<Void> success) {}

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
        logger.info( "Closing" );
        // Cleanup this listener to avoid mem leaks.
        SessionMgr.getSessionMgr().removeSessionModelListener( shutdownListener );

        ModelMgr.getModelMgr().unregisterOnEventBus(this);
        serialize();

        deleteAll();
        SessionMgr.getBrowser().getLayersPanel().closeAlignmentBoard();
    }

    @Override
    public void refresh() {
        logger.info("Refresh called.");

        showLoadingIndicator();

        if ( mip3d == null ) {
            logger.warn( "Have to create a new mip3d on refresh." );
            createMip3d();
            wrapperPanel = createWrapperPanel( mip3d );
        }

        mip3d.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleBoardOpened(AlignmentBoardOpenEvent event) {
        logger.info("Board Opened");

        AlignmentBoardContext abContext = event.getAlignmentBoardContext();
        try {
            Entity alignmentBoard =  ModelMgr.getModelMgr().getEntityById(abContext.getInternalEntity().getId());
            preExistingBoard = UserSettingSerializer.settingsExist( alignmentBoard );
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        handleBoardOpened(abContext);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleItemChanged(AlignmentBoardItemChangeEvent event) {
        logger.debug("Item changed");
        // Check this, to prevent this being completed until the board has been first initialized.
        // Redundant events may be posted at startup.
        if ( boardOpen ) {
            AlignmentBoardContext abContext = event.getAlignmentBoardContext();

            printItemChanged(event.getAlignedItem(), event.getChangeType().toString());
            printAlignmentBoardContext(abContext);

            if ( AlignmentBoardItemChangeEvent.ChangeType.VisibilityChange.equals( event.getChangeType() ) ||
                 AlignmentBoardItemChangeEvent.ChangeType.ColorChange.equals( event.getChangeType() ) ) {

                // Changing the render mapping values.
                this.updateRendering( abContext );

            }
            else if ( ! AlignmentBoardItemChangeEvent.ChangeType.FilterLevelChange.equals( event.getChangeType() ) ) {

                serialize();
                this.updateContents(abContext);

            }
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

        MultiTexVolumeBrickFactory volumeBrickFactory = new MultiTexVolumeBrickFactory();
        if ( ! mip3d.setVolume(
                signalTexture,
                maskTexture,
                volumeBrickFactory,
                renderMapping,
                settingsData.getAcceptedDownsampleRate() )
            ) {
            logger.error( "Failed to load volume to mip3d." );
        }
        else {
            settingsPanel.setVolumeMaxima(signalTexture.getSx(), signalTexture.getSy(), signalTexture.getSz());
        }

    }

    @Override
    public void displayReady() {
        mip3d.refresh();

        // Strip any "show-loading" off the viewer.
        removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        setupToolbar();
        add(wrapperPanel, BorderLayout.CENTER);
        mip3d.resetView();

        // Pull settings back in from last time.
        AlignmentBoardContext abContext = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
        deserializeSettings(abContext);

        if ( !preExistingBoard) {
            mip3d.setResetFirstRedraw( true );
        }
        else {
            // Ensure pixels per scene unit properly accounted-for.
            mip3d.getVolumeModel().setCameraPixelsPerSceneUnit( BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS, mip3d.getVolumeModel().getCameraFocusDistance() );
        }

    }

    @Override
    public void loadCompletion( boolean successful, boolean loadFiles, Throwable error ) {
        if ( successful ) {
            revalidate();
            repaint();
            if ( loadFiles ) {
                mip3d.refresh();
                // Post this event, nagging the outline to update itself.
                // NOTE: better not do any refreshing in this board, if this event is encountered.  Would lead
                // to a cycle of updates between this viewer and the outline!
                new Thread( new Runnable() {
                    public void run() {
                        // Ensure all shown.
                        LayersPanel layersPanel = SessionMgr.getSessionMgr().getActiveBrowser().getLayersPanel();
                        layersPanel.showOutline();
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                layersPanel.getAlignmentBoardContext(),
                                null,
                                AlignmentBoardItemChangeEvent.ChangeType.FilterLevelChange
                        );
                        ModelMgr.getModelMgr().postOnEventBus(event);
                    }
                }).start();
            }
            else {
                if ( mip3d != null ) {
                    mip3d.refreshRendering();
                }
                else {
                    logger.info("Have to create a new MIP3d at load completion.");
                    createMip3d();
                    wrapperPanel = createWrapperPanel( mip3d );
                }
            }
            multiMaskTracker.writeOutstandingDump();
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
            updateContents(abContext);

        }
        else {
            // No outstanding request.  Just turn off the currently-loading state.
            setLoading( false );
        }

    }

    @Override
    public void renderModCompletion() {
        if ( isOutstandingRenderRequest() ) {
            setOutstandingRenderRequest(false);
            AlignmentBoardContext abContext = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
            setRendering( false );
            updateRendering( abContext );
        }
        else {
            setRendering(false);
        }
    }

    //---------------------------------------HELPERS
    private void serialize() {
        AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
        if ( context != null ) {
            Entity alignmentBoard = null;
            try {
                alignmentBoard =  ModelMgr.getModelMgr().getEntityById(context.getInternalEntity().getId());
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
            if ( mip3d != null && settingsPanel != null ) {
                UserSettingSerializer userSettingSerializer = new UserSettingSerializer(
                        alignmentBoard, mip3d.getVolumeModel(), settingsData
                );

                userSettingSerializer.serializeSettings();
            }
            else {
                logger.warn("Attempt at serializing while mip3d={} and settings dialog={}.", mip3d, settingsPanel);
            }
        }
    }

    /** This is synch'd because there may be a race between constructor and an externally-posted event. */
    private synchronized void handleBoardOpened(AlignmentBoardContext abContext) {
        if ( ! boardOpen ) {
            this.getViewerPane().setTitle("Alignment Board: " + abContext.getInternalEntity().getName() + " (" + abContext.getAlignmentContext() + ")");
            printAlignmentBoardContext(abContext);

            // The true update!
            this.updateContents(abContext);
            boardOpen = true;
        }
    }

    private void printAlignmentBoardContext(AlignmentBoardContext abContext) {
        if ( log.isDebugEnabled() ) {
            log.debug("Alignment board: " + abContext.getName());
            log.debug("* Alignment space: " + abContext.getAlignmentContext().getAlignmentSpaceName());
            log.debug("* Optical resolution: "+abContext.getAlignmentContext().getOpticalResolution());
            log.debug("* Pixel resolution: " + abContext.getAlignmentContext().getPixelResolution());

            for(AlignedItem alignedItem : abContext.getAlignedItems()) {

                EntityWrapper itemEntity = alignedItem.getItemWrapper();

                if ( itemEntity instanceof Sample  &&  alignedItem.isVisible() ) {

                    Sample sample = (Sample)itemEntity;

                    log.debug("  Sample: "+sample.getName());
                    log.debug("  * 3d image: "+sample.get3dImageFilepath());

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

                    log.debug("  children:");
                    for(AlignedItem neuronAlignedItem : alignedItem.getAlignedItems()) {
                        EntityWrapper childItemEntity = neuronAlignedItem.getItemWrapper();
                        if (childItemEntity instanceof Neuron) {
                            Neuron neuron = (Neuron)childItemEntity;
                            log.debug("    "+neuron.getName()+" (visible="+neuronAlignedItem.isVisible()+", maskIndex="+neuron.getMaskIndex()+")");
                            log.debug("    * mask: "+neuron.getMask3dImageFilepath());
                            log.debug("    * chan: "+neuron.getChan3dImageFilepath());
                        }
                        else if (childItemEntity instanceof VolumeImage) {
                            VolumeImage reference = (VolumeImage)childItemEntity;
                            log.debug("    "+reference.getName()+" (visible="+neuronAlignedItem.isVisible()+")");
                            log.debug("    * mask: "+reference.getMask3dImageFilepath());
                            log.debug("    * chan: "+reference.getChan3dImageFilepath());
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
        if ( alignedItem != null ) {
            log.debug("* Item Alias: {}", alignedItem.getName());
            log.debug("* Item Name: {}", alignedItem.getItemWrapper().getName());
            log.debug("* Item Visibility: {}", alignedItem.isVisible());
            log.debug("* Item Color: {} (hex={})", alignedItem.getColor(), alignedItem.getColorHex() );
        }
    }

    private void deleteAll() {
        if (loadWorker != null) {
            loadWorker.disregard();
        }
        if ( settingsPanel != null ) {
            settingsPanel.removeAllSettingsListeners();
        }
        tearDownToolbar();
        removeAll();
        boardOpen = false;
        mip3d = null;
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

    /** Rendering synchronization, to avoid thread problems and overloading. */
    private synchronized void setRendering( boolean rendering ) {
        renderingInProgress = rendering;
    }

    private synchronized boolean isRendering() {
        return renderingInProgress;
    }

    private boolean isOutstandingRenderRequest() {
        return outstandingRenderRequest;
    }

    private synchronized void setOutstandingRenderRequest(boolean outstandingRenderRequest) {
        this.outstandingRenderRequest = outstandingRenderRequest;
    }

    /**
     * This is called when the board data has been updated.
     */
    private void updateContents(final AlignmentBoardContext context) {
        logger.warn("Update-board called.");
        try {
            // TEMP
            //if ( brainGlow != null ) {
            //    brainGlow.isRunning = false;
            //} // TEMP

            if  (context != null ) {
                if ( isLoading() ) {
                    setOutstandingLoadRequest( true );
                }
                else {
                    // No launching settings at this point.
                    //tearDownToolbar();
                    showLoadingIndicator();
                    createMip3d();
                    wrapperPanel = createWrapperPanel( mip3d );

                    mip3d.refresh();

                    // Next, setup the volume model with some required data.
                    VolumeModel volumeModel = mip3d.getVolumeModel();
                    volumeModel.setVoxelMicrometers(
                            parseResolution( context.getAlignmentContext().getOpticalResolution() )
                    );
                    volumeModel.setVoxelDimensions(
                            parseDimensions( context.getAlignmentContext().getPixelResolution() )
                    );

                    multiMaskTracker.clear(); // New creation of board data implies discard old mask mappings.

                    // Here, should load volumes, for all the different items given.
                    if ( loadWorker != null  &&  loadWorker.getProgressMonitor() != null ) {
                        loadWorker.getProgressMonitor().close();
                        loadWorker.setProgressMonitor( null );
                    }
                    loadWorker = null;
                    dataSource = new ABContextDataSource(context);
                    if ( cachedDownSampleGuess == null ) {
                        GpuSampler sampler = getGpuSampler();
                        loadWorker = new RenderablesLoadWorker(
                                dataSource,
                                renderMapping,
                                AlignmentBoardViewer.this,
                                settingsData,
                                multiMaskTracker,
                                sampler
                        );
                    }
                    else {
                        loadWorker = new RenderablesLoadWorker(
                                dataSource,
                                renderMapping,
                                AlignmentBoardViewer.this,
                                settingsData,
                                multiMaskTracker
                        );
                    }

                    IndeterminateProgressMonitor monitor =
                            new IndeterminateProgressMonitor(
                                    SessionMgr.getBrowser(), "Updating alignment board...", context.getName()
                            );
                    loadWorker.setProgressMonitor( monitor );
                    fileStats.clear();
                    loadWorker.setFileStats( fileStats );
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

    private GpuSampler getGpuSampler() {
        // Must find the best downsample rate.
        GpuSampler sampler = new GpuSampler( this.getBackground() );
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        GLJPanel feedbackPanel = new GLJPanel( capabilities );
        feedbackPanel.setName( SAMPLER_PANEL_NAME );

        feedbackPanel.setSize( GPU_FEEDBACK_PANEL_SIZE );
        feedbackPanel.setPreferredSize( GPU_FEEDBACK_PANEL_SIZE );
        feedbackPanel.setMaximumSize( GPU_FEEDBACK_PANEL_SIZE );
        feedbackPanel.addGLEventListener( sampler );
        feedbackPanel.setToolTipText( "Reading OpenGL values..." );

        JPanel holder = new JPanel();
        holder.setLayout( new FlowLayout() );
        holder.add( feedbackPanel );
        this.add(holder, BorderLayout.SOUTH);

        revalidate();
        repaint();
        return sampler;
    }

    private void deserializeSettings(AlignmentBoardContext context) {
        Entity alignmentBoard = context.getInternalEntity();
        UserSettingSerializer userSettingSerializer = new UserSettingSerializer(
                alignmentBoard, mip3d.getVolumeModel(), settingsData
        );
        userSettingSerializer.deserializeSettings();
    }

    /**
     * Build out the Mip3D object for rendering all.  Make listeners on it so the viewer changes its data
     * as needed.
     */
    private void createMip3d() {
        //if ( settings != null ) {
        //    settings.removeAllSettingsListeners();
        //}
        tearDownToolbar();
        if ( mip3d != null ) {
            mip3d.releaseMenuActions();
        }
        mip3d = new ScaledMip3d();
        // If the mip3d is re-created, so must the settings dialog be.  It depends on the Mip3d.
        if ( settingsDialog != null ) {
            settingsDialog.dispose();
            settingsDialog.setVisible( false );
            settingsPanel = null;
            settingsDialog = null;
        }
        else if ( settingsPanel != null ) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
        logger.info("New settings");
        controls = new AlignmentBoardControls( mip3d, mip3d.getVolumeModel(), settingsData );
        settingsDialog = new AlignmentBoardControlsDialog( mip3d, mip3d.getVolumeModel(), settingsData, controls );
        settingsPanel = settingsDialog.getControlsPanel();
        settingsPanel.addSettingsListener(
                new AlignmentBoardControlsListener( renderMapping, this )
        );
        deserializeSettings(SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext());
        mip3d.addMenuAction( settingsDialog.getLaunchAction() );
        settingsPanel.update( true );

        double cameraFocusDistance = mip3d.getVolumeModel().getCameraFocusDistance();
        mip3d.getVolumeModel().getCamera3d().setPixelsPerSceneUnit(Math.abs(BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS / cameraFocusDistance));
    }

    private void jostleContainingFrame() {
        // To remind a multi-monitor window of where the tool tips should be shown.
        Browser browser = SessionMgr.getBrowser();
        Point location = browser.getLocation();
        browser.setLocation( new Point( (int)location.getX()+1, (int)location.getY()+1 ) );
        browser.setLocation( location );
    }

    private JPanel createWrapperPanel( Mip3d mip3d ) {
        if ( wrapperPanel != null ) {
            wrapperPanel.removeAll();
            remove( wrapperPanel );
        }
        JPanel rtnVal = new JPanel();
        rtnVal.setLayout(new BorderLayout());
//        JSplitPane mipAndControls = new JSplitPane(
//                JSplitPane.HORIZONTAL_SPLIT,
//                mip3d,
//                settingsPanel
//        );
//        mipAndControls.setDividerLocation(0.5);
//        mipAndControls.setResizeWeight( 1.0 );
//        mipAndControls.setOneTouchExpandable( true );
//        mipAndControls.setContinuousLayout( false );
//        mipAndControls.updateUI();

        rtnVal.add(mip3d, BorderLayout.CENTER);
        jostleContainingFrame();
        return rtnVal;
    }

    /** This must be called to add the button on re-entry to this widget. */
    private void setupToolbar() {
logger.info("Setting up toolbar.");
        JButton launchSettingsButton = new JButton();
        launchSettingsButton.setAction(settingsDialog.getLaunchAction());

        if ( toolbar == null ) {
            toolbar = new JToolBar( JToolBar.HORIZONTAL );
        }

        // Now add buttons for saving files.
        toolbar.add(controls.getColorSaveButton());
        configureButton(controls.getColorSaveButton(), COLOR_SAVE_BTN_NAME);
        toolbar.add(controls.getSearchSaveButton());
        configureButton(controls.getSearchSaveButton(), SEARCH_SAVE_BTN_NAME);
        toolbar.add(controls.getScreenShotButton());
        configureButton(controls.getScreenShotButton(), SCREEN_SHOT_BTN_NAME);

        toolbar.add(launchSettingsButton);
        configureButton(launchSettingsButton, SETTINGS_LAUNCH_BTN_NAME);

        add(toolbar, BorderLayout.PAGE_START);

    }

    private void configureButton(JButton toolbarButton, String name) {
        toolbarButton.setFocusable(false);
        toolbarButton.setRequestFocusEnabled(false);
        toolbarButton.setSelected(false);
        toolbarButton.setName(name);
    }

    /** Cleanup old button, to avoid user temptation to use it, and ensure no duplication. */
    private void tearDownToolbar() {
        if ( toolbar != null ) {
logger.info("Tearing down toolbar.");
            List<Component> toRemove = new ArrayList<Component>();
            for ( Component comp: toolbar.getComponents() ) {
                if ( SETTINGS_LAUNCH_BTN_NAME.equals(comp.getName()) ) {
                    toRemove.add( comp );
                }
                else if ( COLOR_SAVE_BTN_NAME.equals(comp.getName()) ) {
                    toRemove.add( comp );
                }
                else if ( SEARCH_SAVE_BTN_NAME.equals(comp.getName() ) ) {
                    toRemove.add( comp );
                }
                else if ( SCREEN_SHOT_BTN_NAME.equals(comp.getName() ) ) {
                    toRemove.add( comp );
                }
            }
            for ( Component comp: toRemove ) {
                toolbar.remove( comp );
            }

            remove( toolbar );
            toolbar = null;
        }
    }

    /**
     * This is called when the board visibility or coloring has been change.
     */
    private void updateRendering( AlignmentBoardContext context ) {
        logger.debug("Update-rendering called.");

        try {
            if (context != null) {
                if ( isRendering() ) {
                    setOutstandingRenderRequest( true );
                }
                else {
                    setRendering( true );

                    // Here, simply make the rendering change.
                    loadWorker = null;
                    loadWorker = new RenderablesLoadWorker(
                            dataSource, renderMapping, this, settingsData, multiMaskTracker
                    );
                    loadWorker.setLoadFilesFlag( Boolean.FALSE );
                    loadWorker.execute();
                }
            }
        } catch ( Throwable th ) {
            SessionMgr.getSessionMgr().handleException( th );
        }

    }

    private float[] parseResolution( String resolutionString ) {
        float[] rtnVal = new float[ 3 ];
        String[] resolutionStrs = resolutionString.split( "x" );
        if ( resolutionStrs.length == rtnVal.length ) {
            for ( int i = 0; i < rtnVal.length; i++ ) {
                try {
                    rtnVal[ i ] = Float.parseFloat( resolutionStrs[ i ] );
                } catch ( NumberFormatException ex ) {
                    logger.error( "Failed to parse {} member {}.", resolutionString, resolutionStrs[ i ] );
                }
            }
        }
        else {
            logger.error( "Failed to parse {}.", resolutionString );
        }
        return rtnVal;
    }

    private int[] parseDimensions( String dimensionsString ) {
        int[] rtnVal = new int[ 3 ];
        String[] dimensionStrs = dimensionsString.split( "x" );
        if ( dimensionStrs.length == rtnVal.length ) {
            for ( int i = 0; i < rtnVal.length; i++ ) {
                try {
                    rtnVal[ i ] = Integer.parseInt(dimensionStrs[i]);
                } catch ( NumberFormatException ex ) {
                    logger.error( "Failed to parse {} member {}.", dimensionsString, dimensionStrs[ i ] );
                }
            }
        }
        else {
            logger.error( "Failed to parse {}.", dimensionsString );
        }
        return rtnVal;
    }

    //------------------------------Inner Classes
    public static class AlignmentBoardControlsListener implements ControlsListener {
        private AlignmentBoardViewer viewer;
        private RenderMappingI renderMapping;
        public AlignmentBoardControlsListener(RenderMappingI renderMapping, AlignmentBoardViewer viewer) {
            this.viewer = viewer;
            this.renderMapping = renderMapping;
        }
        @Override
        public void setBrightness(double brightness) {
            viewer.mip3d.setGamma((float) brightness);
        }

        @Override
        public void updateSettings() {
            try {
                Thread thread = new Thread( new Runnable() {
                    public void run() {
                        AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
                        viewer.serialize();
                        viewer.updateContents(context);
                    }
                });
                thread.start();
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }

        @Override
        public void updateCropCoords() {
            AlignmentBoardContext context = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
            viewer.mip3d.refreshRendering();

            viewer.updateRendering( context );
        }

        @Override
        public void exportSelection( SavebackEvent event ) {
            AlignmentBoardSettings settingsData = viewer.settingsData;
            VolumeWritebackHandler writebackHandler = new VolumeWritebackHandler(
                    renderMapping,
                    event.getAbsoluteCoords(),
                    event.getCompletionListener(),
                    viewer.mip3d,
                    event.getGammaFactor(),
                    (int)settingsData.getMinimumVoxelCount()
            );
            writebackHandler.writeBackVolumeSelection(event.getMethod());
        }

        @Override
        public void setCropBlackout( boolean blackout ) {
            viewer.mip3d.setCropOutLevel(blackout ? 0.0f : VolumeModel.DEFAULT_CROPOUT);
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
                mip3d.setGamma(gamma);
                try {
                    Thread.sleep( 60 );
                } catch ( Exception ex ) {
                    break;
                }
            }
        }
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
            serialize();
        }

        @Override
        public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
        }

    }

}
