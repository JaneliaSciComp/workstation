package org.janelia.workstation.browser.nodes;

import java.util.Arrays;
import java.util.Collections;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for working with Data Sets.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class, position = 202)
public class ColorDepthLibraryObjectHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        return ColorDepthLibrary.class.isAssignableFrom(clazz);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        return new ColorDepthLibraryNode(parentChildFactory, (ColorDepthLibrary) domainObject);
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        return FilterEditorPanel.class;
    }
    
    @Override   
    public String getLargeIcon(DomainObject domainObject) {
        return "folder_large.png";
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        return true;
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.remove(Collections.singletonList((ColorDepthLibrary) domainObject));
    }

}
