package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
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
        id = "UndoNeuronChange"
)
@ActionRegistration(
        displayName = "Undo a Neuron Change",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "C-Z")
})
public class UndoNeuronChangeAction extends AbstractAction {

    public UndoNeuronChangeAction() {
        super("Undo Neuron Change");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // grab the latest action from historical record
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            // dialog?
            return;
        }

        SimpleWorker restorer = new SimpleWorker() {
            TmNeuronMetadata newNeuron;
            @Override
            protected void doStuff() throws Exception {
                TmHistoricalEvent event = TmModelManager.getInstance().getNeuronHistory().undoAction();
                if (event==null)
                    return;
                Map<Long,byte[]> neuronMap = event.getNeurons();
                ObjectMapper objectMapper = new ObjectMapper();

                for (Long neuronId: neuronMap.keySet()) {
                    TmNeuronMetadata restoredNeuron = objectMapper.readValue(
                            neuronMap.get(neuronId), TmNeuronMetadata.class);
                    NeuronManager.getInstance().restoreNeuron(restoredNeuron);
                }
            }

            @Override
            protected void hadSuccess() {

            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(new Exception( "Could not restore neuron",
                        error));
            }
        };
        restorer.execute();
    }

}
