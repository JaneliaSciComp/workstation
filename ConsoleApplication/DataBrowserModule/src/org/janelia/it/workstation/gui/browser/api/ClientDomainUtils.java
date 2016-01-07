package org.janelia.it.workstation.gui.browser.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous utility methods for dealing with the Domain model on the client side. Generic utility methods for the domain model 
 * are found in the DomainUtils class in the model module. This class only deals with things specific to the client side.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ClientDomainUtils {

    private static final Logger log = LoggerFactory.getLogger(DomainUtils.class);

    protected static final String JANELIA_MODEL_PACKAGE = "org.janelia.it.jacs.model.domain";
        
    private static final List<Class<? extends DomainObject>> searchClasses = new ArrayList<>();
    private static final Map<String,String> searchTypeToClassName = new HashMap<>();
    
    static {
        // TODO: might want to move this stuff to DomainUtils at some point
        
        Reflections reflections = new Reflections(JANELIA_MODEL_PACKAGE);
        for(Class<?> searchClass : reflections.getTypesAnnotatedWith(SearchType.class)) {
            searchClasses.add((Class<? extends DomainObject>)searchClass);
        }
        
        Collections.sort(searchClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final String l1 = o1.getAnnotation(SearchType.class).label();
                final String l2 = o2.getAnnotation(SearchType.class).label();
                return l1.compareTo(l2);
            }
        });  
        
        for(Class<?> searchClazz : searchClasses) {
            String searchTypeKey = searchClazz.getAnnotation(SearchType.class).key();
            // TODO: do we need to find the base class?
//            Class<?> clazz = searchClazz;
//            while (clazz!=null) {
//                MongoMapped mongoMapped = clazz.getAnnotation(MongoMapped.class);
//                if (mongoMapped!=null) {
//                    collectionName = mongoMapped.collectionName();
//                    break;
//                }
//                clazz = clazz.getSuperclass();
//            }
            searchTypeToClassName.put(searchTypeKey, searchClazz.getName());
        }
    }
    
    public static List<Class<? extends DomainObject>> getSearchClasses() {
        return searchClasses;
    }

    public static String getClassNameForSearchType(String type) {
        return searchTypeToClassName.get(type);
    }

    /**
     * Returns true if the current user owns the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean isOwner(DomainObject domainObject) {
        return DomainUtils.isOwner(domainObject, SessionMgr.getSubjectKey());
    } 
    
    /**
     * Returns true if the current user has read access to the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean hasReadAccess(DomainObject domainObject) {
        return DomainUtils.hasReadAccess(domainObject, SessionMgr.getSubjectKey());
    }
    
    /**
     * Returns true if the current user has write access to the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean hasWriteAccess(DomainObject domainObject) {
        return DomainUtils.hasWriteAccess(domainObject, SessionMgr.getSubjectKey());
    }

    /**
     * Generate a list of DomainObjectAttributes for the given domain object class. DomainObjectAttributes are
     * generated for all fields and methods marked with a @SearchAttribute annotation. 
     * @param clazz a class which extends DomainObject
     * @return a list of DomainObjectAttributes
     */
    public static List<DomainObjectAttribute> getSearchAttributes(Class<? extends DomainObject> clazz) {
    	List<DomainObjectAttribute> attrs = new ArrayList<>();
        for (Field field : ReflectionUtils.getAllFields(clazz)) {
            SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                try {
                    Method getter = ReflectionHelper.getGetter(clazz, field.getName());
                    DomainObjectAttribute attr = new DomainObjectAttribute(field.getName(), searchAttributeAnnot.label(), searchAttributeAnnot.key(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), true, getter);
                    attrs.add(attr);
                }
                catch (Exception e) {
                    log.warn("Error getting field " + field.getName() + " on " + clazz.getName(), e);
                }
            }
        }

        for (Method method : clazz.getMethods()) {
            SearchAttribute searchAttributeAnnot = method.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
            	String name = method.getName();
            	if (method.getName().startsWith("get")) {
                    name = name.substring(3, 4).toLowerCase() + name.substring(4);
            	}
                DomainObjectAttribute attr = new DomainObjectAttribute(name, searchAttributeAnnot.label(), searchAttributeAnnot.key(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), true, method);
                attrs.add(attr);
            }
        }

        return attrs;
    }
}
