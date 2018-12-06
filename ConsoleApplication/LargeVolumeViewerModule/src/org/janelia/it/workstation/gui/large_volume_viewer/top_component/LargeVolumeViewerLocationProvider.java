package org.janelia.it.workstation.gui.large_volume_viewer.top_component;

import java.util.List;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.Tiled3dSampleLocationProviderAcceptor;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.openide.util.lookup.ServiceProvider;

/**
 * Can answer queries about where the large volume viewer is pointing, and
 * on which sample.
 * 
 * @author fosterl
 */
@ServiceProvider(service = Tiled3dSampleLocationProviderAcceptor.class, path=Tiled3dSampleLocationProviderAcceptor.LOOKUP_PATH)
public class LargeVolumeViewerLocationProvider implements Tiled3dSampleLocationProviderAcceptor {

    public static final String PROVIDER_UNIQUE_NAME = "LargeVolumeViewer";
    private static final String DESCRIPTION = "Large Volume Viewer";
    
    private final LargeVolumeViewViewer viewer;
    
    public LargeVolumeViewerLocationProvider( LargeVolumeViewViewer viewer ) {
        this.viewer = viewer;
    }
    
    public LargeVolumeViewerLocationProvider() {
        // Need to find the viewer.
        LargeVolumeViewerTopComponent lvv = 
                LargeVolumeViewerTopComponent.getInstance();
        this.viewer = lvv.getLvvv();
    }

    @Override
    public String getProviderUniqueName() {
        return PROVIDER_UNIQUE_NAME;
    }

    @Override
    public String getProviderDescription() {
        return DESCRIPTION;
    }

    @Override
    public ParticipantType getParticipantType() {
        return ParticipantType.both;
    }

    @Override
    public void setSampleLocation(SampleLocation sampleLocation) {
        LargeVolumeViewerTopComponent lvv = 
                LargeVolumeViewerTopComponent.getInstance();
        if (! lvv.isOpened()) {
            lvv.open();
        }
        if (lvv.isOpened()) {
            lvv.requestActive();
            viewer.setLocation(sampleLocation);
        }
        else {
            throw new IllegalStateException("Failed to open Large Volume Viewer.");
        }
    }

    @Override
    public SampleLocation getSampleLocation()
    {
        return viewer.getSampleLocation();
    }

    @Override
    public void playSampleLocations(List<SampleLocation> locationList, boolean autoRotation, int speed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
