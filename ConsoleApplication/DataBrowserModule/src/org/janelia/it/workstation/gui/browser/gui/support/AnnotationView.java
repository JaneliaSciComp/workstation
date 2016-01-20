package org.janelia.it.workstation.gui.browser.gui.support;

import java.util.List;

import org.janelia.it.jacs.model.domain.ontology.Annotation;

/**
 * A common interface for all panels which display a list of annotations.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotationView {

    public List<Annotation> getAnnotations();

    public void setAnnotations(List<Annotation> annotations);

    public void removeAnnotation(Annotation annotation);

    public void addAnnotation(Annotation annotation);
}
