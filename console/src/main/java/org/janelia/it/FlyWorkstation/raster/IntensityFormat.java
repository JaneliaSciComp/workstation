package org.janelia.it.FlyWorkstation.raster;

import java.nio.ByteOrder;

public interface IntensityFormat {
    int getNumChannels();
    int getNumBytesPerChannel();
    ByteOrder getEndian();	
}
