package org.janelia.horta;

import java.net.URL;
import java.util.List;
import org.janelia.console.viewerapi.BasicSampleLocation;
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
        position=110)
public class HortaLocationFocusProvider implements Tiled3dSampleLocationProviderAcceptor {
    public static final String UNIQUE_NAME = "Horta";
    public static final String DESCRIPTION = "Horta - Focus On Location";
    
    private final Logger logger = LoggerFactory.getLogger(HortaLocationFocusProvider.class);
    
    @Override
    public SampleLocation getSampleLocation() {
        NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc == null) {
            logger.info("No neuron tracer component found.");
            return null;
        }
        BasicSampleLocation result = new BasicSampleLocation();
        URL url = null;
        try {
            url = nttc.getCurrentSourceURL();
        } catch (Exception ex) {
            logger.error("Error getting current source URL", ex);
        }
        result.setSampleUrl(url);
        double[] focus = nttc.getStageLocation();
        result.setFocusUm(focus[0], focus[1], focus[2]);
        return result;
    }

    @Override
    public String getProviderUniqueName() {
        return UNIQUE_NAME;
    }

    @Override
    public String getProviderDescription() {
        return DESCRIPTION;
    }
    
    protected NeuronTracerTopComponent getNeuronTracer() {
        return NeuronTracerTopComponent.findThisComponent();
    }

    @Override
    public ParticipantType getParticipantType() {
        return ParticipantType.both;
    }

    @Override
    public void setSampleLocation(SampleLocation sampleLocation) {
        NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc == null) {
            // not an actual error; on some platforms, the Neuron Tracer (aka Horta) may
            //  not be available
            logger.warn("Failed to find Neuron Tracer.");
            return;
        }
        if (! nttc.isOpened()) {
           return;
        }
        if (nttc.isOpened()) {
            nttc.requestActive();
            try {
                sampleLocation.setDefaultColorChannel(0);
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
       NeuronTracerTopComponent nttc = getNeuronTracer();
        if (nttc == null) {
            throw new IllegalStateException("Failed to find Neuron Tracer.");
        }
        if (! nttc.isOpened()) {
           return;
        }
        if (nttc.isOpened()) {
            nttc.requestActive();
            try {                
                nttc.playSampleLocations(locationList, autoRotation, speed, stepScale);
            } catch (Exception ex) {
                throw new RuntimeException("Error playing sample locations", ex);
            }
        }
        else {
            throw new IllegalStateException("Failed to open Neuron Tracer.");
        }
    }
}
