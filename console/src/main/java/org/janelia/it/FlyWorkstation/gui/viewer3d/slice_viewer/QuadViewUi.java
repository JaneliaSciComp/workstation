package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.AdvanceZSlicesAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.GoBackZSlicesAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.MouseMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.NextZSliceAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.OpenFolderAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.OrthogonalModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.OrthogonalModeAction.OrthogonalMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.PanModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.PreviousZSliceAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.RecentFileList;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ResetColorsAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ResetViewAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ResetZoomAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.TraceMouseModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.WheelMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.SliceScanAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZScanMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZScanScrollModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomInAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomMaxAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomMouseModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomOutAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomScrollModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation.AnnotationManager;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation.AnnotationModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation.AnnotationPanel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.SkeletonActor;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import javax.media.opengl.GLProfile;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;
import java.util.List;

/** 
 * Main window for QuadView application.
 * Maintained using Google WindowBuilder design tool.
 * 
 * @author Christopher M. Bruns
 *
 */
public class QuadViewUi extends JPanel
{
	public static GLProfile glProfile = GLProfile.get(GLProfile.GL2);
	// private static final Logger log = LoggerFactory.getLogger(QuadViewUi.class);

	private boolean bAllowOrthoView = true; // false until ready for release
	
	// One shared camera for all viewers.
	// (there's only one viewer now actually, but you know...)
	private BasicObservableCamera3d camera = new BasicObservableCamera3d();
	GLContextSharer orthoViewContextSharer = new GLContextSharer(glProfile);

	private SliceViewer sliceViewer = new SliceViewer(
			orthoViewContextSharer.getCapabilities(),
			orthoViewContextSharer.getChooser(),
			orthoViewContextSharer.getContext(),
			camera);

	// TODO - promote volumeImage, colorModel out of sliceviewer
	TileServer tileServer = sliceViewer.getTileServer();
	private SharedVolumeImage volumeImage = tileServer.getSharedVolumeImage(); // TODO - volume does not belong down there!
	private ImageColorModel imageColorModel = new ImageColorModel(volumeImage);

	// Four quadrants for orthogonal views
	OrthogonalPanel neViewer = new OrthogonalPanel(CoordinateAxis.X, orthoViewContextSharer);
	OrthogonalPanel swViewer = new OrthogonalPanel(CoordinateAxis.Y, orthoViewContextSharer);
	OrthogonalPanel nwViewer = new OrthogonalPanel(CoordinateAxis.Z, orthoViewContextSharer);
	// TODO 3D view
	// TODO obsolete zViewerPanel in favor of OrthogonalPanel
	JPanel zViewerPanel = new JPanel();
	JComponent seViewer = zViewerPanel; // should be same as Z...

	// Group orthogonal viewers for use in action constructors
	List<TileConsumer> allSliceViewers = Arrays.asList(new TileConsumer[] {
			nwViewer.getViewer(),
			neViewer.getViewer(),
			swViewer.getViewer(),
			sliceViewer			
		});
	
	private boolean modifierKeyPressed = false;
	private JPanel zScanPanel = new JPanel();
	private JSlider zScanSlider = new JSlider();
	private JSpinner zScanSpinner = new JSpinner();
	private JSlider zoomSlider = new JSlider(SwingConstants.VERTICAL, 0, 1000, 500);
	
	private JPanel colorPanel = new JPanel();
	private JPanel colorLockPanel = new JPanel();
    private JMenuBar menuBar = new JMenuBar();
    private JPanel toolBarPanel = new JPanel();
	private JSplitPane splitPane = new JSplitPane();
	private ColorChannelWidget colorChannelWidget_0 = new ColorChannelWidget(0, imageColorModel);
	private ColorChannelWidget colorChannelWidget_1 = new ColorChannelWidget(1, imageColorModel);
	private ColorChannelWidget colorChannelWidget_2 = new ColorChannelWidget(2, imageColorModel);
	private ColorChannelWidget colorChannelWidget_3 = new ColorChannelWidget(3, imageColorModel);
	private final ColorChannelWidget colorWidgets[]  = {
		colorChannelWidget_0, 
		colorChannelWidget_1, 
		colorChannelWidget_2, 
		colorChannelWidget_3
	};
	private JLabel statusLabel = new JLabel("status area");
	
	ZScanMode zScanMode = new ZScanMode(volumeImage);
	
	// annotation things
	private AnnotationModel annotationModel = new AnnotationModel();
	private AnnotationManager annotationMgr = new AnnotationManager(annotationModel);

	// Actions
	private final Action openFolderAction = new OpenFolderAction(volumeImage, sliceViewer);
	private RecentFileList recentFileList = new RecentFileList(new JMenu("Open Recent"));
	private final Action resetViewAction = new ResetViewAction(allSliceViewers, volumeImage);
	private final Action resetColorsAction = new ResetColorsAction(imageColorModel);
	// mode actions (and groups)
	private final ZoomMouseModeAction zoomMouseModeAction = new ZoomMouseModeAction();
	private final PanModeAction panModeAction = new PanModeAction();
    private Skeleton skeleton = new Skeleton();
    private final TraceMouseModeAction traceMouseModeAction = new TraceMouseModeAction();
    // 
	private final ButtonGroup mouseModeGroup = new ButtonGroup();
	private final ZScanScrollModeAction zScanScrollModeAction = new ZScanScrollModeAction();
	private final ZoomScrollModeAction zoomScrollModeAction = new ZoomScrollModeAction();
	private final ButtonGroup scrollModeGroup = new ButtonGroup();
	private final OrthogonalModeAction orthogonalModeAction = new OrthogonalModeAction();
	// zoom actions
	private final Action zoomInAction = new ZoomInAction(camera);
	private final Action zoomOutAction = new ZoomOutAction(camera);
	private final Action zoomMaxAction = new ZoomMaxAction(camera, volumeImage);
	private final Action resetZoomAction = new ResetZoomAction(allSliceViewers, volumeImage);
	// Z scan actions
	private final SliceScanAction nextZSliceAction = new NextZSliceAction(volumeImage, camera);
	private final SliceScanAction previousZSliceAction = new PreviousZSliceAction(volumeImage, camera);
	private final SliceScanAction advanceZSlicesAction = new AdvanceZSlicesAction(volumeImage, camera, 10);
	private final SliceScanAction goBackZSlicesAction = new GoBackZSlicesAction(volumeImage, camera, -10);
	//
	private final Action clearCacheAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			clearCache();
		}
	};
	private final Action autoContrastAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			sliceViewer.autoContrastNow();
		}
	};
	private final Action collectGarbageAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			System.gc();
		}
	};

	public Signal1<MouseMode.Mode> mouseModeChangedSignal = 
		    new Signal1<MouseMode.Mode>();
	    public Signal1<WheelMode.Mode> wheelModeChangedSignal = 
	        new Signal1<WheelMode.Mode>();
	
	// Slots
	private Slot1<URL> loadUrlSlot = new Slot1<URL>() {
		@Override
		public void execute(URL url) {
			loadURL(url);
		}
	};
	    
	protected Slot1<Vec3> changeZ = new Slot1<Vec3>() {
		@Override
		public void execute(Vec3 focus) {
			int z = (int)Math.round((focus.getZ()-0.5) / volumeImage.getZResolution());
			zScanSlider.setValue(z);
			zScanSpinner.setValue(z);
		}
	};

	protected Slot1<Double> changeZoom = new Slot1<Double>() {
		@Override
		public void execute(Double zoom) {
			double zoomMin = Math.log(getMinZoom()) / Math.log(2.0);
			double zoomMax = Math.log(getMaxZoom()) / Math.log(2.0);
			double zoomLog = Math.log(zoom) / Math.log(2.0);
			double relativeZoom = (zoomLog - zoomMin) / (zoomMax - zoomMin);
			int sliderValue = (int)Math.round(relativeZoom * 1000.0);
			zoomSlider.setValue(sliderValue);
		}
	};
	
	protected Slot updateRangesSlot = new Slot() {
		@Override
		public void execute() 
		{
			// Z range
			double zMin = volumeImage.getBoundingBox3d().getMin().getZ();
			double zMax = volumeImage.getBoundingBox3d().getMax().getZ();
			int z0 = (int)Math.round(zMin / volumeImage.getZResolution());
			int z1 = (int)Math.round(zMax / volumeImage.getZResolution()) - 1;
			if (z0 > z1)
				z1 = z0;
			// Z-scan is only relevant if there is more than one slice.
			boolean useZScan = ((z1 - z0) > 1);
			if (useZScan) {
				zScanPanel.setVisible(true);
				sliceViewer.setWheelMode(WheelMode.Mode.SCAN);
				zScanScrollModeAction.setEnabled(true);
				zScanScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
				int z = (int)Math.round((camera.getFocus().getZ()-0.5) / volumeImage.getZResolution());
				if (z < z0)
					z = z0;
				if (z > z1)
					z = z1;
				zScanSlider.setMinimum(z0);
				zScanSlider.setMaximum(z1);
				zScanSlider.setValue(z);
				zScanSpinner.setModel(new SpinnerNumberModel(z, z0, z1, 1));
				// Allow octree zsteps to depend on zoom
				TileFormat tileFormat = tileServer.getLoadAdapter().getTileFormat();
				zScanMode.setTileFormat(tileFormat);
				nextZSliceAction.setTileFormat(tileFormat);
				previousZSliceAction.setTileFormat(tileFormat);
				advanceZSlicesAction.setTileFormat(tileFormat);
				goBackZSlicesAction.setTileFormat(tileFormat);
			}
			else { // no Z scan
				zScanPanel.setVisible(false);
				zoomScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
				zScanScrollModeAction.setEnabled(false);
			}
		}
		// TODO update zoom range too?
	};
	
	public Slot1<String> setStatusMessageSlot = new Slot1<String>() {
		@Override
		public void execute(String message) {
			statusLabel.setText(message);
		}
	};
	
	/**
	 * Create the frame.
	 */
	public QuadViewUi(JFrame parentFrame, Entity initialEntity, boolean overrideFrameMenuBar)
	{
		volumeImage.volumeInitializedSignal.connect(onVolumeLoadedSlot);
		sliceViewer.setImageColorModel(imageColorModel);
		
		colorChannelWidget_3.setVisible(false);
		colorChannelWidget_2.setVisible(false);
		colorChannelWidget_1.setVisible(false);
		colorChannelWidget_0.setVisible(false);
		setupUi(parentFrame, overrideFrameMenuBar);
        interceptModifierKeyPresses();
        interceptModeChangeGestures();

		// must come after setupUi(), since it triggers UI changes:
		annotationMgr.setInitialEntity(initialEntity);

        // connect up text UI and model with graphic UI(s):
        skeleton.addAnchorRequestedSignal.connect(annotationMgr.addAnchorRequestedSlot);
        annotationModel.anchorAddedSignal.connect(skeleton.addAnchorSlot);

        // 
        clearCacheAction.putValue(Action.NAME, "Clear Cache");
        clearCacheAction.putValue(Action.SHORT_DESCRIPTION, 
				"Empty image cache (for testing only)");
        //
        autoContrastAction.putValue(Action.NAME, "Auto Contrast");
        autoContrastAction.putValue(Action.SHORT_DESCRIPTION, 
				"Optimize contrast for current view");
        // 
        collectGarbageAction.putValue(Action.NAME, "Collect Garbage");
        //
        sliceViewer.statusMessageChanged.connect(setStatusMessageSlot);
        sliceViewer.setSkeleton(skeleton);
        //
        sliceViewer.setWheelMode(WheelMode.Mode.SCAN);
        // Respond to orthogonal mode changes
        orthogonalModeAction.orthogonalModeChanged.connect(new Slot1<OrthogonalMode>() {
            	@Override
        		public void execute(OrthogonalMode mode) {
        			if (mode == OrthogonalMode.ORTHOGONAL) 
        				setOrthogonalMode();
        			else if (mode == OrthogonalMode.Z_VIEW) 
        				setZViewMode();
        			// repaint(); // not necessary.
        		}
        });
        setZViewMode();
        // Connect mode changes to widgets
        // First connect mode actions to one signal
        panModeAction.setMouseModeSignal.connect(mouseModeChangedSignal);
        zoomMouseModeAction.setMouseModeSignal.connect(mouseModeChangedSignal);
        traceMouseModeAction.setMouseModeSignal.connect(mouseModeChangedSignal);
        zoomScrollModeAction.setWheelModeSignal.connect(wheelModeChangedSignal);
        zScanScrollModeAction.setWheelModeSignal.connect(wheelModeChangedSignal);
        // Next connect that signal to various widgets
        mouseModeChangedSignal.connect(sliceViewer.setMouseModeSlot);
        wheelModeChangedSignal.connect(sliceViewer.setWheelModeSlot);
        // TODO other orthogonal viewers
        OrthogonalPanel viewPanels[] = {neViewer, swViewer, nwViewer};
        SkeletonActor sharedSkeletonActor = sliceViewer.getSkeletonActor();
        sharedSkeletonActor.setSkeleton(sliceViewer.getSkeleton());
        for (OrthogonalPanel v : viewPanels) {
            mouseModeChangedSignal.connect(v.setMouseModeSlot);
            wheelModeChangedSignal.connect(v.setWheelModeSlot);
            v.setCamera(camera);
            // TODO - move most of this setup into OrthogonalViewer class.
            v.getViewer().statusMessageChanged.connect(setStatusMessageSlot);
            v.setSharedVolumeImage(volumeImage);
            v.setSystemMenuItemGenerator(new MenuItemGenerator() {
                @Override
                public List<JMenuItem> getMenus(MouseEvent event) {
                    List<JMenuItem> result = new Vector<JMenuItem>();
                    result.add(addFileMenuItem());
                    result.add(addViewMenuItem());
                    return result;
                }
            });
            v.setTileServer(tileServer);
            v.getViewer().getSliceActor().setImageColorModel(imageColorModel);
            imageColorModel.getColorModelChangedSignal().connect(v.getViewer().repaintSlot);
            // v.getViewer().addActor(new TileOutlineActor(v.getViewTileManager())); // for debugging
            // Add skeleton actor AFTER slice actor
            v.getViewer().setSkeletonActor(sharedSkeletonActor);
        }
        // Set starting interaction modes
        panModeAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        zoomScrollModeAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
	}

	public void clearCache() {
		tileServer.clearCache();
	}
	
	private double getMaxZoom() {
		double maxRes = Math.min(volumeImage.getXResolution(), Math.min(
				volumeImage.getYResolution(),
				volumeImage.getZResolution()));
		return 300.0 / maxRes; // 300 pixels per voxel is probably zoomed enough...
	}
	
	private double getMinZoom() {
		double result = getMaxZoom();
		BoundingBox3d box = volumeImage.getBoundingBox3d();
		Vec3 volSize = new Vec3(box.getWidth(), box.getHeight(), box.getDepth());
		for (TileConsumer viewer : allSliceViewers) {
			if (! viewer.isShowing())
				continue;
			int w = viewer.getViewport().getWidth();
			int h = viewer.getViewport().getHeight();
			if (w <= 0)
				continue;
			if (h <= 0)
				continue;
			// Fit two of the whole volume on the screen
			// Rotate volume to match viewer orientation
			Vec3 rotSize = viewer.getViewerInGround().inverse().times(volSize);
			double zx = 0.5 * w / Math.abs(rotSize.x());
			double zy = 0.5 * h / Math.abs(rotSize.y());
			double z = Math.min(zx, zy);
			result = Math.min(z, result);	
		}
		return result;
	}
	
	private void setOrthogonalMode() {
		nwViewer.setVisible(true);
		neViewer.setVisible(true);
		swViewer.setVisible(true);
		seViewer.setVisible(true);
	}
	
	private void setZViewMode() {
		nwViewer.setVisible(true);
		neViewer.setVisible(false);
		swViewer.setVisible(false);
		seViewer.setVisible(false);		
	}
	
	private void setupUi(JFrame parentFrame, boolean overrideFrameMenuBar) {
        setBounds(100, 100, 994, 653);
        setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// glassPane.setVisible(true);
        setupMenu(parentFrame, overrideFrameMenuBar);
        JPanel toolBarPanel = setupToolBar();
		
		// JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(1.00);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		toolBarPanel.add(splitPane, BorderLayout.CENTER);
		
		// JPanel colorPanel = new JPanel();
		splitPane.setRightComponent(colorPanel);
		colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.Y_AXIS));
		
		// Avoid loop to add color widgets, so WindowBuilder can parse this.
		colorPanel.add(colorWidgets[0]);
		colorPanel.add(colorWidgets[1]);
		colorPanel.add(colorWidgets[2]);
		colorPanel.add(colorWidgets[3]);
		
		// JPanel colorLockPanel = new JPanel();
		colorLockPanel.setVisible(false);
		colorPanel.add(colorLockPanel);
		colorLockPanel.setLayout(new BoxLayout(colorLockPanel, BoxLayout.X_AXIS));
		
		colorLockPanel.add(Box.createHorizontalStrut(30));
		
		JToggleButton lockBlackButton = new JToggleButton("");
		lockBlackButton.setToolTipText("Synchronize channel black levels");
		lockBlackButton.setMargin(new Insets(0, 0, 0, 0));
		lockBlackButton.setRolloverIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockBlackButton.setRolloverSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockBlackButton.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockBlackButton.setSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockBlackButton.setSelected(true);
		colorLockPanel.add(lockBlackButton);
		lockBlackButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setBlackSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalGlue());

		JToggleButton lockGrayButton = new JToggleButton("");
		lockGrayButton.setToolTipText("Synchronize channel gray levels");
		lockGrayButton.setMargin(new Insets(0, 0, 0, 0));
		lockGrayButton.setRolloverIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockGrayButton.setRolloverSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockGrayButton.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockGrayButton.setSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockGrayButton.setSelected(true);
		colorLockPanel.add(lockGrayButton);
		lockGrayButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setGammaSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalGlue());

		JToggleButton lockWhiteButton = new JToggleButton("");
		lockWhiteButton.setToolTipText("Synchronize channel white levels");
		lockWhiteButton.setMargin(new Insets(0, 0, 0, 0));
		lockWhiteButton.setRolloverIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockWhiteButton.setRolloverSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockWhiteButton.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockWhiteButton.setSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockWhiteButton.setSelected(true);
		colorLockPanel.add(lockWhiteButton);
		lockWhiteButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setWhiteSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalStrut(30));

		imageColorModel.getColorModelInitializedSignal().connect(new Slot() {
			@Override
			public void execute() {
				// System.out.println("Updating slider visibility");
				int sc = imageColorModel.getChannelCount();
				colorPanel.setVisible(sc > 0);
				int c = 0;
				for (ColorChannelWidget w : colorWidgets) {
					w.setVisible(c < sc);
					c += 1;
				}
				colorLockPanel.setVisible(sc > 1);
				splitPane.resetToPreferredSizes();
				// TODO Trying without success to get sliders to initially paint correctly
				colorPanel.validate();
				colorPanel.repaint();
			}
		});
		
		JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setResizeWeight(1.00);
		splitPane.setLeftComponent(splitPane_1);

		JPanel viewerPanel = new JPanel();
		splitPane_1.setLeftComponent(viewerPanel);
		viewerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 0;
		c.gridwidth = 1; c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(1,1,1,1);
		
		// Four quadrants for orthogonal views
		// One panel for Z slice viewer (upper left northwest)
		viewerPanel.add(nwViewer, c);
		zViewerPanel.setLayout(new BoxLayout(zViewerPanel, BoxLayout.Y_AXIS));
		// TODO - other three quadrants
		c.gridx = 1; c.gridy = 0;
		viewerPanel.add(neViewer, c);
		c.gridx = 0; c.gridy = 1;
		viewerPanel.add(swViewer, c);
		c.gridx = 1; c.gridy = 1;
		viewerPanel.add(seViewer, c);
		
		sliceViewer.setCamera(camera);
		sliceViewer.setBackground(Color.DARK_GRAY);
		zViewerPanel.add(sliceViewer);
		volumeImage.volumeInitializedSignal.connect(updateRangesSlot);
		camera.getZoomChangedSignal().connect(changeZoom);
        camera.getFocusChangedSignal().connect(changeZ);
		
		// JPanel zScanPanel = new JPanel();
		zViewerPanel.add(zScanPanel);
		zScanPanel.setLayout(new BoxLayout(zScanPanel, BoxLayout.X_AXIS));
	
        ToolButton button_2 = new ToolButton(goBackZSlicesAction);
		button_2.setAction(goBackZSlicesAction);
		button_2.setMargin(new Insets(0, 0, 0, 0));
		button_2.setHideActionText(true);
		button_2.setAlignmentX(0.5f);
		zScanPanel.add(button_2);
		
		ToolButton button_1 = new ToolButton(previousZSliceAction);
		button_1.setAction(previousZSliceAction);
		button_1.setMargin(new Insets(0, 0, 0, 0));
		button_1.setHideActionText(true);
		button_1.setAlignmentX(0.5f);
		zScanPanel.add(button_1);
		zScanSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				setZSlice(zScanSlider.getValue());
			}
		});
		
		// JSlider zScanSlider = new JSlider();
		zScanSlider.setPreferredSize(new Dimension(32767, 29));
		zScanSlider.setMajorTickSpacing(10);
		zScanSlider.setPaintTicks(true);
		zScanPanel.add(zScanSlider);
		
		ToolButton button_3 = new ToolButton(nextZSliceAction);
		button_3.setAction(nextZSliceAction);
		button_3.setMargin(new Insets(0, 0, 0, 0));
		button_3.setHideActionText(true);
		button_3.setAlignmentX(0.5f);
		zScanPanel.add(button_3);
		
		ToolButton button_4 = new ToolButton(advanceZSlicesAction);
		button_4.setAction(advanceZSlicesAction);
		button_4.setMargin(new Insets(0, 0, 0, 0));
		button_4.setHideActionText(true);
		button_4.setAlignmentX(0.5f);
		zScanPanel.add(button_4);
		zScanSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				setZSlice((Integer)zScanSpinner.getValue());
			}
		});
		
		// JSpinner zScanSpinner = new JSpinner();
		zScanSpinner.setPreferredSize(new Dimension(75, 28));
		zScanSpinner.setMaximumSize(new Dimension(120, 28));
		zScanSpinner.setMinimumSize(new Dimension(65, 28));
		zScanPanel.add(zScanSpinner);
		
		JPanel controlsPanel = new JPanel();
		splitPane_1.setRightComponent(controlsPanel);
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		controlsPanel.add(panel_1);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
		
		ToolButton zoomInButton = new ToolButton(zoomInAction);
		zoomInButton.setAlignmentX(0.5f);
		zoomInButton.setMargin(new Insets(0, 0, 0, 0));
		zoomInButton.setHideActionText(true);
		zoomInButton.setAction(zoomInAction);
		// Use a more modest auto repeat for zoom
		zoomInButton.setAutoRepeatDelay(150);
		panel_1.add(zoomInButton);
		
		// JSlider zoomSlider = new JSlider();
		zoomSlider.setOrientation(SwingConstants.VERTICAL);
		zoomSlider.setMaximum(1000);
		// Kludge to get decent vertical JSlider on Windows
		zoomSlider.setPaintTicks(true);
		zoomSlider.setMajorTickSpacing(1000);
		//
		panel_1.add(zoomSlider);
		zoomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				int value = zoomSlider.getValue();
				double relativeZoom = value / 1000.0;
				// log scale
				double zoomMin = Math.log(getMinZoom()) / Math.log(2.0);
				double zoomMax = Math.log(getMaxZoom()) / Math.log(2.0);
				double zoom = zoomMin + relativeZoom * (zoomMax - zoomMin);
				zoom = Math.pow(2.0, zoom);
				camera.setPixelsPerSceneUnit(zoom);
			}
		});
		
		ToolButton zoomOutButton = new ToolButton(zoomOutAction);
		zoomOutButton.setAction(zoomOutAction);
		zoomOutButton.setMargin(new Insets(0, 0, 0, 0));
		zoomOutButton.setHideActionText(true);
		zoomOutButton.setAlignmentX(0.5f);
		zoomOutButton.setAutoRepeatDelay(150); // slow down auto zoom
		panel_1.add(zoomOutButton);
		
		JPanel buttonsPanel = new JPanel();
		controlsPanel.add(buttonsPanel);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		
		JButton btnNewButton_1 = new JButton("New button");
		btnNewButton_1.setAction(resetZoomAction);
		buttonsPanel.add(btnNewButton_1);
		
		JButton btnNewButton = new JButton("New button");
		btnNewButton.setAction(zoomMaxAction);
		buttonsPanel.add(btnNewButton);
		
		JButton resetViewButton = new JButton("New button");
		resetViewButton.setAction(resetViewAction);
		buttonsPanel.add(resetViewButton);
		
		Component verticalGlue = Box.createVerticalGlue();
		buttonsPanel.add(verticalGlue);
		
		JButton btnClearCache = new JButton("Clear Cache");
		btnClearCache.setAction(clearCacheAction);
		buttonsPanel.add(btnClearCache);
		
		Component verticalGlue_1 = Box.createVerticalGlue();
		buttonsPanel.add(verticalGlue_1);
		
		JButton autoContrastButton = new JButton();
		autoContrastButton.setAction(autoContrastAction);
		buttonsPanel.add(autoContrastButton);
		
		JButton resetColorsButton = new JButton();
		resetColorsButton.setAction(resetColorsAction);
		buttonsPanel.add(resetColorsButton);
		
        AnnotationPanel annotationPanel = new AnnotationPanel(annotationMgr, annotationModel);
        controlsPanel.add(annotationPanel);


		JPanel statusBar = new JPanel();
		statusBar.setMaximumSize(new Dimension(32767, 30));
		statusBar.setMinimumSize(new Dimension(10, 30));
		add(statusBar);
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
		
		statusBar.add(statusLabel);
		
        // For slice viewer popup menu.
        sliceViewer.setSystemMenuItemGenerator(new MenuItemGenerator() {
            @Override
            public List<JMenuItem> getMenus(MouseEvent event) {
                List<JMenuItem> result = new Vector<JMenuItem>();
                result.add(addFileMenuItem());
                result.add(addViewMenuItem());
                return result;
            }
        });
	}

	private void interceptModifierKeyPresses() 
	{ 
        // Intercept Shift key strokes at the highest level JComponent we can find.
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_DOWN_MASK, false),
        		"ModifierPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, KeyEvent.CTRL_DOWN_MASK, false),
        		"ModifierPressed");
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0, true),
				"ModifierReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true),
				"ModifierReleased");

        ActionMap actionMap = getActionMap();
        actionMap.put("ModifierPressed", new AbstractAction() 
        {
			private static final long serialVersionUID = 1L;
			@Override
            public void actionPerformed(ActionEvent e) {
                setModifierKeyPressed(true);
            }
        });
        actionMap.put("ModifierReleased", new AbstractAction() 
        {
			private static final long serialVersionUID = 1L;
			@Override
            public void actionPerformed(ActionEvent e) {
                setModifierKeyPressed(false);
            }
        });
	}
	
	private void interceptModeChangeGestures()
	{
        // Press "H" (hand) for Pan mode, etc.
        Action modeActions[] = {
        		panModeAction, 
        		zoomMouseModeAction, 
        		traceMouseModeAction,
        		zoomInAction,
        		zoomOutAction,
        		nextZSliceAction,
        		previousZSliceAction
        		};
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        for (Action action : modeActions) {
        	KeyStroke accelerator = (KeyStroke)action.getValue(Action.ACCELERATOR_KEY);
        	String actionName = (String)action.getValue(Action.NAME);
        	inputMap.put(accelerator, actionName);
        	getActionMap().put(actionName, action);
        }
	}

	private JPanel setupToolBar() {
		add(toolBarPanel);
		toolBarPanel.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar = new JToolBar();
		toolBarPanel.add(toolBar, BorderLayout.NORTH);
		
		JLabel lblNewLabel_1 = new JLabel("");
		lblNewLabel_1.setToolTipText("Mouse Mode:");
		lblNewLabel_1.setFocusable(false);
		lblNewLabel_1.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_left.png")));
		toolBar.add(lblNewLabel_1);

		// TODO - create a shared base class for these mode buttons
		JToggleButton traceMouseModeButton = new JToggleButton("Trace");
		mouseModeGroup.add(traceMouseModeButton);
		traceMouseModeButton.setAction(traceMouseModeAction);
		traceMouseModeButton.setMargin(new Insets(0, 0, 0, 0));
		traceMouseModeButton.setHideActionText(true);
		traceMouseModeButton.setFocusable(false);
		toolBar.add(traceMouseModeButton);
		
		JToggleButton tglBtnPanMode = new JToggleButton("");
		mouseModeGroup.add(tglBtnPanMode);
		tglBtnPanMode.setSelected(true);
		tglBtnPanMode.setAction(panModeAction);
		tglBtnPanMode.setMargin(new Insets(0, 0, 0, 0));
		tglBtnPanMode.setHideActionText(true);
		tglBtnPanMode.setFocusable(false);
		toolBar.add(tglBtnPanMode);
		
		JToggleButton tglbtnZoomMouseMode = new JToggleButton("");
		mouseModeGroup.add(tglbtnZoomMouseMode);
		tglbtnZoomMouseMode.setMargin(new Insets(0, 0, 0, 0));
		tglbtnZoomMouseMode.setFocusable(false);
		tglbtnZoomMouseMode.setHideActionText(true);
		tglbtnZoomMouseMode.setAction(zoomMouseModeAction);
		toolBar.add(tglbtnZoomMouseMode);

		toolBar.addSeparator();
		
		JLabel scrollModeLabel = new JLabel("");
		scrollModeLabel.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_scroll.png")));
		scrollModeLabel.setFocusable(false);
		toolBar.add(scrollModeLabel);
		
		JToggleButton toggleButton = new JToggleButton("");
		scrollModeGroup.add(toggleButton);
		toggleButton.setSelected(true);
		toggleButton.setAction(zScanScrollModeAction);
		toggleButton.setMargin(new Insets(0, 0, 0, 0));
		toggleButton.setHideActionText(true);
		toggleButton.setFocusable(false);
		toolBar.add(toggleButton);
		
		JToggleButton toggleButton_1 = new JToggleButton("");
		scrollModeGroup.add(toggleButton_1);
		toggleButton_1.setAction(zoomScrollModeAction);
		toggleButton_1.setMargin(new Insets(0, 0, 0, 0));
		toggleButton_1.setHideActionText(true);
		toggleButton_1.setFocusable(false);
		toolBar.add(toggleButton_1);
		
		toolBar.addSeparator();
		
		// Temporarily disable orthogonal mode button for next release
		// (until feature is done)
		if (bAllowOrthoView) {
			JButton orthogonalModeButton = new JButton("");
			orthogonalModeButton.setAction(orthogonalModeAction);
			orthogonalModeButton.setMargin(new Insets(0, 0, 0, 0));
			orthogonalModeButton.setHideActionText(true);
			orthogonalModeButton.setFocusable(false);
			toolBar.add(orthogonalModeButton);
		} else {
			// orthogonalModeButton.setEnabled(false);
		}
		
		return toolBarPanel;
	}

	private void setModifierKeyPressed(boolean pressed) 
	{
		// Has the status changed since last time?
		if (pressed == modifierKeyPressed)
			return; // no change
		modifierKeyPressed = pressed; // changed!
		// Shift to select zoom scroll mode
		if (pressed)
			zoomScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
		else if (zScanScrollModeAction.isEnabled())
			zScanScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
	}
	
	private boolean setZSlice(int z) {
		Vec3 oldFocus = camera.getFocus();
		int oldValue = (int)Math.round(oldFocus.getZ() / volumeImage.getZResolution() - 0.5);
		if (oldValue == z)
			return false; // camera is already pretty close
		double halfVoxel = 0.5 * volumeImage.getZResolution();
		double newZ = z * volumeImage.getZResolution() + halfVoxel;
		double minZ = volumeImage.getBoundingBox3d().getMin().getZ() + halfVoxel;
		double maxZ = volumeImage.getBoundingBox3d().getMax().getZ() - halfVoxel;
		newZ = Math.max(newZ, minZ);
		newZ = Math.min(newZ, maxZ);
		camera.setFocus(new Vec3(oldFocus.getX(), oldFocus.getY(), newZ));
		return true;
	}

    private void setupMenu(JFrame parentFrame, boolean overrideFrameMenuBar) {
        if (overrideFrameMenuBar) {
            parentFrame.setJMenuBar(menuBar);
            addFileMenuItem();
            addEditMenuItem();
            addViewMenuItem();
            addHelpMenuItem();
        }
        else {
            toolBarPanel.add(addViewMenuItem());
            // Listener for clicking on buttons
//            JPanel tempPanel = new JPanel();
//            tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.LINE_AXIS));
//            tempPanel.add(menuBar);
//            tempPanel.add(Box.createHorizontalGlue());
//            add(tempPanel);
        }
    }

    private JMenuItem addViewMenuItem() {
        JMenu mnView = new JMenu("View");
        menuBar.add(mnView);

        JMenu mnMouseMode = new JMenu("Mouse Mode");
        mnMouseMode.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_left.png")));
        mnView.add(mnMouseMode);

        JRadioButtonMenuItem panModeItem = new JRadioButtonMenuItem("New radio item");
        panModeItem.setSelected(true);
        panModeItem.setAction(panModeAction);
        mnMouseMode.add(panModeItem);

        JRadioButtonMenuItem zoomMouseModeItem = new JRadioButtonMenuItem("New radio item");
        zoomMouseModeItem.setAction(zoomMouseModeAction);
        mnMouseMode.add(zoomMouseModeItem);

        JRadioButtonMenuItem traceMouseModeItem = new JRadioButtonMenuItem();
        traceMouseModeItem.setAction(traceMouseModeAction);
        mnMouseMode.add(traceMouseModeItem);
        
        JMenu mnScrollMode = new JMenu("Scroll Mode");
        mnScrollMode.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/mouse_scroll.png")));
        mnView.add(mnScrollMode);

        JRadioButtonMenuItem rdbtnmntmNewRadioItem = new JRadioButtonMenuItem("New radio item");
        rdbtnmntmNewRadioItem.setSelected(true);
        rdbtnmntmNewRadioItem.setAction(zScanScrollModeAction);
        mnScrollMode.add(rdbtnmntmNewRadioItem);

        JRadioButtonMenuItem mntmNewMenuItem_2 = new JRadioButtonMenuItem("New menu item");
        mntmNewMenuItem_2.setAction(zoomScrollModeAction);
        mnScrollMode.add(mntmNewMenuItem_2);

        JSeparator separator = new JSeparator();
        mnView.add(separator);

        JMenu mnZoom = new JMenu("Zoom");
        mnView.add(mnZoom);

        mnZoom.add(resetZoomAction);
        mnZoom.add(zoomOutAction);
        mnZoom.add(zoomInAction);
        mnZoom.add(zoomMaxAction);

        JSeparator separator_1 = new JSeparator();
        mnView.add(separator_1);

        JMenu mnZScan = new JMenu("Z Scan");
        mnView.add(mnZScan);

        JMenuItem mntmNewMenuItem = new JMenuItem("New menu item");
        mntmNewMenuItem.setAction(goBackZSlicesAction);
        mnZScan.add(mntmNewMenuItem);

        JMenuItem menuItem_2 = new JMenuItem("New menu item");
        menuItem_2.setAction(previousZSliceAction);
        mnZScan.add(menuItem_2);

        JMenuItem menuItem_1 = new JMenuItem("New menu item");
        menuItem_1.setAction(nextZSliceAction);
        mnZScan.add(menuItem_1);

        JMenuItem menuItem = new JMenuItem("New menu item");
        menuItem.setAction(advanceZSlicesAction);
        mnZScan.add(menuItem);

        JSeparator separator_2 = new JSeparator();
        mnView.add(separator_2);

        JMenuItem mntmNewMenuItem_3 = new JMenuItem("New menu item");
        mntmNewMenuItem_3.setAction(resetColorsAction);
        mnView.add(mntmNewMenuItem_3);
        return mnView;
    }

    public JMenuItem addFileMenuItem() {
        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmNewMenuItem_1 = new JMenuItem("New menu item");
        mntmNewMenuItem_1.setAction(openFolderAction);
        mnFile.add(mntmNewMenuItem_1);

        JMenu recentFileMenu = new JMenu("Open Recent");
        recentFileMenu.setVisible(false);
        mnFile.add(recentFileMenu);
        recentFileList = new RecentFileList(recentFileMenu);
        recentFileList.getOpenUrlRequestedSignal().connect(loadUrlSlot);

        return mnFile;
    }

    public JMenuItem addEditMenuItem() {
        JMenu mnEdit = new JMenu("Edit");
        menuBar.add(mnEdit);
        return mnEdit;
    }

    public JMenuItem addHelpMenuItem() {
        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);
        return mnHelp;
    }

    public JPopupMenu getButtonPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(addFileMenuItem());
        popupMenu.add(addViewMenuItem());
        return popupMenu;
    }

    public boolean loadFile(String pathToFile) throws MalformedURLException {
        File tmpFile = new File(pathToFile);

        // Hard code temporary path translation for Nathan
        boolean fileMissing = ! tmpFile.exists();
        if (fileMissing) {
	        String osName = System.getProperty("os.name").toLowerCase();
	        String mbmPrefix = "/groups/mousebrainmicro/mousebrainmicro/";
	        boolean isLinuxMouseBrainPath = pathToFile.startsWith(mbmPrefix);
	    	List<String> prefixesToTry = new Vector<String>();
	    	if (osName.contains("win")) {
	        	prefixesToTry.add("M:/"); // On my Windows computer
	        	prefixesToTry.add("X:/"); // On Nathan's computer    		
	        	for (File fileRoot : File.listRoots()) { // other drive letters
	        		String p = fileRoot.getAbsolutePath();
	        		prefixesToTry.add(p);
	        		// System.out.println(p);
	        	}
	    	}
	    	if (osName.contains("os x")) {
	    		prefixesToTry.add("/Volumes/mousebrainmicro/");
	    	}
	        if ( (prefixesToTry.size() > 0)
	        		&& isLinuxMouseBrainPath ) 
	        {
	        	String fileSuffix = pathToFile.replace(mbmPrefix, "");
	        	for (String prefix : prefixesToTry) {
	        		File testFile = new File(prefix + fileSuffix);
	        		if (testFile.exists()) {
	        			tmpFile = testFile;
	        			break;
	        		}
	        	}
	        }
        }
        
        if (!tmpFile.exists()) {
            JOptionPane.showMessageDialog(this.getParent(),
                    "Error opening folder " + tmpFile.getName()
                    +" \nIs the file share mounted?",
                    "Folder does not exist.",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // July 1, 2013 elevate url loading from SliceViewer to QuadViewUi.
        URL url = tmpFile.toURI().toURL();
        return loadURL(url);
    }
    
    public boolean loadURL(URL url) {
    	// Check if url exists first...
    	try {
    		url.openStream();
        	return volumeImage.loadURL(url);
    	} catch (IOException exc) {
            JOptionPane.showMessageDialog(this.getParent(),
                    "Error opening folder " + url
                    +" \nIs the file share mounted?",
                    "Could not open folder",
                    JOptionPane.ERROR_MESSAGE);
            return false;
    	}
    }
    
    public Slot1<URL> onVolumeLoadedSlot = new Slot1<URL>() {
		@Override
		public void execute(URL url) {
			recentFileList.add(url);
			imageColorModel.reset(volumeImage);
			resetViewAction.actionPerformed(null);
		}
    };
}
