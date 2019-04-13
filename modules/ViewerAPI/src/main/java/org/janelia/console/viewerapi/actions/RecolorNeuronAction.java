package org.janelia.console.viewerapi.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.commands.RecolorNeuronCommand;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GUI-level Action to change the color of an existing neuron
 */

public final class RecolorNeuronAction extends AbstractAction
{
    private final Component parentWidget;
    private final NeuronModel neuron;
    private final NeuronSet workspace;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public RecolorNeuronAction(
            Component parentWidget,
            NeuronSet workspace,
            NeuronModel neuron)
    {
        super("Change Neuron Color...");
        this.parentWidget = parentWidget;
        this.workspace = workspace;
        this.neuron = neuron;        
    }

    @Override
    public void actionPerformed(ActionEvent ev) 
    {
        // Dynamically update color as user drags the slider
        final Color originalColor = neuron.getColor();
        final JColorChooser colorChooser = new JColorChooser(originalColor);
        
        // Restore neuron color when user cancels the dialog
        ActionListener cancelListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                neuron.setColor(originalColor);
                neuron.getColorChangeObservable().notifyObservers();
            }
        };
        // Set final color when user accepts the dialog
        ActionListener okListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Color newColor = colorChooser.getColor();
                if (newColor.equals(originalColor))
                    return; // no change
                RecolorNeuronCommand cmd = new RecolorNeuronCommand(neuron, newColor, originalColor);
                cmd.setNotify(true);
                if (cmd.execute()) {
                    log.info("Neuron color changed");
                    UndoRedo.Manager undoRedo = workspace.getUndoRedo();
                    if (undoRedo != null)
                        undoRedo.undoableEditHappened(new UndoableEditEvent(this, cmd));
                }
                else {
                    JOptionPane.showMessageDialog(
                            parentWidget,
                            "Failed to change neuron color",
                            "Failed to change neuron color",
                            JOptionPane.WARNING_MESSAGE);                
                }
            }
        };
        // Dynamically update the neuron color as the user fiddles with the sliders
        colorChooser.getSelectionModel().addChangeListener(new ChangeListener() 
        {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                neuron.setColor(colorChooser.getColor());
                // Update display in real time
                neuron.getColorChangeObservable().notifyObservers();
            }
        });
        JDialog colorDialog = JColorChooser.createDialog(parentWidget,
                "Select color for neuron " + neuron.getName(),
                false, // not modal
                colorChooser,
                okListener, cancelListener);
        colorDialog.setVisible(true);
    }

}
