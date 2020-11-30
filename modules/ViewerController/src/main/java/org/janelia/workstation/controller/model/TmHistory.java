package org.janelia.workstation.controller.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.integration.util.FrameworkAccess;

import java.util.*;
import java.util.List;

/**
 * stores history information for doing undo-redos
 */
public class TmHistory {
    List<TmHistoricalEvent> historyOperations = new ArrayList<>();
    int undoStep = 0;
    boolean undoMode = false;
    boolean transaction = false;
    boolean recordHistory = true;
    Set<Long> neuronsLogged = new HashSet<>();

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
        neuronsLogged.clear();
        undoMode = false;

    }

    public List<TmHistoricalEvent> undoAction() {
        if (historyOperations.size()==0)
            return null;
        List<TmHistoricalEvent> actions = new ArrayList<>();
        if (undoMode) {
            if (undoStep==0)
                return null;
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
            actions.addAll(historyOperations.subList(undoStep, range+1));
        } else {
            actions.add(historyOperations.get(undoStep));
        }

        return actions;
    }

    public List<TmHistoricalEvent> redoAction() {
        if (historyOperations.size()==0)
            return null;
        if (!undoMode || undoStep==historyOperations.size()-1) {
            return null;
        } else {
            undoStep++;
        }

        int range = undoStep;
        while (historyOperations.get(undoStep).isMultiAction() && undoStep<historyOperations.size()-1) {
            undoStep++;
        }
        List<TmHistoricalEvent> actions = new ArrayList<>();
        if (range!=undoStep) {
            actions.addAll(historyOperations.subList(range, undoStep+1));
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

    public void checkBackup(TmNeuronMetadata neuron) {
        if (undoMode) {
            historyOperations.clear();
            neuronsLogged.clear();
            undoMode = false;
        }
        if (!neuronsLogged.contains(neuron.getId())) {
            try {
                // add historical event
                ObjectMapper mapper = new ObjectMapper();
                byte[] neuronData = mapper.writeValueAsBytes(neuron);
                TmHistoricalEvent event = new TmHistoricalEvent();
                Map<Long, byte[]> map = new HashMap<>();
                map.put(neuron.getId(), neuronData);
                event.setNeurons(map);
                event.setTimestamp(new Date());
                addHistoricalEvent(event);
                neuronsLogged.add(neuron.getId());
            } catch (JsonProcessingException e) {
                FrameworkAccess.handleException(e);
            }
        }
    }

    public void addHistoricalEvent (TmHistoricalEvent event) {
        if (!recordHistory)
            return;

        // if part of a transaction mark for future undo
        if (transaction)
            event.setMultiAction(true);

        if (undoMode) {
            historyOperations.clear();
            neuronsLogged.clear();
            undoMode = false;
        }

        historyOperations.add(event);
        undoStep = 0;

        if (historyOperations.size()>10)
            historyOperations.remove(0);
    }
}
