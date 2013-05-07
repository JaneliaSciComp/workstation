package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

/**
 * Base class for z slice scanning actions in slice viewer.
 * 
 * @author Christopher M. Bruns
 *
 */
public class ZScanAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	private VolumeImage3d image;
	private Camera3d camera;
	private int sliceCount;
	
	ZScanAction(VolumeImage3d image, Camera3d camera, int sliceCount)
	{
		this.image = image;
		this.camera = camera;
		this.sliceCount = sliceCount;		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		Vec3 oldFocus = camera.getFocus();
		double oldZ = oldFocus.getZ();
		int oldZIndex = (int)(Math.round(oldZ / image.getZResolution()) + 0.1);
		int newZIndex = oldZIndex + sliceCount;
		// Scoot to next multiple of 10, for performance
		if (Math.abs(sliceCount) > 1) {
			int dz = newZIndex % sliceCount;
			if (dz > sliceCount/2.0)
				dz -= sliceCount;
			newZIndex -= dz; // newZIndex is now a multiple of sliceCount
			assert newZIndex % sliceCount == 0;
		}
		double newZ = newZIndex * image.getZResolution();
		double maxZ = image.getBoundingBox3d().getMax().getZ();
		double minZ = image.getBoundingBox3d().getMin().getZ();
		assert maxZ >= minZ;
		if (newZ > maxZ)
			newZ = maxZ;
		if (newZ < minZ)
			newZ = minZ;
		if (newZ == oldZ)
			return; // no change
		Vec3 newFocus = new Vec3(oldFocus.getX(), oldFocus.getY(), newZ);
		// TODO - disallow camera.getFocus().setZ(), which bypasses camera signaling
		camera.setFocus(newFocus);
	}

}
