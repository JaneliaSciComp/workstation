package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.action.EditAction;
import org.janelia.workstation.controller.eventbus.SelectionAnnotationEvent;
import org.janelia.workstation.controller.eventbus.SelectionNeuronsEvent;
import org.janelia.workstation.controller.model.TmHistoricalEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.controller.model.annotations.neuron.NeuronModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

@ActionID(
        category = "Horta",
        id = "UndoNeuronChange"
)
@ActionRegistration(
        displayName = "Undo Neuron Change",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "C-Z")
})
public class UndoNeuronChangeAction extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(SplitNeuronAtVertexAction.class);

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
            List<TmHistoricalEvent> eventList;
            @Override
            protected void doStuff() throws Exception {
                eventList = TmModelManager.getInstance().getNeuronHistory().undoAction();
                if (eventList==null || eventList.size()==0)
                    return;

                log.info("undoing {} actions", eventList.size());
                for (TmHistoricalEvent event: eventList) {
                    log.info("undoing type {} on neurons {}", event.getType(), event.getNeurons().keySet());
                    if (event.getType()== TmHistoricalEvent.EVENT_TYPE.NEURON_DELETE)
                        continue;
                    Map<Long, byte[]> neuronMap = event.getNeurons();
                    ObjectMapper objectMapper = new ObjectMapper();

                    for (Long neuronId : neuronMap.keySet()) {
                        TmModelManager.getInstance().getCurrentReviews().clearLoopedAnnotations();

                        TmNeuronMetadata restoredNeuron = objectMapper.readValue(
                                neuronMap.get(neuronId), TmNeuronMetadata.class);
                        restoredNeuron.initNeuronData();
                        NeuronManager.getInstance().restoreNeuron(restoredNeuron);
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                if (eventList==null || eventList.size()==0)
                    return;
                TmHistoricalEvent event = eventList.get(eventList.size()-1) ;
                Long selectedNeuronId = event.getSelectedItem(TmSelectionState.SelectionCode.NEURON);
                if (selectedNeuronId!=null) {
                    TmNeuronMetadata neuronSelected = NeuronManager.getInstance().getNeuronFromNeuronID(selectedNeuronId);
                    if (neuronSelected==null)
                        return;
                    TmModelManager.getInstance().getCurrentSelections().setCurrentNeuron(neuronSelected);
                    Long selectedVertexId = event.getSelectedItem(TmSelectionState.SelectionCode.VERTEX);
                    if (selectedVertexId!=null) {
                        TmGeoAnnotation vertexSelected = NeuronManager.getInstance().getGeoAnnotationFromID(neuronSelected,
                                selectedVertexId);
                        if (vertexSelected==null)
                            return;

                        TmModelManager.getInstance().getCurrentSelections().setCurrentVertex(vertexSelected);
                        SelectionAnnotationEvent selAnnoEvent = new SelectionAnnotationEvent(this,
                                Arrays.asList(new TmGeoAnnotation[]{vertexSelected}),
                                true, false);
                        ViewerEventBus.postEvent(selAnnoEvent);
                    }
                }
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
