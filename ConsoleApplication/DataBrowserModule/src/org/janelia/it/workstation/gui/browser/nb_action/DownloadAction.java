package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.gui.dialogs.DownloadDialog;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
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
        return "Download...";
    }

    @Override
    protected void performAction (Node[] activatedNodes) {
        List<DomainObject> domainObjectList = new ArrayList<>();
        for(Node node : getSelectedNodes()) {
            if (node instanceof DomainObjectNode) {
                DomainObjectNode domainObjectNode = (DomainObjectNode)node;
                domainObjectList.add(domainObjectNode.getDomainObject());
            }
            else {
                throw new IllegalStateException("Download can only be called on DomainObjectNode");
            }
        }

        DownloadDialog dialog = new DownloadDialog();
        dialog.showDialog(domainObjectList, ResultDescriptor.LATEST);
    }
}
