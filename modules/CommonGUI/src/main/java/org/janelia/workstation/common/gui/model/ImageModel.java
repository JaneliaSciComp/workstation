package org.janelia.workstation.common.gui.model;

import java.awt.image.BufferedImage;
import java.util.List;

import org.janelia.model.domain.ontology.Annotation;

/**
 * Provides a standardized API for accessing information about a set of image objects of type T, 
 * with ids of type S. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ImageModel<T,S> {

    T getImageByUniqueId(S id);
    
    S getImageUniqueId(T imageObject);
    
    String getImageFilepath(T imageObject);
    
    String getImageTitle(T imageObject);

    String getImageSubtitle(T imageObject);

    BufferedImage getStaticIcon(T imageObject);
    
    List<Annotation> getAnnotations(T imageObject);
    
    List<ImageDecorator> getDecorators(T imageObject);

}
