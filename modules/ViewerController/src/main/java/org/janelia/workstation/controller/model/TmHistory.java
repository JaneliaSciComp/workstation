package org.janelia.workstation.controller.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSelectionSpatialFilter;
import org.janelia.workstation.integration.util.FrameworkAccess;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stores history information for doing undo-redos
 */
public class TmHistory {
    List<TmHistoricalEvent> historyOperations = new ArrayList<>();
    int undoStep = 0;
    int markUndo = 0;
    boolean undoMode = false;
    int actionstack = 0;

    public List<TmHistoricalEvent> getHistoryOperations() {
        return historyOperations;
    }

    public void setHistoryOperations(List<TmHistoricalEvent> historyOperations) {
        this.historyOperations = historyOperations;
    }

    public TmHistoricalEvent restoreAction(int step) {
        TmHistoricalEvent event = historyOperations.get(step);
        historyOperations.clear();
        undoMode = false;
        return event;
    }

    public TmHistoricalEvent undoAction() {
        if (undoMode) {
            if (undoStep>0)
                undoStep--;
        } else {
            markUndo = historyOperations.size()-1;
            undoMode = true;
            undoStep = markUndo-1;
        }
        actionstack = 0;
        return historyOperations.get(undoStep);
    }

    public TmHistoricalEvent redoAction() {
        actionstack = 0;
        if (!undoMode || undoStep==markUndo) {
            return null;
        } else {
            undoStep++;
            return historyOperations.get(undoStep);
        }
    }

    public void addHistoricalEvent (TmHistoricalEvent event) {
        // check if we haven't changed this neuron yet; so we can make an initial backup
        boolean createInit = true;
        try {
            if (historyOperations.size()>0) {
                Iterator<Long> newNeuronIter = event.getNeurons().keySet().iterator();
                Long newNeuronID = newNeuronIter.next();

                TmHistoricalEvent prevEvent = historyOperations.get(0);
                if (prevEvent.getNeurons().keySet().contains(newNeuronID)) {
                    createInit = false;
                }
            }
            if (createInit) {
                TmHistoricalEvent backupEvent = new TmHistoricalEvent();
                ObjectMapper objectMapper = new ObjectMapper();
                Iterator<Long> newNeuronIter = event.getNeurons().keySet().iterator();
                Map<Long,byte[]> backupMap = new HashMap<>();
                while (newNeuronIter.hasNext()) {
                    Long newNeuronID = newNeuronIter.next();
                    TmNeuronMetadata backupNeuron = NeuronManager.getInstance().getNeuronFromNeuronID(newNeuronID);
                    backupMap.put(backupNeuron.getId(), objectMapper.writeValueAsBytes(backupNeuron));
                }
                backupEvent.setNeurons(backupMap);
                backupEvent.setTimestamp(new Date());
                backupEvent.setType(TmHistoricalEvent.EVENT_TYPE.NEURON_UPDATE);
                historyOperations.add(backupEvent);
            }
        } catch (JsonProcessingException e) {
            FrameworkAccess.handleException(e);
        }

        // if undo mode, wait for two events before saving this information
        actionstack++;
        if (undoMode && actionstack<2)
            return;
        else {
            historyOperations.add(event);
            undoMode = false;
            markUndo = 0;
            undoStep = 0;
        }
        if (historyOperations.size()>10)
            historyOperations.remove(0);
    }
}
