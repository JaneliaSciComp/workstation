
package org.janelia.horta;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.netbeans.api.progress.ProgressHandle;

/**
 * Implement this to dispose of YML input streams and load a result.
 *
 * @author fosterl
 */
public interface YamlStreamLoader {
    StaticVolumeBrickSource loadYaml(InputStream sourceYamlStream, NeuronTraceLoader loader, ProgressHandle progressHandle) throws IOException, ParseException;
}
