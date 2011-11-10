package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.io.File;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Given an entity with a File Path, reveal the path in Finder.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInFinderAction implements Action {

	private Entity entity;
	
	public OpenInFinderAction(Entity entity) {
		this.entity = entity;
	}
	
	@Override
	public String getName() {
		return "Open in finder";
	}

	@Override
	public void doAction() {
		try {
			String filePath = Utils.getFilePath(entity);
	
			if (Utils.isEmpty(filePath)) {
				filePath = Utils.getDefaultImageFilePath(entity);
				if (filePath == null) {
					throw new Exception("Entity has no file path");
				}
			}
			
			File file = new File(Utils.convertJacsPathLinuxToMac(filePath));
			File parent = file.getParentFile();
			
			if (file.isFile() && parent!=null && !parent.canRead()) {
				throw new Exception("Cannot access "+file.getAbsolutePath());
			}
			else if (file.isDirectory() && !file.canRead()) {
				throw new Exception("Cannot access "+file.getAbsolutePath());
			}
			
			StringBuffer cmd = new StringBuffer("/usr/bin/open ");
			if (file.isFile()) cmd.append("-R ");
			cmd.append(file.getAbsolutePath());
			
			int exitCode = Runtime.getRuntime().exec(cmd.toString()).waitFor();  
			
			if (exitCode != 0) {
				throw new Exception("Error opening file: "+file.getAbsolutePath());
			}
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}

}
