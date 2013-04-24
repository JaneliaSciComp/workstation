package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.awt.Desktop;
import java.io.File;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.FileCallable;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Given an entity with a File Path, reveal the path in Finder.
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

			Utils.cacheAndProcessFileAsync(filePath, new FileCallable() {
                @Override
                public void call(File param) throws Exception {
                    Desktop.getDesktop().open(getFile());
                }
            });
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}

}
