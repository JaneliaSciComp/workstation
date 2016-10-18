package org.janelia.it.workstation.gui.framework.outline;

import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A simple filter for annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AnnotationFilter {

	public boolean accept(OntologyAnnotation annotation);
}
