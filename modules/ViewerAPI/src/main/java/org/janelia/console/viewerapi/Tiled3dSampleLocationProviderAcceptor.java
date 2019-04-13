
package org.janelia.console.viewerapi;

import java.net.URL;
import java.util.List;

/**
 * Implement this to become a provider of focusable location.
 * 
 * @author fosterl
 */
public interface Tiled3dSampleLocationProviderAcceptor {
    public static final String LOOKUP_PATH = "Tiled3dSample/Location/Nodes";
    public enum ParticipantType {
        acceptor, provider, both
    }
    
    ParticipantType getParticipantType();
    
    /** Only acceptor or both type should be called here. */
    void setSampleLocation(SampleLocation location);
    /** workaround to play a whole set of sample locations */    
    void playSampleLocations(List<SampleLocation> locationList, boolean autoRotation, int speed, int stepScale);
    
    /** Provider, or both-type: @return where-at, in case reload required. */
    SampleLocation getSampleLocation();
    /** @return Unique across all impls. Answers: is this one me? */
    String getProviderUniqueName();
    /** @return Shown to user.  Menu item? */
    String getProviderDescription();
}
