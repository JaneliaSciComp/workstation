package org.janelia.horta.nodes;

import java.util.List;
import org.openide.nodes.ChildFactory;

/**
 *
 * @author Christopher Bruns
 */
class MeshChildFactory extends ChildFactory
{

    public MeshChildFactory()
    {
    }

    @Override
    protected boolean createKeys(List list) {
       return true;
    }
    
}
