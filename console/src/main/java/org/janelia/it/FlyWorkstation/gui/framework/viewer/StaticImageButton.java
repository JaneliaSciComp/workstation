package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * An AnnotatedImageButton with a static icon or label.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StaticImageButton extends AnnotatedImageButton {

    private BufferedImage staticIcon;
    private JLabel label;

    public StaticImageButton(Entity entity) {
		super(entity);
	}

	public JComponent init(final Entity entity) {
    	this.label = new JLabel(Icons.getLoadingIcon());
    	return label;
    }
    
	public void rescaleImage(int imageSize) {
		super.rescaleImage(imageSize);
		if (staticIcon!=null) {
			if (imageSize<staticIcon.getHeight() || imageSize<staticIcon.getWidth()) { // Don't scale up icons
				ImageIcon newIcon = new ImageIcon(Utils.getScaledImage(staticIcon, imageSize));
	        	label.setIcon(newIcon);
			}
		}
		label.setPreferredSize(new Dimension(imageSize, imageSize));
		label.revalidate();
		label.repaint();
	}

	public void setViewable(boolean viewable) {
		if (viewable) {
	    	this.staticIcon = Icons.getLargeIconAsBufferedImage(entity);
	    	label.setIcon(new ImageIcon(staticIcon));
			IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
        	rescaleImage(iconDemoPanel.getImagesPanel().getCurrImageSize());
		}
		else {
			this.staticIcon = null;
	    	label.setIcon(Icons.getLoadingIcon());
		}
		label.revalidate();
		label.repaint();
	}
	
}
