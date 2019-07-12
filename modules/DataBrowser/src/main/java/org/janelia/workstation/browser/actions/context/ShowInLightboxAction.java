package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.hud.Hud;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.model.DomainObjectImageModel;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Actions",
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

    private static final Logger log = LoggerFactory.getLogger(ShowInLightboxAction.class);
    private DomainObject domainObject;
    private ArtifactDescriptor resultDescriptor;
    private String typeName;

    @Override
    public String getName() {
        return "Show in Lightbox";
    }

    @Override
    protected void processContext() {
        setEnabledAndVisible(false);
        ViewerContext viewerContext = getViewerContext();
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

    private boolean isSupported(DomainObject domainObject) {
        return domainObject instanceof Sample || domainObject instanceof HasFiles;
    }

    @Override
    public void performAction() {
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.showInLightbox", domainObject);
        Hud.getSingletonInstance().setObjectAndToggleDialog(domainObject, resultDescriptor, typeName, true, true);
    }
}
