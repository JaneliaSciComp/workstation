package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.TmHistoricalEvent;

public class NeuronHistoryEvent extends ViewerEvent {
    TmHistoricalEvent historicalEvent;

    public TmHistoricalEvent getHistoricalEvent() {
        return historicalEvent;
    }

    public void setHistoricalEvent(TmHistoricalEvent historicalEvent) {
        this.historicalEvent = historicalEvent;
    }
}

