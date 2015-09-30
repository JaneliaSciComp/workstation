/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import javax.swing.JOptionPane;
import static org.janelia.it.workstation.cache.large_volume.EHCacheFacade.MAX_3D_CACHE_SIZE;
import org.janelia.it.workstation.gui.dialogs.MemoryCheckDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a partial implementation of the CacheFacadeI, to support common
 * operations across all such facades.
 *
 * @author fosterl
 */
public abstract class AbstractCacheFacade implements CacheFacadeI {

    public static final String CACHE_NAME = "CompressedTiffCache";
    public static final int MAX_3D_CACHE_SIZE = 500;
    public static final int MIN_3D_CACHE_SIZE = 200;
    private static final Logger log = LoggerFactory.getLogger(AbstractCacheFacade.class);
    
    public static boolean usingEhCache() {
        return getNeighborhoodSize() > 0;
    }
    
    public static int getNeighborhoodSize() {
        int rtnVal = 0;
        Integer cache3Dsize = (Integer) SessionMgr.getSessionMgr().getModelProperty(CACHE_NAME);
        if (cache3Dsize != null) {
            rtnVal = cache3Dsize;
        }
        return rtnVal;
    }
    
    public static int checkNeigborhoodSize(int settingInt ) {
        if (settingInt > MAX_3D_CACHE_SIZE) {
            String message = String.format("The 3D cache size setting %d is too large.  Running with %d.", settingInt, MAX_3D_CACHE_SIZE);
            log.warn(message);
            JOptionPane.showMessageDialog(WindowLocator.getMainFrame(), message);
            settingInt = MAX_3D_CACHE_SIZE;
        }
        if (settingInt > 0) {
            // Must warn about memory use.
            MemoryCheckDialog memoryChecker = new MemoryCheckDialog();
            int minMem = 30;
            if (SystemInfo.isLinux) {
                minMem = 24;
            }
            if (! memoryChecker.unusedIfInsufficientMemory("3D Cache", minMem, WindowLocator.getMainFrame())) {
                settingInt = 0;
            }                    
        }
        return settingInt;
    }
    
}
