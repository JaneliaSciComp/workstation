package org.janelia.it.workstation.gui.browser.gui.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Annotations about the entities which the user is currently interacting with.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Annotations {

    protected List<Annotation> annotations = new ArrayList<>();
    protected AnnotationFilter filter;

    public Annotations() {
    }

    public synchronized void clear() {
        annotations.clear();
    }

    public synchronized void init(List<Annotation> annotations) {

        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Method must run outside of the EDT");
        }

        try {
            clear();
            for (Annotation annotation : annotations) {
                annotations.add(annotation);
            }
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public void reload(Long entityId) {

//        if (SwingUtilities.isEventDispatchThread()) {
//            throw new RuntimeException("Method must run outside of the EDT");
//        }
//
//        synchronized (this) {
//            // Remove all the annotations for this entity			
//            if (annotations != null) {
//                List<Annotation> copy = new ArrayList<>(annotations);
//                for (Annotation annotation : copy) {
//                    if (annotation.getTarget() != null && annotation.getTarget().equals(entityId)) {
//                        annotations.remove(annotation);
//                    }
//                }
//            }
//            else {
//                this.annotations = new ArrayList<>();
//            }
//        }
//
//        // Reload them
//        try {
//            for (Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntity(entityId)) {
//                Annotation annotation = new Annotation();
//                if (annotation.getTarget() != null) {
//                    synchronized (this) {
//                        annotations.add(annotation);
//                    }
//                }
//            }
//        }
//        catch (Exception e) {
//            SessionMgr.getSessionMgr().handleException(e);
//        }
    }

    public synchronized void setFilter(AnnotationFilter filter) {
        this.filter = filter;
    }

    public synchronized List<Annotation> getAnnotations() {
        if (annotations == null) {
            return new ArrayList<>();
        }
        // Copy to avoid concurrent modification issues
        return new ArrayList<>(annotations);
    }

    public synchronized List<Annotation> getFilteredAnnotations() {
        List<Annotation> filtered = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (filter != null && !filter.accept(annotation)) {
                continue;
            }
            filtered.add(annotation);
        }
        return filtered;
    }

    public synchronized Map<Reference, List<Annotation>> getFilteredAnnotationMap() {
        Map<Reference, List<Annotation>> filteredMap = new HashMap<>();

        for (Annotation annotation : getFilteredAnnotations()) {
            List<Annotation> oas = filteredMap.get(annotation.getTarget());
            if (oas == null) {
                oas = new ArrayList<>();
                filteredMap.put(annotation.getTarget(), oas);
            }
            oas.add(annotation);
        }

        return filteredMap;
    }
}
