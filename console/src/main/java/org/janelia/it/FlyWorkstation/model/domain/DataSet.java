package org.janelia.it.FlyWorkstation.model.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

public class DataSet extends EntityWrapper {
    
    public DataSet(Entity entity) {
        super(new RootedEntity(entity));
    }

    public String getDataSetIdentifier() {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    }

    public List<String> getPipelineProcesses() {
        List<String> processNames = new ArrayList<String>();
        String value = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        if (value!=null) {
            processNames.addAll(Arrays.asList(value.split(",")));
        }
        return processNames;
    }
    
    public boolean isSAGESynchronized() {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)!=null;
    }
}
