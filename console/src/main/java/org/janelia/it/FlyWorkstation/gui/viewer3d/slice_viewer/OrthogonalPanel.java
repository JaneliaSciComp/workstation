package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

/*
 * GUI widget combining a slice viewer with a slice slider.
 * Intended for use as one of a series of X/Y/Z orthogonal slice views.
 */
public class OrthogonalPanel 
extends JPanel
{
	private JPanel scanPanel = new JPanel();
	private OrthogonalViewer viewer;
	private JSlider slider = new JSlider();
	private JSpinner spinner = new JSpinner();
	private SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel();
	private CoordinateAxis axis;	
	private ObservableCamera3d camera;
	private VolumeImage3d volume;
	
	public OrthogonalPanel(CoordinateAxis axis) {
		this.axis = axis;
		viewer = new OrthogonalViewer(axis);
		spinner.setModel(spinnerNumberModel);
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
			double sMin = bb.getMin().get(ix);
			double sMax = bb.getMax().get(ix);
			double res = volume.getResolution(ix);
			int s0 = (int)Math.round(sMin / res);
			int s1 = (int)Math.round(sMax / res) - 1;
			if (s0 > s1)
				s1 = s0;
			// Need at least 2 slices for slice scanning to be relevant
			boolean bShowScan = ((s0 - s1) > 1);
			scanPanel.setVisible(bShowScan);
			if (bShowScan) {
				slider.setMinimum(s0);
				slider.setMaximum(s1);
				spinnerNumberModel.setMinimum(s0);
				spinnerNumberModel.setMaximum(s1);
				// Update slice value
				int s = (int)Math.round(camera.getFocus().get(ix) / res);
				slider.setValue(s);
				spinner.setValue(s);
			}
		}
	};

	public void incrementSlice(int sliceCount) {
		if (sliceCount == 0)
			return;
		if (camera == null)
			return;
		if (volume == null)
			return;
		int ix = axis.index(); // X, Y, or Z
		double res = volume.getResolution(ix);
		Vec3 dFocus = new Vec3(0,0,0);
		dFocus.set(ix, sliceCount * res);
		camera.setFocus(camera.getFocus().plus(dFocus));
	}
	
	public void setCamera(ObservableCamera3d camera) {
		this.camera = camera;
		viewer.setCamera(camera);
		camera.getFocusChangedSignal().connect(
				setSliceSlot);
	}
	
	public void setVolumeImage3d(VolumeImage3d volumeImage3d) {
		this.volume = volumeImage3d;
		viewer.setVolumeImage3d(volumeImage3d);
	}
}
