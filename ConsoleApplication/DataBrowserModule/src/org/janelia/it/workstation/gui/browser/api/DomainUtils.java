package org.janelia.it.workstation.gui.browser.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainUtils {

    private static final Logger log = LoggerFactory.getLogger(DomainUtils.class);
    
    public static String get2dImageFilepath(HasFiles hasFiles, String role) {
        
        StringBuilder urlSb = new StringBuilder();

        if (hasFiles instanceof HasFilepath) {
            String rootPath = ((HasFilepath)hasFiles).getFilepath();
            if (rootPath!=null) {
                urlSb.append(rootPath);
            }
        }
        
        FileType fileType = FileType.valueOf(role);
        Map<FileType,String> files = hasFiles.getFiles();
        if (files==null) return null;

        String filepath = files.get(fileType);
        if (filepath==null) {
            for(FileType subFileType : FileType.values()) {
                if (subFileType.isIs2dImage()) {
                    filepath = files.get(subFileType);
                    if (filepath!=null) break;
                }
            }
        }
        
        if (filepath==null) return null;
        
        if (urlSb.length()>0) urlSb.append("/");
        urlSb.append(filepath);
        return urlSb.length()>0 ? urlSb.toString() : null;
    }

    public static boolean hasChild(TreeNode treeNode, DomainObject domainObject) {
        for(Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext(); ) {
            Reference iref = i.next();
            if (iref.getTargetId().equals(domainObject.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection==null || collection.isEmpty();
    }
    
    public static String unCamelCase(String s) {
        return s.replaceAll("(?<=\\p{Ll})(?=\\p{Lu})|(?<=\\p{L})(?=\\p{Lu}\\p{Ll})", " ");
    }
    
    public static boolean hasWriteAccess(DomainObject domainObject) {
        return domainObject.getWriters().contains(SessionMgr.getSubjectKey());
    }
}
