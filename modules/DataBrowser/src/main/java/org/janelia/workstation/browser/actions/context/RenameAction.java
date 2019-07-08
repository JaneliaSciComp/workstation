package org.janelia.workstation.browser.actions.context;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "Actions",
        id = "RenameAction"
)
@ActionRegistration(
        displayName = "#CTL_RenameAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 120)
})
@NbBundle.Messages("CTL_RenameAction=Rename")
public class RenameAction extends BaseContextualNodeAction {

    private DomainObject domainObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            setVisible(userCanRename(domainObject));
            setEnabled(isVisible() && ClientDomainUtils.hasWriteAccess(domainObject));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        if (domainObject!=null) {
            renameObject(domainObject);
        }
    }

    private boolean userCanRename(DomainObject domainObject) {
        // TODO: this should be implemented as DomainObjectHandler.supportsRename
        return (domainObject instanceof TreeNode
                || domainObject instanceof TmWorkspace
                || domainObject instanceof TmSample
                || domainObject instanceof ColorDepthMask
                || domainObject instanceof ColorDepthSearch
                || domainObject instanceof Filter
                || domainObject instanceof Ontology)
                &&
                !(domainObject instanceof Workspace);
    }

    private void renameObject(DomainObject domainObject) {
        String newName = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "Name:\n", "Rename "
                + domainObject.getName(), JOptionPane.PLAIN_MESSAGE, null, null, domainObject.getName());
        if (StringUtils.isBlank(newName)) {
            return;
        }
        try {
            DomainMgr.getDomainMgr().getModel().updateProperty(domainObject, "name", newName);
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }
}