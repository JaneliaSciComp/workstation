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
        PRIMARY, SECONDARY
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

    public Object getPrimarySelections() {
        return selections.get(SelectionCode.PRIMARY.name());
    }

    public Object getCurrentSelection() {
        Object currentSelections = selections.get(SelectionCode.PRIMARY.name());
        return currentSelections;
    }

    public void setCurrentSelection(Object selection) {
        selections.put(SelectionCode.PRIMARY.name(), selection);
    }

    public void clearAllSelections() {
        selections.clear();
    }
}
