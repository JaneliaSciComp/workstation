/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.BooleanStateAction;

@ActionID(
        category = "View",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.PropertiesToggleAction"
)
@ActionRegistration(
        displayName = "#CTL_PropertiesToggleAction"
)
@ActionReference(path = "Menu/View", position = 50)
@Messages("CTL_PropertiesToggleAction=Properties Panel")
public final class PropertiesToggleAction extends BooleanStateAction {

    private static final String PROPERTIES_PANEL_SHOWN = "Properties";
    public PropertiesToggleAction() {
        setBooleanState( true );
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        setBooleanState( ! getBooleanState() );
        ViewActionDelegate viewActionDelegate = new ViewActionDelegate();
        viewActionDelegate.toggleOntology( getBooleanState() );
    }

    @Override
    public String getName() {
        return PROPERTIES_PANEL_SHOWN;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }
}
