package org.janelia.it.workstation.gui.browser.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainUtils {

    private static final Logger log = LoggerFactory.getLogger(DomainUtils.class);
    
    public static String getFilepath(HasFilepath hasFilepath) {
        return hasFilepath.getFilepath();
    }
    
    /**
     * @deprecated use the version with FileType instead of this weakly-typed String version
     */
    public static String getFilepath(HasFiles hasFiles, String role) {
        return getFilepath(hasFiles, FileType.valueOf(role));
    }
    
    public static String getFilepath(HasFiles hasFiles, FileType fileType) {
        
        Map<FileType,String> files = hasFiles.getFiles();
        if (files==null) return null;
        String filepath = files.get(fileType);
        if (filepath==null) return null;
        
        StringBuilder urlSb = new StringBuilder();

        if (hasFiles instanceof HasFilepath) {
            String rootPath = ((HasFilepath)hasFiles).getFilepath();
            if (rootPath!=null) {
                urlSb.append(rootPath);
            }
        }
        
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
    
    public static boolean isOwner(DomainObject domainObject) {
        return domainObject.getOwnerKey().equals(SessionMgr.getSubjectKey());
    }

    public static boolean isVirtual(DomainObject domainObject) {
        // TODO: implement this
        return false;
    }
        
    public static List<DomainObjectAttribute> getAttributes(DomainObject domainObject) {

        List<DomainObjectAttribute> attrs = new ArrayList<>();
        Class<?> clazz = domainObject.getClass();
        
        for (Field field : ReflectionUtils.getAllFields(clazz)) {
            SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                try {
                    Method getter = ReflectionHelper.getGetter(clazz, field.getName());
                    DomainObjectAttribute attr = new DomainObjectAttribute(searchAttributeAnnot.key(), searchAttributeAnnot.label(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), getter);
                    attrs.add(attr);
                }
                catch (Exception e) {
                    log.warn("Error getting field " + field.getName() + " on object " + domainObject, e);
                }
            }
        }

        for (Method method : clazz.getMethods()) {
            SearchAttribute searchAttributeAnnot = method.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                DomainObjectAttribute attr = new DomainObjectAttribute(searchAttributeAnnot.key(), searchAttributeAnnot.label(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), method);
                attrs.add(attr);
            }
        }

        return attrs;
    }
}
