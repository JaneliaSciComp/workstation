package org.janelia.it.FlyWorkstation.gui.viewer3d.resolver;

import org.janelia.it.jacs.shared.loader.file_resolver.FileResolver;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/18/13
 * Time: 10:26 AM
 *
 * Trivial implementation fo the resolver, to pass back the given filename.  This is appropriate for
 * some testing scenarios, as it avoids undue dependencies on other resolution methods.
 */
public class TrivialFileResolver implements FileResolver {
    @Override
    public String getResolvedFilename(String fileName) {
        return fileName;
    }
}
