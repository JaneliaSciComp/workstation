package org.janelia.workstation.browser.nodes;

import java.util.Collection;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * Represents the currently selected set of child objects in the currently focused object viewer.
 *
 * This kind of abuses the NetBeans' Node API to aggregate things for performance purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChildObjectsNode extends AbstractNode {

    private Collection objects;

    public ChildObjectsNode(Collection objects) {
        super(Children.LEAF);
        this.objects = objects;
    }

    public Collection getObjects() {
        return objects;
    }
}
