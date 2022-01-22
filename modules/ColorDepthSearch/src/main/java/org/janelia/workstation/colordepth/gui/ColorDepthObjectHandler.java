package org.janelia.workstation.colordepth;

import java.util.Collections;
import java.util.stream.Collectors;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.model.domain.gui.cdmip.ColorDepthMask;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
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
@ServiceProvider(service = DomainObjectHandler.class, position = 202)
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
        else if (ColorDepthLibrary.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        if (domainObject instanceof ColorDepthMask) {
            return null;
        }
        else if (domainObject instanceof ColorDepthSearch) {
            return new ColorDepthSearchNode(parentChildFactory, (ColorDepthSearch)domainObject);
        }
        else if (domainObject instanceof ColorDepthLibrary) {
            return new ColorDepthLibraryNode(parentChildFactory, (ColorDepthLibrary) domainObject);
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }    
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof ColorDepthSearch) {
            return ColorDepthSearchEditorPanel.class;
        }
        else if (domainObject instanceof ColorDepthLibrary) {
            return FilterEditorPanel.class;
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
        else if (domainObject instanceof ColorDepthLibrary) {
            return "folder_large.png";
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        if (domainObject instanceof ColorDepthMask) {
            return true;
        } else if (domainObject instanceof ColorDepthSearch) {
            return true;
        } else if (domainObject instanceof ColorDepthLibrary) {
            return true;
        }
        return false;
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        if (domainObject instanceof ColorDepthMask) {
            ColorDepthMask cdm = (ColorDepthMask) domainObject;
            model.remove(Collections.singletonList(cdm));
            model.removeStorage(cdm.getFiles().entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList()));
        } else if (domainObject instanceof ColorDepthSearch) {
            model.remove(Collections.singletonList(((ColorDepthSearch) domainObject)));
        } else if (domainObject instanceof ColorDepthLibrary) {
            model.remove(Collections.singletonList((ColorDepthLibrary) domainObject));
        } else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }

    @Override
    public int getMaxReferencesBeforeRemoval(DomainObject domainObject) {
        if (domainObject instanceof ColorDepthMask) {
            return 1;
        }
        else if (domainObject instanceof ColorDepthSearch) {
            return 0;
        }
        else if (domainObject instanceof ColorDepthLibrary) {
            return 0;
        }
        else {
            throw new IllegalArgumentException("Domain class not supported: "+domainObject);
        }
    }
}
