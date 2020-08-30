package org.janelia.workstation.controller.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TmHistoricalEvent {
    Map<Long, byte[]> neurons = new HashMap<>();
    public enum EVENT_TYPE {NEURON_UPDATE, NEURON_DELETE, NEURON_CREATE, NEURON_MERGE};
    EVENT_TYPE type;
    Date timestamp;

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

    public EVENT_TYPE getType() {
        return type;
    }

    public void setType(EVENT_TYPE type) {
        this.type = type;
    }
}
