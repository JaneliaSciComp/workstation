package org.janelia.it.workstation.gui.browser.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
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

    /**
     * Returns true if the current user owns the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean isOwner(DomainObject domainObject) {
        return DomainUtils.isOwner(domainObject, AccessManager.getSubjectKey());
    } 
    
    /**
     * Returns true if the current user has read access to the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean hasReadAccess(DomainObject domainObject) {
        return DomainUtils.hasReadAccess(domainObject, AccessManager.getSubjectKey());
    }
    
    /**
     * Returns true if the current user has write access to the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean hasWriteAccess(DomainObject domainObject) {
        return DomainUtils.hasWriteAccess(domainObject, AccessManager.getSubjectKey());
    }

    public static Object getFieldValue(DomainObject o1, String fieldName) throws InvocationTargetException, IllegalAccessException {
        try {
            return ReflectionUtils.get(o1, fieldName);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static List<DomainObjectAttribute> getUniqueAttributes(Collection<DomainObject> domainObjects) {
        Set<Class<? extends DomainObject>> domainClasses = new HashSet<>();
        for(DomainObject domainObject : domainObjects) {
            domainClasses.add(domainObject.getClass());
        }
        return getUniqueAttributes(domainClasses.toArray(new Class[domainClasses.size()]));
    }

    public static List<DomainObjectAttribute> getUniqueAttributes(Class<? extends DomainObject>... domainClasses) {

        Set<DomainObjectAttribute> attrSet = new HashSet<>();

        for(Class<? extends DomainObject> domainClass : domainClasses) {
            for (DomainObjectAttribute attr : DomainUtils.getSearchAttributes(domainClass)) {
                if (attr.isDisplay()) {
                    attrSet.add(attr);
                }
            }
        }

        List<DomainObjectAttribute> attrs = new ArrayList<>(attrSet);
        Collections.sort(attrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });

        return attrs;
    }
}
