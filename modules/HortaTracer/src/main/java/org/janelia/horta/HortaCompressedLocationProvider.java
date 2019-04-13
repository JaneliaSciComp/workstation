package org.janelia.horta;

import java.util.List;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can advertise to acceptors, where Horta is "looking".
 *
 * @author fosterl
 */
@ServiceProvider(
        service = Tiled3dSampleLocationProviderAcceptor.class, 
        path=Tiled3dSampleLocationProviderAcceptor.LOOKUP_PATH,
        position=410)
public class HortaCompressedLocationProvider extends HortaLocationProviderBase implements Tiled3dSampleLocationProviderAcceptor {
    public static final String UNIQUE_NAME = "Horta";
    public static final String DESCRIPTION = "Horta (channel 1) Compressed";
    
    private final Logger logger = LoggerFactory.getLogger(HortaCompressedLocationProvider.class);
    
    @Override
    public String getProviderUniqueName() {
        return UNIQUE_NAME;
    }

    @Override
    public String getProviderDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public SampleLocation getSampleLocation() {
        // Compression is currently supported only on Windows.
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return null;
        }
        return super.getSampleLocation();
    }

    @Override
    public void setSampleLocation(SampleLocation sampleLocation) {
        NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc == null) {
            throw new IllegalStateException("Failed to find Neuron Tracer.");
        }
        if (! nttc.isOpened()) {
            nttc.open();
        }
        if (nttc.isOpened()) {
            nttc.requestActive();
            try {
                sampleLocation.setDefaultColorChannel(0);
                sampleLocation.setCompressed(true);
                nttc.setSampleLocation(sampleLocation);
            } catch (Exception ex) {
                throw new RuntimeException("Error setting sample location", ex);
            }
        }
        else {
            throw new IllegalStateException("Failed to open Neuron Tracer.");
        }
    }

    @Override
    public void playSampleLocations(List<SampleLocation> locationList, boolean autoRotation, int speed, int stepScale) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
