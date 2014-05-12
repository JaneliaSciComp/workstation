/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.ViewDetailsAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewDetailsAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1537, separatorBefore = 1531),
    @ActionReference(path = "Shortcuts", name = "D-I")
})

@Messages("CTL_ViewDetailsAction=View Details")
public final class ViewDetailsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            viewDetails_actionPerformed(e, FacadeManager.getEJBProtocolString(), null);
        } catch (Exception e1) {
            SessionMgr.getSessionMgr().handleException(e1);
        }
    }

    private void viewDetails_actionPerformed(ActionEvent e, String protocol, Object dataSource) throws Exception {
        java.util.List<String> tmpSelections = ModelMgr.getModelMgr().getEntitySelectionModel().getLatestGlobalSelection();
        if (null != tmpSelections && tmpSelections.size() == 1) {
            Entity tmpSelectedEntity = ModelMgr.getModelMgr().getEntityById(Utils.getEntityIdFromUniqueId(tmpSelections.get(0)));
            if (null != tmpSelectedEntity) {
                new EntityDetailsDialog().showForEntity(tmpSelectedEntity);
            }
        }
    }

}
