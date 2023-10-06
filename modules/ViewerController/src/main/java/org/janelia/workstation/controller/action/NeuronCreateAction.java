package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;

@ActionID(
        category = "Horta",
        id = "NeuronCreateAction"
)
@ActionRegistration(
        displayName = "Create a new neuron",
        lazy = true
)
public class NeuronCreateAction extends EditAction {
    
    public NeuronCreateAction() {
        super("Create neuron");
    }

    public void execute(boolean initVertex, Vec3 vertexLoc) {
       createNeuron(initVertex, vertexLoc);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        createNeuron(false, null);
    }

    private void createNeuron(boolean createInitVertex, Vec3 vertexLoc) {
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        // prompt the user for a name, but suggest a standard name
        final String neuronName = NeuronNamePrompter.promptForNeuronName(NeuronManager.getNextNeuronName());
        if (neuronName != null) {
            // create it:
            SimpleWorker creator = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    NeuronManager.getInstance().createNeuron(neuronName).thenApply((newNeuron -> {
                        try {
                            if (createInitVertex) {
                                NeuronManager.getInstance().addRootAnnotation(newNeuron, vertexLoc);
                            }
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                        return newNeuron;
                    })).exceptionally((t) -> {
                        hadError(t);
                        return null;
                    });
                }
                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable t) {
                    FrameworkAccess.handleException("Could not create neuron", t);
                }
            };
            creator.execute();
        }
    }
}
