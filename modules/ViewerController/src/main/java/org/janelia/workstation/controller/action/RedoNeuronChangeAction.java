package org.janelia.workstation.controller.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

@ActionID(
        category = "Horta",
        id = "RedoNeuronChange"
)
@ActionRegistration(
        displayName = "Redo Neuron Change",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "C-Y")
})
public class RedoNeuronChangeAction extends AbstractAction {

    public RedoNeuronChangeAction() {
        super("Redo Neuron Change");
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
                List<TmHistoricalEvent> eventList = TmModelManager.getInstance().getNeuronHistory().redoAction();
                if (eventList==null || eventList.size()==0)
                    return;
                for (TmHistoricalEvent event: eventList) {
                    Map<Long, byte[]> neuronMap = event.getNeurons();
                    ObjectMapper objectMapper = new ObjectMapper();

                    for (Long neuronId : neuronMap.keySet()) {
                        TmNeuronMetadata restoredNeuron = objectMapper.readValue(
                                neuronMap.get(neuronId), TmNeuronMetadata.class);
                        restoredNeuron.initNeuronData();
                        NeuronManager.getInstance().restoreNeuron(restoredNeuron);
                    }
                }
            }

            @Override
            protected void hadSuccess() {

            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(new Exception( "Could not revert neuron change",
                        error));
            }
        };
        restorer.execute();
    }

}
