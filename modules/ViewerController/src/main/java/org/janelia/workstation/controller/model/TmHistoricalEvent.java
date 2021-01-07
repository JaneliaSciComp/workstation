package org.janelia.workstation.controller.model;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TmHistoricalEvent {
    Map<Long, byte[]> neurons = new HashMap<>();
    public enum EVENT_TYPE {NEURON_UPDATE, NEURON_DELETE, NEURON_CREATE, NEURON_MERGE};
    EVENT_TYPE type;
    Date timestamp;
    Map<TmSelectionState.SelectionCode,Long> selectionState = new HashMap<>();
    Boolean multiAction = false;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Map<Long, byte[]> getNeurons() {
        return neurons;
    }

    public void setNeurons(Map<Long, byte[]> neurons) {
        this.neurons = neurons;
    }

    public void addItemToSelectionState (TmSelectionState.SelectionCode key, Long itemID) {
        selectionState.put(key, itemID);
    }

    public Long getSelectedItem(TmSelectionState.SelectionCode type) {
        return selectionState.get(type);
    }

    public EVENT_TYPE getType() {
        return type;
    }

    public void setType(EVENT_TYPE type) {
        this.type = type;
    }

    public Boolean isMultiAction() {
        return multiAction;
    }

    public void setMultiAction(Boolean multiAction) {
        this.multiAction = multiAction;
    }
}
