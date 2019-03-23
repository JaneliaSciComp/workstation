package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.nb_action.DownloadAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.browser.nb_action.RenameAction;
import org.janelia.it.workstation.browser.nb_action.SearchHereAction;
import org.janelia.model.domain.gui.search.Filtering;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class FilterNode extends AbstractDomainObjectNode<Filtering> {
        
    public FilterNode(ChildFactory<?> parentChildFactory, Filtering filter) throws Exception {
        super(parentChildFactory, Children.LEAF, filter);
    }
    
    public Filtering getFilter() {
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

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new OpenInViewerAction());
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(AddToFolderAction.get());
        actions.add(RenameAction.get());
        actions.add(RemoveAction.get());
        actions.add(null);
        actions.add(SearchHereAction.get());
        actions.add(DownloadAction.get());
        return actions.toArray(new Action[actions.size()]);
    }
}
