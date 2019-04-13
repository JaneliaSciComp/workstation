package org.janelia.horta;

import java.net.URL;

import org.janelia.console.viewerapi.BasicSampleLocation;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for horta location providers.  All are very similar.
 *
 * @author fosterl
 */
public abstract class HortaLocationProviderBase implements Tiled3dSampleLocationProviderAcceptor {
    private final Logger logger = LoggerFactory.getLogger(HortaLocationProviderBase.class);
    
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

    protected NeuronTracerTopComponent getNeuronTracer() {
        return NeuronTracerTopComponent.findThisComponent();
    }

    @Override
    public ParticipantType getParticipantType() {
        return ParticipantType.both;
    }

}
