package org.janelia.it.workstation.browser.api.services;

import java.util.Arrays;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.browser.gui.editor.ParentNodeSelectionEditor;
import org.janelia.it.workstation.browser.nodes.FilterNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.search.Filtering;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;

/**
 * A helper for Filters.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = DomainObjectHelper.class, path = DomainObjectHelper.DOMAIN_OBJECT_LOOKUP_PATH)
public class FilterObjectHelper implements DomainObjectHelper {

    @Override
    public boolean isCompatible(DomainObject domainObject) {
        return isCompatible(domainObject.getClass());
    }
    
    @Override
    public boolean isCompatible(Class<? extends DomainObject> clazz) {
        if (Filtering.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
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
