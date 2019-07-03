package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action which can be used to either check or uncheck all the currently selected items in the viewer context.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BaseCheckAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(BaseCheckAction.class);

    private final boolean check;
    private List<DomainObject> domainObjects = new ArrayList<>();
    private ChildSelectionModel<DomainObject, Reference> editSelectionModel;

    BaseCheckAction(boolean check) {
        this.check = check;
    }

    @Override
    protected void processContext() {
        domainObjects.clear();

        log.info("getViewerContext()="+getViewerContext());
        log.info("DomainUIUtils.getDomainObjectImageModel(getViewerContext())="+DomainUIUtils.getDomainObjectImageModel(getViewerContext()));

        if (DomainUIUtils.getDomainObjectImageModel(getViewerContext()) != null
                && getNodeContext().isOnlyObjectsOfType(DomainObject.class)) {
            domainObjects.addAll(getNodeContext().getOnlyObjectsOfType(DomainObject.class));
            // This implicit cast is fine, because we know the viewer context has a domain object image model
            this.editSelectionModel = getViewerContext().getEditSelectionModel();
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        if (check) {
            editSelectionModel.select(domainObjects, false, true);
        }
        else {
            editSelectionModel.deselect(domainObjects, true);
        }
    }
}