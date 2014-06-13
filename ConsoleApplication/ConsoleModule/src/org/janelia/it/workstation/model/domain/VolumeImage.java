package org.janelia.it.workstation.model.domain;

import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * A sample volume image with different representations. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class VolumeImage extends EntityWrapper implements Viewable2d, Viewable3d, Masked3d {

    public VolumeImage(RootedEntity reference) {
        super(reference);
    }

    public String getName() {
    	String name = super.getName();
    	// Remove file extension, e.g. Reference.v3dpbd -> Reference
    	int dot = name.indexOf('.');
    	if (dot>0) name = name.substring(0, dot);
        return name;
    }
    
    @Override
    public String get2dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }
    
    public String get3dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    }

    @Override
    public String getMask3dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE);
    }

    @Override
    public String getChan3dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE);
    }
}
