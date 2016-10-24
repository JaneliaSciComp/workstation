package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.browser.nb_action.SearchHereAction;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class FilterNode extends DomainObjectNode<Filter> {
        
    public FilterNode(ChildFactory<?> parentChildFactory, Filter filter) throws Exception {
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
        if (ClientDomainUtils.isOwner(getFilter())) {
            return Icons.getIcon("script.png").getImage();
        }
        else {
            return Icons.getIcon("script.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(AddToFolderAction.get());
        actions.add(new RenameAction());
        actions.add(RemoveAction.get());
        actions.add(null);
        actions.add(SearchHereAction.get());
        return actions.toArray(new Action[actions.size()]);
    }
}
