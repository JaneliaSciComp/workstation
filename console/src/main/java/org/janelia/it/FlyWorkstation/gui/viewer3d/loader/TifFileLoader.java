package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import loci.formats.in.TiffReader;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Handles TIFF via Loci reading capability.
 */
public class TifFileLoader extends LociFileLoader {

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        this.unCachedFileName = fileName;
        super.loadLociReader( new TiffReader() );
    }

}
