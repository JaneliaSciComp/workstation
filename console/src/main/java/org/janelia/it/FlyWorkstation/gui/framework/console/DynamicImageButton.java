package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.JComponent;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * An AnnotatedImageButton with a dynamic image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicImageButton extends AnnotatedImageButton {

    private DynamicImagePanel dynamicImagePanel;

    public DynamicImageButton(Entity entity) {
		super(entity);
	}
    
    public JComponent init(final Entity entity) {

        String filepath = Utils.getDefaultImageFilePath(entity);
        if (filepath == null) {
        	throw new IllegalStateException("Entity has no filepath");
        }
        
        File file = new File(PathTranslator.convertImagePath(filepath));
        this.dynamicImagePanel = new DynamicImagePanel(file.getAbsolutePath(), ImagesPanel.MAX_THUMBNAIL_SIZE);
        return dynamicImagePanel;
    }
    
	public void cancelLoad() {
		dynamicImagePanel.cancelLoad();
	}

	public void setCache(ImageCache imageCache) {
		dynamicImagePanel.setCache(imageCache);
	}

	public void rescaleImage(int imageSize) {
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
				if (dynamicImagePanel.isViewable()) {
					// TODO: refactor this so it doesn't need to do this kind of dependency access
					IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
			        if (iconDemoPanel.isInverted()) {
			        	dynamicImagePanel.setInvertedColors(true);
			        }
			        else {
			        	dynamicImagePanel.rescaleImage(iconDemoPanel.getImagesPanel().getCurrImageSize());
			        }
				}
				return null;
			}
		});
	}	
}
