package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * An AnnotatedImageButton with a static icon or label.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StaticImageButton extends AnnotatedImageButton {

    private BufferedImage staticIcon;
    private JLabel label;

    public StaticImageButton(final RootedEntity rootedEntity, final IconDemoPanel iconDemoPanel) {
		super(rootedEntity, iconDemoPanel);
	}

	public JComponent init(final RootedEntity rootedEntity) {
    	this.label = new JLabel(Icons.getLoadingIcon());
    	return label;
    }
    
	public void rescaleImage(int width, int height) {
		super.rescaleImage(width, height);
		if (staticIcon!=null) {
			if (width<=staticIcon.getWidth()) { // Don't scale up icons
	        	label.setIcon(new ImageIcon(Utils.getScaledImage(staticIcon, width)));
			}
			else {
				label.setIcon(new ImageIcon(staticIcon));
			}
		}
		label.setPreferredSize(new Dimension(width, height));
		label.revalidate();
		label.repaint();
	}

	public void setViewable(boolean viewable) {
		if (viewable) {
	    	this.staticIcon = Icons.getLargeIconAsBufferedImage(rootedEntity.getEntity());
	    	
			// Register our aspect ratio
			double w = label.getIcon().getIconWidth();
			double h = label.getIcon().getIconHeight();
			setAspectRatio(w, h);
			
			int width = iconDemoPanel.getImagesPanel().getCurrImageSize();
			if (width<=staticIcon.getWidth()) { // Don't scale up icons
				label.setIcon(new ImageIcon(Utils.getScaledImage(staticIcon, width)));	
			}
			else {
				label.setIcon(new ImageIcon(staticIcon));	
			}
		}
		else {
			this.staticIcon = null;
	    	label.setIcon(Icons.getLoadingIcon());
		}
		
		label.revalidate();
		label.repaint();
	}
	
}
