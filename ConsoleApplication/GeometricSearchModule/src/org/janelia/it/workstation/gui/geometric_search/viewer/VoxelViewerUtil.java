package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by murphys on 7/31/2015.
 */
public class VoxelViewerUtil {

    private static final Logger logger = LoggerFactory.getLogger(VoxelViewerUtil.class);

//    public static void doSystemCmd(String commandString) {
//        Properties scProps=new Properties();
//        scProps.setProperty(SystemCall.SHELL_PATH_PROP, "cmd /c");
//        SystemCall sc = new SystemCall(scProps, new File("/tmp/scratch"), null);
//        sc.setDeleteExecTmpFile(false);
//        try {
//            sc.execute(commandString, false);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }

    public static VoxelViewer4DImage createVoxelImageFromStack(File stack) throws Exception {
        FileResolver resolver = new TrivialFileResolver();
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        volumeLoader.loadVolume(stack.getAbsolutePath());
        VoxelViewer4DImage image=new VoxelViewer4DImage();
        volumeLoader.populateVolumeAcceptor(image);
        return image;
    }
    
}
