package org.janelia.workstation.common.nodes;

import java.awt.Image;

import org.janelia.model.domain.gui.search.Filtering;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class FilterNode<T extends Filtering> extends AbstractDomainObjectNode<T> {
        
    public FilterNode(ChildFactory<?> parentChildFactory, T filter) throws Exception {
        super(parentChildFactory, Children.LEAF, filter);
    }
    
    public T getFilter() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getFilter().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getFilter())) {
            return Icons.getIcon("search-white-icon.png").getImage();
        }
        else {
            return Icons.getIcon("search-blue-icon.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
