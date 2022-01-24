package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.model.SampleResultModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "actions",
        id = "ShowInLightboxAction"
)
@ActionRegistration(
        displayName = "#CTL_LightboxToggleAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 1000, separatorBefore = 999),
        @ActionReference(path = "Shortcuts", name = "SPACE")
})
@NbBundle.Messages("CTL_LightboxToggleAction=Show in Lightbox")
public class ShowInLightboxAction extends BaseContextualNodeAction {

    private DomainObject domainObject;
    private ArtifactDescriptor resultDescriptor;
    private String typeName;

    @Override
    public String getName() {
        return "Show in Lightbox";
    }

    @Override
    protected void processContext() {

        this.domainObject = null;
        this.resultDescriptor = null;
        this.typeName = null;
        setEnabledAndVisible(false);

        if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            ViewerContext<?,?> viewerContext = getViewerContext();
            if (viewerContext!=null) {
                DomainObjectImageModel doim = DomainUIUtils.getDomainObjectImageModel(viewerContext);
                if (doim != null) {
                    this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
                    this.resultDescriptor = doim.getArtifactDescriptor();
                    this.typeName = doim.getImageTypeName();
                    setEnabledAndVisible(isSupported(domainObject)
                            && resultDescriptor != null
                            && typeName != null
                            && !viewerContext.isMultiple());
                }
            }
        }
        else if (getNodeContext().isSingleObjectOfType(PipelineResult.class)) {
            SampleResultModel srm = DomainUIUtils.getSampleResultModel(getViewerContext());
            if (srm != null) {
                PipelineResult pipelineResult = getNodeContext().getSingleObjectOfType(PipelineResult.class);
                this.domainObject = pipelineResult.getParentRun().getParent().getParent();
                this.resultDescriptor = srm.getArtifactDescriptor();
                this.typeName = srm.getFileType().name();
                setEnabledAndVisible(true);
            }
        }


    }

    private boolean isSupported(DomainObject domainObject) {
        return domainObject instanceof Sample || domainObject instanceof HasFiles;
    }

    @Override
    public void performAction() {
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.showInLightbox", domainObject);
        Hud.getSingletonInstance().setObjectAndToggleDialog(domainObject, resultDescriptor, typeName, true, true);
    }
}
