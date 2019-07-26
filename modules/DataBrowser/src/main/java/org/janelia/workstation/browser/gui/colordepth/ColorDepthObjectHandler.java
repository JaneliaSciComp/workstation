package org.janelia.workstation.browser.gui.colordepth;

import java.util.Arrays;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for working with color depth nodes.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class)
public class ColorDepthObjectHandler implements DomainObjectHandler {

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
        if (ColorDepthSearch.class.isAssignableFrom(domainObject.getClass())) {
            return new ColorDepthSearchNode(parentChildFactory, (ColorDepthSearch)domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        if (ColorDepthSearch.class.isAssignableFrom(domainObject.getClass())) {
            return ColorDepthSearchEditorPanel.class;
        }
        return null;
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
