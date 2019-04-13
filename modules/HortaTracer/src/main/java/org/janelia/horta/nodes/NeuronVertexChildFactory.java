package org.janelia.horta.nodes;

import java.util.List;
import org.openide.nodes.ChildFactory;

/**
 *
 * @author Christopher Bruns
 */
class NeuronVertexChildFactory extends ChildFactory
{

    public NeuronVertexChildFactory()
    {
    }

    @Override
    protected boolean createKeys(List list)
    {
        return true;
    }
    
}
