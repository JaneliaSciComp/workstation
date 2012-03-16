package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.io.File;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;
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
	 * Returns true if this operation is supported on the current system.
	 * @return
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
			
			File file = new File(PathTranslator.convertPath(filePath));
			File parent = file.getParentFile();
			
			if (file.isFile() && parent!=null && !parent.canRead()) {
				throw new Exception("Cannot access "+file.getAbsolutePath());
			}
			else if (file.isDirectory() && !file.canRead()) {
				throw new Exception("Cannot access "+file.getAbsolutePath());
			}
			
			StringBuffer cmd = new StringBuffer();
			if (SystemInfo.isMac) {
				cmd.append("/usr/bin/open ");
				cmd.append(file.getAbsolutePath());
			}
			else if (SystemInfo.isLinux) {
				cmd.append("gnome-open ");
				cmd.append(file.getAbsolutePath());
			}
			
			if (Runtime.getRuntime().exec(cmd.toString()).waitFor() != 0) {
				throw new Exception("Error opening file: "+file.getAbsolutePath());
			}
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}

}
