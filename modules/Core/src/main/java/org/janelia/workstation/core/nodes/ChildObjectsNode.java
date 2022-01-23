package org.janelia.workstation.core.nodes;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents the currently selected set of child objects in the currently focused object viewer.
 *
 * This kind of abuses the NetBeans' Node API to aggregate things for performance purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChildObjectsNode extends AbstractNode {

    private Collection<Object> objects;

    public ChildObjectsNode(Object object) {
        this(Collections.singleton(object));
    }

    public ChildObjectsNode(Collection<Object> objects) {
        super(Children.LEAF);
        this.objects = objects;
    }

    public Collection<Object> getObjects() {
        return objects;
    }
}
