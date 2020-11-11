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
    private Map<String, Object> primarySelections;
    private Map<String, Object> secondarySelections;
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
        primarySelections = new HashMap<>();
        secondarySelections = new HashMap<>();
    }

    public Object getNeuronSelections() {
        return primarySelections.get(SelectionCode.NEURON.name());
    }

    public TmNeuronMetadata getCurrentNeuron() {
        return (TmNeuronMetadata)primarySelections.get(SelectionCode.NEURON.name());
    }

    public TmGeoAnnotation getCurrentVertex() {
        return (TmGeoAnnotation)primarySelections.get(SelectionCode.VERTEX.name());
    }

    public void setCurrentNeuron(Object selection) {
        primarySelections.put(SelectionCode.NEURON.name(), selection);
    }

    public void setCurrentVertex(Object selection) {
        primarySelections.put(SelectionCode.VERTEX.name(), selection);
    }

    public void clearNeuronSelection() {
        primarySelections.clear();
    }

    public void clearVertexSelection() {
        primarySelections.remove(SelectionCode.VERTEX.name());
    }

    public TmNeuronMetadata getSecondaryNeuron() {
        return (TmNeuronMetadata)secondarySelections.get(SelectionCode.NEURON.name());
    }

    public TmGeoAnnotation getSecondaryVertex() {
        return (TmGeoAnnotation)secondarySelections.get(SelectionCode.VERTEX.name());
    }

    public void setSecondarySelections(Object selection) {
        secondarySelections.put(SelectionCode.NEURON.name(), selection);
    }

    public void setSecondaryVertex(Object selection) {
        secondarySelections.put(SelectionCode.VERTEX.name(), selection);
    }

    public void clearSecondarySelection() {
        secondarySelections.clear();
    }

    public void clearSecondaryVertexSelection() {
        secondarySelections.remove(SelectionCode.VERTEX.name());
    }

    public void clearAllSelections() {
        primarySelections.clear();
        secondarySelections.clear();
    }
}
