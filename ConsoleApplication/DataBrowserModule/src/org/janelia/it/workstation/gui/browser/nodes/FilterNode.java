package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(FilterNode.class);
    
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
        if (!getFilter().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
            // TODO: add a blue version of this icon
            return Icons.getIcon("database.png").getImage();
        }
        else {
            return Icons.getIcon("database.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
}
