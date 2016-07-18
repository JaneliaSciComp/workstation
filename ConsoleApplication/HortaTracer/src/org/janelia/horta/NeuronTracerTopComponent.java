/*//GEN-LINE:initComponents
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.horta;

import org.janelia.horta.render.NeuronMPRenderer;
import org.janelia.horta.actors.ScaleBar;
import org.janelia.horta.actors.CenterCrossHairActor;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
// import com.jogamp.opengl.util.awt.TextRenderer;
// import com.jogamp.opengl.util.awt.Screenshot;
import org.janelia.geometry3d.BrightnessModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.text.Keymap;
import org.janelia.console.viewerapi.BasicSampleLocation;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.RelocationMenuBuilder;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.horta.volume.MouseLightYamlBrickSource;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Quaternion;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.geometry3d.Viewport;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MultipassRenderer;
import org.janelia.gltools.material.VolumeMipMaterial;
import org.janelia.scenewindow.OrbitPanZoomInteractor;
import org.janelia.scenewindow.SceneRenderer;
import org.janelia.scenewindow.SceneRenderer.CameraType;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.scenewindow.fps.FrameTracker;
import org.janelia.console.viewerapi.SynchronizationHelper;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.console.viewerapi.listener.NeuronVertexCreationListener;
import org.janelia.console.viewerapi.listener.NeuronVertexDeletionListener;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertexAdditionObserver;
import org.janelia.console.viewerapi.model.NeuronVertexDeletionObserver;
import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.horta.activity_logging.ActivityLogHelper;
import org.janelia.horta.actors.SpheresActor;
import org.janelia.horta.loader.DroppedFileHandler;
import org.janelia.horta.loader.GZIPFileLoader;
import org.janelia.horta.loader.HortaSwcLoader;
import org.janelia.horta.loader.HortaVolumeCache;
import org.janelia.horta.loader.ObjMeshLoader;
import org.janelia.horta.loader.TarFileLoader;
import org.janelia.horta.loader.TgzFileLoader;
import org.janelia.horta.loader.TilebaseYamlLoader;
import org.janelia.horta.movie.HortaMovieSource;
import org.janelia.horta.nodes.BasicHortaWorkspace;
import org.janelia.horta.nodes.WorkspaceUtil;
import org.janelia.horta.volume.BrickActor;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.console.viewerapi.listener.TolerantMouseClickListener;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.actions.RedoAction;
import org.openide.actions.UndoAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.MouseUtils;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
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
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.horta.NeuronTracerTopComponent")
@ActionReference(path = "Menu/Window/Horta" , position = 0 )
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
        implements VolumeProjection, YamlStreamLoader
{
    public static final String PREFERRED_ID = "NeuronTracerTopComponent";
    public static final String BASE_YML_FILE = "tilebase.cache.yml";

    private SceneWindow sceneWindow;
    private OrbitPanZoomInteractor interactor;
    private HortaMetaWorkspace metaWorkspace;
    private final NeuronVertexSpatialIndex neuronVertexIndex;
    
    // private MultipassVolumeActor mprActor;
    // private VolumeMipMaterial volumeMipMaterial;
    private VolumeMipMaterial.VolumeState volumeState = new VolumeMipMaterial.VolumeState();

    // Avoid letting double clicks move twice
    private long previousClickTime = Long.MIN_VALUE;
    private final long minClickInterval = 400 * 1000000;

    // Cache latest hover information
    private Vector3 mouseStageLocation = null;
    private final Observer cursorCacheDestroyer;

    private TracingInteractor tracingInteractor;
    private StaticVolumeBrickSource volumeSource;
    private CenterCrossHairActor crossHairActor;
    private ScaleBar scaleBar = new ScaleBar();
    private ActivityLogHelper activityLogger = ActivityLogHelper.getInstance();
        
    private final NeuronMPRenderer neuronMPRenderer;
    
    private String currentSource;
    private NeuronTraceLoader loader;
    
    private boolean leverageCompressedFiles = false;
    
    private boolean doCubifyVoxels = false; // Always begin in "no distortion" state
    
    private final NeuronManager neuronManager;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private UndoRedo.Manager undoRedoManager = new UndoRedo.Manager();
    
    public static NeuronTracerTopComponent findThisComponent() {
        return (NeuronTracerTopComponent)WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }
    private int defaultColorChannel = 0;
    
    private final HortaVolumeCache volumeCache;
    private final HortaMovieSource movieSource = new HortaMovieSource(this);
    
    public NeuronTracerTopComponent() {
        // This block is what the wizard created
        initComponents();
        setName(Bundle.CTL_NeuronTracerTopComponent());
        setToolTipText(Bundle.HINT_NeuronTracerTopComponent());

        // Below is custom methods by me CMB
        
        // Insert a specialized SceneWindow into the component
        initialize3DViewer(); // initializes workspace

        // Drag a YML tilebase file to put some data in the viewer
        setupDragAndDropYml();

        neuronManager = new NeuronManager(metaWorkspace);
        neuronVertexIndex = new NeuronVertexSpatialIndex(neuronManager);
        

        // Change default rotation to Y-down, like large-volume viewer
        sceneWindow.getVantage().setDefaultRotation(new Rotation().setFromAxisAngle(
                new Vector3(1, 0, 0), (float) Math.PI));
        sceneWindow.getVantage().resetRotation();

        setupMouseNavigation();
        
        neuronManager.addNeuronVertexCreationListener(tracingInteractor);
        neuronManager.addNeuronVertexDeletionListener(tracingInteractor);
        
        // Redraw the density when annotations are added
        neuronManager.addNeuronVertexCreationListener(new NeuronVertexCreationListener() {
            @Override
            public void neuronVertexCreated(VertexWithNeuron vertexWithNeuron) {
                neuronMPRenderer.setIntensityBufferDirty();
            }
        });
        neuronManager.addNeuronVertexDeletionListener(new NeuronVertexDeletionListener() {
            @Override
            public void neuronVertexesDeleted(VertexCollectionWithNeuron vertexesWithNeurons) {
                neuronMPRenderer.setIntensityBufferDirty();
            }
        });

        // Create right-click context menu
        setupContextMenu(sceneWindow.getInnerComponent());
        
        // Press "V" to hide all neuron models
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("pressed V"), "hideModels");
        inputMap.put(KeyStroke.getKeyStroke("released V"), "unhideModels");
        ActionMap actionMap = getActionMap();
        actionMap.put("hideModels", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // System.out.println("hide models");
                if (neuronMPRenderer.setHideAll(true))
                    redrawNow();
            }
        });
        actionMap.put("unhideModels", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // System.out.println("unhide models");
                if (neuronMPRenderer.setHideAll(false))
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

        
        // Repaint when color map changes
        brightnessModel.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // logger.info("Camera changed");
                redrawNow();
            }
        });

        // Load new volume data when the focus moves
        volumeCache = new HortaVolumeCache(
                (PerspectiveCamera)sceneWindow.getCamera(),
                brightnessModel,
                volumeState,
                defaultColorChannel
        );
        volumeCache.addObserver(new HortaVolumeCache.TileDisplayObserver() {
            @Override
            public void update(BrickActor newTile, Collection<? extends BrickInfo> allTiles) {
                if (! allTiles.contains(newTile.getBrainTile()))
                    return; // Tile is stale, so don't load it
                // System.out.println("Got "+allTiles.size()+" tile(s) to display from Volume Cache");
                
                // Undisplay stale tiles and upload to GPU
                Iterator<GL3Actor> iter = neuronMPRenderer.getVolumeActors().iterator();
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
                    if (! allTiles.contains(actorInfo)) {
                        iter.remove(); // Safe member deletion via iterator
                        neuronMPRenderer.queueObsoleteResource(brickActor);
                    }
                }
                // Upload up to one tile per update call
                if (! tileAlreadyDisplayed) {
                    // System.out.println("Uploading tile "+ newTile.getBrainTile().getLocalPath() +" to GPU");
                    neuronMPRenderer.addVolumeActor(newTile);
                    redrawNow();
                }
            }
        });

        neuronMPRenderer = setUpActors();
        
        setBackgroundColor( metaWorkspace.getBackgroundColor() ); // call this AFTER setUpActors
        // neuronMPRenderer.setWorkspace(workspace); // set up signals in renderer
        metaWorkspace.addObserver(new Observer() {
            // Update is called when the set of neurons changes, or the background color changes
            @Override
            public void update(Observable o, Object arg)
            {
                // Apply tracing interactions to LVV workspace
                // TODO: for now assuming that the largest NeuronSet is the LVV one
                
                Collection<NeuronSet> sets = metaWorkspace.getNeuronSets();
                if (sets.isEmpty()) {} // Do nothing
                else if (sets.size() == 1) {
                    tracingInteractor.setDefaultWorkspace(sets.iterator().next());                    
                }
                else {
                    for (NeuronSet ws : sets) {
                        // Skip initial internal default set
                        if (ws.getName().equals("Temporary Neurons"))
                            continue;
                        // Assume any other set is probably the LVV workspace
                        tracingInteractor.setDefaultWorkspace(ws);
                    }
                }
                
                // Update background color
                setBackgroundColor( metaWorkspace.getBackgroundColor() );
                redrawNow();
            }
        });

        loader = new NeuronTraceLoader(
                NeuronTracerTopComponent.this,
                neuronMPRenderer,
                sceneWindow
        );
        
        // Default to compressed voxels, per user request February 2016
        setCubifyVoxels(true);
        
        loadStartupPreferences();

        metaWorkspace.notifyObservers();

    }
    
    // UNDO
    @Override
    public UndoRedo getUndoRedo() {
        return undoRedoManager;
    }
    
    public void setVolumeSource(StaticVolumeBrickSource volumeSource) {
        this.volumeSource = volumeSource;
        this.volumeCache.setSource(volumeSource);
    }
    
    /** Tells caller what source we are examining. */
    public URL getCurrentSourceURL() throws MalformedURLException, URISyntaxException {
        if (currentSource == null)
            return null;
        return new URI(currentSource).toURL();
    }
    
    public void setSampleLocation(SampleLocation sampleLocation) {
        try {
            leverageCompressedFiles = sampleLocation.isCompressed();
            ViewerLocationAcceptor acceptor = new SampleLocationAcceptor(
                    currentSource, loader, NeuronTracerTopComponent.this, sceneWindow
            );
            acceptor.acceptLocation(sampleLocation);
            currentSource = sampleLocation.getSampleUrl().toString();
            defaultColorChannel = sampleLocation.getDefaultColorChannel();
            volumeCache.setColorChannel(defaultColorChannel);
            activityLogger.logHortaLaunch(sampleLocation);

        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            throw new RuntimeException(
                    "Failed to load location " + sampleLocation.getSampleUrl().toString() + ", " +
                    sampleLocation.getFocusXUm() + "," + sampleLocation.getFocusYUm() + "," + sampleLocation.getFocusZUm()
            );
        }
    }
    
    private NeuronMPRenderer setUpActors() 
    {
        
        // TODO - refactor all stages to use multipass renderer, like this
        NeuronMPRenderer neuronMPRenderer0 = new NeuronMPRenderer(sceneWindow.getGLAutoDrawable(), brightnessModel, metaWorkspace);
        List<MultipassRenderer> renderers = sceneWindow.getRenderer().getMultipassRenderers();
        renderers.clear();
        renderers.add(neuronMPRenderer0);
                
        // 3) Neurite model
        for (GL3Actor tracingActor : tracingInteractor.createActors()) {
            sceneWindow.getRenderer().addActor(tracingActor);
            if (tracingActor instanceof SpheresActor) // highlight hover actor
            {
                SpheresActor spheresActor = (SpheresActor)tracingActor;
                spheresActor.getNeuron().getVertexAddedObservable().addObserver(new NeuronVertexAdditionObserver() {
                    @Override
                    public void update(GenericObservable<VertexWithNeuron> o, VertexWithNeuron arg) {
                        redrawNow();
                    }
                });
                spheresActor.getNeuron().getVertexesRemovedObservable().addObserver(new NeuronVertexDeletionObserver() {
                    @Override
                    public void update(GenericObservable<VertexCollectionWithNeuron> object, VertexCollectionWithNeuron data) {
                        redrawNow();
                    }
                });
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
                min = (float)Math.min(min, i);
                max = (float)Math.max(max, i);
            }
        }
        // logger.info("Min = "+min+"; Max = "+max);
        if (max == Float.MIN_VALUE) {
            return; // no valid intensities found
        }
        brightnessModel.setMinimum(min / 65535f);
        brightnessModel.setMaximum(max / 65535f);
        brightnessModel.notifyObservers();
    }

    public StaticVolumeBrickSource getVolumeSource()
    {
        return volumeSource;
    }
    
    @Override
    public StaticVolumeBrickSource loadYaml(InputStream sourceYamlStream, NeuronTraceLoader loader, ProgressHandle progress) throws IOException, ParseException
    {
        setVolumeSource(new MouseLightYamlBrickSource(sourceYamlStream, leverageCompressedFiles, progress));
        return volumeSource;
    }

    private void setupMouseNavigation() {
        // 1) Delegate tracing interaction to customized class
        tracingInteractor = new TracingInteractor(this, undoRedoManager);

        // 2) Setup 3D viewer mouse interaction
        interactor = new OrbitPanZoomInteractor(
                sceneWindow.getCamera(),
                sceneWindow.getInnerComponent());
        
        // 3) Add custom interactions
        MouseInputListener hortaMouseListener = new MouseInputAdapter() 
        {
            // Show/hide crosshair on enter/exit
            @Override
            public void mouseEntered(MouseEvent event) {
                super.mouseEntered(event);
                crossHairActor.setVisible(true);
                sceneWindow.redrawNow();
            }
            @Override
            public void mouseExited(MouseEvent event) {
                super.mouseExited(event);
                crossHairActor.setVisible(false);
                sceneWindow.redrawNow();
            }

            // Click to center on position
            @Override
            public void mouseClicked(MouseEvent event) 
            {
                // Click to center on position
                if ((event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON1)) {
                    if (System.nanoTime() < (previousClickTime + minClickInterval)) {
                        return;
                    }

                    // Use neuron cursor position, if available, rather than hardware mouse position.
                    Vector3 xyz = null;
                    // NeuriteAnchor hoverAnchor = tracingInteractor.getHoverLocation();
                    // if (hoverAnchor == null) {
                        xyz = worldXyzForScreenXy(event.getPoint());
                    // } else {
                    //     xyz = hoverAnchor.getLocationUm();
                        // logger.info("Using neuron cursor XYZ "+xyz);
                    // }

                    // logger.info(xyz);
                    previousClickTime = System.nanoTime();
                    PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
                    loader.animateToFocusXyz(xyz, pCam.getVantage(), 150);
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

                // reportPickItem(msg, event);

                if (msg.length() > 0) {
                    StatusDisplayer.getDefault().setStatusText(msg.toString(), 1);
                }
            }
        };
        // Allow some slop in mouse position during mouse click to match tracing interactor behavior July 2016 CMB
        TolerantMouseClickListener tolerantMouseClickListener = new TolerantMouseClickListener(hortaMouseListener, 5);
        sceneWindow.getInnerComponent().addMouseListener(tolerantMouseClickListener);
        sceneWindow.getInnerComponent().addMouseMotionListener(tolerantMouseClickListener);

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
                sceneWindow.redrawNow();
            }
        }
        // never skip the final frame
        if (vantage.setRotationInGround(rot)) {
            didMove = true;
        }
        if (didMove) {
            vantage.notifyObservers();
            sceneWindow.redrawNow();
        }
    }

    // Append a message about the item under the cursor
    private void reportPickItem(StringBuilder msg, MouseEvent event) {
        double itemId = neuronMPRenderer.pickIdForScreenXy(event.getPoint());
        msg.append("  Item index under cursor = " + itemId);
    }

    private void reportIntensity(StringBuilder msg, MouseEvent event) {
        // TODO: Use neuron cursor position, if available, rather than hardware mouse position.
        Vector3 worldXyz = null;
        double intensity = 0;
        
            PerspectiveCamera camera = (PerspectiveCamera) sceneWindow.getCamera();
            double relDepthF = neuronMPRenderer.depthOffsetForScreenXy(event.getPoint(), camera);
            worldXyz = worldXyzForScreenXy(event.getPoint(), camera, relDepthF);
            intensity = neuronMPRenderer.intensityForScreenXy(event.getPoint());
            // System.out.println("non-hover intensity = "+intensity);

        mouseStageLocation = worldXyz;
        msg.append(String.format("[% 7.1f, % 7.1f, % 7.1f] \u00B5m",
                worldXyz.get(0), worldXyz.get(1), worldXyz.get(2)));
        if (intensity != -1) {
            msg.append(String.format("  Intensity: % d", (int)intensity));
            // System.out.println("message intensity = "+intensity); // Why is this 0 when depth intensity is nonzero?
        }
        // TODO - print out tile X, Y, Z (voxels)
        // TODO - print out tile identifier       
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
        float screenResolution
                = camera.getVantage().getSceneUnitsPerViewportHeight()
                / (float) camera.getViewport().getHeightPixels();
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
        Vector4 worldXyz = camera_X_world.multiply(new Vector4(cx, cy, (float)cz, 1));
        return new Vector3(worldXyz.get(0), worldXyz.get(1), worldXyz.get(2));
    }

    private final BrightnessModel brightnessModel = new BrightnessModel();
    
    private void loadStartupPreferences() 
    {
        Preferences prefs = NbPreferences.forModule(getClass());
        brightnessModel.setMinimum(
                prefs.getFloat("startupMinIntensityChan0", brightnessModel.getMinimum()) );
        brightnessModel.setMaximum(
                prefs.getFloat("startupMaxIntensityChan0", brightnessModel.getMaximum()) );
        volumeState.projectionMode = 
                prefs.getInt("startupProjectionMode", volumeState.projectionMode);
        volumeState.filteringOrder = 
                prefs.getInt("startupRenderFilter", volumeState.filteringOrder);
        setCubifyVoxels(prefs.getBoolean("bCubifyVoxels", doCubifyVoxels));
        volumeCache.setUpdateCache(
                prefs.getBoolean("bCacheHortaTiles", volumeCache.isUpdateCache()));
    }
    
    private void saveStartupPreferences() {
        Preferences prefs = NbPreferences.forModule(getClass());
        prefs.putFloat("startupMinIntensityChan0", brightnessModel.getMinimum());
        prefs.putFloat("startupMaxIntensityChan0", brightnessModel.getMaximum());
        prefs.putInt("startupProjectionMode", volumeState.projectionMode);
        prefs.putInt("startupRenderFilter", volumeState.filteringOrder);
        prefs.putBoolean("bCubifyVoxels", doCubifyVoxels);
        prefs.putBoolean("bCacheHortaTiles", volumeCache.isUpdateCache());
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
                neuronMPRenderer.setIntensityBufferDirty();
                neuronMPRenderer.setOpaqueBufferDirty();
            }
        });
        brightnessModel.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                neuronMPRenderer.setIntensityBufferDirty();
                // note opaque buffer is not affected by brightness model
                /*
                if (mprActor == null) {
                    return;
                }
                neuronMPRenderer.setIntensityBufferDirty();
                */
            }
        });

        this.setLayout(new BorderLayout());
        sceneWindow = new SceneWindow(vantage, CameraType.PERSPECTIVE);

       // associateLookup(Lookups.singleton(vantage)); // ONE item in lookup
        // associateLookup(Lookups.fixed(vantage, brightnessModel)); // TWO items in lookup
        FrameTracker frameTracker = sceneWindow.getRenderer().getFrameTracker();
        metaWorkspace = new BasicHortaWorkspace(sceneWindow.getVantage());        
        
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
                neuronMPRenderer.setRelativeSlabThickness(vp.getzNearRelative(), vp.getzFarRelative());
                redrawNow();
            }
        });

        associateLookup(Lookups.fixed(
                vantage, 
                brightnessModel, 
                metaWorkspace, 
                frameTracker,
                movieSource,
                vp));

        sceneWindow.setBackgroundColor(Color.DARK_GRAY);
        this.add(sceneWindow.getOuterComponent(), BorderLayout.CENTER);

    }
    
    public void loadDroppedYaml(InputStream yamlStream) throws IOException, ParseException
    {
        // currentSource = Utilities.toURI(file).toURL().toString();
        setVolumeSource(loadYaml(yamlStream, loader, null));
        loader.loadTileAtCurrentFocus(volumeSource);
    }
    
    
    private void setupDragAndDropYml() 
    {
        final DroppedFileHandler droppedFileHandler = new DroppedFileHandler();
        droppedFileHandler.addLoader(new GZIPFileLoader());
        droppedFileHandler.addLoader(new TarFileLoader());
        droppedFileHandler.addLoader(new TgzFileLoader());
        droppedFileHandler.addLoader(new TilebaseYamlLoader(this));
        droppedFileHandler.addLoader(new ObjMeshLoader(this));
        // Put dropped neuron models into "Temporary neurons"
        WorkspaceUtil ws = new WorkspaceUtil(metaWorkspace);
        NeuronSet ns = ws.getOrCreateTemporaryNeuronSet();
        final HortaSwcLoader swcLoader = new HortaSwcLoader(ns, neuronMPRenderer);
        droppedFileHandler.addLoader(swcLoader);
        
        // Allow user to drop tilebase.cache.yml on this window
        setDropTarget(new DropTarget(this, new DropTargetListener() {

            boolean isDropSourceGood(DropTargetDropEvent event) {
                if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                
                
                return true;
            }

            boolean isDropSourceGood(DropTargetDragEvent event) {
                if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                return true;
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

                    // Update after asynchronous load completes
                    swcLoader.runAfterLoad(new Runnable() {
                        @Override
                        public void run()
                        {
                            // Update models after drop.
                            if (metaWorkspace == null) return;
                            WorkspaceUtil ws = new WorkspaceUtil(metaWorkspace);
                            NeuronSet ns = ws.getTemporaryNeuronSetOrNull();
                            if (ns == null) return;
                            if (! ns.getMembershipChangeObservable().hasChanged()) return;
                            ns.getMembershipChangeObservable().notifyObservers();
                            // force repaint - just once per drop action though.
                            metaWorkspace.setChanged();
                            metaWorkspace.notifyObservers();
                        }
                    });
                    
                } catch (UnsupportedFlavorException | IOException ex) {
                    JOptionPane.showMessageDialog(NeuronTracerTopComponent.this, "Error loading dragged file");
                    Exceptions.printStackTrace(ex);
                } 
                
            }
        }));
    }
    
    // Reimplementing internal load tile method, after Les refactored SampleLocation etc.
    private boolean loadTileAtCurrentFocusAsynchronous() 
    {
        if (currentSource == null) 
            return false;
        SampleLocation location = new BasicSampleLocation();
        location.setCompressed(false);
        location.setDefaultColorChannel(defaultColorChannel);
        Vantage vantage = sceneWindow.getCamera().getVantage();
        Vector3 focus = new Vector3(vantage.getFocusPosition());
        location.setFocusUm(focus.getX(), focus.getY(), focus.getZ());
        try {
            location.setSampleUrl(new URL(currentSource));
        } catch (MalformedURLException ex) {
            return false;
        }
        location.setMicrometersPerWindowHeight(
                vantage.getSceneUnitsPerViewportHeight());
        
        setSampleLocation(location);
        return true;
    }
    
    private void setupContextMenu(Component innerComponent) {
        // Context menu for window - at first just to see if it works with OpenGL
        // (A: YES, if applied to the inner component)
        innerComponent.addMouseListener(new MouseUtils.PopupMouseAdapter() {
            private JPopupMenu createMenu() {
                JPopupMenu menu = new JPopupMenu();                

                // Setting popup menu title here instead of in JPopupMenu constructor,
                // because title from constructor is not shown in default look and feel.
                menu.add("Options:").setEnabled(false); // TODO should I place title in constructor?

                // SECTION: View options
                menu.add(new JPopupMenu.Separator());

                if (mouseStageLocation != null) {
                    // Recenter
                    menu.add(new AbstractAction("Recenter on This 3D Position [left-click]") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
                            loader.animateToFocusXyz(mouseStageLocation, pCam.getVantage(), 150);
                        }
                    });
                }

                menu.add(new AbstractAction("Reset Rotation") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Vantage v = sceneWindow.getVantage();
                        animateToCameraRotation(
                                v.getDefaultRotation(),
                                v, 150);
                    }
                });

                menu.add(new JPopupMenu.Separator());

                menu.add(new AbstractAction("Auto Contrast") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        autoContrast();
                    }
                });

                if (currentSource != null) 
                {
                    JCheckBoxMenuItem enableVolumeCacheMenu = new JCheckBoxMenuItem(
                            "Auto-load Image Tiles", volumeCache.isUpdateCache());
                    menu.add(enableVolumeCacheMenu);
                    enableVolumeCacheMenu.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                            volumeCache.toggleUpdateCache();
                            item.setSelected(volumeCache.isUpdateCache());
                        }
                    });

                    menu.add(new AbstractAction("Load Image Tile Here") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            loadTileAtCurrentFocusAsynchronous();
                        }
                    });                    
                }
                
                if (volumeState != null) {
                    JMenu projectionMenu = new JMenu("Projection");
                    
                    menu.add(projectionMenu);
                    
                    projectionMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Maximum Intensity") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                volumeState.projectionMode == 0);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) 
                        {
                            volumeState.projectionMode = 0;
                            neuronMPRenderer.setIntensityBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                
                    
                    projectionMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Occluding") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                volumeState.projectionMode == 1);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.projectionMode = 1;
                            neuronMPRenderer.setIntensityBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                    
                    projectionMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Isosurface") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                volumeState.projectionMode == 2);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.projectionMode = 2;
                            neuronMPRenderer.setIntensityBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                                        
                    JMenu filterMenu = new JMenu("Rendering Filter");
                    menu.add(filterMenu);

                    filterMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Nearest-neighbor (Discrete Voxels)") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                volumeState.filteringOrder == 0);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.filteringOrder = 0;
                            neuronMPRenderer.setIntensityBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));

                    filterMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Trilinear") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                volumeState.filteringOrder == 1);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            volumeState.filteringOrder = 1;
                            neuronMPRenderer.setIntensityBufferDirty();
                            sceneWindow.redrawNow();
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
                            neuronMPRenderer.setIntensityBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                }
                
                if (sceneWindow != null) {
                    JMenu stereoMenu = new JMenu("Stereo3D");
                    menu.add(stereoMenu);

                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Monoscopic (Not 3D)") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                sceneWindow.getRenderer().getStereo3dMode() 
                                        == SceneRenderer.Stereo3dMode.MONO);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.MONO);
                            neuronMPRenderer.setIntensityBufferDirty();
                            neuronMPRenderer.setOpaqueBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                    
                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Left Eye View") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                sceneWindow.getRenderer().getStereo3dMode() 
                                        == SceneRenderer.Stereo3dMode.LEFT);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.LEFT);
                            neuronMPRenderer.setIntensityBufferDirty();
                            neuronMPRenderer.setOpaqueBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                    
                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Right Eye View") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                sceneWindow.getRenderer().getStereo3dMode() 
                                        == SceneRenderer.Stereo3dMode.RIGHT);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.RIGHT);
                            neuronMPRenderer.setIntensityBufferDirty();
                            neuronMPRenderer.setOpaqueBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                    
                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Red/Cyan Anaglyph") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                sceneWindow.getRenderer().getStereo3dMode() 
                                        == SceneRenderer.Stereo3dMode.RED_CYAN);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.RED_CYAN);
                            neuronMPRenderer.setIntensityBufferDirty();
                            neuronMPRenderer.setOpaqueBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                    
                    stereoMenu.add(new JRadioButtonMenuItem(
                            new AbstractAction("Green/Magenta Anaglyph") 
                    {
                        {  
                            putValue(Action.SELECTED_KEY, 
                                sceneWindow.getRenderer().getStereo3dMode() 
                                        == SceneRenderer.Stereo3dMode.GREEN_MAGENTA);
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            sceneWindow.getRenderer().setStereo3dMode(
                                    SceneRenderer.Stereo3dMode.GREEN_MAGENTA);
                            neuronMPRenderer.setIntensityBufferDirty();
                            neuronMPRenderer.setOpaqueBufferDirty();
                            sceneWindow.redrawNow();
                        }
                    }));
                    
                }
                
                JCheckBoxMenuItem cubeDistortMenu = new JCheckBoxMenuItem("Compress Voxels in Z", doCubifyVoxels);
                menu.add(cubeDistortMenu);
                cubeDistortMenu.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                        if (doCubifyVoxels) {
                            setCubifyVoxels(false);
                        }
                        else {
                            setCubifyVoxels(true);
                        }
                        item.setSelected(doCubifyVoxels);
                    }
                });
                
                menu.add(new AbstractAction("Save Screen Shot...") {
                    @Override
                    public void actionPerformed(ActionEvent e) 
                    {
                        BufferedImage image = getScreenShot();
                        if (image == null) {
                            return;
                        }
                        FileDialog chooser = new FileDialog((Frame) null,
                                "Save Neuron Tracer Image",
                                FileDialog.SAVE);
                        chooser.setFile("*.png");
                        chooser.setVisible(true);
                        // logger.info("Screen shot file name = " + chooser.getFile());
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
                            Exceptions.printStackTrace(ex);
                        }
                    }
                });
                
                // I could not figure out how to save the settings every time the application closes,
                // so make the user save the settings on demand.
                menu.add(new AbstractAction("Save Viewer Settings") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        saveStartupPreferences();
                    }
                });
                
                // SECTION: Anchors
                menu.add(new JPopupMenu.Separator());
                final TracingInteractor.InteractorContext interactorContext = tracingInteractor.createContext();

                if (interactorContext.canClearParent()) {
                    menu.add(new AbstractAction("Clear Current Parent Anchor") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        interactorContext.clearParent();
                    }
                });
                }
                
                if (interactorContext.getCurrentParentAnchor() != null) {
                    menu.add(new AbstractAction("Center on Current Parent Anchor") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
                        Vector3 xyz = new Vector3(interactorContext.getCurrentParentAnchor().getLocation());
                        loader.animateToFocusXyz(xyz, pCam.getVantage(), 150);
                    }
                });
                }
                
                // SECTION: Undo/redo
                
                if (undoRedoManager.canUndoOrRedo()) {
                    menu.add(new JPopupMenu.Separator());                
                    if (undoRedoManager.canUndo()) {
                        UndoAction undoAction = SystemAction.get(UndoAction.class);
                        JMenuItem undoItem = undoAction.getPopupPresenter();
                        KeyStroke shortcut = undoItem.getAccelerator();
                        // For some reason, getPopupPresenter() does not include a keyboard shortcut
                        if (shortcut == null) {
                            // Sadly, this attempt to access the global keymap does not work.
                            Keymap keymap = Lookup.getDefault().lookup(Keymap.class);
                            if (keymap != null) {
                                KeyStroke[] keyStrokes = keymap.getKeyStrokesForAction(undoAction);
                                if (keyStrokes.length > 0)
                                    shortcut = keyStrokes[0];
                            }
                            // KeyStroke undoKey2 = (KeyStroke)undoAction.getProperty(Action.ACCELERATOR_KEY); // Nope, protected
                            if (shortcut == null) {
                                shortcut = KeyStroke.getKeyStroke(
                                        KeyEvent.VK_Z,
                                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() // CTRL on Win/Linux, flower on Mac
                                );
                            }
                            undoItem.setAccelerator(shortcut);
                        }
                        menu.add(undoItem);
                    }
                    if (undoRedoManager.canRedo()) {
                        RedoAction redoAction = SystemAction.get(RedoAction.class);
                        JMenuItem redoItem = redoAction.getPopupPresenter();
                        // For some reason, getPopupPresenter() does not include a keyboard shortcut
                        KeyStroke shortcut = redoItem.getAccelerator();
                        if (shortcut == null) {
                            redoItem.setAccelerator(KeyStroke.getKeyStroke(
                                    KeyEvent.VK_Y, // TODO: Shift-flower-Z on Mac
                                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() // CTRL on Win/Linux, flower on Mac
                            ));
                        }
                        menu.add(redoItem);
                    }
                }
                
                // SECTION: Tracing options
                menu.add(new JPopupMenu.Separator());
                // Fetch anchor location before popping menu, because menu causes
                // hover location to clear
                // TODO:
                // NeuriteAnchor hoverAnchor = tracingInteractor.getHoverLocation();
                // tracingInteractor.exportMenuItems(menu, hoverAnchor);

                boolean showLinkToLvv = true;
                if ( (mouseStageLocation != null) && (showLinkToLvv) ) {
                    // Synchronize with LVV
                    // TODO - is LVV present?
                    menu.add(new JPopupMenu.Separator());
                    // Want to lookup, get URL and get focus.
                    SynchronizationHelper helper = new SynchronizationHelper();
                    Collection<Tiled3dSampleLocationProviderAcceptor> locationProviders =
                            helper.getSampleLocationProviders(HortaLocationProvider.UNIQUE_NAME);
                    Tiled3dSampleLocationProviderAcceptor origin = 
                            helper.getSampleLocationProviderByName(HortaLocationProvider.UNIQUE_NAME);
                    logger.info("Found {} synchronization providers for neuron tracer.", locationProviders.size());
                    ViewerLocationAcceptor acceptor = new SampleLocationAcceptor(
                            currentSource, loader, NeuronTracerTopComponent.this, sceneWindow
                    );
                    RelocationMenuBuilder menuBuilder = new RelocationMenuBuilder();
                    if (locationProviders.size() > 1) {
                        JMenu synchronizeAllMenu = new JMenu("Synchronize with Other 3D Viewer.");
                        for (JMenuItem item: menuBuilder.buildSyncMenu(locationProviders, origin, acceptor)) {
                            synchronizeAllMenu.add(item);
                        }
                        menu.add(synchronizeAllMenu);
                    }
                    else if (locationProviders.size() == 1) {
                        for (JMenuItem item : menuBuilder.buildSyncMenu(locationProviders, origin, acceptor)) {
                            menu.add(item);
                        }
                    }
                }
                
                // Cancel/do nothing action
                menu.add(new JPopupMenu.Separator());
                menu.add(new AbstractAction("Close This Menu [ESC]") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                });

                return menu;
            }

            @Override
            protected void showPopup(MouseEvent event) {
                if (!NeuronTracerTopComponent.this.isShowing()) {
                    return;
                }
                // logger.info("showPopup");
                createMenu().show(NeuronTracerTopComponent.this, event.getPoint().x, event.getPoint().y);
            }
        });
    }

    public void setUpdateVolumeCache(boolean doUpdate) {
        volumeCache.setUpdateCache(doUpdate);
    }
    
    public boolean doesUpdateVolumeCache() {
        return volumeCache.isUpdateCache();
    }
    
    public GL3Actor createBrickActor(BrainTileInfo brainTile, int colorChannel) throws IOException 
    {
        return new BrickActor(brainTile, brightnessModel, volumeState, colorChannel);
    }
    
    public double[] getStageLocation() {
        if (mouseStageLocation == null) {
            return null;
        }
        else {
            return new double[] {
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
    private final void initComponents()
    {

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
        return neuronMPRenderer.intensityForScreenXy(xy);
    }

    @Override
    public Vector3 worldXyzForScreenXy(Point2D xy) {
        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        double depthOffset = neuronMPRenderer.depthOffsetForScreenXy(xy, pCam);
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
    
    public boolean setCubifyVoxels(boolean cubify) {
        if (cubify == doCubifyVoxels)
            return false; // no change
        doCubifyVoxels = cubify;
        // TODO - actually cubify
        Vantage v = sceneWindow.getVantage();
        if (doCubifyVoxels) {
            v.setWorldScaleHack(1, 1, 0.4f);
            // logger.info("distort");
        }
        else {
            v.setWorldScaleHack(1, 1, 1);
            // logger.info("undistort");
        }
        v.notifyObservers();
        sceneWindow.redrawNow();
        
        return true;
    }
    
    // Create background gradient using a single base color
    private void setBackgroundColor(Color c) {
        // Update background color
        float[] cf = c.getColorComponents(new float[3]);
        // Convert sRGB to linear RGB
        for (int i = 0; i < 3; ++i)
            cf[i] = cf[i]*cf[i]; // second power is close enough...
        // Create color gradient from single color
        double deltaLuma = 0.05; // desired intensity change
        double midLuma = 0.30*cf[0] + 0.59*cf[1] + 0.11*cf[2];
        double topLuma = midLuma - 0.5*deltaLuma;
        double bottomLuma = midLuma + 0.5*deltaLuma;
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
                (int)(cf[0]*t), (int)(cf[1]*t), (int)(cf[2]*t),
                (int)(cf[0]*b), (int)(cf[1]*b), (int)(cf[2]*b)
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
    
    public void setBackgroundColor(Color topColor, Color bottomColor) {
        neuronMPRenderer.setBackgroundColor(topColor, bottomColor);
        float[] bf = bottomColor.getColorComponents(new float[3]);
        double bottomLuma = 0.30*bf[0] + 0.59*bf[1] + 0.11*bf[2];
        if (bottomLuma > 0.25) { // sRGB luma 0.5 == lRGB luma 0.25...
            scaleBar.setForegroundColor(Color.black);
            scaleBar.setBackgroundColor(new Color(255, 255, 255, 50));
        }
        else {
            scaleBar.setForegroundColor(Color.white);
            scaleBar.setBackgroundColor(new Color(0, 0, 0, 50));                    
        }
    }

    // TODO: Use this for redraw needs
    public void redrawNow() {
        if (! isShowing())
            return;
        sceneWindow.getInnerComponent().repaint();
    }

    @Override
    public void componentOpened() {
        // logger.info("Horta opened");
        neuronManager.onOpened();
        // loadStartupPreferences();
    }
    
    // NOTE: componentClosed() is only called when just the Horta window is closed, not
    // when the whole application closes.
    @Override
    public void componentClosed() {
        // logger.info("Horta closed");
        // saveStartupPreferences(); // not called at application close...
        neuronManager.onClosed();
    }

    @Override
    public boolean isNeuronModelAt(Point2D xy)
    {
        return neuronMPRenderer.isNeuronModelAt(xy, 
                sceneWindow.getCamera());
    }

    @Override
    public boolean isVolumeDensityAt(Point2D xy)
    {
        return neuronMPRenderer.isVolumeDensityAt(xy,
                sceneWindow.getCamera());
    }

    @Override
    public NeuronVertexSpatialIndex getVertexIndex()
    {
        return neuronVertexIndex;
    }

    void registerLoneDisplayedTile(BrickActor boxMesh) {
        volumeCache.registerLoneDisplayedTile(boxMesh);
    }

    // API for use by external HortaMovieSource class
    
    public boolean setVisibleActors(Collection<String> visibleActorNames)
    {
        // TODO: This is just neurons for now...
        for (NeuronSet neuronSet : metaWorkspace.getNeuronSets()) {
            for (NeuronModel neuron : neuronSet) {
                String n = neuron.getName();
                boolean bWas = neuron.isVisible();
                boolean bIs = visibleActorNames.contains(n);
                if (bWas == bIs)
                    continue;
                neuron.setVisible(bIs);
                neuron.getVisibilityChangeObservable().notifyObservers();
            }
        }
        
        return false;
    }
    
    public Collection<String> getVisibleActorNames() {
        Collection<String> result = new HashSet<>();

        // TODO: This is just neurons for now...
        for (NeuronSet neuronSet : metaWorkspace.getNeuronSets()) {
            for (NeuronModel neuron : neuronSet) {
                if (neuron.isVisible())
                    result.add(neuron.getName());
            }
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
        
    public BufferedImage getScreenShot() 
    {
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

    public void addMeshActor(GL3Actor meshActor) {
        neuronMPRenderer.addMeshActor(meshActor);
    }

}
