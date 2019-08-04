package org.janelia.workstation.core.nodes;

import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 * An identifiable node which represents a user object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class UserObjectNode<T> extends IdentifiableNode {

    public UserObjectNode(Children children) {
        super(children);
    }

    public UserObjectNode(Children children, Lookup lookup) {
        super(children, lookup);
    }

    public abstract T getObject();

    public abstract void update(T refreshed);
}
