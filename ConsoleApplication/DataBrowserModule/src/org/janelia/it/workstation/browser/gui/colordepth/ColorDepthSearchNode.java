package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.workstation.browser.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.browser.nb_action.RenameAction;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class ColorDepthSearchNode extends AbstractDomainObjectNode<ColorDepthSearch> {
        
    public ColorDepthSearchNode(ChildFactory<?> parentChildFactory, ColorDepthSearch mask) throws Exception {
        super(parentChildFactory, Children.LEAF, mask);
    }
    
    public ColorDepthSearch getColorDepthSearch() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getColorDepthSearch().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthSearch())) {
            return Icons.getIcon("drive_magnify.png").getImage();
        }
        else {
            return Icons.getIcon("drive_magnify.png").getImage();
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
        return actions.toArray(new Action[actions.size()]);
    }
}
