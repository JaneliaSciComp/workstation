package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

public class Neuron extends EntityWrapper implements Viewable2d {

    public Neuron(RootedEntity neuronFragment) {
        super(neuronFragment);
    }

    @Override
    protected void loadContextualizedChildren() throws Exception {
    }
    
    public Integer getMaskIndex() {
        String value = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
        if (StringUtils.isEmpty(value)) return null;
        return Integer.parseInt(value);
    }

    @Override
    public String get2dImageFilepath() {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }
}
