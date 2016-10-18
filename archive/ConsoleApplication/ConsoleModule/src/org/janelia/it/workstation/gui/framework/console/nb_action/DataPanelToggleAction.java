package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.BooleanStateAction;

import java.awt.event.ActionEvent;

@ActionID(
        category = "View",
        id = "DataPanelToggleAction"
)
@ActionRegistration(
        displayName = "#CTL_DataPanelToggleAction",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 0),
    @ActionReference(path = "Shortcuts", name = "M-D")
})
@Messages("CTL_DataPanelToggleAction=Data Explorer Show/Hide")
public final class DataPanelToggleAction extends BooleanStateAction {
    
    public static final String DATA_PANEL_SHOWN = "Data Explorer Show/Hide";

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
