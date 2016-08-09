package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.nb_action.AddToFolderAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.browser.nb_action.RemoveAction;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class TmSampleNode extends DomainObjectNode {

    public TmSampleNode(ChildFactory parentChildFactory, TmSample sample) throws Exception {
        super(parentChildFactory, Children.LEAF, sample);
    }
    
    public TmSample getSample() {
        return (TmSample)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSample().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getSample())) {
            return Icons.getIcon("beaker.png").getImage();
        }
        else {
            return Icons.getIcon("beaker.png").getImage();
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
        actions.add(new CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(AddToFolderAction.get());
        actions.add(new RenameAction());
        actions.add(RemoveAction.get());
        return actions.toArray(new Action[actions.size()]);
    }
}
