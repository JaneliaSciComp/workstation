package org.janelia.it.workstation.gui.framework.viewer;

import java.util.List;

import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A common interface for all panels which display a list of annotations.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotationView {

    public List<OntologyAnnotation> getAnnotations();

    public void setAnnotations(List<OntologyAnnotation> annotations);

    public void removeAnnotation(OntologyAnnotation annotation);

    public void addAnnotation(OntologyAnnotation annotation);
}
