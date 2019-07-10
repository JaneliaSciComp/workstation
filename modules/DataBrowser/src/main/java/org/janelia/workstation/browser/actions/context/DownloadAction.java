package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.browser.gui.dialogs.download.DownloadWizardAction;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "Actions",
        id = "DownloadAction"
)
@ActionRegistration(
        displayName = "#CTL_DownloadAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 400, separatorBefore = 399),
        @ActionReference(path = "Shortcuts", name = "D-D")
})
@NbBundle.Messages("CTL_DownloadAction=Download Files...")
public class DownloadAction extends BaseContextualNodeAction {

    private Collection<DomainObject> domainObjects = new ArrayList<>();

    @Override
    protected void processContext() {
        domainObjects.clear();
        if (getNodeContext().isOnlyObjectsOfType(DomainObject.class)) {
            domainObjects.addAll(getNodeContext().getOnlyObjectsOfType(DomainObject.class));
            // Hide for ontology terms
            setEnabledAndVisible(DomainUIUtils.getObjectsOfType(domainObjects, OntologyTerm.class).isEmpty());
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        return domainObjects.size() > 1 ? "Download " + domainObjects.size() + " Items..." : "Download...";
    }

    @Override
    public void performAction() {
        Collection<DomainObject> domainObjects = new ArrayList<>(this.domainObjects);
        new DownloadWizardAction(domainObjects, null).actionPerformed(null);
    }
}