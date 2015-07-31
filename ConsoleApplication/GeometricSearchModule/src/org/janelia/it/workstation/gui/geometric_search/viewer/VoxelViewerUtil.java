package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.jacs.shared.utils.SystemCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Created by murphys on 7/31/2015.
 */
public class VoxelViewerUtil {

    private static final Logger logger = LoggerFactory.getLogger(VoxelViewerTransferHandler.class);

    public static File findFile(String filePath) {
        File localFile = new File(filePath);
        if (!localFile.exists()) {
            try {
                String fileCmd="scp murphys@tem-dvid.int.janelia.org:"+filePath+" /cygdrive/c/tmp";
                doSystemCmd(fileCmd);
                logger.info("Check1");
                localFile = new File("C:\\tmp\\"+localFile.getName());
                logger.info("Check2");
            } catch (Exception ex) {
                logger.info("Check3");
                ex.printStackTrace();
            }
            logger.info("Check4");
        }
        if (localFile.exists()) {
            logger.info("Check5");
            return localFile;
        } else {
            logger.info("Could not find new file="+localFile.getAbsolutePath());
            return null;
        }
    }

    public static void doSystemCmd(String commandString) {
        Properties scProps=new Properties();
        scProps.setProperty(SystemCall.SHELL_PATH_PROP, "cmd /c");
        SystemCall sc = new SystemCall(scProps, new File("/tmp/scratch"), null);
        sc.setDeleteExecTmpFile(false);
        try {
            sc.execute(commandString, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
