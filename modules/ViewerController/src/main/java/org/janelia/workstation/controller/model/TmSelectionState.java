package org.janelia.workstation.controller.model;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.AnnotationCategory;
import org.janelia.workstation.controller.eventbus.SelectionEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 // support the capability to select multiple neurons and multiple annotation points at the same time
 */
public class TmSelectionState {
    private Map<String, List<DomainObject>> selections = new HashMap<>();
    private AnnotationCategory selectionMode;
    public enum SelectionCode {
        PRIMARY, SECONDARY
    };

    public TmSelectionState() {
        selections = new HashMap<>();
        selections.put(SelectionCode.PRIMARY.name(),new CopyOnWriteArrayList());
    }

    public List<DomainObject> getSelectionGroup(String selectionKey) {
        return selections.get(selectionKey);
    }

    public void setSelectionGroup(String selectionKey, List<DomainObject> selectionGroup) {
        selections.put(selectionKey, selectionGroup);
    }

    public List<DomainObject> getPrimarySelections() {
        return selections.get(SelectionCode.PRIMARY.name());
    }

    public DomainObject getCurrentSelection() {
        List<DomainObject> currentSelections = selections.get(SelectionCode.PRIMARY.name());
        if (currentSelections.size()==1)
            return currentSelections.get(0);
        else
            return null;
    }

    public void setCurrentSelection(DomainObject selection) {
        List currentSelections = selections.get(SelectionCode.PRIMARY.name());
        currentSelections.clear();
        currentSelections.add(selection);
    }

    public void clearAllSelections() {
        selections.clear();
    }
}
