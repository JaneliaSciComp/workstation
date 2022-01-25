package org.janelia.workstation.lm.actions.context;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.workstation.browser.gui.dialogs.download.DownloadWizardAction;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.SampleResultModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "actions",
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
    private ArtifactDescriptor defaultResultDescriptor;

    @Override
    protected void processContext() {
        domainObjects.clear();
        defaultResultDescriptor = null;

        if (getNodeContext().isOnlyObjectsOfType(DomainObject.class)) {
            domainObjects.addAll(getNodeContext().getOnlyObjectsOfType(DomainObject.class));
            // Hide for ontology terms
            setEnabledAndVisible(DomainUIUtils.getObjectsOfType(domainObjects, OntologyTerm.class).isEmpty());
        }
        else if (getNodeContext().isSingleObjectOfType(PipelineResult.class)) {
            SampleResultModel srm = DomainUIUtils.getSampleResultModel(getViewerContext());
            if (srm != null) {
                PipelineResult pipelineResult = getNodeContext().getSingleObjectOfType(PipelineResult.class);
                domainObjects.add(pipelineResult.getParentRun().getParent().getParent());
                defaultResultDescriptor = srm.getArtifactDescriptor();
                setVisible(true);
                setEnabled(DomainUtils.getDefault3dImageFilePath(pipelineResult)!=null);
            }
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
        new DownloadWizardAction(domainObjects, defaultResultDescriptor).actionPerformed(null);
    }
}