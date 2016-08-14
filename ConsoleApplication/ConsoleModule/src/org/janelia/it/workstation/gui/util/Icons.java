package org.janelia.it.workstation.gui.util;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
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

    public static final Map<String,ImageIcon> cache = new HashMap<String,ImageIcon>();
    public static final Map<String,BufferedImage> imageCache = new HashMap<String,BufferedImage>();

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
    	return getIcon("spinner.gif");
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

        else if ("Custom".equals(type))
            return getIcon("page_white_text.png");

        else if ("EnumItem".equals(type))
            return getIcon("page.png");

        else if ("EnumText".equals(type))
            return getIcon("page_go.png");
        
        return getIcon("bullet_error.png");
    }

    /**
     * Returns a small icon which represents the given entity. If the entity is an Ontology Element then this method
     * delegates to getOntologyIcon(). 
     * @param entity
     * @see getOntologyIcon()
     * @return
     */
    public static ImageIcon getIcon(Entity entity) {
    	return getIcon(entity, false);
    }

    /**
     * @param entity
     * @return
     */
    public static BufferedImage getLargeIconAsBufferedImage(Entity entity) {
        String type = entity.getEntityTypeName();
    	if (imageCache.containsKey(type)) {
    		return imageCache.get(type);
    	}
        ImageIcon icon = getIcon(entity, true);
        if (icon==null) return null;
    	BufferedImage bi = Utils.toBufferedImage(icon.getImage());
    	imageCache.put(type, bi);
    	return bi;
    }

    /**
     * Returns an icon which represents the given entity. If the entity is an Ontology Element then this method
     * delegates to getOntologyIcon(). 
     * @param entity
     * @param large format icon?
     * @return
     */
    public static ImageIcon getIcon(Entity entity, boolean large) {
        
        if (entity.getEntityTypeName()==null) {
            return getIcon(large ? "error_large.png" : "bullet_error.png");
        }
        
        String type = entity.getEntityTypeName();
        
        if (EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE.equals(type)) {
        	if (large) return getIcon("workspace_large.png");
        	return getIcon("workspace.png");    
        }
        else if (EntityConstants.TYPE_TILE_MICROSCOPE_NEURON.equals(type)) {
        	if (large) return getIcon("monitor_large.png");
        	return getIcon("monitor.png");    
        }
        else if (EntityConstants.TYPE_PROPERTY_SET.equals(type)) {
        	if (large) return getIcon("properties_large.png");
        	return getIcon("properties.png");    
        }
        else if (EntityConstants.TYPE_WORKSPACE.equals(type)) {
        	if (large) return getIcon("folder_large.png");
        	return getIcon("folder_green.png");    
        }
        else if (EntityConstants.TYPE_FOLDER.equals(type) || EntityConstants.TYPE_ONTOLOGY_ROOT.equals(type)) {
            if (large) return getIcon("folder_large.png");
            
            String typeSuffix = "";
            if (EntityUtils.isProtected(entity)) {
                if (entity.getName().equals(EntityConstants.NAME_DATA_SETS)) {
                    typeSuffix = "_database";
                }
                else if (entity.getName().equals(EntityConstants.NAME_SHARED_DATA)) {
                    typeSuffix = "_user";
                }
                else {
                    typeSuffix = "_key";
                }
            }
            
            if (entity.getOwnerKey()!=null && !ModelMgrUtils.isOwner(entity)) {
                return getIcon("folder_blue"+typeSuffix+".png");
            }
            else {
                if (entity.getName().equals(EntityConstants.NAME_ALIGNMENT_BOARDS)) {
                    return getIcon("folder_palette.png");
                }
                else {
                    return getIcon("folder"+typeSuffix+".png");    
                }
            }
        }
        else if (EntityConstants.TYPE_SAMPLE.equals(type)
                || EntityConstants.TYPE_SCREEN_SAMPLE.equals(type)
                || EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE.equals(type)) {
            if (!large && EntityConstants.VALUE_BLOCKED.equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
                return getIcon("block.png");
            }
            return getIcon(large ? "folder_files_large.png" : "beaker.png");
        }
        else if (EntityConstants.TYPE_SCREEN_SAMPLE_CROSS.equals(type)) {
            return getIcon(large ? "gear_large.png" : "intersect.png");
        }
        else if (EntityConstants.TYPE_SUPPORTING_DATA.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "folder_page.png");
        }
        else if (EntityConstants.TYPE_LSM_SUMMARY_RESULT.equals(type) ||
                 EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT.equals(type) ||
                 EntityConstants.TYPE_POST_PROCESSING_RESULT.equals(type) ||
                 EntityConstants.TYPE_CELL_COUNTING_RESULT.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "folder_image.png");
        }
        else if (EntityConstants.TYPE_ALIGNMENT_RESULT.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "folder_image.png");
        }
        else if (EntityConstants.TYPE_COMPARTMENT_SET.equals(type)) {
            return getIcon(large ? "file_large.png" : "matrix.png");
        }
        else if (EntityConstants.TYPE_COMPARTMENT.equals(type)) {
            return getIcon(large ? "file_large.png" : "matrix.png");
        }
        else if (EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "bricks.png");
        }
        else if (EntityConstants.TYPE_IMAGE_TILE.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "plugin.png");
        }
        else if (EntityConstants.TYPE_IMAGE_2D.equals(type)) {
            return getIcon(large ? "file_large.png" : "image.png");
        }
        else if (EntityConstants.TYPE_MYERS_NEURON_SEPARATION_FILE.equals(type)) {
            return getIcon(large ? "file_large.png" : "image.png");
        }
        else if (EntityConstants.TYPE_TEXT_FILE.equals(type)) {
            return getIcon(large ? "file_large.png" : "page_white.png");
        }
        else if (EntityConstants.TYPE_LSM_STACK.equals(type)  
        		|| EntityConstants.TYPE_IMAGE_3D.equals(type) 
                || EntityConstants.TYPE_ALIGNED_BRAIN_STACK.equals(type)) {
            return getIcon(large ? "file_large.png" : "images.png");
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type) || EntityConstants.TYPE_CURATED_NEURON.equals(type)) {
            return getIcon(large ? "file_large.png" : "brick.png");
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION.equals(type) || EntityConstants.TYPE_CURATED_NEURON_COLLECTION.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "folder_brick.png");
        }
        else if (EntityConstants.TYPE_ONTOLOGY_ELEMENT.equals(type)) {
            return getOntologyIcon(entity);
        }
        else if (EntityConstants.TYPE_ANNOTATION.equals(type)) {
            return getIcon("page_white_edit.png");
        }
        else if (EntityConstants.TYPE_FLY_LINE.equals(type)) {
            return getIcon(large ? "fruit_fly.png" : "fruit_fly_small_17.png");
        }
        else if (EntityConstants.TYPE_SWC_FILE.equals(type)) {
            return getIcon(large ? "swc_large.png" : "swc_small.png");
        }
        else if (EntityConstants.TYPE_V3D_ANO_FILE.equals(type)) {
            return getIcon(large ? "ano_large.png" : "ano_small.png");
        }
        else if (EntityConstants.TYPE_MOVIE.equals(type)) {
            return getIcon(large ? "file_large.png" : "movie.png");
        }
        else if (EntityConstants.TYPE_FILE.equals(type)) {
            return getIcon(large ? "file_large.png" : "page_white.png");
        }
        else if (EntityConstants.TYPE_PIPELINE_RUN.equals(type)) {
            return getIcon(large ? "folder_files_large.png" : "folder_go.png");
        }
        else if (EntityConstants.TYPE_ALIGNMENT_BOARD.equals(type)) {
            return getIcon(large ? "folder_graphite_palette_large.png" : "folder_graphite_palette.png");
        }
        else if (EntityConstants.TYPE_ALIGNED_ITEM.equals(type)) {
            return getIcon(large ? "file_large.png" : "shape_handles.png");
        }
        else if (EntityConstants.IN_MEMORY_TYPE_VIRTUAL_ENTITY.equals(type)) {
            return getIcon(large ? "file_large.png" : "images.png");
        }
        else if (EntityConstants.IN_MEMORY_TYPE_PLACEHOLDER_ENTITY.equals(type)) {
            return null;
        }
        
        return getIcon(large ? "error_large.png" : "bullet_error.png");
    }
}
