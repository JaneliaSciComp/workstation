package org.janelia.it.workstation.browser.nodes;

import org.janelia.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 * A node which has an identifier.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IdentifiableNode<T> extends AbstractNode implements HasIdentifier {

    public IdentifiableNode(Children children) {
        super(children);
    }

    public IdentifiableNode(Children children, Lookup lookup) {
        super(children, lookup);
    }

    public abstract Long getId();

    public abstract void update(T refreshed);
    
}
