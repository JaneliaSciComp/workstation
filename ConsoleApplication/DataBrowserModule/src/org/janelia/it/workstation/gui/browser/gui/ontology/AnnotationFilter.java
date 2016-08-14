package org.janelia.it.workstation.gui.browser.gui.ontology;

import org.janelia.it.jacs.model.domain.ontology.Annotation;

/**
 * A simple filter for annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotationFilter {
    public boolean accept(Annotation annotation);
}
