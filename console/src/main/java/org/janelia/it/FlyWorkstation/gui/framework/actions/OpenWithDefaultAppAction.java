package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.io.File;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.DesktopApi;
import org.janelia.it.FlyWorkstation.shared.util.FileCallable;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Given an entity with a File Path, open the file with the default application associated with that file type
 * by the operating system.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenWithDefaultAppAction implements Action {

	private String filePath;
	
	/**
	 * @return true if this operation is supported on the current system.
	 */
	public static boolean isSupported() {
		return SystemInfo.isMac || SystemInfo.isLinux;
	}

	public OpenWithDefaultAppAction(Entity entity) {
		this.filePath = EntityUtils.getAnyFilePath(entity);
	}
	
	public OpenWithDefaultAppAction(String filePath) {
		this.filePath = filePath;
	}
	
	@Override
	public String getName() {
		return "Open file with default application";
	}

	@Override
	public void doAction() {
		try {
			if (filePath == null) {
				throw new Exception("Entity has no file path");
			}

			Utils.processStandardFilepath(filePath, new FileCallable() {
                @Override
                public void call(File file) throws Exception {
                    if (file==null) {
                        JOptionPane.showMessageDialog(SessionMgr.getBrowser(),
                                "Could not open file path", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                    	if (!DesktopApi.open(file)) {
                            JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(),
                                    "Error opening file path", "Error", JOptionPane.ERROR_MESSAGE);
                    	}
                    }
                }
            });
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}

}
