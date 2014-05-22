package org.janelia.it.workstation.gui.framework.actions;

import java.io.File;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Given an entity with a File Path, reveal the path in Finder.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInFinderAction implements Action {
    
	private Entity entity;
	
	/**
	 * @return true if this operation is supported on the current system.
	 */
	public static boolean isSupported() {
        return (org.janelia.it.workstation.shared.util.SystemInfo.isMac || org.janelia.it.workstation.shared.util.SystemInfo.isLinux || org.janelia.it.workstation.shared.util.SystemInfo.isWindows);
	}
	
	public OpenInFinderAction(Entity entity) {
		this.entity = entity;
	}
	
	@Override
	public String getName() {
		if (org.janelia.it.workstation.shared.util.SystemInfo.isMac) {
			return "Reveal In Finder";
		}
		else if (org.janelia.it.workstation.shared.util.SystemInfo.isLinux) {
			return "Reveal In File Manager";
		}
        else if (org.janelia.it.workstation.shared.util.SystemInfo.isWindows) {
            return "Reveal In Windows Explorer";
        }
		return null;
	}

	@Override
	public void doAction() {
		try {
			final String filePath = EntityUtils.getAnyFilePath(entity);
			if (filePath == null) {
				throw new Exception("Entity has no file path");
			}
			
            org.janelia.it.workstation.shared.util.Utils.processStandardFilepath(filePath, new org.janelia.it.workstation.shared.util.FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file == null) {
                        JOptionPane.showMessageDialog(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        if (!org.janelia.it.workstation.gui.util.DesktopApi.browse(file)) {
                            // NO-FRAME SessionMgr.getSessionMgr().getActiveBrowser(),
                            JOptionPane.showMessageDialog(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame(),
                                    "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
		}
		catch (Exception e) {
			org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(e);
		}
	}
}
