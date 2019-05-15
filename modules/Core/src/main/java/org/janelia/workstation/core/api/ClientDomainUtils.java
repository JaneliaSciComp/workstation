package org.janelia.workstation.core.api;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasName;
import org.janelia.workstation.core.model.ImageModel;


/**
 * Miscellaneous utility methods for dealing with the Domain model on the client side. Generic utility methods for the domain model 
 * are found in the DomainUtils class in the model module. This class only deals with things specific to the client side.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ClientDomainUtils {

    /**
     * Returns true if the current user owns the given domain object.
     * @param domainObject
     * @return
     */
    public static boolean isOwner(DomainObject domainObject) {
        if (!AccessManager.loggedIn()) return false;
        if (domainObject==null) return false;
        return DomainUtils.isOwner(domainObject, AccessManager.getSubjectKey());
    } 
    
    /**
     * Returns true if the current user has read access to the given domain object.
     * Always returns true if the current user is an admin.
     * @param domainObject can they read this?
     * @return T=Yes; F=No
     */
    public static boolean hasReadAccess(DomainObject domainObject) {
        if (domainObject==null) return false;
        if (AccessManager.getAccessManager().isAdmin()) return true;
        return DomainUtils.hasReadAccess(domainObject, AccessManager.getReaderSet());
    }
    
    /**
     * Returns true if the current user has write access to the given domain object.
     * Always returns true if the current user is an admin.
     * @param domainObject can they write this?
     * @return T=Yes; F=No
     */
    public static boolean hasWriteAccess(DomainObject domainObject) {
        if (domainObject==null) return false;
        if (AccessManager.getAccessManager().isAdmin()) return true;
        return DomainUtils.hasWriteAccess(domainObject, AccessManager.getWriterSet());
    }
    
    /**
     * Given a list of named things and a potential name, choose a name which is not already used, by adding #<number>
     * suffix.
     * @param objects named objects
     * @param prefix name to start with
     * @param numberFirst If this is false and the prefix is not yet used, it is simply returned as-is.
     * @return
     */
    public static String getNextNumberedName(List<? extends HasName> objects, String prefix, boolean numberFirst) {

        long max = 0;
        Pattern p = Pattern.compile("^"+prefix+" #(?<number>\\d+)$");        
        for(HasName object : objects) {
            Matcher m = p.matcher(object.getName());
            if (m.matches()) {
                long number = Long.parseLong(m.group("number"));
                max = Math.max(number, max);
            }
        }
        
        if (!numberFirst && max==0) {
            return prefix;
        }
        
        return prefix+" #"+(max+1);
    }

    public static <T,S> Collection<T> getObjectsFromModel(Collection<S> ids, ImageModel<T,S> imageModel) {
        return ids.stream().map(imageModel::getImageByUniqueId).collect(Collectors.toList());
    }
}
