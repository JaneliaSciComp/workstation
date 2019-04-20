package org.janelia.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.janelia.workstation.browser.gui.dialogs.download.DownloadWizardAction;
import org.janelia.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.common.nb_action.NodePresenterAction;
import org.openide.nodes.Node;

/**
 * Action which implements File Download. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class DownloadAction extends NodePresenterAction {

    private final static DownloadAction singleton = new DownloadAction();
    public static DownloadAction get() {
        return singleton;
    }

    private DownloadAction() {
    }
    
    @Override
    public String getName() {
        return "Download Files...";
    }

    @Override
    protected void performAction (Node[] activatedNodes) {
        List<DomainObject> domainObjectList = new ArrayList<>();
        for(Node node : getSelectedNodes()) {
            if (node instanceof AbstractDomainObjectNode) {
                AbstractDomainObjectNode<?> domainObjectNode = (AbstractDomainObjectNode<?>)node;
                domainObjectList.add(domainObjectNode.getDomainObject());
            }
            else {
                throw new IllegalStateException("Download can only be called on DomainObjectNode");
            }
        }

        DownloadWizardAction wizard = new DownloadWizardAction(domainObjectList, null);
        wizard.actionPerformed(null);
    }
}
