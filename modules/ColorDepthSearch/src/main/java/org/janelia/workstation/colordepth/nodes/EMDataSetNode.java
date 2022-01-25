package org.janelia.workstation.colordepth.nodes;

import org.janelia.model.domain.flyem.EMDataSet;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.FilterNode;
import org.openide.nodes.ChildFactory;

import java.awt.*;

/**
 * Node representing a single data set which is a filter for the samples in it.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EMDataSetNode extends FilterNode<EMDataSet> {

    public EMDataSetNode(ChildFactory<?> parentChildFactory, EMDataSet dataSet) throws Exception {
        super(parentChildFactory, dataSet);
    }

    public EMDataSet getDataSet() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getDataSet().getDataSetIdentifier();
    }

    @Override
    public String getExtraLabel() {
        return null;
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("database.png").getImage();
    }
}
