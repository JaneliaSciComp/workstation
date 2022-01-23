package org.janelia.workstation.common.gui.model;

import org.janelia.model.domain.enums.FileType;
import org.janelia.workstation.core.model.descriptors.ArtifactDescriptor;

public interface SampleResultModel {

    ArtifactDescriptor getArtifactDescriptor();

    FileType getFileType();
}
