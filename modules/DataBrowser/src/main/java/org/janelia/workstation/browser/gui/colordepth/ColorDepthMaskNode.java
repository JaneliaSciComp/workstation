package org.janelia.workstation.browser.gui.colordepth;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.workstation.browser.nb_action.AddToFolderAction;
import org.janelia.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.workstation.browser.nb_action.RemoveAction;
import org.janelia.workstation.browser.nb_action.RenameAction;
import org.janelia.workstation.browser.nodes.TreeNodeNode;
import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.openide.nodes.ChildFactory;

public class ColorDepthMaskNode extends TreeNodeNode {

    public ColorDepthMaskNode(ChildFactory<?> parentChildFactory, ColorDepthMask mask) {
        super(parentChildFactory, mask);
    }
    
    public ColorDepthMask getColorDepthMask() {
        return (ColorDepthMask)getDomainObject();
    }
            
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthMask())) {
            return Icons.getIcon("image.png").getImage();
        }
        else {
            return Icons.getIcon("image.png").getImage();
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
        actions.add(new AddMaskAction());
        return actions.toArray(new Action[actions.size()]);
    }

    protected final class AddMaskAction extends AbstractAction {

        public AddMaskAction() {
            putValue(NAME, "Add Mask to Color Depth Search...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new ColorDepthSearchDialog().showForMask(getColorDepthMask());
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

}
