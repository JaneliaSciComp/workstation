package org.janelia.it.workstation.browser.gui.colordepth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.sample.Sample;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A helper for working with color depth nodes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHelper.class, path = DomainObjectHelper.DOMAIN_OBJECT_LOOKUP_PATH)
public class ColorDepthNodeObjectHelper implements DomainObjectHelper {

    private Logger log = LoggerFactory.getLogger(ColorDepthNodeObjectHelper.class);
    
    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        if (ColorDepthMask.class.isAssignableFrom(clazz)) {
            return true;
        }
        else if (ColorDepthSearch.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (ColorDepthMask.class.isAssignableFrom(domainObject.getClass())) {
            return new ColorDepthMaskNode(parentChildFactory, (ColorDepthMask)domainObject);
        }
        else if (ColorDepthSearch.class.isAssignableFrom(domainObject.getClass())) {
            return new ColorDepthSearchNode(parentChildFactory, (ColorDepthSearch)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }
    
    @Override
    public String getLargeIcon(DomainObject domainObject) {
        if (domainObject instanceof ColorDepthMask) {
            return "question_block_large.png";
        }
        else if (domainObject instanceof ColorDepthSearch) {
            return "search_large.png";
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        if (ColorDepthMask.class.isAssignableFrom(domainObject.getClass())) {
            return true;
        }
        else if (ColorDepthSearch.class.isAssignableFrom(domainObject.getClass())) {
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        if (ColorDepthMask.class.isAssignableFrom(domainObject.getClass())) {
            model.remove(Arrays.asList((ColorDepthMask)domainObject));
        }
        else if (ColorDepthSearch.class.isAssignableFrom(domainObject.getClass())) {
            model.remove(Arrays.asList(((ColorDepthSearch)domainObject)));
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }
}
