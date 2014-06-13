package org.janelia.it.workstation.model.domain;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.model.entity.RootedEntity;

/**
 * A neuron with different representations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neuron extends EntityWrapper implements Viewable2d, Masked3d, MaskIndexed {

    public Neuron(RootedEntity neuronFragment) {
        super(neuronFragment);
    }

    @Override
    public Integer getMaskIndex() {
        String value = getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (StringUtils.isEmpty(value)) return null;
        return Integer.parseInt(value)+1;
    }

    @Override
    public String get2dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
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
