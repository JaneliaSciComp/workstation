package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.swing.*;
import org.apache.commons.lang.SystemUtils;
import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.lvv.ChannelBrightnessStats;
import org.janelia.it.jacs.shared.lvv.ImageBrightnessStats;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.it.workstation.gui.large_volume_viewer.action.BasicMouseMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.MouseMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.PanMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.TraceMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.WheelMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZScanMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZoomMode;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListenerAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.MessageListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.RepaintListener;
import org.janelia.it.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActorStateUpdater;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;
import org.janelia.workstation.gui.opengl.GLActor;
import org.janelia.workstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.workstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.openide.util.NbPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Viewer widget for viewing 2D quadtree tiles from pyramid data structure
public class LargeVolumeViewer implements MouseModalWidget, TileConsumer, RepaintListener {

    private static final Logger LOG = LoggerFactory.getLogger(LargeVolumeViewer.class);

    protected MouseMode mouseMode;
    protected MouseMode.Mode mouseModeId;

    protected WheelMode wheelMode;
    protected WheelMode.Mode wheelModeId;
    protected ObservableCamera3d camera;
    protected SliceRenderer renderer = new SliceRenderer();
    protected Viewport viewport = renderer.getViewport();
    protected RubberBand rubberBand = new RubberBand();

    private SharedVolumeImage sharedVolumeImage = new SharedVolumeImage();
    protected TileServer tileServer = new TileServer(sharedVolumeImage);
    protected VolumeImage3d volumeImage = sharedVolumeImage;
    protected SliceActor sliceActor;
    private ImageColorModel imageColorModel;
    private final BasicMouseMode pointComputer = new BasicMouseMode();
    protected SkeletonActor skeletonActor = new SkeletonActor();
    private final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(pointComputer);

    private MessageListener messageListener;

    // Popup menu
    MenuItemGenerator systemMenuItemGenerator;
    MenuItemGenerator modeMenuItemGenerator;

    // May 2015 extend by containment, not inheritance
    // Store GLCanvas or GLJPanel in terms of these interfaces:
    GLDrawableWrapper glCanvas;

    public LargeVolumeViewer(final GLCapabilities capabilities,
            final GLCapabilitiesChooser chooser,
            final GLContext sharedContext,
            ObservableCamera3d camera) {
        // Use GLCanvas on Linux, for better frame rate on EnginFrame
        // But not on Windows, so we could still use Java2D decorations
        if (SystemUtils.IS_OS_LINUX) {
            glCanvas = new GLCanvasWrapper(capabilities, chooser, sharedContext) {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    rubberBand.paint(g2);
                }
            };
        } else {
            glCanvas = new GLJPanelWrapper(capabilities, chooser, sharedContext) {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    rubberBand.paint(g2);
                }
            };
        }

        init(camera);
    }

    /**
     * @param messageListener the messageListener to set
     */
    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    private void init(ObservableCamera3d camera) {
        glCanvas.getInnerAwtComponent().addMouseListener(this);
        glCanvas.getInnerAwtComponent().addMouseMotionListener(this);
        glCanvas.getInnerAwtComponent().addMouseWheelListener(this);
        glCanvas.getInnerAwtComponent().addKeyListener(this);
        setMouseMode(MouseMode.Mode.PAN);
        setWheelMode(WheelMode.Mode.SCAN);
        glCanvas.getGLAutoDrawable().addGLEventListener(renderer);
        setCamera(camera);

        ViewTileManager viewTileManager = new ViewTileManager(this);
        viewTileManager.setVolumeImage(tileServer.getSharedVolumeImage());
        viewTileManager.setTextureCache(tileServer.getTextureCache());
        tileServer.addViewTileManager(viewTileManager);
        sliceActor = new SliceActor(viewTileManager);

        // gray background for testing
        // this.renderer.setBackgroundColor(new Color(0.5f, 0.5f, 0.5f, 0.0f));
        // renderer.setBackgroundColor(Color.white);
        // black background for production
        renderer.setBackgroundColor(Color.black);
        glCanvas.getInnerAwtComponent().setPreferredSize(new Dimension(600, 600));
        rubberBand.setRepaintListener(this);
        // setToolTipText("Double click to center on a point.");
        renderer.addActor(sliceActor);
        // renderer.addActor(new TileOutlineActor(viewTileManager));
        // Initialize pointComputer for interconverting pixelXY <=> sceneXYZ
        pointComputer.setCamera(getCamera());
        pointComputer.setWidget(this, false);

        renderer.addActor(skeletonActor);
        SkeletonActorStateUpdater sasUpdater = skeletonActor.getModel().getUpdater();
        sasUpdater.addListener(this);

        // PopupMenu
        glCanvas.getInnerAwtComponent().addMouseListener(new MouseHandler() {
            @Override
            protected void popupTriggered(MouseEvent e) {
                if (e.isConsumed()) {
                    return;
                }
                JPopupMenu popupMenu = new JPopupMenu();
                // Mode specific menu items first
                List<JMenuItem> modeItems = modeMenuItemGenerator.getMenus(e);
                List<JMenuItem> systemMenuItems = systemMenuItemGenerator.getMenus(e);
                if ((modeItems.size() == 0) && (systemMenuItems.size() == 0)) {
                    return;
                }
                if ((modeItems != null) && (modeItems.size() > 0)) {
                    for (JMenuItem item : modeItems) {
                        if (item == null) {
                            popupMenu.addSeparator();
                        } else {
                            popupMenu.add(item);
                        }
                    }
                    if (systemMenuItems.size() > 0) {
                        popupMenu.addSeparator();
                    }
                }
                // Generic menu items last
                for (JMenuItem item : systemMenuItems) {
                    if (item == null) {
                        popupMenu.addSeparator();
                    } else {
                        popupMenu.add(item);
                    }
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                e.consume();
            }
        });

        NbPreferences.forModule(ApplicationPanel.class).addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                if (evt.getKey().equals(ApplicationPanel.PREFERENCE_Z_THICKNESS)) {
                    skeletonActor.setZThicknessInPixels(ApplicationPanel.getZThickness());
                    LOG.info("Z thickness changed: " + skeletonActor.getZoomedZThicknessInPixels());
                }
            }
        });
    }

    void autoContrastNow() {
        ImageBrightnessStats bs = tileServer.getCurrentBrightnessStats();
        if (bs == null) {
            return;
        }
        // Remember which channel has the extreme values
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int minChan = 0;
        int maxChan = 0;
        // Set one channel at a time
        for (int c = 0; c < bs.size(); ++c) {
            if (c >= imageColorModel.getChannelCount()) {
                break;
            }
            ChannelColorModel chanModel = imageColorModel.getChannel(c);
            ChannelBrightnessStats chanStats = bs.get(c);
            // int cMin = chanStats.getMin();
            int cMin = chanStats.estimateQuantile(0.05);
            int cMax = chanStats.estimateQuantile(1.0);
            chanModel.setBlackLevel(cMin);
            chanModel.setWhiteLevel(cMax);
            if (chanStats.getMax() >= max) {
                max = cMax;
                maxChan = c;
            }
            if (cMin <= min) {
                min = cMin;
                minChan = c;
            }
        }
        // In case black and/or white levels are locked,
        // re-specify the most extreme values
        if (bs.size() > 1) {
            imageColorModel.getChannel(maxChan).setWhiteLevel(max);
            imageColorModel.getChannel(minChan).setBlackLevel(min);
        }
    }

    public BoundingBox3d getBoundingBox3d() {
        BoundingBox3d bb = new BoundingBox3d();
        for (GLActor a : renderer.getActors()) {
            bb.include(a.getBoundingBox3d());
        }
        return bb;
    }

    @Override
    public ObservableCamera3d getCamera() {
        return camera;
    }

    public ImageColorModel getImageColorModel() {
        return imageColorModel;
    }

    @Override
    public Point2D getPixelOffsetFromCenter(Point2D point) {
        double dx = point.getX() - glCanvas.getInnerAwtComponent().getWidth() / 2.0;
        double dy = point.getY() - glCanvas.getInnerAwtComponent().getHeight() / 2.0;
        return new Point2D.Double(dx, dy);
    }

    public RubberBand getRubberBand() {
        return rubberBand;
    }

    @Override
    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        mouseMode.mouseClicked(event);
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        mouseMode.mouseDragged(event);
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        mouseMode.mouseMoved(event);
        sendLocationMessage(event.getPoint());
    }

    @Override
    public void mousePressed(MouseEvent event) {
        System.out.println("large volume: mouse pressed");
        mouseMode.mousePressed(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        System.out.println("large volume: mouse released");
        mouseMode.mouseReleased(event);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        this.wheelMode.mouseWheelMoved(event);
        sendLocationMessage(event.getPoint());
    }

    //------------------------IMPLEMENTS MouseWheelModeListener
    @Override
    public void setMouseMode(MouseMode.Mode modeId) {
        if (modeId == this.mouseModeId) {
            return; // no change
        }
        this.mouseModeId = modeId;
        if (modeId == MouseMode.Mode.PAN) {
            this.mouseMode = new PanMode();
        } else if (modeId == MouseMode.Mode.TRACE) {
            TraceMode traceMode = new TraceMode(getSkeleton());
            traceMode.setViewport(getViewport());
            traceMode.setActor(skeletonActor);
            this.mouseMode = traceMode;
        } else if (modeId == MouseMode.Mode.ZOOM) {
            this.mouseMode = new ZoomMode();
        } else {
            LOG.error("Unknown mouse mode");
            return;
        }
        this.mouseMode.setCamera(camera);
        this.mouseMode.setWidget(this, true);
        glCanvas.getOuterJComponent().setToolTipText(mouseMode.getToolTipText());
        this.modeMenuItemGenerator = mouseMode.getMenuItemGenerator();
    }

    @Override
    public void setWheelMode(WheelMode.Mode wheelModeId) {
        if (this.wheelModeId == wheelModeId) {
            return;
        }
        this.wheelModeId = wheelModeId;
        if (wheelModeId == WheelMode.Mode.ZOOM) {
            this.wheelMode = new ZoomMode();
        } else if (wheelModeId == WheelMode.Mode.SCAN) {
            this.wheelMode = new ZScanMode(volumeImage);
        }
        this.wheelMode.setWidget(this, false);
        this.wheelMode.setCamera(camera);
    }

    public void setCamera(ObservableCamera3d camera) {
        if (camera == null) {
            return;
        }
        if (camera == this.camera) {
            return;
        }
        if (this.camera != null && cameraListener != null) {
            this.camera.removeCameraListener(cameraListener);
        }
        this.camera = camera;
        this.cameraListener = new CameraListenerAdapter() {
            @Override
            public void viewChanged() {
                repaint();
            }
        };
        // Update image whenever camera changes
        this.camera.addCameraListener(cameraListener);
        renderer.setCamera(camera);
        mouseMode.setCamera(camera);
        wheelMode.setCamera(camera);
        pointComputer.setCamera(camera);
        skeletonActor.setCamera(camera);
        skeletonActor.setViewport(getViewport());
        skeletonActor.setPointComputer(pointComputer);
    }
    public CameraListenerAdapter cameraListener;

    public TileServer getTileServer() {
        return tileServer;
    }

    public void setImageColorModel(ImageColorModel imageColorModel) {
        if (this.imageColorModel == imageColorModel) {
            return;
        }
        this.imageColorModel = imageColorModel;
        sliceActor.setImageColorModel(imageColorModel);
    }

    public void setNeuronStyleModel(NeuronStyleModel nsModel) {
        skeletonActor.getModel().setNeuronStyleModel(nsModel);
    }

    public Skeleton getSkeleton() {
        return skeletonActor.getModel().getSkeleton();
    }

    public void setSkeleton(Skeleton skeleton) {
        skeletonActor.getModel().setSkeleton(skeleton);
    }

    public SkeletonActor getSkeletonActor() {
        return skeletonActor;
    }

    public void setSkeletonActor(SkeletonActor skeletonActor) {
        this.skeletonActor = skeletonActor;
    }

    public void setSystemMenuItemGenerator(MenuItemGenerator systemMenuItemGenerator) {
        this.systemMenuItemGenerator = systemMenuItemGenerator;
    }

    @Override
    public JComponent getComponent() {
        return glCanvas.getOuterJComponent();
    }

    @Override
    public MouseMode getMouseMode() {
        return mouseMode;
    }

    @Override
    public CoordinateAxis getSliceAxis() {
        return CoordinateAxis.Z;
    }

    @Override
    public Rotation3d getViewerInGround() {
        return new Rotation3d(); // identity matrix
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        mouseMode.mouseEntered(event);
    }

    @Override
    public void mouseExited(MouseEvent event) {
        mouseMode.mouseExited(event);
    }

    // KeyListener interface
    @Override
    public void keyTyped(KeyEvent event) {
        mouseMode.keyTyped(event);
    }

    @Override
    public void keyPressed(KeyEvent event) {
        mouseMode.keyPressed(event);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        mouseMode.keyPressed(event);
    }

    @Override
    public boolean isShowing() {
        return glCanvas.getInnerAwtComponent().isShowing();
    }

    @Override
    public void repaint() {
        glCanvas.repaint();
    }

    private void sendLocationMessage(Point mouseLocation) {
        if (messageListener != null) {
            String msg = micronCoordsFormatter.formatForPresentation(mouseLocation);
            messageListener.message(msg);
            LOG.trace("Message to status {}.", msg);
        }
    }

}
