package org.janelia.console.viewerapi.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.SelectPrimaryAnchorCommand;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SelectParentAnchorAction extends AbstractAction implements Action
{
    private final NeuronSet workspace;
    private final NeuronVertex newParentAnchor;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public SelectParentAnchorAction(NeuronSet workspace, NeuronVertex anchor)
    {
        super("Set Anchor As Parent");
        this.workspace = workspace;
        this.newParentAnchor = anchor;
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        if (workspace == null)
            return;
        SelectPrimaryAnchorCommand cmd = new SelectPrimaryAnchorCommand(
                workspace,
                newParentAnchor);
        cmd.setNotify(true); // Top-level Commands notify their listeners
        try {
            if (cmd.execute()) {
                // Actions, like this one, are responsible for the undo/redo stack
                // log.info("Parent anchor selected");
                //UndoRedo.Manager undoRedo = workspace.getUndoRedo();
                //if (undoRedo != null)
                 //   undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
            }
        }
        catch (Exception exc) {
            // log.info("Parent anchor selection failed");
        }              
    }
}
