package org.janelia.it.workstation.browser.gui.support;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.janelia.it.workstation.browser.gui.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieve icons in the class path by filename. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Icons {

    private static final Logger log = LoggerFactory.getLogger(Icons.class);
    
    private static final Icon missingIcon = new MissingIcon();

    public static final Map<String,ImageIcon> cache = new HashMap<>();
    public static final Map<String,BufferedImage> imageCache = new HashMap<>();

    /**
     * Returns an animated icon for representing a missing image.
     * @return
     */
    public static Icon getMissingIcon() {
    	return missingIcon;
    }

    /**
     * Returns an animated icon for representing a data loading state.
     * @return
     */
    public static ImageIcon getLoadingIcon() {
    	return getIcon("loader.gif");
    }

    /**
     * Returns an icon for representing the "expand all" tree operation.
     * @return
     */
    public static ImageIcon getExpandAllIcon() {
    	return getIcon("expand_all.png");
    }

    /**
     * Returns an icon for representing the "collapse all" tree operation.
     * @return
     */
    public static ImageIcon getCollapseAllIcon() {
    	return getIcon("collapse_all.png");
    }
    
    /**
     * Returns an icon for representing the "refresh" operation.
     * @return
     */
    public static ImageIcon getRefreshIcon() {
    	return getIcon("refresh.png");
    }
    
    /**
     * Returns the icon with the given filename. This method caches icons locally, so successive calls will 
     * return the cached image.
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    public static ImageIcon getIcon(String filename) {
    	if (cache.containsKey(filename)) return cache.get(filename);
    	try {
	    	ImageIcon icon = UIUtils.getClasspathImage(filename);
	    	cache.put(filename, icon);
	    	return icon;
    	} 
    	catch (FileNotFoundException e) {
            log.error("Error loading icon from classpath: "+filename, e);
    		return null;
    	}
    }
    
    public static BufferedImage getImage(String filename) {
        String imagePath = "/images/" + filename;
        String key = imagePath;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        try {
            BufferedImage image = ImageIO.read(Icons.class.getResourceAsStream(imagePath));
            imageCache.put(key, image);
            return image;
        }
        catch (IOException e) {
            log.error("Error loading image from classpath: "+filename, e);
            return null;
        }
    }
}
