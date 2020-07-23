package org.janelia.horta;

import java.net.URL;

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
        position=120)
public class HortaLocationProviderChannel2 
    extends HortaLocationProvider
    implements Tiled3dSampleLocationProviderAcceptor 
{
    public static final String UNIQUE_NAME = "Horta";
    public static final String DESCRIPTION = "Horta (channel 2)";
    
    private final Logger logger = LoggerFactory.getLogger(HortaLocationProviderChannel2.class);
    
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
                sampleLocation.setDefaultColorChannel(1);
                //nttc.setSampleLocation(sampleLocation);
            } catch (Exception ex) {
                logger.error("Error setting channel location", ex);
            }
        }
        else {
            throw new IllegalStateException("Failed to open Neuron Tracer.");
        }
    }
}
