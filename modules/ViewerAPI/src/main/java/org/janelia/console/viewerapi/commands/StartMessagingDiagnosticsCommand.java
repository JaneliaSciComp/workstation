package org.janelia.console.viewerapi.commands;

import org.janelia.console.viewerapi.Command;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import java.awt.*;

public class StartMessagingDiagnosticsCommand
        implements Command
{
    private NeuronModel neuron;
    private NeuronSet neuronSetAdapter;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public StartMessagingDiagnosticsCommand(
            NeuronModel targetNeuron,
            NeuronSet bridgeToLVV)
    {
        neuron = targetNeuron;
        neuronSetAdapter = bridgeToLVV;
    }
    
    @Override
    public boolean execute() {
        neuronSetAdapter.startUpMessagingDiagnostics(neuron);
        return true;
    }
    
}
