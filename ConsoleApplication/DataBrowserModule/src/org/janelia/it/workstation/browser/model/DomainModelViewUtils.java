package org.janelia.it.workstation.browser.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for extracting information from the domain model for view purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainModelViewUtils {

    private final static Logger log = LoggerFactory.getLogger(DomainModelViewUtils.class);
    
    private final static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd hh:mma");

    public static String getDateString(Date date) {
        return dateFormatter.format(date).toLowerCase();
    }

    // TODO: move all this domain-specific logic to a confocal module

    /**
     * Returns a list of the types which the given domain object can be mapped to using the map functions.
     * @param domainObject
     * @return
     */
    public static Set<Class<? extends DomainObject>> getMappableTypes(DomainObject domainObject) {

        Set<Class<? extends DomainObject>> types = new HashSet<>();
        
        if (domainObject instanceof Sample) {
            types.add(LSMImage.class);
        }
        else if (domainObject instanceof LSMImage) {
            types.add(Sample.class);
        }

        else if (domainObject instanceof NeuronFragment) {
            types.add(Sample.class);
            types.add(LSMImage.class);
        }

        return types;
    }
    
    /**
     * Map the given objects to the given class, and return a list of joined objects.
     * @param domainObjects
     * @param targetClass
     * @return
     * @throws Exception
     */
    public static <T extends DomainObject> List<T> map(Collection<DomainObject> domainObjects, Class<T> targetClass) throws Exception {

        List<T> mapped = new ArrayList<>();
        for (DomainObject domainObject : domainObjects) {
            for(T result : map(domainObject, targetClass)) {
                if (result != null) {
                    mapped.add(result);
                }
            }
        }
        
        return mapped;
    }
    
    /**
     * Map the given object to the given class, and return a list of joined objects.
     * @param domainObject
     * @param targetClass
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends DomainObject> List<T> map(DomainObject domainObject, Class<T> targetClass) throws Exception {

        List<T> mapped = new ArrayList<>();
        
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            
            if (targetClass.equals(LSMImage.class)) {
                List<LSMImage> lsms = DomainMgr.getDomainMgr().getModel().getDomainObjectsAs(LSMImage.class, sample.getLsmReferences());
                mapped.addAll((Collection<? extends T>) lsms);
            }
            else if (targetClass.equals(Sample.class)) {
                mapped.add((T)sample);
            }
            else {
                log.warn("Cannot map Samples to "+targetClass.getSimpleName());
            }
            
        }
        else if (domainObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage)domainObject;
            
            if (targetClass.equals(Sample.class)) {
                Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(lsm.getSample());
                mapped.add((T) sample);
            }
            else if (targetClass.equals(LSMImage.class)) {
                mapped.add((T)lsm);
            }
            else {
                log.warn("Cannot map LSMImage to "+targetClass.getSimpleName());
            }
            
        }
        else if (domainObject instanceof NeuronFragment) {
            NeuronFragment fragment = (NeuronFragment)domainObject;
            Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());

            if (targetClass.equals(Sample.class)) {
                mapped.add((T) sample);
            }
            else if (targetClass.equals(LSMImage.class)) {
                mapped.addAll(map(sample, targetClass));
            }
            else if (targetClass.equals(NeuronFragment.class)) {
                mapped.add((T)fragment);
            }
            else {
                log.warn("Cannot map NeuronFragment to "+targetClass.getSimpleName());
            }
            
        }
        
        return mapped;
    }
}
