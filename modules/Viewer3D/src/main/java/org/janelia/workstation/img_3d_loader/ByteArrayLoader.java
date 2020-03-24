/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.workstation.img_3d_loader;

import org.janelia.workstation.img_3d_loader.AbstractVolumeFileLoader;

/**
 * Do not load any file into memory.  Trivial realization of the abstract loader.
 * @author fosterl
 */
public class ByteArrayLoader extends AbstractVolumeFileLoader {

    @Override
    public void loadVolumeFile( String fileName ) throws Exception {
        setUnCachedFileName(fileName);
    }

}
