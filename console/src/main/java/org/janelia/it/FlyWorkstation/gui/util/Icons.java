package org.janelia.it.FlyWorkstation.gui.util;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.framework.console.MissingIcon;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
        
        if (EntityConstants.TYPE_FOLDER.equals(type)) {
            return getIcon("folder.png");
        }
        else if (EntityConstants.TYPE_LSM_STACK_PAIR.equals(type)) {
            return getIcon("folder_image.png");
        }
        else if (EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(type)) {
            return getIcon("folder_image.png");
        }
        else if (EntityConstants.TYPE_SAMPLE.equals(type)) {
            return getIcon("beaker.png");
        }
        else if (EntityConstants.TYPE_TIF_2D.equals(type)) {
            return getIcon("image.png");
        }
        else if (EntityConstants.TYPE_TIF_3D.equals(type) 
        		|| EntityConstants.TYPE_LSM_STACK.equals(type) 
        		|| EntityConstants.TYPE_TIF_3D_LABEL_MASK.equals(type) 
        		|| EntityConstants.TYPE_STITCHED_V3D_RAW.equals(type)) {
            return getIcon("images.png");
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)) {
            return getIcon("brick.png");
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(type)) {
            return getIcon("folder.png");
        }
        else if (EntityConstants.TYPE_SUPPORTING_DATA.equals(type)) {
            return getIcon("folder_page.png");
        }
        else if (EntityConstants.TYPE_ONTOLOGY_ELEMENT.equals(type) || EntityConstants.TYPE_ONTOLOGY_ROOT.equals(type)) {
            return getOntologyIcon(entity);
        }
        else if (EntityConstants.TYPE_ANNOTATION.equals(type)) {
            return getIcon("page_white_edit.png");
        }

        return null;
    }

    
}
