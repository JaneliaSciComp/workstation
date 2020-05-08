package org.janelia.workstation.controller.model;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.AnnotationCategory;
import org.janelia.workstation.controller.eventbus.SelectionEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 // support the capability to select multiple neurons and multiple annotation points at the same time
 */
public class TmSelectionState {
    private Map<String, Object> selections;
    private AnnotationCategory selectionMode;
    public enum SelectionCode {
        NEURON, VERTEX
    };
    long foo = 0;
    private static TmSelectionState instance;

    public static TmSelectionState getInstance() {
        if (instance == null)
            instance = new TmSelectionState();
        return instance;
    }

    private TmSelectionState() {
        selections = new HashMap<>();
    }

    public Object getNeuronSelections() {
        return selections.get(SelectionCode.NEURON.name());
    }

    public TmNeuronMetadata getCurrentNeuron() {
        return (TmNeuronMetadata)selections.get(SelectionCode.NEURON.name());
    }

    public TmGeoAnnotation getCurrentVertex() {
        return (TmGeoAnnotation)selections.get(SelectionCode.VERTEX.name());
    }

    public void setCurrentNeuron(Object selection) {
        selections.put(SelectionCode.NEURON.name(), selection);
    }

    public void setCurrentVertex(Object selection) {
        selections.put(SelectionCode.VERTEX.name(), selection);
    }

    public void clearAllSelections() {
        selections.clear();
    }
}
