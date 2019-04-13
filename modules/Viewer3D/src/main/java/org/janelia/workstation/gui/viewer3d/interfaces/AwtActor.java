package org.janelia.workstation.gui.viewer3d.interfaces;

import java.awt.Graphics2D;

public interface AwtActor {
	boolean isVisible();
	void paint(Graphics2D g);
	void setVisible(boolean visible);
}
