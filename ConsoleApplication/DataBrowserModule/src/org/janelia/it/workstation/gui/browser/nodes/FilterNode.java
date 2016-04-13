package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class FilterNode extends DomainObjectNode {
        
    public FilterNode(ChildFactory parentChildFactory, Filter filter) throws Exception {
        super(parentChildFactory, Children.LEAF, filter);
    }
    
    public Filter getFilter() {
        return (Filter)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getFilter().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (getFilter().getOwnerKey().equals(AccessManager.getSubjectKey())) {
            return Icons.getIcon("search-blue-icon.png").getImage();
        }
        else {
            return Icons.getIcon("search-white-icon.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }   
}
