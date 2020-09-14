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
    boolean undoMode = false;
    boolean transaction = false;
    boolean recordHistory = true;

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

    public void clearHistory() {
        this.historyOperations.clear();
    }

    public List<TmHistoricalEvent> undoAction() {
        List<TmHistoricalEvent> actions = new ArrayList<>();
        if (undoMode) {
            if (undoStep>0)
                undoStep--;
        } else {
            undoMode = true;
            undoStep = historyOperations.size()-2;
            if (undoStep<0)
                undoStep = 0;
        }

        int range = undoStep;
        while (historyOperations.get(undoStep).isMultiAction() && undoStep>0) {
            undoStep--;
        }
        if (range!=undoStep) {
            int startRange = undoStep;
            if (startRange>0)
                startRange++;
            actions.addAll(historyOperations.subList(startRange, range+1));
        } else {
            actions.add(historyOperations.get(undoStep));
        }

        return actions;
    }

    public List<TmHistoricalEvent> redoAction() {
        if (!undoMode || undoStep==historyOperations.size()-1) {
            return null;
        } else {
            undoStep++;
        }

        while (historyOperations.get(undoStep).isMultiAction() && undoStep<historyOperations.size()-1) {
            undoStep++;
        }

        List<TmHistoricalEvent> actions = new ArrayList<>();
        int range = undoStep;
        if (range!=undoStep) {
            actions.addAll(historyOperations.subList(undoStep+1, range+1));
        } else {
            actions.add(historyOperations.get(undoStep));
        }

        return actions;
    }

    public void startTransaction() {
        transaction = true;
    }

    public void endTransaction() {
        transaction = false;
    }

    public void setRecordHistory(Boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    public void addHistoricalEvent (TmHistoricalEvent event) {
        if (!recordHistory)
            return;

        // if part of a transaction mark for future undo
        if (transaction)
            event.setMultiAction(true);

        // check if we haven't changed this neuron yet; so we can make an initial backup
       /* boolean createInit = true;
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
        }*/

        if (undoMode)
            historyOperations.clear();

        historyOperations.add(event);
        undoMode = false;
        undoStep = 0;

        if (historyOperations.size()>10)
            historyOperations.remove(0);
    }
}
