package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Graphics2D;
import java.awt.Image;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.AwtActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;

/**
 * Paints a cross-hair to indicate the exact center of the view
 * @author brunsc
 *
 */
public class ReticleActor implements AwtActor {
	private boolean visible = true;
	private Viewport viewport;
	private Image reticleImage;

	public ReticleActor(Viewport viewport) {
		this.viewport = viewport;
		reticleImage = Icons.getIcon("center_crosshair.png").getImage();
	}
	
	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void paint(Graphics2D g) {
		if (! isVisible())
			return;
		int x = viewport.getWidth()/2 - 8;
		int y = viewport.getHeight()/2 - 8;
		g.drawImage(reticleImage, x, y, null);
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

}
