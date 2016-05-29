package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import java.awt.image.BufferedImage;
import java.util.List;

import org.janelia.it.jacs.model.domain.ontology.Annotation;

public interface ImageModel<T,S> {

    public T getImageByUniqueId(S id);
    
    public S getImageUniqueId(T imageObject);
    
    public String getImageFilepath(T imageObject);
    
    public String getImageTitle(T imageObject);

    public String getImageSubtitle(T imageObject);

    public BufferedImage getStaticIcon(T imageObject);
    
    public List<Annotation> getAnnotations(T imageObject);

}
