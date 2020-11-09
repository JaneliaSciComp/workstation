package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.TmHistoricalEvent;

public class NeuronHistoryEvent extends ViewerEvent {
    TmHistoricalEvent historicalEvent;
    public NeuronHistoryEvent(TmHistoricalEvent event) {
        historicalEvent = event;
    }

    public TmHistoricalEvent getHistoricalEvent() {
        return historicalEvent;
    }
}

