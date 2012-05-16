package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.JComponent;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * An AnnotatedImageButton with a dynamic image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImageButton extends AnnotatedImageButton {

    private DynamicImagePanel dynamicImagePanel;

    public DynamicImageButton(final RootedEntity rootedEntity, final IconDemoPanel iconDemoPanel) {
		super(rootedEntity, iconDemoPanel);
	}
    
    public JComponent init(final RootedEntity rootedEntity) {

    	String imageRole = iconDemoPanel.getCurrImageRole();
    	
        String filepath = EntityUtils.getDefaultImageFilePath(rootedEntity.getEntity(), imageRole);
        if (filepath == null) {
        	throw new IllegalStateException("Entity has no filepath");
        }
        
        File file = new File(PathTranslator.convertPath(filepath));
        this.dynamicImagePanel = new DynamicImagePanel(file.getAbsolutePath(), ImagesPanel.MAX_THUMBNAIL_SIZE) {
            protected void syncToViewerState() {
            	this.displaySize = iconDemoPanel.getImagesPanel().getCurrImageSize();
        		Boolean invertImages = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
        				ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
                if (invertImages!=null && invertImages) {
                	setInvertedColors(true);
                }
                else {
                	rescaleImage(iconDemoPanel.getImagesPanel().getCurrImageSize());
                }
            }
        };
        return dynamicImagePanel;
    }
    
	public void cancelLoad() {
		dynamicImagePanel.cancelLoad();
	}

	public void setCache(ImageCache imageCache) {
		dynamicImagePanel.setCache(imageCache);
	}

	public void rescaleImage(int imageSize) {
		super.rescaleImage(imageSize);
		dynamicImagePanel.rescaleImage(imageSize);
    	dynamicImagePanel.setPreferredSize(new Dimension(imageSize, imageSize));
	}

	public void setInvertedColors(boolean inverted) {
		dynamicImagePanel.setInvertedColors(inverted);
	}

	public void setViewable(boolean viewable) {
        dynamicImagePanel.setViewable(viewable, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// This is a bit of a hack. Whenever an image loads, check if its the image expected in the HUD, and
				// updated the HUD if necessary.
				if (rootedEntity.getEntity().getId().equals(iconDemoPanel.getHud().getEntityId())) {
					iconDemoPanel.getHud().setTitle(getRootedEntity().getEntity().getName());
					iconDemoPanel.getHud().setImage(dynamicImagePanel.getMaxSizeImage());
				}
				return null;
			}
        	
		});
	}

	public DynamicImagePanel getDynamicImagePanel() {
		return dynamicImagePanel;
	}	
}
