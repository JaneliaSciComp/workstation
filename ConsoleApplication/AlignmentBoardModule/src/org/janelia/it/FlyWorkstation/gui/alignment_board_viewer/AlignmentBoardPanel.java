package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.util.Collections;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControls;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsPanel;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.ControlsListener;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.GpuSampler;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.SavebackEvent;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.FileStats;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture.ABContextDataSource;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.VolumeWritebackHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateNoteProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.FlyWorkstation.gui.viewer3d.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.events.AlignmentBoardOpenEvent;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.top_component.AlignmentBoardControlsTopComponent;
import org.janelia.it.FlyWorkstation.gui.util.WindowLocator;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/3/13
 * Time: 4:42 PM
 *
 * Shows alignment board relevant entities in 3D.
 */
@ServiceProvider(service = AlignmentBoardCtrlPnlSvc.class)
public class AlignmentBoardPanel extends JPanel implements AlignmentBoardControllable, AlignmentBoardCtrlPnlSvc {

    private static final String SETTINGS_LAUNCH_BTN_NAME = "AlignmentBoard::SettingsLaunchButton";
    private static final String COLOR_SAVE_BTN_NAME = "AlignmentBoard::ColorSaveButton";
    private static final String SEARCH_SAVE_BTN_NAME = "AlignmentBoard::SearchSaveButton";
    private static final String SCREEN_SHOT_BTN_NAME = "AlignmentBoard::ScreenShotButton";
    private static final String SETTINGS_PANEL_NAME = "AlignmentBoard::SettingsPanel";
    public static final String SAMPLER_PANEL_NAME = "GpuSampler";
    private static final Dimension GPU_FEEDBACK_PANEL_SIZE = new Dimension( 1, 1 );
    public static final String TOO_MANY_ITEMS_MSG = "The large number of items in the alignment board is causing image degradation.\n" +
            "To improve the image, please remove some reference channels or samples.";

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
    private Logger logger = LoggerFactory.getLogger(AlignmentBoardPanel.class);

    private boolean loadingInProgress = false;
    private boolean outstandingLoadRequest = false;

    private boolean renderingInProgress = false;
    private boolean outstandingRenderRequest = false;

    private boolean preExistingBoard = true;

    private boolean boardOpen = false;
    private boolean connectEditEvents = true;

    private Double cachedDownSampleGuess = null;
    private AlignmentBoardSettings settingsData;
    private ShutdownListener shutdownListener;
    private JToolBar toolbar;
    private ABContextDataSource dataSource;
    private FileStats fileStats;
    
    private InstanceContent content = new InstanceContent();

    public AlignmentBoardPanel() {
        logger.info( "C'tor" );
        settingsData = new AlignmentBoardSettings();
        multiMaskTracker = new MultiMaskTracker();
        fileStats = new FileStats();
        multiMaskTracker.setFileStats( fileStats );
        renderMapping = new ConfigurableColorMapping( multiMaskTracker, fileStats );
        setLayout(new BorderLayout());
        
        setTransferHandler( new AlignmentBoardEntityTransferHandler( this ) );

        // Saveback settings.
        shutdownListener = new ShutdownListener();
        SessionMgr.getSessionMgr().addSessionModelListener( shutdownListener );                
    }

    /** Call this at clear-time. */
    public void clear() {
        logger.info("Clearing the a-board.");
        tearDownToolbar();
    }

    /** Call this to show loading state. */
    public void showLoadingIndicator() {
        setLoading(true);
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

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
    public void close() {
        logger.info( "Closing" );
        // Cleanup this listener to avoid mem leaks.
        LayersPanel layersPanel = AlignmentBoardMgr.getInstance().getLayersPanel();
        removeSettingsPanel( layersPanel );
        serialize();

        deleteAll();
        AlignmentBoardMgr.getInstance().getLayersPanel().closeAlignmentBoard();
    }

    /** Call this for refresh time. */
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

    public void handleBoardOpened(AlignmentBoardOpenEvent event) {
        logger.info("Board Opened");

        boardOpen = false;
        
        AlignmentBoardContext abContext = event.getAlignmentBoardContext();
        try {
            Entity alignmentBoard =  ModelMgr.getModelMgr().getEntityById(abContext.getInternalEntity().getId());
            preExistingBoard = UserSettingSerializer.settingsExist( alignmentBoard );
            
            // Carry out any steps to prepare GUI for presence of this panel.
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        handleBoardOpened(abContext);
    }

    public void handleItemChanged(AlignmentBoardItemChangeEvent event) {
        logger.debug("Item changed");
        if ( ! connectEditEvents ) {
            logger.info("Ignoring board open, because connection has been bypassed.");
            return;
        }

        // Check this, to prevent this being completed until the board has been first initialized.
        // Redundant events may be posted at startup.
        if ( boardOpen ) {
            final AlignmentBoardContext abContext = event.getAlignmentBoardContext();

            if ( AlignmentBoardItemChangeEvent.ChangeType.VisibilityChange.equals( event.getChangeType() ) ||
                 AlignmentBoardItemChangeEvent.ChangeType.ColorChange.equals( event.getChangeType() ) ) {

                // Changing the render mapping values.
                this.updateRendering( abContext );

            }
            else if ( ! AlignmentBoardItemChangeEvent.ChangeType.FilterLevelChange.equals( event.getChangeType() ) ) {
                logger.info( "Change type {}.", event.getChangeType() );
                SimpleWorker updateContentsWorker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        serialize();
                    }

                    @Override
                    protected void hadSuccess() {
                        updateContents(abContext);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException( error );
                    }
                };
                updateContentsWorker.execute();
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

        logger.info("Setting Mip3d Volume.");
        MultiTexVolumeBrickFactory volumeBrickFactory = new MultiTexVolumeBrickFactory();
        if ( ! mip3d.setVolume(
                signalTexture,
                maskTexture,
                volumeBrickFactory,
                renderMapping,
                settingsData.getAcceptedDownsampleRate() )
            ) {
            String msg = "Failed to load volume to mip3d.";
            logger.error(msg);
            throw new RuntimeException( msg );
        }
        else {
            logger.info("Setting volume maxima on settings.");
            settingsPanel.setVolumeMaxima(signalTexture.getSx(), signalTexture.getSy(), signalTexture.getSz());
        }

        multiMaskTracker.checkDepthExceeded();
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
        AlignmentBoardContext abContext = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
        deserializeSettings(abContext);

        if ( !preExistingBoard) {
            mip3d.setResetFirstRedraw( true );
        }
        else {
            // Ensure pixels per scene unit properly accounted-for.
            mip3d.getVolumeModel().setCameraPixelsPerSceneUnit( BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS, mip3d.getVolumeModel().getCameraFocusDistance() );
        }

        if ( settingsPanel != null ) {
            settingsPanel.setEnabled( true );
        }
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
        if ( successful ) {
            revalidate();
            repaint();
            if ( loadFiles ) {
                mip3d.refresh();
                // Post this event, nagging the outline to update itself.
                // NOTE: better not do any refreshing in this board, if this event is encountered.  Would lead
                // to a cycle of updates between this viewer and the outline!
                final LayersPanel layersPanel = AlignmentBoardMgr.getInstance().getLayersPanel();
                layersPanel.showOutline();   // On the event thread!

                // Notify of any limits exceeded.
                checkLimits();

                new Thread( new Runnable() {
                    public void run() {
                        // Ensure all shown.
                        AlignmentBoardItemChangeEvent event = new AlignmentBoardItemChangeEvent(
                                layersPanel.getAlignmentBoardContext(),
                                null,
                                AlignmentBoardItemChangeEvent.ChangeType.FilterLevelChange
                        );
                        ModelMgr.getModelMgr().postOnEventBus( event );
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
            if ( logger.isDebugEnabled() ) {
                multiMaskTracker.writeOutstandingDump();
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
            AlignmentBoardContext abContext = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();

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
            AlignmentBoardContext abContext = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
            setRendering( false );
            updateRendering(abContext);
        }
        else {
            setRendering(false);
        }
    }

    public void serializeInWorker() {
        AlignmentBoardContext context = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
        if ( context != null ) {
            if ( mip3d != null && settingsPanel != null ) {
                SimpleWorker serializeWorker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        AlignmentBoardContext context =
                                AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
                        Entity alignmentBoard =  ModelMgr.getModelMgr().getEntityById(context.getInternalEntity().getId());

                        UserSettingSerializer userSettingSerializer = new UserSettingSerializer(
                                alignmentBoard, mip3d.getVolumeModel(), settingsData
                        );
                        userSettingSerializer.serializeSettings();
                    }

                    @Override
                    protected void hadSuccess() {
                        // Nothing more. exiting
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException( error );
                    }
                };
                serializeWorker.execute();
            }
            else {
                logger.warn("Attempt at serializing while mip3d={} and settings dialog={}.", mip3d, settingsPanel);
            }
        }
    }

    //------------------------------IMPLEMENTS AlignmentBoardCtrlPnlSvc
    @Override
    public AlignmentBoardControlsPanel getControlsComponent() {
        return settingsPanel;
    }

    //---------------------------------------HELPERS
    private void serialize() {
        AlignmentBoardContext context = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
        if ( context != null ) {
            if ( mip3d != null && settingsPanel != null ) {
                try {
                        Entity alignmentBoard =  ModelMgr.getModelMgr().getEntityById(context.getInternalEntity().getId());

                        UserSettingSerializer userSettingSerializer = new UserSettingSerializer(
                                alignmentBoard, mip3d.getVolumeModel(), settingsData
                        );
                        userSettingSerializer.serializeSettings();
                } catch ( Throwable error ) {
                    SessionMgr.getSessionMgr().handleException( error );
                }
            }
            else {
                logger.warn("Attempt at serializing while mip3d={} and settings dialog={}.", mip3d, settingsPanel);
            }
        }
    }

    /**
     * After load completion, can check whether any late-detectable limits were exceeded.
     */
    private void checkLimits() {
        if ( fileStats != null ) {
            StringBuilder errorStack = new StringBuilder();
            if ( fileStats.isMasksExhausted() ) {
                errorStack.append(
                        TOO_MANY_ITEMS_MSG +
                                "\nYou may also set the max neuron count lower, or the min neuron size higher to " +
                                "exclude items from display."
                );
            }
            else if ( fileStats.getMaxDepthExceededCount() > 0 ) {
                errorStack.append( TOO_MANY_ITEMS_MSG );
            }

            if ( errorStack.length() > 0 ) {
                logger.info("Launching message dialog");
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), errorStack.toString());
                logger.info("Message dialog complete.");
            }
        }
    }

    /** This is synch'd because there may be a race between constructor and an externally-posted event. */
    private synchronized void handleBoardOpened(AlignmentBoardContext abContext) {
        if ( ! boardOpen ) {
            // The true update!
            this.updateContents(abContext);
            boardOpen = true;
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
                    int[] axialLengths = parseDimensions( context.getAlignmentContext().getPixelResolution() );
                    volumeModel.setVoxelDimensions( axialLengths );

                    multiMaskTracker.clear(); // New creation of board data implies discard old mask mappings.

                    // Here, should load volumes, for all the different items given.
                    if ( loadWorker != null  &&  loadWorker.getProgressMonitor() != null ) {
                        loadWorker.getProgressMonitor().close();
                        loadWorker.setProgressMonitor( null );
                    }
                    loadWorker = null;
                    dataSource = new ABContextDataSource(context);
                    if ( cachedDownSampleGuess == null ) {
                        GpuSampler sampler = getGpuSampler(context.getAlignmentContext());
                        loadWorker = new RenderablesLoadWorker(
                                dataSource,
                                renderMapping,
                                AlignmentBoardPanel.this,
                                settingsData,
                                multiMaskTracker,
                                sampler
                        );
                        loadWorker.setAxialLengths(axialLengths);
                    }
                    else {
                        loadWorker = new RenderablesLoadWorker(
                                dataSource,
                                renderMapping,
                                AlignmentBoardPanel.this,
                                settingsData,
                                multiMaskTracker
                        );
                    }

                    IndeterminateNoteProgressMonitor monitor =
                            new IndeterminateNoteProgressMonitor(
                                    SessionMgr.getMainFrame(), "Updating alignment board...", context.getName()
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

    private GpuSampler getGpuSampler(AlignmentContext alignmentContext) {
        float[] opticalResolution = parseResolution(alignmentContext.getOpticalResolution());

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
        LayersPanel layersPanel = AlignmentBoardMgr.getInstance().getLayersPanel();

        mip3d = new ScaledMip3d();

        // If the mip3d is re-created, so must the settings dialog be.  It depends on the Mip3d.
        removeSettingsPanel(layersPanel);
        logger.info("New settings");
        controls = new AlignmentBoardControls( mip3d, mip3d.getVolumeModel(), settingsData );
        
        settingsPanel = new AlignmentBoardControlsPanel( controls );
        settingsPanel.setName( SETTINGS_PANEL_NAME );
        settingsPanel.setEnabled( false );
        
        deserializeSettings(AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext());
        //mip3d.addMenuAction( settingsDialog.getLaunchAction() );
        settingsPanel.update( true );

        double cameraFocusDistance = mip3d.getVolumeModel().getCameraFocusDistance();
        mip3d.getVolumeModel().getCamera3d().setPixelsPerSceneUnit(Math.abs(BaseRenderer.DISTANCE_TO_SCREEN_IN_PIXELS / cameraFocusDistance));

        settingsPanel.addSettingsListener(
                new AlignmentBoardControlsListener( renderMapping, this )
        );
        settingsPanel.setReadyForOutput( true );  // Major events will not be generated by controls until this is turned on.

        //@TODO this points out a problem with controls panel.  May need to
        // modify whole concept to work better in NetBeans.  There should be
        // no need for AlignmentBoardPanel to "know" about a top component.
        // It does not know its own owning component.  Dependencies need to
        // be reconsidered, as well as listening, such that the settings can
        // be dealt with in a Framework-friendly fashion.
        AlignmentBoardControlsTopComponent ctrlTc = 
                (AlignmentBoardControlsTopComponent)WindowLocator.getByName("AlignmentBoardControlsTopComponent");
        ctrlTc.setControls( settingsPanel );
    }

    private void removeSettingsPanel(LayersPanel layersPanel) {
        if ( settingsDialog != null ) {
            settingsDialog.dispose();
            settingsDialog.setVisible( false );
            settingsPanel = null;
            settingsDialog = null;
        }
        else if ( settingsPanel != null ) {
            layersPanel.remove( settingsPanel );
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }

    private void jostleContainingFrame() {
        // To remind a multi-monitor window of where the tool tips should be shown.
        Component container = SessionMgr.getMainFrame();
        Point location = container.getLocation();
        container.setLocation( new Point( (int)location.getX()+1, (int)location.getY()+1 ) );
        container.setLocation( location );
    }

    private JPanel createWrapperPanel( Mip3d mip3d ) {
        if ( wrapperPanel != null ) {
            wrapperPanel.removeAll();
            remove( wrapperPanel );
        }
        JPanel rtnVal = new JPanel();
        rtnVal.setLayout(new BorderLayout());

        rtnVal.add(mip3d, BorderLayout.CENTER);
        jostleContainingFrame();
        return rtnVal;
    }

    /** This must be called to add the button on re-entry to this widget. */
    private void setupToolbar() {
        tearDownToolbar(); // Calling this just in case it was left dangling.
        //JButton launchSettingsButton = new JButton();
        //launchSettingsButton.setAction(settingsDialog.getLaunchAction());

        if ( toolbar == null ) {
            toolbar = new JToolBar( JToolBar.HORIZONTAL );
        }

        // Now add buttons for saving files.
        configureButton(controls.getColorSave(), COLOR_SAVE_BTN_NAME);
        configureButton(controls.getSearchSave(), SEARCH_SAVE_BTN_NAME);
        configureButton(controls.getScreenShot(), SCREEN_SHOT_BTN_NAME);
        //configureButton(launchSettingsButton, SETTINGS_LAUNCH_BTN_NAME);

        toolbar.add(controls.getColorSave());
        toolbar.add(controls.getSearchSave());
        toolbar.add(controls.getScreenShot());

        toolbar.add(controls.getSearch());

        toolbar.add(controls.getBlackout());
        toolbar.add(controls.getColorSaveBrightness());
        toolbar.add(controls.getConnectEvents()); // NOTE: can omit this control, here.

        //toolbar.add(launchSettingsButton);

        add(toolbar, BorderLayout.PAGE_START);

    }

    private void configureButton(AbstractButton toolbarButton, String name) {
        toolbarButton.setFocusable(false);
        toolbarButton.setRequestFocusEnabled(false);
        toolbarButton.setSelected(false);
        toolbarButton.setName(name);
    }

    /** Cleanup old button, to avoid user temptation to use it, and ensure no duplication. */
    private void tearDownToolbar() {
        if ( toolbar != null ) {
            toolbar.removeAll();
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
        private AlignmentBoardPanel viewer;
        private RenderMappingI renderMapping;
        public AlignmentBoardControlsListener(RenderMappingI renderMapping, AlignmentBoardPanel viewer) {
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
                        AlignmentBoardContext context = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
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
            AlignmentBoardContext context = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
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
                    (int)settingsData.getMinimumVoxelCount(),
                    (int)settingsData.getMaximumNeuronCount()
            );
            writebackHandler.writeBackVolumeSelection(event.getMethod());
        }

        @Override
        public void setCropBlackout( boolean blackout ) {
            viewer.mip3d.setCropOutLevel(blackout ? 0.0f : VolumeModel.DEFAULT_CROPOUT);
        }

        @Override
        public void setConnectEditEvents( boolean connectEditEvents ) {
            viewer.connectEditEvents = connectEditEvents;
        }

        @Override
        public void forceRebuild() {
            AlignmentBoardContext ctx = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
            viewer.updateContents( ctx );
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
            serializeInWorker();
        }

        //@Override
        public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
        }

    }

}
