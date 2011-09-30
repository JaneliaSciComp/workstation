package org.janelia.it.FlyWorkstation.gui.util;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.framework.console.MissingIcon;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Retrieve icons in the classpath by filename. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Icons {

    private static Icon missingIcon = new MissingIcon();

    public static Map<String,ImageIcon> cache = new HashMap<String,ImageIcon>();

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
    public static Icon getLoadingIcon() {
    	return getIcon("spinner.gif");
    }

    /**
     * Returns an icon for representing the "expand all" tree operation.
     * @return
     */
    public static Icon getExpandAllIcon() {
    	return getIcon("expand_all.png");
    }

    /**
     * Returns an icon for representing the "collapse all" tree operation.
     * @return
     */
    public static Icon getCollapseAllIcon() {
    	return getIcon("collapse_all.png");
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
	    	ImageIcon icon = Utils.getClasspathImage(filename);
	    	cache.put(filename, icon);
	    	return icon;
    	} 
    	catch (FileNotFoundException e) {
    		return null;
    	}
    }
    
    /**
     * Returns the correct icon for the given Ontology Element. 
     * @param entity
     * @return
     */
    public static ImageIcon getOntologyIcon(Entity entity) {
        String type = entity.getValueByAttributeName("Ontology Term Type");

        if ("Category".equals(type))
            return getIcon("folder.png");

        else if ("Enum".equals(type))
            return getIcon("folder_page.png");

        else if ("Interval".equals(type))
            return getIcon("page_white_code.png");

        else if ("Tag".equals(type))
            return getIcon("page_white.png");

        else if ("Text".equals(type))
            return getIcon("page_white_text.png");

        else if ("EnumItem".equals(type))
            return getIcon("page.png");

        return null;
    }

    /**
     * Returns an icon which represents the given entity. If the entity is an Ontology Element then this method
     * delegates to getOntologyIcon(). 
     * @param entity
     * @see getOntologyIcon()
     * @return
     */
    public static ImageIcon getIcon(Entity entity) {
        String type = entity.getEntityType().getName();
        
        if ("Folder".equals(type)) {
            return getIcon("folder.png");
        }
        else if ("LSM Stack Pair".equals(type)) {
            return getIcon("folder_image.png");
        }
        else if ("Neuron Separator Pipeline Result".equals(type)) {
            return getIcon("folder_image.png");
        }
        else if ("Sample".equals(type)) {
            return getIcon("beaker.png");
        }
        else if ("Tif 2D Image".equals(type)) {
            return getIcon("image.png");
        }
        else if ("Tif 3D Image".equals(type) || "LSM Stack".equals(type) || "Tif 3D Label Mask".equals(type) || "Stitched V3D Raw Stack".equals(type)) {
            return getIcon("images.png");
        }
        else if ("Neuron Fragment".equals(type)) {
            return getIcon("brick.png");
        }
        else if ("Supporting Data".equals(type)) {
            return getIcon("folder_page.png");
        }
        else if ("Ontology Element".equals(type) || "Ontology Root".equals(type)) {
            return getOntologyIcon(entity);
        }
        else if ("Annotation".equals(type)) {
            return getIcon("page_white_edit.png");
        }

        return null;
    }

    
}
