package org.janelia.jacs2.dao;

import org.janelia.it.jacs.model.domain.sample.Image;

public interface SampleImageDao extends DomainObjectDao<Image> {
    void updateImageFiles(Image image);
}
