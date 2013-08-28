package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Dimension;
import java.util.concurrent.Callable;

import javax.swing.JComponent;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * An AnnotatedImageButton with a dynamic image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImageButton extends AnnotatedImageButton {

    private DynamicImagePanel dynamicImagePanel;

    public DynamicImageButton(final RootedEntity rootedEntity, final IconPanel iconPanel) {
		super(rootedEntity, iconPanel);
	}
    
    public JComponent init(final RootedEntity rootedEntity) {

    	String imageRole = iconPanel.getCurrImageRole();
    	
        String filepath = EntityUtils.getImageFilePath(rootedEntity.getEntity(), imageRole);
        if (filepath == null) {
        	throw new IllegalStateException("Entity has no filepath");
        }

        // send original file path so that file path translation or local caching occurs
        // asynchronously within the the load image worker
        this.dynamicImagePanel = new DynamicImagePanel(filepath, ImagesPanel.MAX_IMAGE_WIDTH) {
            protected void syncToViewerState() {
            	this.displaySize = iconPanel.getImagesPanel().getMaxImageWidth();
        		Boolean invertImages = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
        				ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
                if (invertImages!=null && invertImages) {
                	setInvertedColors(true);
                }
            }
        };
        return dynamicImagePanel;
    }
    
	public void cancelLoad() {
		dynamicImagePanel.cancelLoad();
	}

	public void setImageSize(int width, int height) {
		super.setImageSize(width, height);
		dynamicImagePanel.rescaleImage(width);
    	dynamicImagePanel.setPreferredSize(new Dimension(width, height));
	}

	public void setInvertedColors(boolean inverted) {
		dynamicImagePanel.setInvertedColors(inverted);
	}

	public void setViewable(boolean viewable) {
		super.setViewable(viewable);
        dynamicImagePanel.setViewable(viewable, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Register our image height
                if (dynamicImagePanel.getMaxSizeImage()!=null && dynamicImagePanel.getImage()!=null) {
                    double w = dynamicImagePanel.getImage().getIconWidth();
                    double h = dynamicImagePanel.getImage().getIconHeight();
                    registerAspectRatio(w, h);
                }
                return null;
            }

        });
	}
}
