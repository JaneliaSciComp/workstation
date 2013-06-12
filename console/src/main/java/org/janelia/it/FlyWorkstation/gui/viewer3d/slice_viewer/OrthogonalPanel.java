package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;

import com.jogamp.newt.event.KeyEvent;

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
	private SharedVolumeImage volume;

	public OrthogonalPanel(CoordinateAxis axis) {
		this.axis = axis;
		viewer = new OrthogonalViewer(axis);
		spinner.setModel(spinnerNumberModel);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(viewer);
		scanPanel.setLayout(new BoxLayout(scanPanel, BoxLayout.X_AXIS));
		scanPanel.add(new JLabel(" "+axis.getName()));
		// TODO add buttons to scan panel
		scanPanel.add(new ToolButton(new PreviousSliceAction()));
		slider.setMajorTickSpacing(10);
		slider.setPaintTicks(true); // Avoid windows slider display bug
		scanPanel.add(slider);
		scanPanel.add(new ToolButton(new NextSliceAction()));
		spinner.setPreferredSize(new Dimension(
				60, // good value for showing up to thousands place
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
			public void stateChanged(ChangeEvent arg0) {
				setSlice((Integer)spinner.getValue());
			}
		});
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
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
			double sMin = bb.getMin().get(ix);
			double sMax = bb.getMax().get(ix);
			double res = volume.getResolution(ix);
			int s0 = (int)Math.round(sMin / res);
			int s1 = (int)Math.round(sMax / res) - 1;
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
				slider.setMajorTickSpacing(tickSpacing);
			}
			slider.setMinimum(s0);
			slider.setMaximum(s1);
			spinnerNumberModel.setMinimum(s0);
			spinnerNumberModel.setMaximum(s1);
			// Update slice value
			int s = (int)Math.round(camera.getFocus().get(ix) / res);
			slider.setValue(s);
			spinner.setValue(s);
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
	
	public void setSharedVolumeImage(SharedVolumeImage volumeImage3d) {
		if (this.volume == volumeImage3d)
			return; // no change
		this.volume = volumeImage3d;
		viewer.setVolumeImage3d(volumeImage3d);
		volume.volumeInitializedSignal.connect(setSliceSlot);
	}
	
	private boolean setSlice(int s) {
		if (camera == null)
			return false;
		if (volume == null)
			return false;
		Vec3 oldFocus = camera.getFocus();
		int ix = axis.index();
		int oldValue = (int)Math.round(oldFocus.get(ix) / volume.getResolution(ix));
		if (oldValue == s)
			return false; // camera is already pretty close
		double newS = s * volume.getResolution(ix);
		double minS = volume.getBoundingBox3d().getMin().get(ix);
		double maxS = volume.getBoundingBox3d().getMax().get(ix);
		newS = Math.max(newS, minS);
		newS = Math.min(newS, maxS);
		Vec3 newFocus = new Vec3(oldFocus.x(), oldFocus.y(), oldFocus.z());
		newFocus.set(ix, newS);
		camera.setFocus(newFocus);
		return true;
	}

	class IncrementSliceAction extends AbstractAction {
		int increment;

		IncrementSliceAction(int increment) {
			this.increment = increment;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			incrementSlice(increment);
		}
	}
	
	class PreviousSliceAction extends IncrementSliceAction {
		PreviousSliceAction() {
			super(-1);
			putValue(NAME, "Previous "+axis.getName()+" Slice");
			putValue(SMALL_ICON, Icons.getIcon("z_stack_up.png"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_PAGE_UP);
			KeyStroke accelerator = KeyStroke.getKeyStroke(
				KeyEvent.VK_PAGE_UP, 0);
			putValue(ACCELERATOR_KEY, accelerator);
			putValue(SHORT_DESCRIPTION,
					"View previous "+axis.getName()+" slice"
					+"\n (Shortcut: "+accelerator+")"
					);		
		}
	}
	
	class NextSliceAction extends IncrementSliceAction {
		NextSliceAction() {
			super(1);
			putValue(NAME, "Next "+axis.getName()+" Slice");
			putValue(SMALL_ICON, Icons.getIcon("z_stack_down.png"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_PAGE_DOWN);
			KeyStroke accelerator = KeyStroke.getKeyStroke(
				KeyEvent.VK_PAGE_DOWN, 0);
			putValue(ACCELERATOR_KEY, accelerator);
			putValue(SHORT_DESCRIPTION,
					"View next "+axis.getName()+" slice"
					+"\n (Shortcut: "+accelerator+")"
					);		
		}
	}
}
