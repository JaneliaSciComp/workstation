package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.filestore.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import java.io.File;

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
        return (SystemInfo.isMac || SystemInfo.isLinux || SystemInfo.isWindows);
	}
	
	public OpenInFinderAction(Entity entity) {
		this.entity = entity;
	}
	
	@Override
	public String getName() {
		if (SystemInfo.isMac) {
			return "Reveal In Finder";
		}
		else if (SystemInfo.isLinux) {
			return "Reveal In File Manager";
		}
        else if (SystemInfo.isWindows) {
            return "Reveal In Windows Explorer";
        }
		return null;
	}

	@Override
	public void doAction() {
		try {
			String filePath = EntityUtils.getAnyFilePath(entity);
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
            else if (!file.exists()) {
                throw new Exception("Cannot access "+file.getAbsolutePath());
            }
			
            String[] cmdArr=null;
			if (SystemInfo.isMac) {
                if (file.isFile()) {
                    cmdArr=new String[3];
                    cmdArr[0]="/usr/bin/open";
                    cmdArr[1]="-R";
                    cmdArr[2]=file.getAbsolutePath();
                } else {
                    cmdArr=new String[2];
                    cmdArr[0]="/usr/bin/open";
                    cmdArr[1]=file.getAbsolutePath();
                }
			}
			else if (SystemInfo.isLinux) {
                cmdArr=new String[2];
                cmdArr[0]="gnome-open";
				if (file.isFile()) {
                    cmdArr[1]=file.getParentFile().getAbsolutePath();
				}
				else {
                    cmdArr[1]=file.getAbsolutePath();
				}
			}
            else if (SystemInfo.isWindows) {
                cmdArr=new String[2];
                cmdArr[0]="explorer";
                if (file.isFile()) {
                    cmdArr[1]="/select,"+file.getAbsolutePath();
                }
                else {
                    cmdArr[1]="/e,"+file.getAbsolutePath();
                }
            }
            int returnCode = Runtime.getRuntime().exec(cmdArr).waitFor();
			if ((returnCode != 0 && !SystemInfo.isWindows) ||
                 (returnCode>1 && SystemInfo.isWindows)) {
				throw new Exception("Error opening file: "+file.getAbsolutePath());
			}
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}

}
