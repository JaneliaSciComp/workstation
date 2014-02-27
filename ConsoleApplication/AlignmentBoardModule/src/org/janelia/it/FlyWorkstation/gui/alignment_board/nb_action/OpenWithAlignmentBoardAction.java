/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.alignment_board.nb_action;

import org.janelia.it.FlyWorkstation.nb_action.ContextSuitable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.FlyWorkstation.gui.alignment_board.Launcher;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Tools",
        id = "org.janelia.it.FlyWorkstation.gui.alignment_board.nb_action.OpenWithAlignmentBoardAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenWithAlignmentBoardAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1300),
    @ActionReference(path = "Loaders/application/x-nbsettings/Actions", position = 0)
})
@Messages("CTL_OpenWithAlignmentBoardAction=Open in Alignment Board Viewer")
public final class OpenWithAlignmentBoardAction implements ActionListener {

    private final ContextSuitable context;

    public OpenWithAlignmentBoardAction(ContextSuitable context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        new Launcher().launch( context.getEntity().getId() );
    }
}
