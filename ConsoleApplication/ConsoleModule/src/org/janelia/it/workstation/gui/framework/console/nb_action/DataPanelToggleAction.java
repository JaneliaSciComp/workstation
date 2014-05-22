/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.BooleanStateAction;

@ActionID(
        category = "View",
        id = "DataPanelToggleAction"
)
@ActionRegistration(
        displayName = "#CTL_DataPanelToggleAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 0),
    @ActionReference(path = "Shortcuts", name = "M-D")
})
@Messages("CTL_DataPanelToggleAction=Data Panel")
public final class DataPanelToggleAction extends BooleanStateAction {
    public static final String DATA_PANEL_SHOWN = "DataPanelShown";

    public DataPanelToggleAction() {
        setBooleanState( true );
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        setBooleanState( !getBooleanState() );
        new ViewActionDelegate().toggleDataPanel( getBooleanState() );
    }

    @Override
    public String getName() {
        return DATA_PANEL_SHOWN;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }
}
