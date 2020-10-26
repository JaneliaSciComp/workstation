package org.janelia.workstation.controller.model;

import org.janelia.model.domain.tiledMicroscope.TmReviewTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TmReviewState {
    Set<Long> reviewedAnnotations = new HashSet<Long>();
    private TmReviewTask currentTask;

    public void addReviewedAnnotation(Long annId) {
        reviewedAnnotations.add(annId);
    }

    public void addReviewedAnnotationList(Collection<Long> annList) {
        reviewedAnnotations.addAll(annList);
    }

    public boolean isReviewedAnnotation(Long annId) {
        return (reviewedAnnotations.contains(annId));
    }

    public void removeReviewedAnnotation(Long annId) {
        reviewedAnnotations.remove(annId);
    }

    public void removeReviewedAnnotationList(Collection<Long> annList) {
        reviewedAnnotations.removeAll(annList);
    }

    public TmReviewTask getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(TmReviewTask currentTask) {
        this.currentTask = currentTask;
    }
}
