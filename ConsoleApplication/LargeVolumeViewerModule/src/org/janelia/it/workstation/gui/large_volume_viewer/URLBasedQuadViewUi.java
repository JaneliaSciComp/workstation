package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/** 
 * Main window for QuadView application.
 * Maintained using Google WindowBuilder design tool.
 * 
 * @author Christopher M. Bruns
 *
 */
@SuppressWarnings("serial")
public class URLBasedQuadViewUi extends QuadViewUi {
    private static final Logger LOG = LoggerFactory.getLogger(URLBasedQuadViewUi.class);

	/**
	 * Create the frame.
	 */
	URLBasedQuadViewUi(JFrame parentFrame, DomainObject initialObject, boolean overrideFrameMenuBar, AnnotationModel annotationModel) {
        super(parentFrame, initialObject, overrideFrameMenuBar, annotationModel);
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
            String sampleVolumePathURI = String.format("mouselight/samples/%s/", sample.getId());
            url = new URL(restServerURL + sampleVolumePathURI);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        initializeSnapshot3dLauncher(url);
        return loadDataFromURL(url);
    }

}
