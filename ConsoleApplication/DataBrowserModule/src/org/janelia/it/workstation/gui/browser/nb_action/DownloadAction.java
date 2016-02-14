package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.gui.dialogs.FileExportDialog;
import org.janelia.it.workstation.gui.browser.model.ResultDescriptor;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * Action which implements File Download. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class DownloadAction extends NodeAction {

    private final static DownloadAction singleton = new DownloadAction();
    public static DownloadAction get() {
        return singleton;
    }
    
    private final List<Node> selected = new ArrayList<>();
    private final List<Node> toDownload = new ArrayList<>();
    
    private DownloadAction() {
    }
    
    @Override
    public String getName() {
        return "Download...";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("DownloadAction");
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        toDownload.clear();
        for(Node node : activatedNodes) {            
            if (node instanceof DomainObjectNode) {
                toDownload.add(node);
            }
            selected.add(node);
        }
        return toDownload.size()==selected.size();
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        List<DomainObject> domainObjectList = new ArrayList<>();
        for(Node node : toDownload) {
            if (node instanceof DomainObjectNode) {
                DomainObjectNode domainObjectNode = (TreeNodeNode)node;
                domainObjectList.add(domainObjectNode.getDomainObject());
            }
            else {
                throw new IllegalStateException("Download can only be called on DomainObjectNode");
            }
        }

        FileExportDialog dialog = new FileExportDialog();
        dialog.showDialog(domainObjectList, ResultDescriptor.LATEST);
    }
}
