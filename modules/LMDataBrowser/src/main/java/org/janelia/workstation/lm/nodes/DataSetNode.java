package org.janelia.workstation.lm.nodes;

import java.awt.Image;

import org.janelia.model.domain.sample.DataSet;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.FilterNode;
import org.openide.nodes.ChildFactory;

/**
 * Node representing a single data set which is a filter for the samples in it.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetNode extends FilterNode<DataSet> {

    public DataSetNode(ChildFactory<?> parentChildFactory, DataSet dataSet) throws Exception {
        super(parentChildFactory, dataSet);
    }

    public DataSet getDataSet() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getDataSet().getIdentifier();
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
