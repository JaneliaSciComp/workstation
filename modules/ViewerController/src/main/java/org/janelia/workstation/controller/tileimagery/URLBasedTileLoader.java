package org.janelia.workstation.controller.tileimagery;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.rendering.JADEBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.controller.NeuronManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class URLBasedTileLoader extends TileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(URLBasedTileLoader.class);

    private final JadeServiceClient jadeServiceClient;

    /**
     * Create the frame.
     */
    public URLBasedTileLoader(JadeServiceClient jadeServiceClient) {
        this.jadeServiceClient = jadeServiceClient;
    }

        /**
         * given a string containing the canonical Linux path to the data,
         * open the data in the viewer
         *
         * @param sample
         * @return
         */
        @Override
        public boolean loadData(TmSample sample) {
            URL url;
            try {
                String restServerURL = ConsoleProperties.getInstance().getProperty("mouselight.rest.url");
                if (!restServerURL.endsWith("/")) restServerURL = restServerURL+"/";
                String sampleVolumePathURI = String.format("mouselight/samples/%s/", sample.getId());
                URI uri = URI.create(restServerURL).resolve(sampleVolumePathURI);
                url = uri.toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
            return loadDataFromURL(url);
        }

        @Override
        RenderedVolumeLocation getRenderedVolumeLocation(TmSample tmSample) {
            return jadeServiceClient.findDataLocation(tmSample.getLargeVolumeOctreeFilepath())
                    .map(dataLocation -> new JADEBasedRenderedVolumeLocation(dataLocation))
                    .orElseThrow(() -> {
                        LOG.warn("No jade location found for {}", tmSample);
                        return new IllegalArgumentException("No location found for " + tmSample.getLargeVolumeOctreeFilepath());
                    })
                    ;
        }
}
