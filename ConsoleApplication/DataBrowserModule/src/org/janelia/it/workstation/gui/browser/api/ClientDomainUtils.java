package org.janelia.it.workstation.gui.browser.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.janelia.it.workstation.gui.browser.model.DomainObjectAttribute;
import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.reflections.ReflectionUtils;
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
    
    public static boolean hasWriteAccess(DomainObject domainObject) {
        return DomainUtils.hasWriteAccess(domainObject, SessionMgr.getSubjectKey());
    }
    
    public static boolean isOwner(DomainObject domainObject) {
        return DomainUtils.isOwner(domainObject, SessionMgr.getSubjectKey());
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
    
    public static List<DomainObjectId> getDomainObjectIdList(Collection<DomainObject> objects) {
        List<DomainObjectId> list = new ArrayList<>();
        for(DomainObject domainObject : objects) {
            if (domainObject!=null) {
                list.add(DomainObjectId.createFor(domainObject));
            }
        }
        return list;
    }

    public static Map<DomainObjectId, DomainObject> getMapByDomainObjectId(Collection<DomainObject> objects) {
        Map<DomainObjectId, DomainObject> objectMap = new HashMap<>();
        for (DomainObject domainObject : objects) {
            if (domainObject != null) {
                objectMap.put(DomainObjectId.createFor(domainObject), domainObject);
            }
        }
        return objectMap;
    }
    
    public static DomainObjectId getIdForReference(Reference ref) {
        Class<? extends DomainObject> clazz = DomainUtils.getObjectClass(ref.getCollectionName());
        if (clazz==null) {
            // TODO: reenable this warning once we clean up the database
            //log.warn("Cannot generate DomainObjectId for unrecognized target type: "+ref.getTargetType());
            return null;
        }
        return new DomainObjectId(clazz.getName(), ref.getTargetId());
    }

    public static Reference getReferenceForId(DomainObjectId id) {
        Reference ref = new Reference();
        Class<? extends DomainObject> objectClass = DomainUtils.getObjectClassByName(id.getClassName());
        if (objectClass==null) {
            throw new IllegalArgumentException("No such object class: "+id.getClassName());
        }
        ref.setCollectionName(DomainUtils.getCollectionName(objectClass));
        ref.setTargetId(id.getId());
        return ref;
    }
}
