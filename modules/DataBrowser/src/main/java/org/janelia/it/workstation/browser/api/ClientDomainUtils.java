package org.janelia.it.workstation.browser.api;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasName;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;


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
    
    /**
     * Return a list of image decorators for the given domain object.
     * @param imageObject
     * @return
     */
    public static List<ImageDecorator> getDecorators(DomainObject imageObject) {
        List<ImageDecorator> decorators = new ArrayList<>();
        if (imageObject instanceof Sample) {
            Sample sample = (Sample)imageObject;
            if (sample.isSamplePurged()) {
                decorators.add(ImageDecorator.PURGED);
            }
            if (!sample.isSampleSageSynced()) {
                decorators.add(ImageDecorator.DESYNC);
            }   
        }
        else if (imageObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage)imageObject;
            if (!lsm.isLSMSageSynced()) {
                decorators.add(ImageDecorator.DESYNC);
            }   
        }
        
        return decorators;
    }
    
    /**
     * Return a list of image decorators for the given pipeline result.
     * @param result
     * @return
     */
    public static List<ImageDecorator> getDecorators(PipelineResult result) {
        List<ImageDecorator> decorators = new ArrayList<>();
        if (result.getPurged()!=null && result.getPurged()) {
            decorators.add(ImageDecorator.PURGED);
        }
        return decorators;
    }
}
