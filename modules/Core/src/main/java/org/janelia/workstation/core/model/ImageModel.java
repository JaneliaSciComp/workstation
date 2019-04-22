package org.janelia.workstation.core.model;

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

    /**
     * Returns the image object identified by the given id.
     * @param id
     * @return
     */
    T getImageByUniqueId(S id);

    /**
     * Returns the unique id for the given image object.
     * @param imageObject
     * @return
     */
    S getImageUniqueId(T imageObject);

    /**
     * Returns the filepath for the given image object.
     * @param imageObject
     * @return
     */
    String getImageFilepath(T imageObject);

    /**
     * Returns the title for the given image object.
     * @param imageObject
     * @return
     */
    String getImageTitle(T imageObject);

    /**
     * Returns the subtitle that should be displayed for the given image object.
     * @param imageObject
     * @return
     */
    String getImageSubtitle(T imageObject);

    /**
     * Returns the static icon that should be displayed for the given image object type. If the
     * returned image is null, then the image filepath should be loaded for display instead.
     * @param imageObject
     * @return
     */
    BufferedImage getStaticIcon(T imageObject);

    /**
     * Returns all the viewable annotations for the given image object.
     * @param imageObject
     * @return
     */
    List<Annotation> getAnnotations(T imageObject);

    /**
     * Returns all of the decorators on the given image object.
     * @param imageObject
     * @return
     */
    List<Decorator> getDecorators(T imageObject);

}
