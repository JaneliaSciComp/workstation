package org.janelia.workstation.browser.api.services;

import java.util.Arrays;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.search.Filtering;
import org.janelia.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.workstation.common.nodes.FilterNode;
import org.janelia.workstation.common.gui.editor.ParentNodeSelectionEditor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for Filters.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHandler.class, position = 999)
public class FilterObjectHandler implements DomainObjectHandler {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        return Filtering.class.isAssignableFrom(clazz);
    }

    @Override
    public Node getNode(DomainObject domainObject, ChildFactory parentChildFactory) throws Exception {
        return new FilterNode(parentChildFactory, (Filtering)domainObject);
    }

    @Override
    public Class<? extends ParentNodeSelectionEditor<? extends DomainObject,?,?>> getEditorClass(DomainObject domainObject) {
        return FilterEditorPanel.class;
    }
    
    @Override   
    public String getLargeIcon(DomainObject domainObject) {
        return "search_large.png";
    }

    @Override
    public boolean supportsRemoval(DomainObject domainObject) {
        return true;
    }
    
    @Override
    public void remove(DomainObject domainObject) throws Exception {
        DomainModel model = DomainMgr.getDomainMgr().getModel();
        model.remove(Arrays.asList(((Filtering)domainObject)));
    }
}
