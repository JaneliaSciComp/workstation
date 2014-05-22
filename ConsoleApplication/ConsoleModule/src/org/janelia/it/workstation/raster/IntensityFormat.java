package org.janelia.it.workstation.raster;

import java.nio.ByteOrder;

public interface IntensityFormat {
    int getNumChannels();
    int getNumBytesPerChannel();
    ByteOrder getEndian();	
}
