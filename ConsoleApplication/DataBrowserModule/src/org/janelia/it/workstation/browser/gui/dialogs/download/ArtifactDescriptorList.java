package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.util.ArrayList;

/**
 * A little hack to get around type erasure, as described in "Known Issues" here:
 * http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 */
public class ArtifactDescriptorList extends ArrayList<ArtifactDescriptor> {
    
    public ArtifactDescriptorList() {
    }
    
}