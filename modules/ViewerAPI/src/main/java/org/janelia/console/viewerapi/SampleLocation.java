package org.janelia.console.viewerapi;

import java.net.URL;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 *
 * @author Christopher Bruns
 */
public interface SampleLocation
{
    URL getSampleUrl();
    void setSampleUrl(URL url);

    double getFocusXUm();
    double getFocusYUm();
    double getFocusZUm();    
    void setFocusUm(double x, double y, double z);
        
    float[] getRotationAsQuaternion();
    void setRotationAsQuaternion(float[] rotation);
    
    void setInterpolate(boolean interpolate);
    boolean getInterpolate();
    
    double getMicrometersPerWindowHeight();
    void setMicrometersPerWindowHeight(double zoom);

    // TODO - remove this temporary hack once Horta can show all channels
    int getDefaultColorChannel();
    void setDefaultColorChannel(int channelIndex);
    
    boolean isCompressed();
    void setCompressed(boolean compressed);
    
    /** Optional, may not be supported. */
    Long getWorkspaceId();
    /** Optional, may not be supported. */
    Long getSampleId();
    /** Optional, may not be supported. */
    TmSample getSample();
    /** Optional, may not be supported. */
    Long getNeuronId();   
    void setNeuronId(Long neuronId);
    /** Optional, may not be supported. */
    Long getNeuronVertexId();
    void setNeuronVertexId(Long neuronVertexId);
}
