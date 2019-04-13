
package org.janelia.scenewindow;

import java.util.Collection;
import org.janelia.geometry3d.CompositeObject3d;
import org.janelia.geometry3d.Vantage;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public interface Scene extends CompositeObject3d {
    Collection<? extends Light> getLights();
    Collection<? extends Vantage> getCameras();
    Scene add(Light light);
    Scene add(Vantage camera);
    // int getIndex(); // GUID
    Vantage getVantage();
}
