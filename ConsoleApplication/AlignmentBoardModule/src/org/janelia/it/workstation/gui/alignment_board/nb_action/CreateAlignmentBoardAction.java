package org.janelia.it.workstation.gui.alignment_board.nb_action;

import org.janelia.it.workstation.gui.alignment_board_viewer.creation.AlignmentBoardCreator;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Use this to make new alignment boards.
 * 
 * @author fosterl
 */
@ActionID(
        category = "File/New",
        id = "org.janelia.it.workstation.gui.alignment_board.action.CreateAlignmentBoardAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateAlignmentBoardAction"
)
@ActionReference(path = "Menu/File/New", position = 1200)
@Messages("CTL_CreateAlignmentBoardAction=New Alignment Board")
public final class CreateAlignmentBoardAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new AlignmentBoardCreator().execute();
    }    

}
