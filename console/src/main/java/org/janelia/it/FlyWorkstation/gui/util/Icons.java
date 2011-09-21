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
    	try {
    		return getIcon("spinner.gif");
    	}
    	catch (FileNotFoundException e) {
    		return null;
    	}
    }

    /**
     * Returns an icon for representing the "expand all" tree operation.
     * @return
     */
    public static Icon getExpandAllIcon() {
    	try {
        	return getIcon("expand_all.png");
    	}
    	catch (FileNotFoundException e) {
    		return null;
    	}
    }

    /**
     * Returns an icon for representing the "collapse all" tree operation.
     * @return
     */
    public static Icon getCollapseAllIcon() {
    	try {
        	return getIcon("collapse_all.png");
    	}
    	catch (FileNotFoundException e) {
    		return null;
    	}
    }
    
    /**
     * Returns the icon with the given filename. This method caches icons locally, so successive calls will 
     * return the cached image.
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    public static ImageIcon getIcon(String filename) throws FileNotFoundException {
    	if (cache.containsKey(filename)) return cache.get(filename);
    	ImageIcon icon = Utils.getClasspathImage(filename);
    	cache.put(filename, icon);
    	return icon;
    }
    
    /**
     * Returns the correct icon for the given Ontology Element. 
     * @param entity
     * @return
     */
    public static ImageIcon getOntologyIcon(Entity entity) {
        String termType = entity.getValueByAttributeName("Ontology Term Type");

        try {
            if (termType=="Category")
                return getIcon("folder.png");

            else if (termType=="Enum")
                return getIcon("folder_page.png");

            else if (termType=="Interval")
                return getIcon("page_white_code.png");

            else if (termType=="Tag")
                return getIcon("page_white.png");

            else if (termType=="Text")
                return getIcon("page_white_text.png");

            else if (termType=="EnumItem")
                return getIcon("page.png");
        }
        catch (FileNotFoundException e) {
        	e.printStackTrace();
        }

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

        try {
	        if (type=="Folder") {
	            return getIcon("folder.png");
	        }
	        else if (type=="LSM Stack Pair") {
	            return getIcon("folder_image.png");
	        }
	        else if (type=="Neuron Separator Pipeline Result") {
	            return getIcon("folder_image.png");
	        }
	        else if (type=="Sample") {
	            return getIcon("beaker.png");
	        }
	        else if (type=="Tif 2D Image") {
	            return getIcon("image.png");
	        }
	        else if (type=="Tif 3D Image" || type=="LSM Stack" || type=="Tif 3D Label Mask" || type=="Stitched V3D Raw Stack") {
	            return getIcon("images.png");
	        }
	        else if (type=="Neuron Fragment") {
	            return getIcon("brick.png");
	        }
	        else if (type=="Supporting Data") {
	            return getIcon("folder_page.png");
	        }
	        else if (type=="Ontology Element" || type=="Ontology Root") {
	            return getOntologyIcon(entity);
	        }
	        else if (type=="Annotation") {
	            return getIcon("page_white_edit.png");
	        }
        }
        catch (FileNotFoundException e) {
        	e.printStackTrace();
        }

        return null;
    }

    
}
