package org.janelia.workstation.core.nodes;

import java.io.IOException;

import org.janelia.model.domain.interfaces.HasIdentifier;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 * A NetBeans node which has an identifier.
 *
 * In a perfect world, we could just use intersection types (e.g. <T extends Node & HasIdentifier>) for this sort of
 * thing. Unfortunately, NetBeans eschews interfaces and thus forces this inheritance hierarchy.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class IdentifiableNode extends AbstractNode implements HasIdentifier {

    public IdentifiableNode(Children children) {
        super(children);
    }

    public IdentifiableNode(Children children, Lookup lookup) {
        super(children, lookup);
    }

    public abstract Long getId();

    @Override
    public void destroy() throws IOException {
        NodeTracker.getInstance().deregisterNode(this);
    }
}
