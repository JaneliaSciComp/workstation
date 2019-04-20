package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.workstation.browser.gui.inspector.DomainInspectorPanel;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=110)
public class ChangePermissionsBuilder implements ContextualActionBuilder {

    private static ChangePermissionsAction action = new ChangePermissionsAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ClientDomainUtils.isOwner((DomainObject)obj);
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class ChangePermissionsAction extends ViewerContextAction {

        @Override
        protected String getName() {
            return "Change Permissions";
        }

        @Override
        protected boolean isVisible() {
            return !getViewerContext().isMultiple();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DomainObject domainObject = getViewerContext().getDomainObject();
            new DomainDetailsDialog().showForDomainObject(domainObject, DomainInspectorPanel.TAB_NAME_PERMISSIONS);
        }
    }
}
