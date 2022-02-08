package org.janelia.workstation.browser.nodes;

import org.janelia.model.domain.files.SyncedPath;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

import java.awt.*;

/**
 * Node representing a single SyncedPath. This might be an N5 container, or image, or anything else found on the
 * file system by the SyncedPath discovery process.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncedPathNode extends AbstractDomainObjectNode<SyncedPath> {

    public SyncedPathNode(ChildFactory<?> parentChildFactory, Children children, SyncedPath path) {
        super(parentChildFactory, children, path);
    }

    public SyncedPathNode(ChildFactory<?> parentChildFactory, SyncedPath path) {
        this(parentChildFactory, Children.LEAF, path);
    }

    public SyncedPath getSyncedPath() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getSyncedPath().getName();
    }

    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder_red.png").getImage();
    }

    @Override
    public boolean canDestroy() {
        return true;
    }
}
