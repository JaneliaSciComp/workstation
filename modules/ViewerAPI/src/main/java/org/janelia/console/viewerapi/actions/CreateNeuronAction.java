package org.janelia.console.viewerapi.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.CreateNeuronCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to create a new neuron rooted at a particular XYZ location.
 */

public final class CreateNeuronAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronSet workspace;
    private final float[] anchorXyz = new float[3];
    private final float anchorRadius;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public CreateNeuronAction(
            Component parentWidget,
            NeuronSet workspace,
            float[] anchorXyz,
            float anchorRadius)
    {
        super("Create a New Neuron Model Here...");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        System.arraycopy(anchorXyz, 0, this.anchorXyz, 0, 3);
        this.anchorRadius = anchorRadius;
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        // come up with a unique neuron name
        String defaultName = getNextNeuronName();

        // ask the user to confirm creation, and to review name
        Object neuronName = JOptionPane.showInputDialog(
                parentWidget,
                "Create new neuron here?",
                "Create new neuron",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defaultName); // default button
        if (neuronName == null) {
            return; // User pressed "Cancel"
        }
        String errorMessage = "Failed to create neuron";
        try {
            CreateNeuronCommand cmd = new CreateNeuronCommand(
                    workspace,
                    neuronName.toString(),
                    anchorXyz,
                    anchorRadius);
            cmd.setNotify(true); // Because it's a top-level Command now
            if (cmd.execute()) {
                log.info("Neuron created");
                UndoRedo.Manager undoRedo = workspace.getUndoRedo();
                if (undoRedo != null)
                    undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
                return;
            }
        }
        catch (Exception exc) {
            errorMessage += ":\n" + exc.getMessage();
        }
        JOptionPane.showMessageDialog(
                parentWidget,
                errorMessage,
                "Failed to create neuron",
                JOptionPane.WARNING_MESSAGE);                
    }    
    
    /**
     * Lifted/modified from LVV AnnotationManager.getNextNeuronName
     * given a workspace, return a new generic neuron name (probably something
     * like "Neuron 12", where the integer is based on whatever similarly
     * named neurons exist already)
     */
    private String getNextNeuronName() 
    {
        // go through existing neuron names; try to parse against
        //  standard template; remember largest integer found
        Pattern pattern = Pattern.compile("Neuron[ _]([0-9]+)");
        Long maximum = 0L;
        for (NeuronModel neuron : workspace) {
            if (neuron.getName() == null) {
                // skip unnamed neurons
                continue;
            }
            Matcher matcher = pattern.matcher(neuron.getName());
            if (matcher.matches()) {
                Long index = Long.parseLong(matcher.group(1));
                if (index > maximum)
                    maximum = index;
            }
        }
        return String.format("Neuron %d", maximum + 1);
    }

}
