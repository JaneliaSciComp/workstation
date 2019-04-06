package org.janelia.it.workstation.browser.gui.model;

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

    public T getImageByUniqueId(S id);
    
    public S getImageUniqueId(T imageObject);
    
    public String getImageFilepath(T imageObject);
    
    public String getImageTitle(T imageObject);

    public String getImageSubtitle(T imageObject);

    public BufferedImage getStaticIcon(T imageObject);
    
    public List<Annotation> getAnnotations(T imageObject);
    
    public List<ImageDecorator> getDecorators(T imageObject);

}
