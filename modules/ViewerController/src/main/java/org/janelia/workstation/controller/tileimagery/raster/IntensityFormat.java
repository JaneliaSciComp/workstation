package org.janelia.workstation.controller.tileimagery.raster;

import java.nio.ByteOrder;

public interface IntensityFormat {
    int getNumChannels();
    int getNumBytesPerChannel();
    ByteOrder getEndian();	
}
