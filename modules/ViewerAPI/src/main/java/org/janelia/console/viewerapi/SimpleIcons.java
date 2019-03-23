package org.janelia.console.viewerapi;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * Retrieve icons in the class path by filename. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleIcons {

    public static final Map<String,ImageIcon> cache = new HashMap<>();
    public static final Map<String,BufferedImage> imageCache = new HashMap<>();

    /**
     * Returns the icon with the given filename. This method caches icons locally, so successive calls will 
     * return the cached image.
     * @param filename
     * @return
     */
    public static ImageIcon getIcon(String filename) {
    	if (cache.containsKey(filename)) return cache.get(filename);
    	try {
	    	ImageIcon icon = getClasspathImage(filename);
	    	cache.put(filename, icon);
	    	return icon;
    	} 
    	catch (FileNotFoundException e) {
    		return null;
    	}
    }
    
    /**
     * Load an image that is found in the /images directory within the classpath.
     * Borrowed from Utils class from main console module.  Any further
     * borrowings imply a need to move Utils down to this module.
     * LLF
     */
    public static ImageIcon getClasspathImage(String filename) throws FileNotFoundException {
        try {
            URL picURL = SimpleIcons.class.getResource("/images/" + filename);
            return new ImageIcon(picURL);
        }
        catch (Exception e) {
            throw new FileNotFoundException("/images/" + filename);
        }
    }

}
