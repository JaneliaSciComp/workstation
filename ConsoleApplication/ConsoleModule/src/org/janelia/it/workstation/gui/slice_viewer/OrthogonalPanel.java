package org.janelia.it.workstation.gui.slice_viewer;

import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.slice_viewer.action.MouseMode;
import org.janelia.it.workstation.gui.slice_viewer.action.WheelMode;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.signal.Slot;
import org.janelia.it.workstation.signal.Slot1;

/*
 * GUI widget combining a slice viewer with a slice slider.
 * Intended for use as one of a series of X/Y/Z orthogonal slice views.
 */
public class OrthogonalPanel 
extends JPanel
{
    private JPanel scanPanel = new JPanel();
	private JSlider slider = new JSlider();
	private JSpinner spinner = new JSpinner();
	private SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel();
	private CoordinateAxis axis;	
	private ObservableCamera3d camera;
	private SharedVolumeImage volume;

	private OrthogonalViewer viewer;
	private ViewTileManager viewTileManager;
	private TileServer tileServer;
	
	public Slot1<MouseMode.Mode> setMouseModeSlot = new Slot1<MouseMode.Mode>() 
	{
        @Override
        public void execute(MouseMode.Mode modeId) {
            viewer.setMouseMode(modeId);
        }
	};
	
    public Slot1<WheelMode.Mode> setWheelModeSlot = new Slot1<WheelMode.Mode>() 
    {
        @Override
        public void execute(WheelMode.Mode modeId) {
            viewer.setWheelMode(modeId);
        }
    };
    
    public OrthogonalPanel(CoordinateAxis axis) {
		this.axis = axis;
		viewer = new OrthogonalViewer(axis);
		init();
	}	
	
	public OrthogonalPanel(CoordinateAxis axis, GLContextSharer contextSharer) {
		this.axis = axis;
		viewer = new OrthogonalViewer(axis,
				contextSharer.getCapabilities(),
				contextSharer.getChooser(),
				contextSharer.getContext());
		init();
	}

	private void init() {
		viewTileManager = new ViewTileManager(viewer);
		viewer.setSliceActor(new SliceActor(viewTileManager));
		spinner.setModel(spinnerNumberModel);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(viewer);
		scanPanel.setLayout(new BoxLayout(scanPanel, BoxLayout.X_AXIS));
		scanPanel.add(new JLabel(" "+axis.getName()));
		scanPanel.add(new ToolButton(new OrthogonalViewer.PreviousSliceAction(viewer)));
		slider.setMajorTickSpacing(10);
		slider.setPaintTicks(true); // Avoid windows slider display bug
		scanPanel.add(slider);
		scanPanel.add(new ToolButton(new OrthogonalViewer.NextSliceAction(viewer)));
		spinner.setPreferredSize(new Dimension(
				73, // good value for showing up to ten thousands place
				spinner.getPreferredSize().height));
		spinner.setMaximumSize(new Dimension(
				spinner.getPreferredSize().width,
				spinner.getPreferredSize().height));
		spinner.setMinimumSize(new Dimension(
				spinner.getPreferredSize().width,
				spinner.getPreferredSize().height));
		scanPanel.add(spinner);
		add(scanPanel);
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				setSlice((Integer)spinner.getValue());
			}
		});
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				setSlice(slider.getValue());
			}
		});
	}
	
	/**
	 * Update slider and spinner after camera motion
	 */
	public Slot setSliceSlot = new Slot() {
		@Override
		public void execute() {
			if (volume == null)
				return;
			if (camera == null)
				return;
			// Update ranges of slider and spinner
			BoundingBox3d bb = volume.getBoundingBox3d();
			int ix = axis.index(); // X, Y, or Z
            double res = volume.getResolution(ix);
            double halfVoxel = 0.5 * res;
			double sMin = bb.getMin().get(ix) + halfVoxel;
			double sMax = bb.getMax().get(ix) - halfVoxel;
			int s0 = (int)Math.round(sMin / res - 0.5);
			int s1 = (int)Math.round(sMax / res - 0.5);
			if (s0 > s1)
				s1 = s0;
			// Need at least 2 slices for slice scanning to be relevant
			boolean bShowScan = ((s1 - s0) > 1);
			if (! bShowScan)
				return;
			boolean bRangeChanged = false;
			if (s0 != slider.getMinimum())
				bRangeChanged = true;
			if (s1 != slider.getMaximum())
				bRangeChanged = true;
			if (bRangeChanged) {
				scanPanel.setVisible(bShowScan);
				int range = s1 - s0;
				int tickSpacing = 10;
				if (range > 1000)
					tickSpacing = 100;
				if (range > 10000)
					tickSpacing = 1000;
				slider.setMajorTickSpacing(tickSpacing);
			}
			slider.setMinimum(s0);
			slider.setMaximum(s1);
			spinnerNumberModel.setMinimum(s0);
			spinnerNumberModel.setMaximum(s1);
			// Update slice value
			int s = (int)Math.round(camera.getFocus().get(ix) / res - 0.5);
			if (s < s0) s = s0;
			if (s > s1) s = s1;
			slider.setValue(s);
			spinner.setValue(s);
		}
	};

	public void setCamera(ObservableCamera3d camera) {
		this.camera = camera;
		viewer.setCamera(camera);
		camera.getFocusChangedSignal().connect(
				setSliceSlot);
	}
	
	public void setSharedVolumeImage(SharedVolumeImage volumeImage3d) {
		if (this.volume == volumeImage3d)
			return; // no change
		this.volume = volumeImage3d;
		viewer.setVolumeImage3d(volumeImage3d);
		volume.volumeInitializedSignal.connect(setSliceSlot);
		viewTileManager.setVolumeImage(volumeImage3d);
	}
	
	public void setTileServer(TileServer tileServer) {
		if (this.tileServer == tileServer)
			return; // no change
		viewTileManager.setTextureCache(tileServer.getTextureCache());
		tileServer.addViewTileManager(viewTileManager);
		//
		viewer.setTileServer(tileServer);
		viewer.setTileServer(tileServer);
	}
	
	public void setSystemMenuItemGenerator(MenuItemGenerator systemMenuItemGenerator)
	{
		viewer.setSystemMenuItemGenerator(systemMenuItemGenerator);
	}

	private boolean setSlice(int s) {
		if (camera == null)
			return false;
		if (volume == null)
			return false;
		Vec3 oldFocus = camera.getFocus();
		int ix = axis.index();
		int oldValue = (int)Math.round(oldFocus.get(ix) / volume.getResolution(ix) - 0.5);
		if (oldValue == s)
			return false; // camera is already pretty close
		double halfVoxel = 0.5 * volume.getResolution(ix);
		double newS = s * volume.getResolution(ix) + halfVoxel;
		double minS = volume.getBoundingBox3d().getMin().get(ix) + halfVoxel;
		double maxS = volume.getBoundingBox3d().getMax().get(ix) - halfVoxel;
		newS = Math.max(newS, minS);
		newS = Math.min(newS, maxS);
		Vec3 newFocus = new Vec3(oldFocus.x(), oldFocus.y(), oldFocus.z());
		newFocus.set(ix, newS);
		camera.setFocus(newFocus);
		return true;
	}
	
	public JSlider getSlider() {
		return slider;
	}

	public ViewTileManager getViewTileManager() {
		return viewTileManager;
	}

	public void setSlider(JSlider slider) {
		this.slider = slider;
	}

    public OrthogonalViewer getViewer() {
        return viewer;
    }

}
