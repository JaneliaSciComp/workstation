package org.janelia.it.workstation.gui.viewer3d.loader;

import loci.formats.in.ZeissLSMReader;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Uses ZEISS funcitionality for Loci file loading.
 */
public class LsmFileLoader extends LociFileLoader {
    @Override
    public void loadVolumeFile( String volumeFileName ) throws Exception {
        this.unCachedFileName = volumeFileName;
        super.loadLociReader( new ZeissLSMReader() );
    }
}
