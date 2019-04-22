package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;

/**
 * Action which can be used to either check or uncheck all the currently selected items in the viewer context.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CheckSelectedAction extends AbstractAction implements ViewerContextReceiver {

    static final CheckSelectedAction CHECK_ACTION = new CheckSelectedAction(true);
    static final CheckSelectedAction UNCHECK_ACTION = new CheckSelectedAction(false);
    
    private final boolean check;
    private List<DomainObject> domainObjectList;
    private ChildSelectionModel<DomainObject, Reference> editSelectionModel;

    private CheckSelectedAction(boolean check) {
        this.check = check;
    }
    
    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        ContextualActionUtils.setVisible(this,false);
        if (DomainUIUtils.getDomainObjectImageModel(viewerContext) != null) {
            this.domainObjectList = new ArrayList<>(DomainUIUtils.getSelectedDomainObjects(viewerContext));
            this.editSelectionModel = viewerContext.getEditSelectionModel();
            ContextualActionUtils.setVisible(this, editSelectionModel != null);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (check) {
            editSelectionModel.select(domainObjectList, false, true);
        }
        else {
            editSelectionModel.deselect(domainObjectList, true);
        }
    }
}