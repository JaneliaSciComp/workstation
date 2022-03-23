package org.janelia.horta;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.text.Keymap;

import Jama.Matrix;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import org.janelia.geometry3d.ObservableInterface;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.controller.dialog.NeuronColorDialog;
import org.janelia.workstation.controller.listener.ColorModelListener;
import org.janelia.workstation.controller.listener.UnmixingListener;
import org.janelia.workstation.controller.listener.NeuronVertexCreationListener;
import org.janelia.workstation.controller.listener.NeuronVertexDeletionListener;
import org.janelia.workstation.controller.listener.NeuronVertexUpdateListener;
import org.janelia.workstation.controller.listener.TolerantMouseClickListener;
import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ColorSwatch;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.controller.model.annotations.neuron.VertexCollectionWithNeuron;
import org.janelia.workstation.controller.model.annotations.neuron.VertexWithNeuron;
import org.janelia.geometry3d.*;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.MultipassRenderer;
import org.janelia.gltools.material.TransparentEnvelope;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.janelia.horta.actions.ResetHortaRotationAction;
import org.janelia.horta.activity_logging.ActivityLogHelper;
import org.janelia.horta.actors.CenterCrossHairActor;
import org.janelia.horta.actors.ScaleBar;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.blocks.KtxOctreeBlockTileSource;
import org.janelia.horta.controller.HortaManager;
import org.janelia.horta.loader.DroppedFileHandler;
import org.janelia.horta.loader.GZIPFileLoader;
import org.janelia.horta.loader.HortaKtxLoader;
import org.janelia.horta.loader.HortaVolumeCache;
import org.janelia.horta.loader.LZ4FileLoader;
import org.janelia.horta.loader.ObjMeshLoader;
import org.janelia.horta.loader.TarFileLoader;
import org.janelia.horta.loader.TgzFileLoader;
import org.janelia.horta.loader.TilebaseYamlLoader;
import org.janelia.horta.movie.HortaMovieSource;
import org.janelia.horta.render.NeuronMPRenderer;
import org.janelia.horta.volume.BrickActor;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.LocalVolumeBrickSource;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeLoaderImpl;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.scenewindow.OrbitPanZoomInteractor;
import org.janelia.scenewindow.SceneRenderer;
import org.janelia.scenewindow.SceneRenderer.CameraType;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.scenewindow.fps.FrameTracker;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.access.ModelTranslation;
import org.janelia.workstation.controller.action.*;
import org.janelia.workstation.controller.dialog.NeuronGroupsDialog;
import org.janelia.workstation.controller.dialog.NeuronHistoryDialog;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.controller.options.ApplicationPanel;
import org.janelia.workstation.core.api.LocalCacheMgr;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.actions.RedoAction;
import org.openide.actions.UndoAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.MouseUtils;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.horta//NeuronTracer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = NeuronTracerTopComponent.PREFERRED_ID,
        iconBase = "org/janelia/horta/images/neuronTracerCubic16.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.horta.NeuronTracerTopComponent")
@ActionReference(path = "Menu/Window/Horta", position = 2)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_NeuronTracerAction",
        preferredID = NeuronTracerTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_NeuronTracerAction=Horta 3D",
    "CTL_NeuronTracerTopComponent=Horta 3D",
    "HINT_NeuronTracerTopComponent=Horta Neuron Tracer window"
})
public final class NeuronTracerTopComponent extends TopComponent
        implements VolumeProjection, NeuronVertexUpdateListener, NeuronVertexDeletionListener, NeuronVertexCreationListener {

    static final String PREFERRED_ID = "NeuronTracerTopComponent";
    private static final int CACHE_CONCURRENCY = 10;
    private SceneWindow sceneWindow;
    private OrbitPanZoomInteractor worldInteractor;

    private VolumeMipMaterial.VolumeState volumeState = new VolumeMipMaterial.VolumeState();

    // Avoid letting double clicks move twice
    private long previousClickTime = Long.MIN_VALUE;
    private final long minClickInterval = 400 * 1000000;

    // Cache latest hover information
    private Vector3 mouseStageLocation = null;
    private final Observer cursorCacheDestroyer;

    private TracingInteractor tracingInteractor;

    private final RenderedVolumeLoader renderedVolumeLoader;
    // Old way for loading raw tiles
    private StaticVolumeBrickSource volumeSource;
    // New way for loading ktx tiles
    private KtxOctreeBlockTileSource ktxSource;

    private CenterCrossHairActor crossHairActor;
    private ScaleBar scaleBar = new ScaleBar();
    private ActivityLogHelper activityLogger = ActivityLogHelper.getInstance();

    private final NeuronMPRenderer neuronMPRenderer;

    private String currentSource;
    private final NeuronTraceLoader neuronTraceLoader;

    private boolean leverageCompressedFiles = false;

    private boolean doCubifyVoxels = false; // Always begin in "no distortion" state

    private boolean pausePlayback = false;

    // review animation
    private PlayReviewManager playback;
    
    private final HortaManager hortaManager;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static NeuronTracerTopComponent findThisComponent() {
        return (NeuronTracerTopComponent) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }
    private int defaultColorChannel = 0;

    private final HortaVolumeCache volumeCache;

    private final KtxBlockMenuBuilder ktxBlockMenuBuilder = new KtxBlockMenuBuilder();

    public static NeuronTracerTopComponent getInstance() {
        return findThisComponent();
    }

    public NeuronTracerTopComponent() {
        renderedVolumeLoader = new RenderedVolumeLoaderImpl();

        // This block is what the wizard created
        initComponents();
        setName(Bundle.CTL_NeuronTracerTopComponent());
        setToolTipText(Bundle.HINT_NeuronTracerTopComponent());

        // Below is custom methods by me CMB

        // Insert a specialized SceneWindow into the component
        initialize3DViewer(); // initializes workspace

        // Change default rotation to Y-down, like large-volume viewer
        sceneWindow.getVantage().setDefaultRotation(new Rotation().setFromAxisAngle(
                new Vector3(1, 0, 0), (float) Math.PI));
        sceneWindow.getVantage().resetRotation();

        setupMouseNavigation();

        // Create right-click context menu
        setupContextMenu(sceneWindow.getInnerComponent());

        // Press "V" to hide all neuron models
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("pressed V"), "hideModels");
        inputMap.put(KeyStroke.getKeyStroke("released V"), "unhideModels");
        ActionMap actionMap = getActionMap();
        actionMap.put("hideModels", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean bChanged = false;
                if (getNeuronMPRenderer().setHideAll(true))
                    bChanged = true;
                // Use "v" key to show/hide primary "P" anchor
                for (GL3Actor actor : tracingActors) {
                    if (actor.isVisible()) {
                        actor.setVisible(false);
                        bChanged = true;
                    }
                }
                if (bChanged)
                    redrawNow();
            }
        });
        actionMap.put("unhideModels", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean bChanged = false;
                if (getNeuronMPRenderer().setHideAll(false))
                    bChanged = true;
                // Use "v" key to show/hide primary "P" anchor
                for (GL3Actor actor : tracingActors) {
                    if (!actor.isVisible()) {
                        actor.setVisible(true);
                        bChanged = true;
                    }
                }
                if (bChanged)
                    redrawNow();
            }
        });

        // When the camera changes, that blows our cached cursor information
        cursorCacheDestroyer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                mouseStageLocation = null; // clear cursor cache
            }
        };
        sceneWindow.getCamera().addObserver(cursorCacheDestroyer);

        // Load new volume data when the focus moves
        volumeCache = new HortaVolumeCache(
                (PerspectiveCamera) sceneWindow.getCamera(),
                imageColorModel,
                // brightnessModel,
                volumeState,
                defaultColorChannel
        );
        volumeCache.addObserver(new HortaVolumeCache.TileDisplayObserver() {
            @Override
            public void update(BrickActor newTile, Collection<? extends BrickInfo> allTiles) {
                if (!allTiles.contains(newTile.getBrainTile()))
                    return; // Tile is stale, so don't load it

                // Undisplay stale tiles and upload to GPU
                Iterator<GL3Actor> iter = getNeuronMPRenderer().getVolumeActors().iterator();
                boolean tileAlreadyDisplayed = false;
                while (iter.hasNext()) {
                    GL3Actor actor = iter.next();
                    if (!(actor instanceof BrickActor))
                        continue;
                    BrickActor brickActor = (BrickActor) actor;
                    BrickInfo actorInfo = brickActor.getBrainTile();
                    // Check whether maybe the new tile is already displayed somehow
                    if (actorInfo.isSameBrick(newTile.getBrainTile())) {
                        tileAlreadyDisplayed = true;
                        continue;
                    }
                    // Remove displayed tiles that are no longer current
                    if (!allTiles.contains(actorInfo)) {
                        iter.remove(); // Safe member deletion via iterator
                        getNeuronMPRenderer().queueObsoleteResource(brickActor);
                    }
                }
                // Upload up to one tile per update call
                if (!tileAlreadyDisplayed) {
                    getNeuronMPRenderer().addVolumeActor(newTile);
                    redrawNow();
                }
            }
        });

        TetVolumeActor.getInstance().setVolumeState(volumeState);
        TetVolumeActor.getInstance().getDynamicTileUpdateObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                getNeuronMPRenderer().setIntensityBufferDirty();
                redrawNow();
            }
        });

        neuronMPRenderer = setUpActors();

        hortaManager = new HortaManager(this, getNeuronMPRenderer(), tracingInteractor);
        //hortaManager.addNeuronVertexCreationListener(tracingInteractor);
        //hortaManager.addNeuronVertexDeletionListener(tracingInteractor);
        //hortaManager.addNeuronVertexUpdateListener(tracingInteractor);

        hortaManager.addNeuronVertexCreationListener(this);
        hortaManager.addNeuronVertexDeletionListener(this);
        hortaManager.addNeuronVertexUpdateListener(this);
        hortaManager.addNeuronSelectionListener(tracingInteractor);
        hortaManager.addNeuronDeletionListener(tracingInteractor);

        hortaManager.addNeuronCreationListener(neuronMPRenderer);
        hortaManager.addNeuronDeletionListener(neuronMPRenderer);
        hortaManager.addNeuronUpdateListener(neuronMPRenderer);

        // Drag a YML tilebase file to put some data in the viewer
        setupDragAndDropYml();

        Color backgroundColor = new Color(0.1f, 0.1f, 0.1f, 1f);
        setBackgroundColor(backgroundColor); // call this AFTER setUpActors

        // neuronMPRenderer.setWorkspace(workspace); // set up signals in renderer

        neuronTraceLoader = new NeuronTraceLoader(
                NeuronTracerTopComponent.this,
                getNeuronMPRenderer(),
                sceneWindow);

        // Default to compressed voxels, per user request February 2016
        setCubifyVoxels(true);

        //metaWorksopace.notifyObservers();
        playback = new PlayReviewManager(sceneWindow, this, neuronTraceLoader);
        if (TmModelManager.getInstance().getCurrentSample()==null)
            return;
        initSampleLocation();
        initColorModel();
        if (TmModelManager.getInstance().getCurrentWorkspace()!=null) {
            initMeshes();
        }
        ViewerEventBus.registerForEvents(this);

        MovieEvent movieEvent = new MovieEvent(this);
        ViewerEventBus.postEvent(movieEvent);

        loadStartupPreferences();

        // fire off notice for checkboxes, etc.
        ViewerOpenEvent openEvent = new ViewerOpenEvent(this,
                ViewerOpenEvent.VIEWER.HORTA);
        ViewerEventBus.postEvent(openEvent);

        // init color sliders if option set
        if (ApplicationPanel.isLoadColorSliders()) {
            TopComponent tc = WindowManager.getDefault().findTopComponent("ColorSlidersTopComponent");
            if (tc != null) {
                tc.open();
                tc.requestActive();
            }
        }
    }

    public SceneWindow getSceneWindow() {
        return sceneWindow;
    }
    
    public void stopPlaybackReview() {
        playback.setPausePlayback(true);
    }
        
    public void resumePlaybackReview(PlayReviewManager.PlayDirection direction) {
        playback.resumePlaythrough(direction);
    }

    public void updatePlaybackSpeed(boolean increase) {
        playback.updateSpeed(increase);
    }

    // convert to a listener on the viewereventbus
    private void setDefaultWorkspace(TmWorkspace workspace) {
      //  activeNeuronSet = workspace;
     //   tracingInteractor.setDefaultWorkspace(activeNeuronSet);
    }

    // UNDO
    @Override
    public UndoRedo getUndoRedo() {
        if (getUndoRedoManager() == null)
            return super.getUndoRedo();
        return getUndoRedoManager();
    }

    private UndoRedo.Manager getUndoRedoManager() {
         return null;
        //return activeNeuronSet.getUndoRedo();
    }

    void setVolumeSource(StaticVolumeBrickSource volumeSource) {
        this.volumeSource = volumeSource;
        this.volumeCache.setSource(volumeSource);
        // Don't load both ktx and raw tiles...
        if (volumeSource != null) {
            setKtxSource(null);
        }
    }

    /** Tells caller what source we are examining. */
    URL getCurrentSourceURL() throws MalformedURLException, URISyntaxException {
        if (currentSource == null)
            return null;
        return new URI(currentSource).toURL();
    }

    @Subscribe
    void playSampleLocations(AnimationEvent event) {
        List<TmViewState> locationList = event.getAnimationSteps();
        // do a quick check to see if
        sceneWindow.setControlsVisibility(true);
        ViewEvent initAnimation = new ViewEvent(this,locationList.get(0).getCameraFocusX(),
                locationList.get(0).getCameraFocusY(),
                locationList.get(0).getCameraFocusZ(),
                locationList.get(0).getZoomLevel(),
        null, false);
        setSampleLocation(initAnimation);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
        playback.reviewPoints(locationList, event.isAutoRotation(), event.getSpeed(), event.getStepScale());
    }

    // center on the brain sample
    public void initSampleLocation() {
        volumeCache.clearAllTiles();
        Vec3 voxelCenter = TmModelManager.getInstance().getVoxelCenter();
        ViewEvent event = new ViewEvent(this,voxelCenter.getX(),
                voxelCenter.getY(),voxelCenter.getZ(),5000,
                null, false);
        setSampleLocation(event);
    }

    public void initColorModel() {
        TmWorkspace tmWorkspace = TmModelManager.getInstance().getCurrentWorkspace();
        if (tmWorkspace==null) {
            imageColorModel = new ImageColorModel();
            TmModelManager.getInstance().getCurrentView().setColorModel("default", imageColorModel);
            ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                    imageColorModel);
            ViewerEventBus.postEvent(modelEvent);
            return;
        }
        // check if preferences, otherwise use workspace default color model
        TmColorModel userWorkspaceColorModel = null;
        try {
            LinkedHashMap<String, Object> modelMap = FrameworkAccess.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MOUSELIGHT_COLORMODEL,
                    tmWorkspace.getId().toString(), null);
            ObjectMapper mapper = new ObjectMapper();
            userWorkspaceColorModel = mapper.convertValue(modelMap, TmColorModel.class);
        } catch (Exception e) {
            FrameworkAccess.handleException("Problems retrieving user color model for workspace", e);
        }
        if (userWorkspaceColorModel==null) {
            userWorkspaceColorModel = tmWorkspace.getColorModel3d();
        }
        if (userWorkspaceColorModel!=null) {
            ModelTranslation.updateColorModel(userWorkspaceColorModel, imageColorModel);
            if (userWorkspaceColorModel.getUnmixParams()!=null && userWorkspaceColorModel.getUnmixParams().length==4)
                TetVolumeActor.getInstance().setUnmixingParams(userWorkspaceColorModel.getUnmixParams());
        }
        TmModelManager.getInstance().getCurrentView().setColorModel("default", imageColorModel);
        ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                 imageColorModel);
        ViewerEventBus.postEvent(modelEvent);
    }

    @Subscribe
    void setSampleLocation(ViewEvent event) {
        try {
            //leverageCompressedFiles = sampleLocation.isCompressed();
            playback.clearPlayState();
            Quaternion q = new Quaternion();
            float[] quaternionRotation = event.getCameraRotation();
            if (quaternionRotation != null) {
                q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
            }
            ViewLoader viewLoader = new ViewLoader(
                    neuronTraceLoader,this, sceneWindow
            );

            Vec3 location = new Vec3(event.getCameraFocusX(),
                    event.getCameraFocusY(), event.getCameraFocusZ());
            double zoom = event.getZoomLevel();
            viewLoader.loadView(location, zoom);
            Vantage vantage = sceneWindow.getVantage();
            if (quaternionRotation != null) {
                vantage.setRotationInGround(new Rotation().setFromQuaternion(q));
            } else {
                vantage.setRotationInGround(vantage.getDefaultRotation());
            }

            currentSource = TmModelManager.getInstance().getTileLoader().getUrl().toString();
            defaultColorChannel = 0;
            volumeCache.setColorChannel(defaultColorChannel);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to set the view in Horta", ex
            );
        }
    }

    private void playAnimation() {
        /**
         if (sampleLocation.getInterpolate()) {
         // figure out number of steps
         Vantage vantage = sceneWindow.getVantage();
         float[] startLocation = vantage.getFocus();
         double distance = Math.sqrt(Math.pow(sampleLocation.getFocusXUm()-startLocation[0],2) +
         Math.pow(sampleLocation.getFocusYUm()-startLocation[1],2) +
         Math.pow(sampleLocation.getFocusZUm()-startLocation[2],2));
         // # of steps is 1 per uM
         int steps = (int) Math.round(distance);
         if (steps < 1)
         steps = 1;
         } else {
         **/
    }

    private List<GL3Actor> tracingActors = new ArrayList<>();

    private NeuronMPRenderer setUpActors() {
        // TODO - refactor all stages to use multipass renderer, like this
        NeuronMPRenderer neuronMPRenderer0 = new NeuronMPRenderer(
                sceneWindow.getGLAutoDrawable(),
                // brightnessModel,
                imageColorModel);
        List<MultipassRenderer> renderers = sceneWindow.getRenderer().getMultipassRenderers();
        renderers.clear();
        renderers.add(neuronMPRenderer0);


        // 3) Neurite model
        tracingActors.clear();
        for (GL3Actor tracingActor : tracingInteractor.createActors(this)) {
            tracingActors.add(tracingActor);
            sceneWindow.getRenderer().addActor(tracingActor);

        }
        if (TmModelManager.getInstance().getCurrentWorkspace()!=null) {
            for (TmNeuronMetadata neuron: NeuronManager.getInstance().getNeuronList()) {
                neuronMPRenderer0.addNeuronActors(neuron);
            }
        }

        // 4) Scale bar
        sceneWindow.getRenderer().addActor(scaleBar);

        // 5) Cross hair
        /* */
        crossHairActor = new CenterCrossHairActor();
        sceneWindow.getRenderer().addActor(crossHairActor);
        /* */

        return neuronMPRenderer0;
    }

    public void autoContrast() {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        Point p = new Point();
        for (int x = 0; x < this.getWidth(); ++x) {
            for (int y = 0; y < this.getHeight(); ++y) {
                p.x = x;
                p.y = y;
                double i = this.getIntensity(p);
                if (i <= 0) {
                    continue;
                }
                min = (float) Math.min(min, i);
                max = (float) Math.max(max, i);
            }
        }
        // logger.info("Min = "+min+"; Max = "+max);
        if (max == Float.MIN_VALUE) {
            return; // no valid intensities found
        }

        for (int c = 0; c < imageColorModel.getChannelCount(); ++c) {
            ChannelColorModel chan = imageColorModel.getChannel(c);
            chan.setBlackLevel((int) (min));
            chan.setWhiteLevel((int) (max));
        }
        imageColorModel.fireColorModelChanged();
    }

    StaticVolumeBrickSource getVolumeSource() {
        return volumeSource;
    }

    private void setupMouseNavigation() {
        // Set up tracingInteractor BEFORE OrbitPanZoomInteractor,
        // so tracingInteractor has the opportunity to take precedence
        // during dragging.

        // Delegate tracing interaction to customized class
        tracingInteractor = new TracingInteractor(this, getUndoRedoManager());
        tracingInteractor.setMetaWorkspace(TmModelManager.getInstance().getCurrentWorkspace());

        // push listening into HortaMouseEventDispatcher
        final boolean bDispatchMouseEvents = true;

        Component interactorComponent = getMouseableComponent();
        MouseInputListener listener = new TolerantMouseClickListener(tracingInteractor, 5);
        if (!bDispatchMouseEvents) {
            interactorComponent.addMouseListener(listener);
            interactorComponent.addMouseMotionListener(listener);
        }
        interactorComponent.addKeyListener(tracingInteractor);

        // Setup 3D viewer mouse interaction
        worldInteractor = new OrbitPanZoomInteractor(
                sceneWindow.getCamera(),
                sceneWindow.getInnerComponent());

        // TODO: push listening into HortaMouseEventDispatcher
        if (!bDispatchMouseEvents) {
            interactorComponent.addMouseListener(worldInteractor);
            interactorComponent.addMouseMotionListener(worldInteractor);
        }
        interactorComponent.addMouseWheelListener(worldInteractor);

        // 3) Add custom interactions
        MouseInputListener hortaMouseListener = new MouseInputAdapter() {
            // Show/hide crosshair on enter/exit
            @Override
            public void mouseEntered(MouseEvent event) {
                super.mouseEntered(event);
                crossHairActor.setVisible(true);
                redrawNow();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                super.mouseExited(event);
                crossHairActor.setVisible(false);
                redrawNow();
            }

            // Click to center on position
            @Override
            public void mouseClicked(MouseEvent event) {
                // Click to center on position
                if ((event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON1)) {
                    if (System.nanoTime() < (previousClickTime + minClickInterval)) {
                        return;
                    }

                    // Use neuron cursor position, if available, rather than hardware mouse position.
                    Vector3 xyz = worldXyzForScreenXy(event.getPoint());

                    // logger.info(xyz);
                    previousClickTime = System.nanoTime();
                    PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
                    neuronTraceLoader.animateToFocusXyz(xyz, pCam.getVantage(), 150);
                }
            }

            // Hover to show location in status bar
            @Override
            public void mouseMoved(MouseEvent event) {
                super.mouseMoved(event);

                // Print out screen X, Y (pixels)
                StringBuilder msg = new StringBuilder();
                final boolean showWindowCoords = false;
                if (showWindowCoords) {
                    msg.append("Window position (pixels):");
                    msg.append(String.format("[% 4d", event.getX()));
                    msg.append(String.format(", % 4d", event.getY()));
                    msg.append("]");
                }

                reportIntensity(msg, event);

                if (msg.length() > 0) {
                    StatusDisplayer.getDefault().setStatusText(msg.toString(), 1);
                }
            }
        };

        if (!bDispatchMouseEvents) {
            // Allow some slop in mouse position during mouse click to match tracing interactor behavior July 2016 CMB
            TolerantMouseClickListener tolerantMouseClickListener = new TolerantMouseClickListener(hortaMouseListener, 5);
            interactorComponent.addMouseListener(tolerantMouseClickListener);
            interactorComponent.addMouseMotionListener(tolerantMouseClickListener);
        }

        if (bDispatchMouseEvents) {
            HortaMouseEventDispatcher listener0 = new HortaMouseEventDispatcher(tracingInteractor, worldInteractor, hortaMouseListener);
            // Allow some slop in mouse position during mouse click to match tracing interactor behavior July 2016 CMB
            TolerantMouseClickListener tolerantMouseClickListener = new TolerantMouseClickListener(listener0, 5);
            interactorComponent.addMouseListener(tolerantMouseClickListener);
            interactorComponent.addMouseMotionListener(tolerantMouseClickListener);
        }

    }

    private void animateToCameraRotation(Rotation rot, Vantage vantage, int milliseconds) {
        Quaternion startRot = vantage.getRotationInGround().convertRotationToQuaternion();
        Quaternion endRot = rot.convertRotationToQuaternion();
        long startTime = System.nanoTime();
        long totalTime = milliseconds * 1000000;
        final int stepCount = 40;
        boolean didMove = false;
        for (int s = 0; s < stepCount - 1; ++s) {
            // skip frames to match expected time
            float alpha = s / (float) (stepCount - 1);
            double expectedTime = startTime + alpha * totalTime;
            if ((long) expectedTime < System.nanoTime()) {
                continue; // skip this frame
            }
            Quaternion mid = startRot.slerp(endRot, alpha);
            if (vantage.setRotationInGround(new Rotation().setFromQuaternion(mid))) {
                didMove = true;
                vantage.notifyObservers();
                sceneWindow.redrawImmediately();
            }
        }
        // never skip the final frame
        if (vantage.setRotationInGround(rot)) {
            didMove = true;
        }
        if (didMove) {
            vantage.notifyObservers();
            redrawNow();
        }
    }

    private void reportIntensity(StringBuilder msg, MouseEvent event) {
        // TODO: Use neuron cursor position, if available, rather than hardware mouse position.
        Vector3 worldXyz = null;
        double intensity = 0;

        PerspectiveCamera camera = (PerspectiveCamera) sceneWindow.getCamera();
        double relDepthF = getNeuronMPRenderer().depthOffsetForScreenXy(event.getPoint(), camera);
        worldXyz = worldXyzForScreenXy(event.getPoint(), camera, relDepthF);
        intensity = getNeuronMPRenderer().coreIntensityForScreenXy(event.getPoint());
        double volOpacity = getNeuronMPRenderer().volumeOpacityForScreenXy(event.getPoint());

        mouseStageLocation = worldXyz;
        msg.append(String.format("[% 7.1f, % 7.1f, % 7.1f] \u00B5m",
                worldXyz.get(0), worldXyz.get(1), worldXyz.get(2)));
        if (intensity != -1) {
            msg.append(String.format(";  Intensity: %d", (int) intensity));
            msg.append(String.format(";  Max Opacity: %4.2f", (float) volOpacity));
        }
    }

    /**
     * TODO this could be a member of PerspectiveCamera
     *
     * @param xy in window pixels, as reported by MouseEvent.getPoint()
     * @param camera
     * @param depthOffset in scene units (NOT PIXELS)
     * @return
     */
    private Vector3 worldXyzForScreenXy(Point2D xy, PerspectiveCamera camera, double depthOffset) {
        // Camera frame coordinates
        float screenResolution = camera.getVantage().getSceneUnitsPerViewportHeight() / (float) camera.getViewport().getHeightPixels();
        float cx = 2.0f * ((float) xy.getX() / (float) camera.getViewport().getWidthPixels() - 0.5f);
        cx *= screenResolution * 0.5f * camera.getViewport().getWidthPixels();
        float cy = -2.0f * ((float) xy.getY() / (float) camera.getViewport().getHeightPixels() - 0.5f);
        cy *= screenResolution * 0.5f * camera.getViewport().getHeightPixels();

        // TODO Adjust cx, cy for foreshortening
        float screenDepth = camera.getCameraFocusDistance();
        double itemDepth = screenDepth + depthOffset;
        double foreshortening = itemDepth / screenDepth;
        cx *= foreshortening;
        cy *= foreshortening;

        double cz = -itemDepth;
        Matrix4 modelViewMatrix = camera.getViewMatrix();
        Matrix4 camera_X_world = modelViewMatrix.inverse(); // TODO - cache this invers
        Vector4 worldXyz = camera_X_world.multiply(new Vector4(cx, cy, (float) cz, 1));
        return new Vector3(worldXyz.get(0), worldXyz.get(1), worldXyz.get(2));
    }

    private ImageColorModel imageColorModel = new ImageColorModel(65535, 3);

    public void loadStartupPreferences() {
        Preferences prefs = NbPreferences.forModule(getClass());

        // Load camera state
        Vantage vantage = sceneWindow.getVantage();
        vantage.setConstrainedToUpDirection(prefs.getBoolean("dorsalIsUp", vantage.isConstrainedToUpDirection()));

        ViewEvent event = new ViewEvent(this,
                prefs.getFloat("focusX", vantage.getFocus()[0]),
                prefs.getFloat("focusY", vantage.getFocus()[1]),
                prefs.getFloat("focusZ", vantage.getFocus()[2]),
                prefs.getFloat("zoom", vantage.getSceneUnitsPerViewportHeight()),
                null, false);
        setSampleLocation(event);
        Viewport viewport = sceneWindow.getCamera().getViewport();
        viewport.setzNearRelative(prefs.getFloat("slabNear", viewport.getzNearRelative()));
        viewport.setzFarRelative(prefs.getFloat("slabFar", viewport.getzFarRelative()));

        volumeState.projectionMode =
                prefs.getInt("startupProjectionMode", volumeState.projectionMode);
        volumeState.filteringOrder =
                prefs.getInt("startupRenderFilter", volumeState.filteringOrder);
        setCubifyVoxels(prefs.getBoolean("bCubifyVoxels", doCubifyVoxels));
        volumeCache.setUpdateCache(
                prefs.getBoolean("bCacheHortaTiles", doesUpdateVolumeCache()));
        setPreferKtx(prefs.getBoolean("bPreferKtxTiles", isPreferKtx()));
    }

    public void initMeshes() {
        List<TmObjectMesh> meshActorList = TmModelManager.getInstance().getCurrentWorkspace().getObjectMeshList();
        if (meshActorList==null)
            return;
        HashMap<String, TmObjectMesh> meshMap = new HashMap<>();
        for (TmObjectMesh meshActor : meshActorList) {
            meshMap.put(meshActor.getName(), meshActor);
        }
        for (MeshActor meshActor : getMeshActors()) {
            meshMap.remove(meshActor.getMeshName());
        }
        for (TmObjectMesh mesh : meshMap.values()) {
            MeshGeometry meshGeometry;
            try {
                // when users share workspaces, sometimes object meshes 
                //  can't be loaded by everyone who sees the workspace;
                //  that's ok, but log it
                if (!Paths.get(mesh.getPathToObjFile()).toFile().exists()) {
                    throw new RuntimeException("unable to load mesh " + mesh.getName());
                }
                meshGeometry = WavefrontObjLoader.load(Files.newInputStream(Paths.get(mesh.getPathToObjFile())));
                TransparentEnvelope material = new TransparentEnvelope();
                Color color = meshGeometry.getDefaultColor();
                if (color != null) {
                    material.setDiffuseColor(color);
                }
                final MeshActor meshActor = new MeshActor(
                        meshGeometry,
                        material,
                        null
                );
                meshActor.setMeshName(mesh.getName());
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        addMeshActor(meshActor);
                    }
                });
            } catch (Exception ex) {
                FrameworkAccess.handleException("Failed to load mesh actors. Please check the scene editor for path information",
                        ex);
            }
        }
    }

    private void saveStartupPreferences() {
        Preferences prefs = NbPreferences.forModule(getClass());

        // Save brightness settings and visibility for each channel
        for (int cix = 0; cix < imageColorModel.getChannelCount(); ++cix) {
            ChannelColorModel c = imageColorModel.getChannel(cix);
            prefs.putFloat("startupMinIntensityChan" + cix, c.getNormalizedMinimum());
            prefs.putFloat("startupMaxIntensityChan" + cix, c.getNormalizedMaximum());
            prefs.putBoolean("startupVisibilityChan" + cix, c.isVisible());
            prefs.putInt("startupRedChan" + cix, c.getColor().getRed());
            prefs.putInt("startupGreenChan" + cix, c.getColor().getGreen());
            prefs.putInt("startupBlueChan" + cix, c.getColor().getBlue());
        }
        // Save channel unmixing parameters
        float[] unmix = TetVolumeActor.getInstance().getUnmixingParams();
        for (int i = 0; i < unmix.length; ++i) {
            prefs.putFloat("startupUnmixingParameter" + i, unmix[i]);
        }
        // Save camera state
        Vantage vantage = sceneWindow.getVantage();
        prefs.putBoolean("dorsalIsUp", vantage.isConstrainedToUpDirection());
        prefs.putFloat("zoom", vantage.getSceneUnitsPerViewportHeight());
        prefs.putFloat("focusX", vantage.getFocus()[0]);
        prefs.putFloat("focusY", vantage.getFocus()[1]);
        prefs.putFloat("focusZ", vantage.getFocus()[2]);
        Viewport viewport = sceneWindow.getCamera().getViewport();
        prefs.putFloat("slabNear", viewport.getzNearRelative());
        prefs.putFloat("slabFar", viewport.getzFarRelative());
        // 
        prefs.putInt("startupProjectionMode", volumeState.projectionMode);
        prefs.putInt("startupRenderFilter", volumeState.filteringOrder);
        prefs.putBoolean("bCubifyVoxels", doCubifyVoxels);
        prefs.putBoolean("bCacheHortaTiles", doesUpdateVolumeCache());
        prefs.putBoolean("bPreferKtxTiles", isPreferKtx());
    }

    private void initialize3DViewer() {
        // Insert 3D viewer component
        Vantage vantage = new Vantage(null);
        vantage.setUpInWorld(new Vector3(0, 0, -1));
        vantage.setConstrainedToUpDirection(true);
        // vantage.setSceneUnitsPerViewportHeight(100); // TODO - resize to fit object

        // We want camera change events to register in volume Viewer BEFORE
        // they do in SceneWindow. So Create volume viewer first.
        vantage.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                getNeuronMPRenderer().setIntensityBufferDirty();
                getNeuronMPRenderer().setOpaqueBufferDirty();
            }
        });

        TetVolumeActor.getInstance().setHortaVantage(vantage);

        // Set default colors to mouse light standard...
        imageColorModel.getChannel(0).setColor(Color.green);
        imageColorModel.getChannel(1).setColor(Color.magenta);
        imageColorModel.getChannel(2).setColor(new Color(0f, 0.5f, 1.0f)); // unmixed channel in Economo blue
        imageColorModel.addColorModelListener(new ColorModelListener() {
            @Override
            public void colorModelChanged() {
                getNeuronMPRenderer().setIntensityBufferDirty();
                redrawNow();
            }
        });

        // add TetVolumeActor as listener for ImageColorModel changes from SliderPanel events
        imageColorModel.addUnmixingParameterListener(new UnmixingListener() {
            @Override
            public void unmixingParametersChanged(float[] unmixingParams) {
                TetVolumeActor.getInstance().setUnmixingParams(unmixingParams);
            }

        });

        this.setLayout(new BorderLayout());
        sceneWindow = new SceneWindow(vantage, CameraType.PERSPECTIVE);

        // associateLookup(Lookups.singleton(vantage)); // ONE item in lookup
        // associateLookup(Lookups.fixed(vantage, brightnessModel)); // TWO items in lookup
        FrameTracker frameTracker = sceneWindow.getRenderer().getFrameTracker();

        // reduce near clipping of volume block surfaces
        Viewport vp = sceneWindow.getCamera().getViewport();
        vp.setzNearRelative(0.93f);
        vp.setzFarRelative(1.07f);
        vp.getChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                Viewport vp = sceneWindow.getCamera().getViewport();
                // logger.info("zNearRelative = " + vp.getzNearRelative());
                // TODO: should that be updateRelativeSlabThickness?
                getNeuronMPRenderer().setRelativeSlabThickness(vp.getzNearRelative(), vp.getzFarRelative());
                redrawNow();
            }
        });

        associateLookup(Lookups.fixed(
                vantage,
                vp));

        sceneWindow.setBackgroundColor(Color.DARK_GRAY);

        this.add(sceneWindow.getOuterComponent(), BorderLayout.CENTER);
    }

    public void loadDroppedYaml(String sourceName, InputStream yamlStream) throws IOException {
        setVolumeSource(new LocalVolumeBrickSource(URI.create(sourceName), yamlStream, leverageCompressedFiles, v -> {}));
        neuronTraceLoader.loadTileAtCurrentFocus(volumeSource);
    }

    private void setupDragAndDropYml() {
        final DroppedFileHandler droppedFileHandler = new DroppedFileHandler();
        droppedFileHandler.addLoader(new GZIPFileLoader());
        droppedFileHandler.addLoader(new LZ4FileLoader());
        droppedFileHandler.addLoader(new TarFileLoader());
        droppedFileHandler.addLoader(new TgzFileLoader());
        droppedFileHandler.addLoader(new TilebaseYamlLoader(this));
        droppedFileHandler.addLoader(new ObjMeshLoader(this));
        droppedFileHandler.addLoader(new HortaKtxLoader(this.getNeuronMPRenderer()));

        // Allow user to drop tilebase.cache.yml on this window
        setDropTarget(new DropTarget(this, new DropTargetListener() {

            boolean isDropSourceGood(DropTargetDropEvent event) {
                return event.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            boolean isDropSourceGood(DropTargetDragEvent event) {
                return event.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                if (!isDropSourceGood(dtde)) {
                    dtde.rejectDrop();
                    return;
                }
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                Transferable t = dtde.getTransferable();

                try {
                    List<File> fileList = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                    // Drop could be YAML and/or SWC
                    for (File f : fileList) {
                        droppedFileHandler.handleFile(f);
                    }

                } catch (UnsupportedFlavorException | IOException ex) {
                    logger.warn("Error loading dragged file", ex);
                    JOptionPane.showMessageDialog(NeuronTracerTopComponent.this, "Error loading dragged file");
                }

            }
        }));
    }

    private void setupContextMenu(Component innerComponent) {
        // Context menu for window - at first just to see if it works with OpenGL
        // (A: YES, if applied to the inner component)
        innerComponent.addMouseListener(new MouseUtils.PopupMouseAdapter() {
            private JPopupMenu createMenu(Point popupMenuScreenPoint) {
                JPopupMenu topMenu = new JPopupMenu();

                Vector3 mouseXyz = worldXyzForScreenXy(popupMenuScreenPoint);
                Vector3 focusXyz = sceneWindow.getVantage().getFocusPosition();
                final HortaMenuContext menuContext = new HortaMenuContext(
                        topMenu,
                        popupMenuScreenPoint,
                        mouseXyz,
                        focusXyz,
                        getNeuronMPRenderer(),
                        sceneWindow
                );

                // Setting popup menu title here instead of in JPopupMenu constructor,
                // because title from constructor is not shown in default look and feel.
                topMenu.add("Options:").setEnabled(false); // TODO should I place title in constructor?

                // SECTION: View options
                AbstractAction syncViewsAction = new AbstractAction("Synchronize Views At This Location") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // send out a view event to synchronize
                        Vantage vantage = sceneWindow.getVantage();
                        Matrix m2v = TmModelManager.getInstance().getMicronToVoxMatrix();
                        Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                                {vantage.getFocus()[0],},
                                {vantage.getFocus()[1],},
                                {vantage.getFocus()[2],},
                                {1.0,},});
                        // NeuronVertex API requires coordinates in micrometers
                        Jama.Matrix voxLoc = m2v.times(micLoc);
                        Vec3 voxelXyz = new Vec3(
                                (float) voxLoc.get(0, 0),
                                (float) voxLoc.get(1, 0),
                                (float) voxLoc.get(2, 0));
                        TmViewState currView = TmModelManager.getInstance().getCurrentView();
                        currView.setCameraFocusX(voxelXyz.getX());
                        currView.setCameraFocusY(voxelXyz.getY());
                        currView.setCameraFocusZ(voxelXyz.getZ());
                        currView.setZoomLevel(vantage.getSceneUnitsPerViewportHeight());
                        ViewEvent syncViewEvent = new ViewEvent(this,vantage.getFocus()[0],
                                vantage.getFocus()[1],vantage.getFocus()[2],
                                vantage.getSceneUnitsPerViewportHeight(),
                                null, false);
                        ViewerEventBus.postEvent(syncViewEvent);
                    }
                };
                topMenu.add(syncViewsAction);

                AbstractAction copyLocToClipboard = new AbstractAction("Copy Micron Location To Clipboard") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // send out a view event to synchronize
                        Vantage vantage = sceneWindow.getVantage();
                        Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                                {vantage.getFocus()[0],},
                                {vantage.getFocus()[1],},
                                {vantage.getFocus()[2],},
                                {1.0,},});
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        String vantageStr = "[" + vantage.getFocus()[0] +"," +
                                vantage.getFocus()[1] + "," + vantage.getFocus()[2] + "]";
                        StringSelection selection = new StringSelection(vantageStr);
                        clipboard.setContents(selection, selection);
                    }
                };
                topMenu.add(copyLocToClipboard);
                topMenu.add(new JPopupMenu.Separator());


                Action resetRotationAction =
                        new ResetHortaRotationAction(NeuronTracerTopComponent.this);

                // Annotators want "Reset Rotation" on the top level menu
                // Issue JW-25370
                topMenu.add(resetRotationAction);

                JMenu snapMenu = new JMenu("Snap To Signal Radius");
                topMenu.add(snapMenu);

                JPanel snapPanel = new JPanel(new FlowLayout());

                JTextField snapSelectionText = new JTextField();
                snapSelectionText.setText(Integer.toString(tracingInteractor.getSnapRadius()));
                snapSelectionText.setEnabled(false);

                JSlider snapSelectionSlider = new JSlider(JSlider.HORIZONTAL);
                snapSelectionSlider.setMinimum(1);
                snapSelectionSlider.setMaximum(20);
                snapSelectionSlider.setValue(tracingInteractor.getSnapRadius());
                ChangeListener cl = e -> {
                    JSlider x = (JSlider) e.getSource();
                    tracingInteractor.setSnapRadius(x.getValue());
                    snapSelectionText.setText(Integer.toString(x.getValue()));
                };
                snapSelectionSlider.addChangeListener(cl);
                snapPanel.add(snapSelectionSlider);
                snapPanel.add(snapSelectionText);

                snapMenu.add(snapPanel);

                JMenu viewMenu = new JMenu("View");
                topMenu.add(viewMenu);

                if (mouseStageLocation != null) {
                    // Recenter
                    viewMenu.add(new AbstractAction("Recenter on This 3D Position [left-click]") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
                            neuronTraceLoader.animateToFocusXyz(mouseStageLocation, pCam.getVantage(), 150);
                        }
                    });
                }

                viewMenu.add(resetRotationAction);

                // menu.add(new JPopupMenu.Separator());
                viewMenu.add(new AbstractAction("Auto Contrast") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        autoContrast();
                    }
                });

                ktxBlockMenuBuilder.populateMenus(menuContext);

                if (currentSource != null) {
                    JCheckBoxMenuItem enableVolumeCacheMenu = new JCheckBoxMenuItem(
                            "Auto-load Image Tiles", doesUpdateVolumeCache());
                    topMenu.add(enableVolumeCacheMenu);
                    enableVolumeCacheMenu.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
                            volumeCache.toggleUpdateCache();
                            item.setSelected(doesUpdateVolumeCache());
                            TetVolumeActor.getInstance().setAutoUpdate(doesUpdateVolumeCache());
                        }
                    });
                }

                if (volumeState != null) {
                    JMenu projectionMenu = new JMenu("Projection");

                    viewMenu.add(projectionMenu);

                    projectionMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Maximum Intensity") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    volumeState.projectionMode == 0);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.projectionMode = 0;
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            redrawNow();
                        }
                    }));

                    projectionMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Occluding") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    volumeState.projectionMode == 1);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.projectionMode = 1;
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            redrawNow();
                        }
                    }));

                    JMenu filterMenu = new JMenu("Rendering Filter");
                    viewMenu.add(filterMenu);

                    filterMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Nearest-neighbor (Discrete Voxels)") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    volumeState.filteringOrder == 0);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.filteringOrder = 0;
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            redrawNow();
                        }
                    }));

                    filterMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Trilinear") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    volumeState.filteringOrder == 1);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.filteringOrder = 1;
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            redrawNow();
                        }
                    }));

                    filterMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Tricubic (Slow & Smooth)") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    volumeState.filteringOrder == 3);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.filteringOrder = 3;
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            redrawNow();
                        }
                    }));

                    JMenu strategyMenu = new JMenu("Block Loading Strategy");
                    viewMenu.add(strategyMenu);
                    strategyMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Highest resolution only") {
                                {
                                    putValue(Action.SELECTED_KEY,
                                            volumeState.blockStrategy == 0);
                                }

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    volumeState.blockStrategy = 0;
                                    TetVolumeActor.getInstance().changeStrategy(VolumeMipMaterial.VolumeState.BLOCK_STRATEGY_FINEST_8_MAX);
                                    getNeuronMPRenderer().setIntensityBufferDirty();
                                    redrawNow();
                                }
                            }));

                    strategyMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Multi-resolution octree") {
                                {
                                    putValue(Action.SELECTED_KEY,
                                            volumeState.blockStrategy == 1);
                                }

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    volumeState.blockStrategy = 1;
                                    TetVolumeActor.getInstance().changeStrategy(VolumeMipMaterial.VolumeState.BLOCK_STRATEGY_OCTTREE);
                                    getNeuronMPRenderer().setIntensityBufferDirty();
                                    redrawNow();
                                }
                            }));
                }

                if (sceneWindow != null) {
                    JMenu stereoMenu = new JMenu("Stereo3D");
                    viewMenu.add(stereoMenu);

                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Monoscopic (Not 3D)") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    sceneWindow.getRenderer().getStereo3dMode()
                                    == SceneRenderer.Stereo3dMode.MONO);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.MONO);
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            getNeuronMPRenderer().setOpaqueBufferDirty();
                            redrawNow();
                        }
                    }));

                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Left Eye View") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    sceneWindow.getRenderer().getStereo3dMode()
                                    == SceneRenderer.Stereo3dMode.LEFT);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.LEFT);
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            getNeuronMPRenderer().setOpaqueBufferDirty();
                            redrawNow();
                        }
                    }));

                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Right Eye View") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    sceneWindow.getRenderer().getStereo3dMode()
                                    == SceneRenderer.Stereo3dMode.RIGHT);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.RIGHT);
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            getNeuronMPRenderer().setOpaqueBufferDirty();
                            redrawNow();
                        }
                    }));

                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Red/Cyan Anaglyph") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    sceneWindow.getRenderer().getStereo3dMode()
                                    == SceneRenderer.Stereo3dMode.RED_CYAN);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.RED_CYAN);
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            getNeuronMPRenderer().setOpaqueBufferDirty();
                            redrawNow();
                        }
                    }));

                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Green/Magenta Anaglyph") {
                        {
                            putValue(Action.SELECTED_KEY,
                                    sceneWindow.getRenderer().getStereo3dMode()
                                    == SceneRenderer.Stereo3dMode.GREEN_MAGENTA);
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.GREEN_MAGENTA);
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            getNeuronMPRenderer().setOpaqueBufferDirty();
                            redrawNow();
                        }
                    }));

                }

                JCheckBoxMenuItem cubeDistortMenu = new JCheckBoxMenuItem("Compress Voxels in Z", doCubifyVoxels);
                viewMenu.add(cubeDistortMenu);
                cubeDistortMenu.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
                        if (doCubifyVoxels) {
                            setCubifyVoxels(false);
                        } else {
                            setCubifyVoxels(true);
                        }
                        item.setSelected(doCubifyVoxels);
                    }
                });

                // Unmixing menu
                if (TetVolumeActor.getInstance().getBlockCount() > 0) {
                    JMenu unmixMenu = new JMenu("Tracing Channel");

                    unmixMenu.add(new JMenuItem(
                            new AbstractAction("Unmix Channel 1 Using Current Brightness") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TetVolumeActor.getInstance().unmixChannelOne();
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            imageColorModel.setUnmixParameters(TetVolumeActor.getInstance().getUnmixingParams());
                            ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                                     imageColorModel);
                            ViewerEventBus.postEvent(modelEvent);
                            redrawNow();
                        }
                    }));

                    unmixMenu.add(new JMenuItem(
                            new AbstractAction("Unmix Channel 2 Using Current Brightness") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TetVolumeActor.getInstance().unmixChannelTwo();
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            imageColorModel.setUnmixParameters(TetVolumeActor.getInstance().getUnmixingParams());
                            ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                                     imageColorModel);
                            ViewerEventBus.postEvent(modelEvent);
                            redrawNow();
                        }
                    }));

                    unmixMenu.add(new JMenuItem(
                            new AbstractAction("Average Channels 1 and 2") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TetVolumeActor.getInstance().traceChannelOneTwoAverage();
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            imageColorModel.setUnmixParameters(TetVolumeActor.getInstance().getUnmixingParams());
                            ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                                     imageColorModel);
                            ViewerEventBus.postEvent(modelEvent);
                            redrawNow();
                        }
                    }));

                    unmixMenu.add(new JMenuItem(
                            new AbstractAction("Raw Channel 1") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TetVolumeActor.getInstance().traceChannelOneRaw();
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            imageColorModel.setUnmixParameters(TetVolumeActor.getInstance().getUnmixingParams());
                            ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                                     imageColorModel);
                            ViewerEventBus.postEvent(modelEvent);
                            redrawNow();
                        }
                    }));

                    unmixMenu.add(new JMenuItem(
                            new AbstractAction("Raw Channel 2") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TetVolumeActor.getInstance().traceChannelTwoRaw();
                            getNeuronMPRenderer().setIntensityBufferDirty();
                            imageColorModel.setUnmixParameters(TetVolumeActor.getInstance().getUnmixingParams());
                            ColorModelUpdateEvent modelEvent = new ColorModelUpdateEvent(this,
                                     imageColorModel);
                            ViewerEventBus.postEvent(modelEvent);
                            redrawNow();
                        }
                    }));
                    topMenu.add(unmixMenu);

                }

                viewMenu.add(new AbstractAction("Save Screen Shot...") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        BufferedImage image = getScreenShot();
                        if (image == null) {
                            return;
                        }
                        FileDialog chooser = new FileDialog((Frame) null,
                                "Save Neuron Tracer Image",
                                FileDialog.SAVE);
                        chooser.setFile("*.png");
                        chooser.setVisible(true);
                        if (chooser.getFile() == null) {
                            return;
                        }
                        if (chooser.getFile().isEmpty()) {
                            return;
                        }
                        File outputFile = new File(chooser.getDirectory(), chooser.getFile());
                        try {
                            ImageIO.write(image, "png", outputFile);
                        } catch (IOException ex) {
                            throw new RuntimeException("Error saving screenshot", ex);
                        }
                    }
                });

                // I could not figure out how to save the settings every time the application closes,
                // so make the user save the settings on demand.
                viewMenu.add(new AbstractAction("Save Viewer Settings") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        saveStartupPreferences();
                    }
                });

                final TracingInteractor.InteractorContext interactorContext = tracingInteractor.createContext();

                // SECTION: Anchors
                if ((interactorContext.getCurrentParentAnchor() != null) || (interactorContext.getHighlightedAnchor() != null)) {
                    topMenu.add(new JPopupMenu.Separator());
                    topMenu.add("Anchor").setEnabled(false);

                    String neuronName = null;
                    if (interactorContext.getHighlightedAnchor()!=null) {
                        neuronName = NeuronManager.getInstance().getNeuronFromNeuronID(
                                interactorContext.getHighlightedAnchor().getNeuronId()).getName();
                    } else if (interactorContext.getCurrentParentAnchor()!=null) {
                        neuronName = NeuronManager.getInstance().getNeuronFromNeuronID(
                                interactorContext.getCurrentParentAnchor().getNeuronId()).getName();
                    }
                    if (neuronName!=null)
                        topMenu.add(new JMenuItem(new CopyToClipboardAction("Name",neuronName)));

                    if (interactorContext.canUpdateAnchorRadius()) {
                        topMenu.add(new AbstractAction("Adjust Anchor Radius") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                interactorContext.updateAnchorRadius();
                            }
                        });
                    }

                    if (interactorContext.canClearParent()) {
                        topMenu.add(new AbstractAction("Clear Current Parent Anchor") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                interactorContext.clearParent();
                            }
                        });
                    }

                    topMenu.add(new AbstractAction("Edit Neuron Groups") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NeuronGroupsDialog ngDialog = new NeuronGroupsDialog();
                            ngDialog.showDialog();
                        }
                    });

                    if (interactorContext.getHighlightedAnchor() != null) {
                        TmGeoAnnotation vertex = interactorContext.getHighlightedAnchor();

                        topMenu.add(new AbstractAction("Center on Current Anchor") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
                                float[] vtxLocation = TmModelManager.getInstance().getLocationInMicrometers(vertex.getX(),
                                        vertex.getY(), vertex.getZ());
                                ViewEvent event = new ViewEvent(this,vtxLocation[0],
                                        vtxLocation[1],
                                        vtxLocation[2],
                                        300,
                                        null,
                                        false);
                                setSampleLocation(event);
                            }
                        });

                        topMenu.add(new AbstractAction("Add/Edit Note on Anchor") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                AddEditNoteAction action = new AddEditNoteAction();
                                action.execute(vertex.getNeuronId(), vertex.getId());
                            }
                        });

                        topMenu.add(new AbstractAction("Edit Neuron Tags") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(vertex.getNeuronId());
                                NeuronManager.getInstance().editNeuronTags(neuron);
                            }
                        });

                        topMenu.add(new AbstractAction("Delete Neuron Subtree") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                DeleteNeuronSubtreeAction action = new DeleteNeuronSubtreeAction();
                                action.execute(vertex.getNeuronId(), vertex.getId());
                            }
                        });

                        if (!vertex.isRoot() || vertex.getChildIds().size() == 0) {
                            topMenu.add(new AbstractAction("Delete Vertex") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    DeleteVertexLinkAction action = new DeleteVertexLinkAction();
                                    action.execute(vertex.getNeuronId(), vertex.getId());
                                }
                            });
                        }

                        if (!vertex.isRoot()) {
                            topMenu.add(new AbstractAction("Set Vertex as Neuron Root") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    RerootNeuronAction action = new RerootNeuronAction();
                                    action.execute(vertex.getNeuronId(), vertex.getId());
                                }
                            });

                            topMenu.add(new AbstractAction("Split Neuron Edge Between Vertices") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    SplitNeuronBetweenVerticesAction action = new SplitNeuronBetweenVerticesAction();
                                    action.execute(vertex.getNeuronId(), vertex.getId());
                                }
                            });

                            topMenu.add(new AbstractAction("Split Neurite At Vertex") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    SplitNeuronAtVertexAction action = new SplitNeuronAtVertexAction();
                                    action.execute(vertex.getNeuronId(), vertex.getId());
                                }
                            });
                        }
                        topMenu.add(new AbstractAction("Transfer Neurite") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                TransferNeuriteAction action = new TransferNeuriteAction();
                                action.execute(vertex.getNeuronId(), vertex.getId());
                            }
                        });
                    }
                }

                // SECTION: Neuron edits
                TmNeuronMetadata indicatedNeuron = interactorContext.getHighlightedNeuron();
                if (indicatedNeuron != null) {
                    topMenu.add(new JPopupMenu.Separator());
                    topMenu.add("Neuron '"
                            + indicatedNeuron.getName()
                            + "':").setEnabled(false);

                    AbstractAction hideNeuronAction = new AbstractAction("Hide Neuron") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(indicatedNeuron.getId());
                            NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(
                                    this, Arrays.asList(new TmNeuronMetadata[]{indicatedNeuron}));
                            ViewerEventBus.postEvent(updateEvent);
                        }
                    };
                    topMenu.add(hideNeuronAction);

                    AbstractAction toggleVisAction = new AbstractAction("Set neuron radius...") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NeuronSetRadiusAction setRadiusAction = new NeuronSetRadiusAction();
                            setRadiusAction.execute(indicatedNeuron.getId());
                        }
                    };
                    topMenu.add(toggleVisAction);

                    // Toggle Visiblity (maybe we could only hide from here though...)
                    AbstractAction clearLoopIndicator = new AbstractAction("Clear Loop Indicator") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            TmModelManager.getInstance().getCurrentReviews().clearLoopedAnnotations();

                            NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(this,
                                    Arrays.asList(indicatedNeuron));
                            ViewerEventBus.postEvent(updateEvent);
                        }
                    };
                    topMenu.add(clearLoopIndicator);

                    JMenu selectLoopColorMenu = new JMenu("Set Loop Indicator Color");
                    topMenu.add(selectLoopColorMenu);

                    JPanel selectLoopColorPanel = new JPanel(new FlowLayout());

                    ColorSwatch swatch = new ColorSwatch();
                    Color currLoopColor = TmModelManager.getInstance().getCurrentView().getColorLoopIndicator();
                    swatch.setColor(currLoopColor);
                    selectLoopColorPanel.add(swatch);
                    swatch.addMouseListener(new MouseInputAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent event) {
                            NeuronColorDialog dialog = new NeuronColorDialog(
                                    null,
                                    currLoopColor);
                            dialog.setVisible(true);
                            if (dialog.styleChosen()) {
                                TmModelManager.getInstance().getCurrentView().setColorLoopIndicator(dialog.getChosenColor());
                            }
                        }

                    });
                    selectLoopColorMenu.add(selectLoopColorPanel);


                    // Change Neuron Color
                    if (interactorContext.canRecolorNeuron()) {
                        topMenu.add(new AbstractAction("Change Neuron Color...") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                interactorContext.recolorNeuron();
                            }
                        });
                    }

                    // Change Neuron Name
                    if (!TmModelManager.getInstance().getCurrentView().isProjectReadOnly()) {
                        topMenu.add(new NeuronRenameAction(indicatedNeuron));
                    }

                    if (interactorContext.canMergeNeurite()) {
                        topMenu.add(new AbstractAction("Merge neurites...") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                interactorContext.mergeNeurites();
                            }
                        });
                    }

                    // Delete Neuron DANGER!
                    if (interactorContext.canDeleteNeuron()) {
                        // Extra separator due to danger...
                        topMenu.add(new JPopupMenu.Separator());
                        topMenu.add(new NeuronDeleteAction(indicatedNeuron));
                    }
                }

                // Cancel/do nothing action
                topMenu.add(new JPopupMenu.Separator());
                topMenu.add(new AbstractAction("Close This Menu [ESC]") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                });

                return topMenu;
            }

            @Override
            protected void showPopup(MouseEvent event) {
                if (!NeuronTracerTopComponent.this.isShowing()) {
                    return;
                }
                createMenu(event.getPoint()).show(NeuronTracerTopComponent.this, event.getPoint().x, event.getPoint().y);
            }
        });
    }

    RenderedVolumeLoader getRenderedVolumeLoader() {
        return renderedVolumeLoader;
    }

    void setUpdateVolumeCache(boolean doUpdate) {
        volumeCache.setUpdateCache(doUpdate);
    }

    boolean doesUpdateVolumeCache() {
        return volumeCache.isUpdateCache();
    }

    GL3Actor createBrickActor(BrainTileInfo brainTile, int colorChannel) throws IOException {
        return new BrickActor(brainTile, imageColorModel, volumeState, colorChannel);
    }

    double[] getStageLocation() {
        if (mouseStageLocation == null) {
            return null;
        } else {
            return new double[]{
                mouseStageLocation.getX(), mouseStageLocation.getY(), mouseStageLocation.getZ()
            };
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private final void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>                        

    // Variables declaration - do not modify                     
    // End of variables declaration                   

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    // VolumeProjection implementation below:
    @Override
    public Component getMouseableComponent() {
        return sceneWindow.getInnerComponent();
    }

    @Override
    public double getIntensity(Point2D xy) {
        return getNeuronMPRenderer().coreIntensityForScreenXy(xy);
    }

    @Override
    public Vector3 worldXyzForScreenXy(Point2D xy) {
        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        double depthOffset = getNeuronMPRenderer().depthOffsetForScreenXy(xy, pCam);
        return worldXyzForScreenXy(xy, pCam, depthOffset);
    }

    @Override
    public Vector3 worldXyzForScreenXyInPlane(Point2D xy) {
        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        double depthOffset = 0.0;
        Vector3 xyz = worldXyzForScreenXy(
                xy,
                pCam,
                depthOffset);
        return xyz;
    }

    @Override
    public float getPixelsPerSceneUnit() {
        Vantage vantage = sceneWindow.getVantage();
        Viewport viewport = sceneWindow.getCamera().getViewport();
        return viewport.getHeightPixels() / vantage.getSceneUnitsPerViewportHeight();
    }

    private boolean setCubifyVoxels(boolean cubify) {
        if (cubify == doCubifyVoxels)
            return false; // no change
        doCubifyVoxels = cubify;
        // TODO - actually cubify
        Vantage v = sceneWindow.getVantage();
        if (doCubifyVoxels) {
            v.setWorldScaleHack(1, 1, 0.4f);
            // logger.info("distort");
        } else {
            v.setWorldScaleHack(1, 1, 1);
            // logger.info("undistort");
        }
        v.notifyObservers();
        redrawNow();

        return true;
    }

    // Create background gradient using a single base color
    private void setBackgroundColor(Color c) {
        // Update background color
        float[] cf = c.getColorComponents(new float[3]);
        // Convert sRGB to linear RGB
        for (int i = 0; i < 3; ++i)
            cf[i] = cf[i] * cf[i]; // second power is close enough...
        // Create color gradient from single color
        double deltaLuma = 0.05; // desired intensity change
        double midLuma = 0.30 * cf[0] + 0.59 * cf[1] + 0.11 * cf[2];
        double topLuma = midLuma - 0.5 * deltaLuma;
        double bottomLuma = midLuma + 0.5 * deltaLuma;
        if (bottomLuma > 1.0) { // user wants it REALLY light
            bottomLuma = 1.0; // white
            topLuma = midLuma; // whatever color user said
        }
        if (topLuma < 0.0) { // user wants it REALLY dark
            topLuma = 0.0; // black
            bottomLuma = midLuma; // whatever color user said
        }
        Color topColor = c;
        Color bottomColor = c;
        if (midLuma > 0) {
            float t = (float) (255 * topLuma / midLuma);
            float b = (float) (255 * bottomLuma / midLuma);
            int[] tb = {
                (int) (cf[0] * t), (int) (cf[1] * t), (int) (cf[2] * t),
                (int) (cf[0] * b), (int) (cf[1] * b), (int) (cf[2] * b)
            };
            // Clamp color components to range 0-255
            for (int i = 0; i < 6; ++i) {
                if (tb[i] < 0) tb[i] = 0;
                if (tb[i] > 255) tb[i] = 255;
            }
            topColor = new Color(tb[0], tb[1], tb[2]);
            bottomColor = new Color(tb[3], tb[4], tb[5]);
        }
        setBackgroundColor(topColor, bottomColor);
    }

    private void setBackgroundColor(Color topColor, Color bottomColor) {
        getNeuronMPRenderer().setBackgroundColor(topColor, bottomColor);
        float[] bf = bottomColor.getColorComponents(new float[3]);
        double bottomLuma = 0.30 * bf[0] + 0.59 * bf[1] + 0.11 * bf[2];
        if (bottomLuma > 0.25) { // sRGB luma 0.5 == lRGB luma 0.25...
            scaleBar.setForegroundColor(Color.black);
            scaleBar.setBackgroundColor(new Color(255, 255, 255, 50));
        } else {
            scaleBar.setForegroundColor(Color.white);
            scaleBar.setBackgroundColor(new Color(0, 0, 0, 50));
        }
    }

    // TODO: Use this for redraw needs
    public void redrawNow() {
        if (!isShowing())
            return;
        sceneWindow.getInnerComponent().repaint();
    }

    @Override
    public void componentOpened() {
        hortaManager.onOpened();
    }

    // NOTE: componentClosed() is only called when just the Horta window is closed, not
    // when the whole application closes.
    @Override
    public void componentClosed() {
        hortaManager.onClosed();
        // clear out SWCbuffers; exceptions should not be allowed to
        //  escape, and in the past, they have
        try {
            getNeuronMPRenderer().clearNeuronReconstructions();
        } catch (Exception e) {
            logger.warn("exception suppressed when closing Horta top component", e);
        }

        // fire off notice for checkboxes, etc.
        ViewerCloseEvent viewerCloseEvent = new ViewerCloseEvent(this, ViewerCloseEvent.VIEWER.HORTA);
        ViewerEventBus.postEvent(viewerCloseEvent);
    }

    @Override
    public boolean isNeuronModelAt(Point2D xy) {
        return getNeuronMPRenderer().isNeuronModelAt(xy);
    }

    @Override
    public boolean isVolumeDensityAt(Point2D xy) {
        return getNeuronMPRenderer().isVolumeDensityAt(xy);
    }

    void registerLoneDisplayedTile(BrickActor boxMesh) {
        volumeCache.registerLoneDisplayedTile(boxMesh);
    }

    public void clearAllTiles() {
        // this is a workaround for clearing RAW tiles until we can clean up the controllers for Horta
        setPreferKtx(true);
        TetVolumeActor.getInstance().setAutoUpdate(false);
        reloadSampleLocation();
    }

    // API for use by external HortaMovieSource class
    public List<MeshActor> getMeshActors() {
        return getNeuronMPRenderer().getMeshActors();
    }

    public ObservableInterface getMeshObserver() {
        return getNeuronMPRenderer().getMeshObserver();
    }

    public boolean setVisibleActors(Collection<String> visibleActorNames) {
        List<TmNeuronMetadata> neuronUpdateList = new ArrayList<>();
        // TODO: This is just neurons for now...
        for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
            String n = neuron.getName();
            boolean bIs = visibleActorNames.contains(n);
            if (bIs && TmModelManager.getInstance().getCurrentView().isHidden(neuron.getId())) {
                TmModelManager.getInstance().getCurrentView().removeAnnotationFromHidden(neuron.getId());
                neuronUpdateList.add(neuron);
            }
            if (!bIs)
                TmModelManager.getInstance().getCurrentView().addAnnotationToHidden(neuron.getId());
        }
        NeuronUpdateEvent updateEvent = new NeuronUpdateEvent(this,neuronUpdateList);
        ViewerEventBus.postEvent(updateEvent);

        return false;
    }

    @Subscribe
    public void toggleMeshVisibility(MeshVisibilityEvent event) {
        TmObjectMesh mesh = event.getMesh();
        for (MeshActor meshActor : this.getMeshActors()) {
            if (meshActor.getMeshName().equals(mesh.getName())) {
                meshActor.setIsVisible(!event.isVisible());
                getNeuronMPRenderer().setOpaqueBufferDirty();
                break;
            }
        }
    }

    public void setVisibleMeshes(Collection<String> visibleMeshes) {
        for (MeshActor meshActor : this.getMeshActors()) {
            String meshName = meshActor.getMeshName();
            boolean bWas = meshActor.isVisible();
            boolean bIs = visibleMeshes.contains(meshName);
            if (bWas == bIs)
                continue;
            meshActor.setVisible(bIs);
            getNeuronMPRenderer().setOpaqueBufferDirty();
        }
    }

    public Collection<String> getVisibleActorNames() {
        Collection<String> result = new HashSet<>();

        // TODO: This is just neurons for now...
        for (TmNeuronMetadata neuron : NeuronManager.getInstance().getNeuronList()) {
            if (!TmModelManager.getInstance().getCurrentView().isHidden(neuron.getId()))
                result.add(neuron.getName());
        }

        return result;
    }

    public Collection<String> getVisibleMeshes() {
        Collection<String> result = new HashSet<>();

        // TODO: This is just neurons for now...
        for (MeshActor meshActor : getMeshActors()) {
            if (meshActor.isVisible())
                result.add(meshActor.getMeshName());
        }

        return result;
    }

    public Vantage getVantage() {
        return sceneWindow.getVantage();
    }

    public void redrawImmediately() {
        GLAutoDrawable glad = sceneWindow.getGLAutoDrawable();
        glad.display();
        glad.swapBuffers();
    }

    public BufferedImage getScreenShot() {
        GLAutoDrawable glad = sceneWindow.getGLAutoDrawable();
        glad.getContext().makeCurrent();
        // In Jogl 2.1.3, Screenshot is deprecated, but the non-deprecated method does not work. Idiots.
        // BufferedImage image = Screenshot.readToBufferedImage(glad.getSurfaceWidth(), glad.getSurfaceHeight());
        // In Jogl 2.2.4, this newer screenshot method seems to work OK
        AWTGLReadBufferUtil rbu = new AWTGLReadBufferUtil(glad.getGLProfile(), false);
        BufferedImage image = rbu.readPixelsToBufferedImage(glad.getGL(), true);
        glad.getContext().release();
        return image;
    }

    public void addMeshActor(MeshActor meshActor) {
        getNeuronMPRenderer().addMeshActor(meshActor);
    }

    @Subscribe
    public void deleteMeshActor(MeshDeleteEvent deleteEvent) {
        List<MeshActor> meshActorList = getNeuronMPRenderer().getMeshActors();
        TmObjectMesh mesh = deleteEvent.getMesh();
        MeshActor targetMesh = null;
        for (MeshActor meshActor: meshActorList) {
            if (meshActor.getMeshName().equals(mesh.getName())) {
                targetMesh = meshActor;
            }
        }
        if (targetMesh!=null)
            getNeuronMPRenderer().removeMeshActor(targetMesh);
        redrawNow();
    }

    @Subscribe
    public void meshUpdateEvent(MeshUpdateEvent event) {
        try {
            if (event.getProperty()== MeshUpdateEvent.PROPERTY.NAME) {
                String oldName = event.getOldValue();
                for (MeshActor meshActor : this.getMeshActors()) {
                    String meshName = meshActor.getMeshName();
                    if (meshName.equals(oldName)) {
                        meshActor.setMeshName(event.getMesh().getName());
                        break;
                    }
                }
            }
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }

    KtxOctreeBlockTileSource getKtxSource() {
        return ktxSource;
    }

    void setKtxSource(KtxOctreeBlockTileSource ktxSource) {
        this.ktxSource = ktxSource;
        TetVolumeActor.getInstance().setKtxTileSource(ktxSource);
        // Don't load both ktx and raw tiles at the same time
        if (ktxSource != null) {
            setVolumeSource(null);
        }
    }

    public void loadPersistentTileAtFocus() throws IOException {
        Vector3 focus = getVantage().getFocusPosition();
        loadPersistentTileAtLocation(focus);
    }

    void loadPersistentTileAtLocation(Vector3 location) throws IOException {
        if (ktxSource == null) {
            // this is the case when the action is triggered by a LoadHortaTileAtFocus  and there's no ktx source set yet
            KtxOctreeBlockTileSource source = createKtxSource();
            if (source == null) {
                return;
            }
        }
        neuronTraceLoader.loadKtxTileAtLocation(ktxSource, location, true);
    }

    private KtxOctreeBlockTileSource createKtxSource() {
        TmSample tmSample = TmModelManager.getInstance().getCurrentSample();
        if (tmSample == null) {
            logger.error("Internal Error: No sample has been set for creating a KTX source");
            JOptionPane.showMessageDialog(
                    this,
                    "No sample set for creating a KTX source",
                    "KTX sample source error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        try {
            return new KtxOctreeBlockTileSource(getCurrentSourceURL(), getTileLoader()).init(tmSample);
        } catch (Exception e) {
            logger.warn("Error initializing KTX source for {}", TmModelManager.getInstance().getCurrentSample(), e);
            JOptionPane.showMessageDialog(
                    this,
                    "Error initializing KTX source for sample at " + tmSample.getLargeVolumeOctreeFilepath(),
                    "Error initializing KTX source",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    TileLoader getTileLoader() {
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
            return new CachedTileLoader(
                    new JadeBasedTileLoader(new JadeServiceClient(ConsoleProperties.getString("jadestorage.rest.url"), () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false))),
                    LocalCacheMgr.getInstance().getLocalFileCacheStorage(),
                    CACHE_CONCURRENCY,
                    Executors.newFixedThreadPool(
                            CACHE_CONCURRENCY,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("HortaTileCacheWriter-%d")
                                    .setDaemon(true)
                                    .build())
            );
        } else {
            return new FileBasedTileLoader();
        }
    }


    public void resetRotation() {
        Vantage v = sceneWindow.getVantage();
        animateToCameraRotation(
                v.getDefaultRotation(),
                v, 150);
    }

    boolean isPreferKtx() {
        return ktxBlockMenuBuilder.isPreferKtx();
    }

    public void setPreferKtx(boolean doPreferKtx) {
        ktxBlockMenuBuilder.setPreferKtx(doPreferKtx);
    }

    public NeuronMPRenderer getNeuronMPRenderer() {
        return neuronMPRenderer;
    }

    public void reloadSampleLocation() {
        try {
            ViewLoader viewLoader = new ViewLoader(
                    neuronTraceLoader,this, sceneWindow
            );
            Vantage vantage = sceneWindow.getVantage();
            Vec3 currFocus = new Vec3(vantage.getFocus()[0], vantage.getFocus()[1],
                    vantage.getFocus()[2]);
            double currZoom = vantage.getSceneUnitsPerViewportHeight();

            viewLoader.loadView(currFocus, currZoom);
        } catch (Exception e) {
            FrameworkAccess.handleException("Problems reloading tile strategy.",e);
        }
    }

    @Override
    public void neuronVertexUpdated(VertexWithNeuron vertexWithNeuron) {
        neuronMPRenderer.markAsDirty(vertexWithNeuron.neuron.getId());
        redrawNow();
    }

    @Override
    public void neuronVertexesDeleted(VertexCollectionWithNeuron vertexesWithNeurons) {
        neuronMPRenderer.markAsDirty(vertexesWithNeurons.neuron.getId());
        redrawNow();
    }

    @Override
    public void neuronVertexCreated(VertexWithNeuron vertexWithNeuron) {
        neuronMPRenderer.markAsDirty(vertexWithNeuron.neuron.getId());
        redrawNow();
    }
}
